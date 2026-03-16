# AI 模块重构执行计划文档

## 1. 文档目的

本文档用于基于当前项目真实代码状态，输出一份可直接指导工程落地的 AI 模块重构执行计划。

本文档目标不是泛分析，而是：

- 明确当前 AI 模块最核心的结构问题
- 给出分优先级的重构建议清单
- 标出每条建议的改动范围、价值、风险和是否建议立刻执行
- 给出下一阶段最小可落地的 3 步重构路线

本文档严格遵守以下边界：

- 不破坏 `/api/chat`
- 不破坏 `ChatResponse` 顶层字段：
  - `intent`
  - `message`
  - `products`
  - `orders`
  - `actions`
- 商品必须继续来自真实数据库
- 购物车、订单、支付必须继续走真实后端服务
- 当前阶段不拆仓库级 `ai-module`
- 当前阶段坚持最小可落地重构，不做平台化过度设计

---

## 2. 当前 AI 模块真实现状

### 2.1 当前代码结构

当前 AI / chat 相关核心类包括：

- `com.mall.service.ChatService`
- `com.mall.service.chat.intent.ChatIntent`
- `com.mall.service.chat.intent.ChatMessageParser`
- `com.mall.service.chat.intent.RuleBasedChatIntentDetector`
- `com.mall.service.chat.memory.ChatMemoryService`
- `com.mall.service.ChatSessionContextService`
- `com.mall.service.chat.retrieval.ChatProductResolver`
- `com.mall.service.chat.support.ChatResponseFactory`
- `com.mall.service.chat.llm.ChatAiAssistant`
- `com.mall.service.chat.llm.ChatAiAnalysis`
- `com.mall.service.chat.llm.DeepSeekChatClient`

### 2.2 当前真实调用链路

当前 `/api/chat` 的后端调用流程为：

1. 前端请求 `/api/chat`
2. `ChatController -> ChatService`
3. `ChatService` 先调用 `ChatAiAssistant.analyze`
4. DeepSeek 当前负责：
   - intent 分析兜底
   - searchQuery 改写
   - `GENERAL_QA` 文案
   - `recommendationReason`
5. `ChatService` 根据 intent 分流到真实业务能力
6. `ChatProductResolver` 执行商品搜索或目标商品解析
7. `ChatMemoryService` / `ChatSessionContextService` 保存最近商品和最近订单上下文
8. `ChatResponseFactory` 组装 `ChatResponse`
9. 若 DeepSeek 失败，则回退到规则流

### 2.3 当前支持的 intent

- `SEARCH_PRODUCT`
- `RECOMMEND_PRODUCT`
- `ADD_TO_CART`
- `VIEW_CART`
- `BUY_NOW`
- `VIEW_ORDER`
- `PAY_GUIDE`
- `GENERAL_QA`

### 2.4 当前推荐商品能力真实判断

当前“推荐商品”已经不再是单纯搜索包装，而是：

- DeepSeek 可输出：
  - `intent=RECOMMEND_PRODUCT`
  - `searchQuery`
  - `recommendationReason`
- 后端再用 `searchQuery` 查询真实商品库
- 最终用 `recommendationReason` 作为推荐文案

但当前推荐能力仍然不是完整推荐系统，原因是：

- 没有 embedding 检索
- 没有 rerank
- 没有用户长期偏好画像
- 没有显式推荐策略层

因此，当前推荐能力的准确定位应为：

**“LLM 参与语义理解和推荐解释，但商品召回本质上仍然是基于真实数据库搜索的推荐型检索。”**

---

## 3. 当前最核心的结构问题

### 3.1 `ChatService` 仍然过厚

当前 `ChatService` 同时承担了：

- LLM 分析调用
- intent 决策
- 路由编排
- 搜索和推荐链路调度
- 购物车/订单业务调度
- memory 写回
- response 装配调度

问题：

- 搜索与推荐已经开始共享又分叉，逻辑重复会继续增长
- 后续接 query planning、推荐策略、上下文解释时，复杂度仍会回流到 `ChatService`
- 测试仍偏向“大 service 级”验证，难以细粒度保护行为

结论：

- 这是当前最需要优先处理的结构问题之一

### 3.2 intent 层缺少协调器

当前 intent 决策仍然是：

- AI 返回合法 intent 就采用
- 否则走规则 detector

问题：

- 高风险动作意图缺少专门保护
- 没有 `source`、`reason`、`fallback` 这类决策元信息
- 规则与 LLM 冲突没有独立收敛点

结论：

- 需要显式引入 `IntentCoordinator`

### 3.3 retrieval 层职责仍然混杂

`ChatProductResolver` 当前同时负责：

- query 提取后的商品搜索
- AI 改写 query 搜索
- 商品 ID 解析
- 序号商品引用解析
- 默认目标商品兜底

问题：

- search / recommend / target resolve 还没有真正分边界
- 后续 embedding、rerank、推荐 recall 策略没有自然挂点

结论：

- 需要最小拆出 `query planning / recall / target resolve`

### 3.4 memory 目前更像存取包装，而不是上下文解释层

当前 memory 能力主要是：

- remember
- ordinal -> productId

问题：

- 还不能统一表达“最近商品上下文”“最近订单上下文”“上一轮动作语义”
- 后续支持“这个商品”“刚才那个订单”“继续支付那个”等表达时，会继续散落实现

结论：

- memory 需要提升为上下文解释层

### 3.5 LLM 层缺少稳定 schema / prompt / parser 边界

当前 `ChatAiAssistant` 仍同时承担：

- prompt
- DeepSeek 调用
- JSON 解析
- 归一化
- fallback

问题：

- prompt 硬编码
- parser 仍偏脆弱
- schema 校验和字段归一化边界不够明确
- 后续再加 AI 输出字段时风险会持续放大

结论：

- 这是另一个 P0 级问题

---

## 4. 分优先级重构建议清单

以下建议均不影响冻结契约。

### 4.1 P0：必须尽快做

#### 建议 1：引入 `IntentCoordinator`

- 建议标题：拆分 intent 判定协调层
- 优先级：P0
- 要解决的问题：
  - 当前 intent 决策是简单覆盖式策略
  - 高风险动作意图缺少保护
  - rule / LLM 冲突没有落点
- 推荐改法：
  - 保留 `RuleBasedChatIntentDetector`
  - 保留 `ChatAiAssistant`
  - 新增 `IntentCoordinator`
  - 新增 `ResolvedIntentDecision`
  - 字段建议至少包括：
    - `intent`
    - `source`
    - `reason`
    - `usedFallback`
- 改动范围：
  - `service/chat/intent/*`
  - `ChatService`
- 为什么现在做：
  - 这是保护交易链路的第一层护栏
- 风险/代价：
  - 低到中等
- 是否影响冻结契约：否
- 是否建议本轮就做：是

#### 建议 2：把 `ChatAiAssistant` 拆成稳定 LLM 门面

- 建议标题：拆分 prompt / parser / validator
- 优先级：P0
- 要解决的问题：
  - 当前 LLM 边界不够稳
  - schema 演进风险高
- 推荐改法：
  - `ChatAiAssistant` 作为门面保留
  - 新增：
    - `ChatAiPromptBuilder`
    - `ChatAiResponseParser`
    - `ChatAiAnalysisValidator`
  - 对 `ChatAiAnalysis` 做统一字段归一化
  - parser 必须覆盖：
    - 非法 JSON
    - 额外前后文本
    - intent 非法值
    - 空字符串清洗
- 改动范围：
  - `service/chat/llm/*`
- 为什么现在做：
  - 这是后续继续扩展推荐字段、检索字段的基础
- 风险/代价：
  - 低
- 是否影响冻结契约：否
- 是否建议本轮就做：是

#### 建议 3：拆薄 `ChatService`

- 建议标题：引入 orchestrator 和少量场景 handler
- 优先级：P0
- 要解决的问题：
  - `ChatService` 仍然承担过多职责
- 推荐改法：
  - 保留 `ChatService` 为外部入口
  - 新增 `ChatOrchestrator` 或 `ChatFlowCoordinator`
  - 仅拆少量 handler：
    - `ProductSearchHandler`
    - `RecommendationHandler`
    - `CartActionHandler`
    - `OrderActionHandler`
- 改动范围：
  - `ChatService`
  - 新增 `service/chat/handler/*` 或 `service/chat/flow/*`
- 为什么现在做：
  - 不先拆薄，后续任何 AI 增量都会重新堆回主类
- 风险/代价：
  - 中等
- 是否影响冻结契约：否
- 是否建议本轮就做：是

### 4.2 P1：应该做，能显著提高扩展性

#### 建议 4：重构 retrieval 边界

- 建议标题：分离 `query planning / recall / target resolve`
- 优先级：P1
- 要解决的问题：
  - search / recommend / target resolve 还没有清晰边界
- 推荐改法：
  - 新增：
    - `ChatQueryPlanner`
    - `ProductQueryPlan`
    - `ProductRecallService`
    - `TargetProductResolver`
  - 当前 ranking 先只留挂点，不强行上复杂能力
- 改动范围：
  - `service/chat/retrieval/*`
- 为什么现在做：
  - 这是后续 embedding 和 rerank 的接入前置边界
- 风险/代价：
  - 中等
- 是否影响冻结契约：否
- 是否建议本轮就做：建议做最小版

#### 建议 5：把 recommendation 提升为独立内部服务层

- 建议标题：新增 `ChatRecommendationService`
- 优先级：P1
- 要解决的问题：
  - 推荐已经不是简单搜索变体
  - 推荐策略和推荐解释没有专属落点
- 推荐改法：
  - 新增 `ChatRecommendationService`
  - 负责：
    - 接收 AI 分析结果
    - 生成推荐查询计划
    - 调真实商品 recall
    - 生成推荐回复和解释
- 改动范围：
  - `service/chat/recommendation/*`
  - `ChatService` 或 orchestrator
- 为什么现在做：
  - 继续让推荐挂在搜索流程里，会污染搜索职责
- 风险/代价：
  - 中等
- 是否影响冻结契约：否
- 是否建议本轮就做：是，但只做内部服务层

#### 建议 6：把 memory 提升为上下文解释层

- 建议标题：从 `ChatMemoryService` 演进到 `ChatContextInterpreter`
- 优先级：P1
- 要解决的问题：
  - 目前上下文能力偏底层
- 推荐改法：
  - 保留 `ChatSessionContextService` 负责落库
  - 提升 memory 层能力，对外统一提供：
    - `loadContextSnapshot()`
    - `resolveReferencedProduct()`
    - `resolveRecentOrder()`
    - `rememberTurnResult()`
- 改动范围：
  - `service/chat/memory/*`
  - `ChatSessionContextService`
- 为什么现在做：
  - 这是支持更自然指代和多轮上下文的基础
- 风险/代价：
  - 中等
- 是否影响冻结契约：否
- 是否建议本轮就做：可做最小版

#### 建议 7：把 `ChatResponseFactory` 演进为场景响应装配层

- 建议标题：统一管理默认文案、AI 文案覆盖与 action 策略
- 优先级：P1
- 要解决的问题：
  - handler 拆开后，如果 response 装配不收口，复杂度只是平移
- 推荐改法：
  - 继续保留 `ChatResponseFactory`
  - 按场景明确方法：
    - `buildSearchResponse`
    - `buildRecommendationResponse`
    - `buildAddToCartResponse`
    - `buildBuyNowResponse`
    - `buildOrderViewResponse`
    - `buildPayGuideResponse`
  - 统一管理：
    - 默认文案
    - AI 文案覆盖
    - action 排序策略
- 改动范围：
  - `service/chat/support/ChatResponseFactory.java`
- 为什么现在做：
  - 这能防止 handler 拆完后响应规则分散
- 风险/代价：
  - 低
- 是否影响冻结契约：否
- 是否建议本轮就做：是

### 4.3 P2：可以后续再做

#### 建议 8：补 AI 主链路评测样例

- 建议标题：建立最小回归样例集
- 优先级：P2
- 要解决的问题：
  - 当前对 AI 行为的保护还不够体系化
- 推荐改法：
  - 增加样例驱动测试，至少覆盖：
    - 搜索
    - 推荐
    - 加购
    - 立即购买
    - 查看订单
    - 支付引导
    - 序号引用
    - 非法 JSON
    - rule / LLM 冲突
- 改动范围：
  - `src/test/java/com/mall/chat/*`
- 为什么现在做：
  - 后续持续演进会越来越依赖回归样例
- 风险/代价：
  - 低
- 是否影响冻结契约：否
- 是否建议本轮就做：可以先做一部分

#### 建议 9：补 AI 决策链路可观测性

- 建议标题：增加结构化日志和关键指标
- 优先级：P2
- 要解决的问题：
  - 无法系统观察：
    - LLM 命中率
    - fallback 比例
    - parser 失败原因
    - recommendation query 改写效果
- 推荐改法：
  - 增加结构化日志字段：
    - `intentSource`
    - `usedFallback`
    - `llmLatencyMs`
    - `rewrittenQuery`
    - `responseAssembledBy`
- 改动范围：
  - `service/chat/llm/*`
  - `service/chat/intent/*`
  - `ChatService` / orchestrator
- 为什么现在做：
  - 不是当前最急，但后续调优会需要
- 风险/代价：
  - 低
- 是否影响冻结契约：否
- 是否建议本轮就做：否

#### 建议 10：为 embedding / rerank 预留扩展位

- 建议标题：在 retrieval 层预留 recall 后排序挂点
- 优先级：P2
- 要解决的问题：
  - 后续提升搜索和推荐质量时，没有自然扩展位
- 推荐改法：
  - 在 retrieval / recommendation 层预留：
    - `RecallStrategy`
    - `RankingPolicy`
  - 当前仅允许简单规则排序占位
- 改动范围：
  - `service/chat/retrieval/*`
  - `service/chat/recommendation/*`
- 为什么现在做：
  - 当前不是阻塞项，但之后一定会用到
- 风险/代价：
  - 低到中等
- 是否影响冻结契约：否
- 是否建议本轮就做：否

---

## 5. 下一阶段最小可落地 3 步路线

### 第一步：稳住 intent 和 LLM 输出边界

本步目标：

- 引入 `IntentCoordinator`
- 清理 intent 决策逻辑
- 稳定 `ChatAiAssistant` 的 prompt / parser / validator

本步收益：

- 提升交易链路安全性
- 提升 LLM 输出可控性
- 不改外部契约

### 第二步：拆薄 `ChatService`

本步目标：

- 把 `ChatService` 退回入口职责
- 引入 orchestrator 和少量 handler
- 让 recommendation 和 search 有独立编排落点

本步收益：

- 控制复杂度扩散
- 为 retrieval 和 memory 升级留出承载点

### 第三步：补 retrieval 和 context 的最小边界

本步目标：

- 增加 `ChatQueryPlanner`
- 增加 `ProductRecallService`
- 增加 `TargetProductResolver`
- 增加 `ChatContextInterpreter`

本步边界：

- 仍然只查真实商品库
- 仍然不引入 embedding / rerank
- 但已经为后续演进铺好接口位置

---

## 6. 如果只做 3 件事，建议做什么

### 6.1 第一件：做 `IntentCoordinator`

原因：

- 这是保护交易型 intent 的最低成本护栏

### 6.2 第二件：重构 `ChatAiAssistant`

原因：

- 这是后续所有 AI 字段扩展和 prompt 演进的基础

### 6.3 第三件：拆薄 `ChatService`

原因：

- 这是控制 AI 模块后续膨胀速度的主手术位

---

## 7. 最终结论

当前项目的 AI 模块已经从“纯规则 chat”进入到了“规则 + DeepSeek + 真实业务链路编排”的阶段，已经具备继续演进的基础。

但当前最关键的问题不是“继续往里加更多 AI 能力”，而是：

**先把 AI 决策链路的结构稳定下来。**

现阶段最适合的做法不是推翻重做，也不是平台化扩张，而是：

1. 先稳住 intent 和 LLM 边界
2. 再拆薄 `ChatService`
3. 再为 retrieval / recommendation / memory 建立最小扩展层

如果按这个顺序推进，当前 AI 模块会从“能跑、能演示”进入“可持续迭代、可继续接 embedding / rerank / 推荐策略”的状态。
