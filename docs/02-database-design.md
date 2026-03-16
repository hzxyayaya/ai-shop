# AI 聊天式电商项目数据库冻结文档

## 1. 文档目的

本文档作为 `docs/02-database-design.md` 当前阶段初稿，用于冻结 AI 聊天式电商项目的数据库核心表、字段边界、索引建议与联调依赖。

本文档目标：

- 明确当前阶段必须存在的核心表
- 明确每张表的职责与字段设计建议
- 明确主键、唯一键、索引约束
- 明确前后端与 AI 联调依赖的数据边界
- 避免在当前阶段引入复杂电商模型

本文档优先级低于架构冻结文档，但高于局部实现约定。数据库主结构如需变更，应先更新本文档。

---

## 2. 当前阶段核心表清单

当前阶段核心表固定为：

- `product`
- `user_account`
- `cart_item`
- `order_info`
- `payment_record`
- `chat_session_context`

当前阶段不新增以下主交易模型：

- `sku`
- `stock`
- `order_item` 作为主实现
- `address`
- `coupon`
- `shipment`

---

## 3. 数据库总体设计原则

### 3.1 设计原则

- 数据库采用 PostgreSQL
- 商品表 `product` 为既有核心表
- 商品当前只表达 SPU，不表达 SKU
- 购物车必须绑定 `user_id`
- 订单当前采用单主表 `order_info`
- 订单商品快照通过 `items_json` 存储
- 支付记录单独建表
- AI 会话上下文单独建表

### 3.2 命名原则

- 表名采用小写下划线风格
- 字段名采用小写下划线风格
- 主键统一命名为 `id`
- 时间字段统一采用 `created_at`、`updated_at`

### 3.3 当前阶段边界

- 不引入复杂范式拆分来增加联调成本
- 不为了“未来可能扩展”而提前设计复杂结构
- 当前结构以可运行、可联调、可冻结为优先

---

## 4. 核心表设计

## 4.1 `product`

### 4.1.1 表职责

`product` 表用于存储电商商品基础信息，是首页瀑布流、商品搜索、AI 检索、购物车展示与订单快照生成的基础数据来源。

当前阶段该表只表达 SPU 商品，不支持 SKU、多规格、库存等扩展能力。

### 4.1.2 字段设计建议

| 字段名 | 类型建议 | 字段用途 |
|---|---|---|
| `id` | `bigint` | 商品主键，系统内部唯一标识 |
| `category` | `varchar(64)` | 商品分类，用于分类展示与筛选 |
| `title` | `varchar(255)` | 商品标题，用于展示、搜索、推荐文案 |
| `price` | `numeric(10,2)` | 商品价格，用于展示、加购、下单金额计算 |
| `sales` | `varchar(64)` | 商品销量展示原始值，保留采集文本语义 |
| `image_url` | `text` | 商品主图地址，用于首页卡片、弹窗、聊天商品卡片 |
| `shop_name` | `varchar(128)` | 店铺名称，用于商品展示与订单快照 |
| `embedding` | `vector(2048)` | 商品向量，用于语义召回、AI 检索与推荐 |

### 4.1.3 主键、唯一键、索引建议

- 主键：`id`
- 普通索引：`category`
- 普通索引：`shop_name`
- 视搜索策略可补充：
  - `title` 文本检索索引
  - `embedding` 向量索引

### 4.1.4 冻结说明

- `product` 表必须存在
- 字段 `id`、`category`、`title`、`price`、`sales`、`image_url`、`shop_name`、`embedding` 不允许擅自改名
- 前端商品卡片、购物车商品信息、订单快照、AI 商品返回均依赖该表字段语义

---

## 4.2 `user_account`

### 4.2.1 表职责

`user_account` 表用于存储用户注册登录信息与基础状态信息，是 JWT 鉴权、购物车绑定、订单归属、支付归属的用户主体来源。

### 4.2.2 字段设计建议

| 字段名 | 类型建议 | 字段用途 |
|---|---|---|
| `id` | `bigserial` 或 `bigint` | 用户主键 |
| `username` | `varchar(64)` | 用户名登录标识，要求唯一 |
| `email` | `varchar(128)` | 邮箱登录标识，要求唯一 |
| `password_hash` | `varchar(255)` | 密码哈希，不保存明文密码 |
| `nickname` | `varchar(64)` | 用户昵称，用于前端展示，可为空 |
| `status` | `varchar(32)` | 用户状态，如 `ACTIVE`、`DISABLED` |
| `created_at` | `timestamp with time zone` | 创建时间 |
| `updated_at` | `timestamp with time zone` | 更新时间 |

### 4.2.3 主键、唯一键、索引建议

- 主键：`id`
- 唯一键：`username`
- 唯一键：`email`
- 可选索引：`status`

### 4.2.4 冻结说明

- `user_account` 表必须存在
- `username`、`email`、`password_hash` 不允许擅自改名
- 登录、注册、JWT 载荷解析、用户归属判断依赖该表

---

## 4.3 `cart_item`

### 4.3.1 表职责

`cart_item` 表用于存储用户真实购物车数据，支持加入购物车、数量调整、勾选结算、删除商品等场景。

当前阶段购物车以 `user_id` 为唯一绑定主体，不支持游客会话购物车作为主实现。

### 4.3.2 字段设计建议

| 字段名 | 类型建议 | 字段用途 |
|---|---|---|
| `id` | `bigserial` 或 `bigint` | 购物车项主键 |
| `user_id` | `bigint` | 用户主键引用，表示购物车归属 |
| `product_id` | `bigint` | 商品主键引用，表示购物车中的商品 |
| `quantity` | `integer` | 商品购买数量，当前阶段必须大于等于 1 |
| `checked` | `boolean` | 是否勾选，用于购物车结算 |
| `created_at` | `timestamp with time zone` | 创建时间 |
| `updated_at` | `timestamp with time zone` | 更新时间 |

### 4.3.3 主键、唯一键、索引建议

- 主键：`id`
- 唯一键：`(user_id, product_id)`
- 普通索引：`user_id`
- 普通索引：`product_id`
- 组合索引：`(user_id, checked)`，用于勾选结算查询

### 4.3.4 冻结说明

- `cart_item` 表必须存在
- `user_id`、`product_id`、`quantity`、`checked` 不允许擅自改名
- 购物车页、立即购买区分逻辑、勾选结算逻辑依赖该表结构

---

## 4.4 `order_info`

### 4.4.1 表职责

`order_info` 表用于存储当前阶段订单主记录，是立即购买与购物车勾选结算的统一落点。

当前阶段订单采用简化模型，不拆分复杂订单明细表作为主实现。商品快照通过 `items_json` 保存，以确保订单展示不受后续商品信息变化影响。

### 4.4.2 字段设计建议

| 字段名 | 类型建议 | 字段用途 |
|---|---|---|
| `id` | `bigserial` 或 `bigint` | 订单主键 |
| `order_no` | `varchar(64)` | 订单号，对外展示与支付关联使用 |
| `user_id` | `bigint` | 下单用户主键引用 |
| `items_json` | `jsonb` | 订单商品快照，支持单商品与多商品订单 |
| `total_amount` | `numeric(10,2)` | 订单总金额 |
| `status` | `varchar(32)` | 订单业务状态 |
| `pay_status` | `varchar(32)` | 支付状态 |
| `created_at` | `timestamp with time zone` | 创建时间 |
| `updated_at` | `timestamp with time zone` | 更新时间 |

### 4.4.3 主键、唯一键、索引建议

- 主键：`id`
- 唯一键：`order_no`
- 普通索引：`user_id`
- 组合索引：`(user_id, created_at desc)`，用于订单列表
- 普通索引：`pay_status`
- 普通索引：`status`

### 4.4.4 冻结说明

- `order_info` 表必须存在
- `order_no`、`user_id`、`items_json`、`total_amount`、`status`、`pay_status` 不允许擅自改名
- 前端订单页、支付关联、AI 订单摘要返回依赖该表结构
- 当前阶段不将 `order_item` 作为主实现依赖

---

## 4.5 `payment_record`

### 4.5.1 表职责

`payment_record` 表用于记录支付发起、第三方流水、回调内容与支付状态，是订单支付链路的支付事实表。

### 4.5.2 字段设计建议

| 字段名 | 类型建议 | 字段用途 |
|---|---|---|
| `id` | `bigserial` 或 `bigint` | 支付记录主键 |
| `order_no` | `varchar(64)` | 对应订单号，用于与订单关联 |
| `user_id` | `bigint` | 支付归属用户 |
| `pay_amount` | `numeric(10,2)` | 支付金额 |
| `pay_status` | `varchar(32)` | 支付状态 |
| `alipay_trade_no` | `varchar(128)` | 支付宝交易号，回调成功后写入 |
| `callback_content` | `jsonb` 或 `text` | 支付回调原始内容，用于审计与问题排查 |
| `created_at` | `timestamp with time zone` | 创建时间 |
| `updated_at` | `timestamp with time zone` | 更新时间 |

### 4.5.3 主键、唯一键、索引建议

- 主键：`id`
- 普通索引：`order_no`
- 普通索引：`user_id`
- 普通索引：`alipay_trade_no`
- 组合索引：`(user_id, created_at desc)`

说明：

- 当前阶段是否对 `order_no` 设置唯一键，取决于是否允许一次订单多次支付尝试共存多条记录
- 若采用“一次订单仅保留一条当前支付记录”，则可将 `order_no` 设为唯一键
- 若采用“保留重试轨迹”，则不建议唯一，但应加索引

### 4.5.4 冻结说明

- `payment_record` 表必须存在
- `order_no`、`pay_amount`、`pay_status`、`alipay_trade_no`、`callback_content` 不允许擅自改名
- 支付回调处理、订单支付状态同步、支付结果查询依赖该表

---

## 4.6 `chat_session_context`

### 4.6.1 表职责

`chat_session_context` 表用于存储 AI 聊天过程中的轻量上下文，支持“第一个/第二个商品”等引用解析，以及最近一次商品推荐结果与最近订单上下文记录。

该表是聊天编排辅助表，不是业务主事实表。

### 4.6.2 字段设计建议

| 字段名 | 类型建议 | 字段用途 |
|---|---|---|
| `id` | `bigserial` 或 `bigint` | 会话上下文主键 |
| `user_id` | `bigint` | 当前用户主键引用 |
| `session_id` | `varchar(64)` | 会话标识，用于区分不同聊天线程 |
| `last_intent` | `varchar(64)` | 最近一次识别出的意图 |
| `last_user_message` | `text` | 最近一次用户原始输入 |
| `last_product_ids_json` | `jsonb` | 最近一次返回的商品 ID 列表 |
| `last_order_no` | `varchar(64)` | 最近一次关联订单号 |
| `updated_at` | `timestamp with time zone` | 最近更新时间 |

### 4.6.3 主键、唯一键、索引建议

- 主键：`id`
- 唯一键：`session_id`
- 普通索引：`user_id`
- 组合索引：`(user_id, updated_at desc)`

说明：

- 如果业务定义为“同一用户可同时存在多个聊天会话”，则 `session_id` 全局唯一即可
- 如果业务定义更强调用户隔离，也可采用唯一键 `(user_id, session_id)`，但当前推荐优先保持 `session_id` 单值唯一

### 4.6.4 冻结说明

- `chat_session_context` 表必须存在
- `session_id`、`last_intent`、`last_product_ids_json`、`last_order_no` 不允许擅自改名
- AI 上下文承接、“第一个/第二个”引用解析依赖该表结构

---

## 5. 主键、唯一键、索引汇总建议

## 5.1 主键建议

- `product.id`
- `user_account.id`
- `cart_item.id`
- `order_info.id`
- `payment_record.id`
- `chat_session_context.id`

## 5.2 唯一键建议

- `user_account.username`
- `user_account.email`
- `cart_item(user_id, product_id)`
- `order_info.order_no`
- `chat_session_context.session_id`

补充说明：

- `payment_record.order_no` 是否唯一，需要根据支付重试策略决定，当前文档只冻结“必须有索引”，不强制唯一

## 5.3 索引建议

- `product.category`
- `product.shop_name`
- `product.title` 文本检索索引，可选
- `product.embedding` 向量索引，可选但强烈建议
- `cart_item.user_id`
- `cart_item.product_id`
- `cart_item(user_id, checked)`
- `order_info.user_id`
- `order_info(user_id, created_at desc)`
- `order_info.pay_status`
- `order_info.status`
- `payment_record.order_no`
- `payment_record.user_id`
- `payment_record.alipay_trade_no`
- `chat_session_context.user_id`
- `chat_session_context(user_id, updated_at desc)`

---

## 6. 当前阶段冻结的数据边界

## 6.1 必须存在的表

以下表当前阶段必须存在：

- `product`
- `user_account`
- `cart_item`
- `order_info`
- `payment_record`
- `chat_session_context`

缺少任一表都会影响当前主链路：

- 缺少 `product`：无法完成商品展示与 AI 检索
- 缺少 `user_account`：无法完成登录与用户归属
- 缺少 `cart_item`：无法完成真实购物车
- 缺少 `order_info`：无法完成下单主链路
- 缺少 `payment_record`：无法完成支付留痕与回调审计
- 缺少 `chat_session_context`：无法稳定支持引用解析

## 6.2 不能被擅自改名的关键字段

以下字段为当前阶段冻结字段，不允许擅自改名：

### `product`

- `id`
- `category`
- `title`
- `price`
- `sales`
- `image_url`
- `shop_name`
- `embedding`

### `user_account`

- `id`
- `username`
- `email`
- `password_hash`

### `cart_item`

- `id`
- `user_id`
- `product_id`
- `quantity`
- `checked`

### `order_info`

- `id`
- `order_no`
- `user_id`
- `items_json`
- `total_amount`
- `status`
- `pay_status`

### `payment_record`

- `id`
- `order_no`
- `user_id`
- `pay_amount`
- `pay_status`
- `alipay_trade_no`
- `callback_content`

### `chat_session_context`

- `id`
- `user_id`
- `session_id`
- `last_intent`
- `last_user_message`
- `last_product_ids_json`
- `last_order_no`

## 6.3 前后端联调依赖的结构

以下结构属于前后端联调核心依赖：

- `product` 的商品字段集合
- `cart_item` 的 `checked` 与 `quantity`
- `order_info` 的 `order_no`、`items_json`、`total_amount`、`status`、`pay_status`
- `payment_record` 的 `order_no`、`pay_status`
- `chat_session_context` 的 `session_id`、`last_product_ids_json`、`last_order_no`

说明：

- 前端商品卡片、购物车、订单列表、支付结果页均依赖这些字段语义
- AI 模块的商品卡片生成、订单摘要生成、引用解析也依赖这些字段

---

## 7. `items_json` 推荐结构示例

### 7.1 设计目标

`items_json` 用于存储订单创建瞬间的商品快照，避免后续 `product` 表内容变更影响历史订单展示。

当前阶段该字段必须同时支持：

- 立即购买：单商品订单
- 购物车勾选结算：多商品订单

### 7.2 推荐结构

```json
[
  {
    "product_id": 10001,
    "title": "轻薄通勤双肩包",
    "category": "箱包",
    "price": 199.00,
    "quantity": 2,
    "image_url": "https://example.com/p10001.jpg",
    "shop_name": "城市轻选店"
  },
  {
    "product_id": 10002,
    "title": "便携咖啡随行杯",
    "category": "家居",
    "price": 59.00,
    "quantity": 1,
    "image_url": "https://example.com/p10002.jpg",
    "shop_name": "慢饮生活馆"
  }
]
```

### 7.3 字段说明

| 字段名 | 类型建议 | 用途 |
|---|---|---|
| `product_id` | `bigint` | 商品 ID，保留商品来源关联 |
| `title` | `string` | 下单时商品标题快照 |
| `category` | `string` | 下单时商品分类快照 |
| `price` | `number` | 下单时商品单价快照 |
| `quantity` | `integer` | 购买数量 |
| `image_url` | `string` | 下单时商品主图快照 |
| `shop_name` | `string` | 下单时店铺名快照 |

### 7.4 冻结建议

- `items_json` 顶层推荐固定为数组
- 每个元素代表一个订单商品快照
- 不在当前阶段加入 SKU、规格、库存、优惠、地址等复杂字段

---

## 8. `embedding` 字段用途说明

### 8.1 字段定位

`product.embedding` 是商品语义向量字段，类型建议为 `vector(2048)`。

### 8.2 主要用途

- 将商品标题、分类、补充描述等语义信息编码为向量
- 支持 AI 场景下的语义检索
- 支持“用户自然语言提问 -> 商品召回”的向量匹配
- 作为关键词检索之外的补充召回手段

### 8.3 使用原则

- `embedding` 只用于检索，不直接作为前端展示字段
- AI 返回商品必须基于真实数据库检索结果，不允许脱离 `product` 真值
- 向量召回应与关键词检索结合，不建议作为唯一召回来源

### 8.4 冻结说明

- `embedding` 字段必须保留
- 当前维度固定为 `2048`
- 不允许擅自改成其他维度而不更新文档与检索实现

---

## 9. 当前阶段明确不进入数据库主线的内容

以下结构不进入当前阶段主线数据库设计：

- `sku` 相关表
- 库存表
- 多规格属性表
- `order_item` 复杂拆表
- 地址表
- 优惠券表
- 发货表
- 售后表

说明：

- 这些能力不应提前进入主干设计
- 如确需扩展，必须先更新架构与数据库冻结文档

---

## 10. 文档维护规则

- 本文档是当前阶段数据库冻结文档
- 主表增删、关键字段改名、索引策略重大变化，应先更新本文档
- 本文档更新后，应同步检查：
  - `docs/01-architecture-and-contract.md`
  - `docs/03-api-spec.md`
  - `docs/04-agent-task-board.md`
  - `docs/05-integration-plan.md`

---

## 11. 版本记录

### v1.0

- 冻结当前阶段 6 张核心表
- 冻结关键字段命名与职责边界
- 给出主键、唯一键、索引建议
- 冻结 `items_json` 推荐结构
- 明确 `embedding` 字段用途与边界
