# AI 对话能力进度对照文档

## 1. 文档目的

本文档用于对照当前项目中已经落地的 AI 对话能力，与目标方案中计划实现的能力，明确：

- 当前已经完成了什么
- 当前还缺什么
- 每项能力目前处于什么完成度
- 下一阶段最值得优先推进的内容是什么

本文档聚焦当前项目实际代码与当前联调状态，不做脱离现状的泛化描述。

---

## 2. 当前目标清单

当前计划中的 AI 服务能力主要包括：

1. 基本问题的回答
2. 商品查询
3. 工具调用（Function Tool），用于加入购物车
4. 支付功能
5. 流式输出（Streaming Response）
6. 会话级记忆存储（Session-based Memory）
7. 通过后端保存用户提问上下文，实现多段对话

---

## 3. 当前代码对应的核心模块

当前后端 AI 主链路涉及以下核心类：

- `com.mall.service.ChatService`
- `com.mall.service.chat.intent.ChatIntent`
- `com.mall.service.chat.intent.ChatMessageParser`
- `com.mall.service.chat.intent.RuleBasedChatIntentDetector`
- `com.mall.service.chat.llm.ChatAiAssistant`
- `com.mall.service.chat.llm.ChatAiAnalysis`
- `com.mall.service.chat.llm.DeepSeekChatClient`
- `com.mall.service.chat.retrieval.ChatProductResolver`
- `com.mall.service.chat.memory.ChatMemoryService`
- `com.mall.service.ChatSessionContextService`
- `com.mall.service.chat.support.ChatResponseFactory`

当前前端聊天页相关模块包括：

- `frontend/src/views/chat/index.vue`
- `frontend/src/components/chat/ChatMessage.vue`
- `frontend/src/services/chat.js`

---

## 4. 当前进度对照

### 4.1 基本问题的回答

目标定义：

- 针对用户的一些基础咨询进行回复

当前状态：

- 已接入
- 当前通过 `GENERAL_QA` 处理
- DeepSeek 输出 `replyMessage`
- 后端通过 `ChatResponseFactory.generalQa(...)` 返回

当前完成度判断：

- 已完成基础版

说明：

- 当前已具备基础问答能力
- 但仍然属于单轮简化问答，没有复杂知识检索和长上下文推理

---

### 4.2 商品查询

目标定义：

- 支持用户对商品信息进行检索

当前状态：

- 已接入
- 当前通过 `SEARCH_PRODUCT` 处理
- `ChatAiAssistant` 可对用户表达做 `searchQuery` 改写
- 最终仍调用真实 `ProductService.searchProducts(...)`
- 商品结果来自真实数据库，不由 LLM 生成

当前完成度判断：

- 已完成基础版

说明：

- 当前查询能力已经满足基础聊天式商品搜索
- 但还没有 embedding 检索、rerank 或复杂推荐策略

---

### 4.3 工具调用（Function Tool）实现加入购物车

目标定义：

- 通过工具调用实现“加入购物车”的功能

当前状态：

- 功能已经实现
- 当前支持通过聊天触发加入购物车
- 实现方式是：
  - 识别出 `ADD_TO_CART`
  - `ChatService.handleAddToCart(...)`
  - 调用真实 `CartService.addToCart(...)`

当前完成度判断：

- 业务功能已实现
- 但还不是标准化的 Function Tool 架构

说明：

- 从用户视角看，“聊天加购物车”已经能用
- 但从技术架构角度看，当前仍是“intent 路由到 service”，不是 LLM 显式工具调用协议

---

### 4.4 支付功能

目标定义：

- 如果用户意图是购买商品，则调用支付接口进行处理

当前状态：

- 已接入
- 当前支持：
  - `BUY_NOW`
  - `PAY_GUIDE`
- 后端走真实订单与支付能力
- 前端支付表单提交、回调、状态查询已经联通

当前完成度判断：

- 已完成基础版

说明：

- 支付链路已经不是 mock
- 已经具备真实购买和支付引导能力

---

### 4.5 流式输出（Streaming Response）

目标定义：

- 主对话接口支持以流式形式实时输出回复内容

当前状态：

- 未实现
- 当前 `/api/chat` 返回的是一次性 `ChatResponse`
- 不是 SSE
- 不是 chunked token 流
- 前端也没有逐步渲染 token 的逻辑

当前完成度判断：

- 尚未开始或未落地

说明：

- 当前用户仍需要等待整轮结果返回
- 即时反馈体验尚未进入下一阶段

---

### 4.6 会话级记忆存储（Session-based Memory）

目标定义：

- 系统以 Session 为单位存储用户历史记忆

当前状态：

- 已有基础能力
- 后端已存在 `chat_session_context` 对应的 session 级上下文存储
- 当前保存内容主要包括：
  - 最近意图
  - 最近用户消息
  - 最近商品 ID 列表
  - 最近订单号
- 前端当前也已补上 session 侧边栏，用于展示与切换前端会话

当前完成度判断：

- 已完成基础版

说明：

- 当前已经具备 session 级上下文能力
- 但这个 memory 更偏“任务上下文”，还不是完整聊天历史系统

---

### 4.7 通过后端保存用户提问上下文，实现多段对话

目标定义：

- 后端保存用户提问上下文，支持多段、多轮对话

当前状态：

- 已做一部分
- 当前已经支持典型多轮场景：
  - 先搜商品
  - 再说“第一个 / 第二个 / 第三个”
  - 再执行加购或购买
- 当前主要依赖：
  - `ChatMemoryService`
  - `ChatSessionContextService`
  - `ChatProductResolver.resolveTargetProduct(...)`

当前完成度判断：

- 已实现基础多轮能力
- 尚未完成完整多轮历史记忆体系

说明：

- 当前更像“引用式多轮上下文”
- 还不是“完整对话历史 + 上下文总结 + 多轮 prompt 注入”的成熟形态

---

## 5. 当前整体判断

从产品角度看，当前已经具备：

- AI 基础问答
- AI 商品搜索
- AI 商品推荐
- AI 加入购物车
- AI 立即购买
- AI 查看订单
- AI 支付引导
- 基于 session 的上下文引用

从架构成熟度看，当前仍处于：

- 已有基础闭环
- 但还未进入标准化 AI 对话平台阶段

更准确地说：

- 现在系统已经具备“AI 电商对话基础闭环”
- 但还没有“流式输出 + 标准化工具调用 + 完整多轮历史记忆”

---

## 6. 目标对照总表

| 能力项 | 当前状态 | 完成度判断 | 备注 |
|---|---|---|---|
| 基本问题回答 | 已接入 | 已完成基础版 | `GENERAL_QA` 已通 |
| 商品查询 | 已接入 | 已完成基础版 | 查真实商品库 |
| 加入购物车 | 已接入 | 已完成基础版 | 但不是标准 Function Tool |
| 支付功能 | 已接入 | 已完成基础版 | 真实订单与支付链路已通 |
| 流式输出 | 未实现 | 未开始/未落地 | `/api/chat` 仍为一次性返回 |
| Session 记忆 | 已接入 | 已完成基础版 | 当前是上下文摘要型 memory |
| 前端查看会话记录 | 已接入 | 已完成基础版 | 当前前端已补侧边栏 |
| 后端多轮上下文 | 部分完成 | 基础版已通 | 支持引用式多轮，不是完整消息历史 |

---

## 7. 当前还缺的关键能力

如果要与目标方案完全对齐，当前主要还缺以下 3 项：

### 7.1 流式输出能力

当前缺口：

- 后端没有流式聊天接口
- 前端没有流式 token 渲染

价值：

- 可以显著改善等待体验
- 让 AI 输出更接近实时交互

### 7.2 标准化工具调用能力

当前缺口：

- 目前是 `intent -> service` 的内部路由
- 还不是标准化的 tool invocation 协议

价值：

- 更利于扩展“加购”“购买”“支付”“查订单”等工具
- 更利于后续统一管控工具执行边界

### 7.3 完整后端多轮历史存储能力

当前缺口：

- 当前 session context 主要保存摘要信息
- 没有完整保存用户每轮问答消息列表

价值：

- 后续可以基于完整历史做多轮 prompt 注入
- 可以支持更复杂的上下文理解
- 可以支撑服务端聊天记录查看与恢复

---

## 8. 下一阶段建议优先级

建议下一阶段按以下顺序推进：

### P0

1. 完整后端聊天历史存储
2. 统一前后端 session / message 模型

### P1

3. 流式输出
4. 工具调用结构标准化

### P2

5. 更强的上下文解释
6. 更复杂的推荐能力

---

## 9. 适合对外说明的当前进度表述

可以对当前项目状态这样表述：

> 当前项目已完成 AI 聊天式电商的基础闭环，支持基础问答、商品查询、商品推荐、加入购物车、立即购买、查看订单、支付引导，以及基于 Session 的上下文记忆与引用。  
> 当前系统已具备基础多轮对话能力，但流式输出、标准化工具调用和完整聊天历史持久化仍属于下一阶段建设内容。

---

## 10. 最终结论

当前项目并不是“还没开始 AI”，而是已经完成了第一阶段可演示闭环。

当前真正的阶段判断应该是：

- 已完成：AI 电商对话基础版
- 正在逼近：更完整的多轮对话系统
- 尚未完成：流式输出、标准化 Function Tool、完整后端历史记忆

如果接下来要继续补齐目标方案，建议最优先投入：

1. 后端完整消息历史存储
2. 流式输出
3. 工具调用标准化
