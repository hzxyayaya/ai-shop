# AI 聊天返回协议冻结文档

## 1. 文档目的

本文档用于冻结 AI 聊天式电商项目在 `/chat` 页面及相关交易链路中的 AI 返回协议，作为前端、后端与 AI 模块联调的统一依据。

本文档目标：

- 冻结 `ChatResponse` 顶层结构
- 冻结商品节点、订单摘要节点、动作节点结构
- 明确 `intent` 与 `actions` 的配合关系
- 明确前端渲染方式
- 通过完整 JSON 示例降低联调歧义

说明：

- 本文档仅描述 AI 聊天返回协议
- 不包含 Java、Vue 等具体实现代码
- 不扩展复杂 Agent 框架概念

---

## 2. ChatResponse 顶层结构定义

AI 聊天响应的 `data` 部分固定为以下结构：

```json
{
  "intent": "SEARCH_PRODUCT",
  "message": "我帮你找到了 2 个更适合通勤的商品。",
  "products": [],
  "orders": [],
  "actions": []
}
```

### 2.1 顶层字段说明

| 字段 | 类型 | 是否必填 | 说明 |
|---|---|---|---|
| `intent` | `string` | 是 | AI 当前轮识别出的主意图 |
| `message` | `string` | 是 | 面向用户展示的自然语言文本 |
| `products` | `array` | 是 | 商品卡片列表，无结果时返回空数组 |
| `orders` | `array` | 是 | 订单摘要列表，无结果时返回空数组 |
| `actions` | `array` | 是 | 前端可直接渲染的动作按钮列表，无动作时返回空数组 |

### 2.2 冻结规则

- `intent`、`message`、`products`、`orders`、`actions` 五个顶层字段必须始终存在
- 无数据时返回空数组，不返回 `null`
- 顶层结构不允许擅自新增前端强依赖字段
- 当前阶段前端消息渲染以该结构为唯一协议基础

---

## 3. Product 节点结构定义

`products` 数组中的每个元素固定为商品卡片节点。

```json
{
  "id": 10001,
  "category": "箱包",
  "title": "轻薄通勤双肩包",
  "price": 199.00,
  "sales": "2万+人付款",
  "imageUrl": "https://example.com/p10001.jpg",
  "shopName": "城市轻选店"
}
```

### 3.1 字段说明

| 字段 | 类型 | 是否必填 | 说明 |
|---|---|---|---|
| `id` | `number` | 是 | 商品 ID |
| `category` | `string` | 是 | 商品分类 |
| `title` | `string` | 是 | 商品标题 |
| `price` | `number` | 是 | 商品价格 |
| `sales` | `string` | 是 | 商品销量展示文本 |
| `imageUrl` | `string` | 是 | 商品主图地址 |
| `shopName` | `string` | 是 | 店铺名称 |

### 3.2 冻结规则

- 字段集合必须与当前商品字段冻结一致
- 字段命名统一使用驼峰
- `products` 中的商品必须来自真实数据库结果
- 不允许返回虚构商品或超出当前商品字段范围的复杂结构

---

## 4. Order 摘要节点结构定义

`orders` 数组中的每个元素固定为订单摘要节点。

```json
{
  "orderNo": "ORD202603130001",
  "totalAmount": 199.00,
  "status": "CREATED",
  "payStatus": "UNPAID",
  "createdAt": "2026-03-13T16:30:00+08:00",
  "items": [
    {
      "productId": 10001,
      "title": "轻薄通勤双肩包",
      "category": "箱包",
      "price": 199.00,
      "quantity": 1,
      "amount": 199.00,
      "imageUrl": "https://example.com/p10001.jpg",
      "shopName": "城市轻选店"
    }
  ]
}
```

### 4.1 字段说明

| 字段 | 类型 | 是否必填 | 说明 |
|---|---|---|---|
| `orderNo` | `string` | 是 | 订单号 |
| `totalAmount` | `number` | 是 | 订单总金额 |
| `status` | `string` | 是 | 订单状态 |
| `payStatus` | `string` | 是 | 支付状态 |
| `createdAt` | `string` | 是 | ISO 8601 时间字符串 |
| `items` | `array` | 是 | 订单商品快照列表 |

### 4.2 `items` 子节点结构

```json
{
  "productId": 10001,
  "title": "轻薄通勤双肩包",
  "category": "箱包",
  "price": 199.00,
  "quantity": 1,
  "amount": 199.00,
  "imageUrl": "https://example.com/p10001.jpg",
  "shopName": "城市轻选店"
}
```

### 4.3 冻结规则

- `orders` 只返回当前登录用户可见的订单
- 当前阶段 `orders` 节点表达“订单摘要”，不是完整复杂订单模型
- `items` 由 `items_json` 映射得到，对外统一使用驼峰字段

---

## 5. Action 节点结构定义

`actions` 数组中的每个元素固定为动作按钮节点。

```json
{
  "type": "ADD_TO_CART",
  "label": "加入购物车",
  "targetId": "10001"
}
```

### 5.1 字段说明

| 字段 | 类型 | 是否必填 | 说明 |
|---|---|---|---|
| `type` | `string` | 是 | 动作类型 |
| `label` | `string` | 是 | 前端按钮展示文案 |
| `targetId` | `string` | 是 | 动作目标 ID，商品 ID、订单号或空字符串 |

### 5.2 冻结规则

- `actions` 用于驱动前端按钮交互
- 一个动作节点只表达一个明确动作
- `targetId` 统一使用字符串，避免前后端类型分歧

---

## 6. 当前支持的 action type 枚举建议

当前阶段固定支持以下 `action type`：

| type | 说明 | 常见 targetId |
|---|---|---|
| `ADD_TO_CART` | 将商品加入购物车 | 商品 ID |
| `BUY_NOW` | 直接创建立即购买订单 | 商品 ID |
| `GO_CART` | 跳转购物车页 | 空字符串 |
| `PAY_NOW` | 对订单发起支付 | 订单号 |
| `VIEW_ORDER` | 跳转订单页或查看指定订单 | 订单号或空字符串 |
| `GO_HOME` | 跳转首页 | 空字符串 |

### 6.1 冻结规则

- 当前阶段只允许返回上述枚举
- 不允许 AI 模块擅自新增动作类型
- 新动作类型必须先更新协议文档，再进入前后端实现

---

## 7. intent 和 actions 的关系说明

## 7.1 设计原则

- `intent` 表示“这一轮 AI 识别出的主业务语义”
- `actions` 表示“前端当前可执行的下一步操作”
- `intent` 决定本轮响应的主要业务方向
- `actions` 决定本轮响应是否需要进一步推动用户完成交易动作

## 7.2 关系约束

### `SEARCH_PRODUCT`

- 主要输出：`message + products`
- 常见动作：`ADD_TO_CART`、`BUY_NOW`

### `RECOMMEND_PRODUCT`

- 主要输出：`message + products`
- 常见动作：`ADD_TO_CART`、`BUY_NOW`

### `ADD_TO_CART`

- 主要输出：`message`
- 常见动作：`GO_CART`

### `VIEW_CART`

- 主要输出：`message`
- 常见动作：`GO_CART`

### `BUY_NOW`

- 主要输出：`message + orders`
- 常见动作：`PAY_NOW`

### `CREATE_ORDER`

- 主要输出：`message + orders`
- 常见动作：`PAY_NOW`、`VIEW_ORDER`

### `VIEW_ORDER`

- 主要输出：`message + orders`
- 常见动作：`PAY_NOW`、`VIEW_ORDER`

### `PAY_GUIDE`

- 主要输出：`message + orders`
- 常见动作：`PAY_NOW`

### `GENERAL_QA`

- 主要输出：`message`
- 常见动作：通常为空，也可以返回 `GO_HOME`

## 7.3 实践建议

- 一个 `intent` 可以对应多个 `actions`
- `actions` 应服务于主意图，不应与 `intent` 语义冲突
- 如果 AI 只是解释说明，没有明确下一步动作，可以返回空数组

---

## 8. 前端渲染建议

## 8.1 文本消息如何展示

- 始终展示 `message` 文本
- 文本消息作为聊天气泡主内容
- 当同时存在 `products`、`orders`、`actions` 时，文本消息应位于最上方，作为本轮响应摘要
- 文本应简洁，避免大段冗长说明

## 8.2 商品卡片如何展示

- 当 `products` 非空时，在文本消息下方展示商品卡片列表
- 卡片建议展示：
  - 商品图
  - 标题
  - 分类
  - 价格
  - 销量
  - 店铺名
- 商品卡片应支持与首页商品卡片保持视觉一致或近似一致
- 当前阶段优先支持纵向卡片列表或横向可滚动列表

## 8.3 动作按钮如何展示

- 当 `actions` 非空时，在文本消息或卡片区域下方展示按钮组
- 按钮数量建议控制在 1 到 3 个
- 按钮文案使用 `label`
- 按钮点击后由前端根据 `type + targetId` 执行对应行为
- 高优先级动作建议放在前面，例如：
  - 商品推荐后优先展示 `ADD_TO_CART`、`BUY_NOW`
  - 创建订单后优先展示 `PAY_NOW`

## 8.4 订单摘要如何展示

- 当 `orders` 非空时，在文本消息下方展示订单摘要卡片
- 订单摘要建议展示：
  - 订单号
  - 总金额
  - 订单状态
  - 支付状态
  - 商品简要列表
- 对于支付引导场景，订单摘要卡片应与 `PAY_NOW` 按钮相邻展示

---

## 9. 完整 JSON 示例

## 9.1 搜索商品示例

```json
{
  "intent": "SEARCH_PRODUCT",
  "message": "我帮你找到 2 款更适合通勤的双肩包，可以先看看价格和风格。",
  "products": [
    {
      "id": 10001,
      "category": "箱包",
      "title": "轻薄通勤双肩包",
      "price": 199.00,
      "sales": "2万+人付款",
      "imageUrl": "https://example.com/p10001.jpg",
      "shopName": "城市轻选店"
    },
    {
      "id": 10003,
      "category": "箱包",
      "title": "简约学生双肩包",
      "price": 129.00,
      "sales": "8000+人付款",
      "imageUrl": "https://example.com/p10003.jpg",
      "shopName": "轻旅生活馆"
    }
  ],
  "orders": [],
  "actions": [
    {
      "type": "ADD_TO_CART",
      "label": "加入购物车",
      "targetId": "10001"
    },
    {
      "type": "BUY_NOW",
      "label": "立即购买",
      "targetId": "10001"
    }
  ]
}
```

## 9.2 加购物车示例

```json
{
  "intent": "ADD_TO_CART",
  "message": "已将该商品加入购物车，你可以继续挑选，或者先去购物车结算。",
  "products": [],
  "orders": [],
  "actions": [
    {
      "type": "GO_CART",
      "label": "查看购物车",
      "targetId": ""
    },
    {
      "type": "GO_HOME",
      "label": "继续逛逛",
      "targetId": ""
    }
  ]
}
```

## 9.3 创建订单 / 支付引导示例

```json
{
  "intent": "CREATE_ORDER",
  "message": "订单已经创建完成，你现在可以直接去支付。",
  "products": [],
  "orders": [
    {
      "orderNo": "ORD202603130010",
      "totalAmount": 129.00,
      "status": "CREATED",
      "payStatus": "UNPAID",
      "createdAt": "2026-03-13T16:30:00+08:00",
      "items": [
        {
          "productId": 10003,
          "title": "简约学生双肩包",
          "category": "箱包",
          "price": 129.00,
          "quantity": 1,
          "amount": 129.00,
          "imageUrl": "https://example.com/p10003.jpg",
          "shopName": "轻旅生活馆"
        }
      ]
    }
  ],
  "actions": [
    {
      "type": "PAY_NOW",
      "label": "立即支付",
      "targetId": "ORD202603130010"
    },
    {
      "type": "VIEW_ORDER",
      "label": "查看订单",
      "targetId": "ORD202603130010"
    }
  ]
}
```

---

## 10. 联调冻结规则

以下内容为当前阶段联调冻结项，不允许各模块擅自修改：

- `ChatResponse` 顶层 5 个字段
- `Product` 节点字段集合
- `Order` 摘要节点关键字段
- `Action` 节点结构
- `action type` 枚举集合
- `targetId` 使用字符串类型

以下情况必须先更新文档，再修改实现：

- 新增或删除顶层字段
- 新增动作类型
- 调整商品字段命名
- 调整订单摘要结构
- 改变前端渲染依赖字段

---

## 11. 版本记录

### v1.0

- 冻结 `ChatResponse` 顶层结构
- 冻结商品、订单摘要、动作节点协议
- 冻结 `intent` 与 `actions` 的协作规则
- 给出前端渲染建议与 3 组完整 JSON 示例
