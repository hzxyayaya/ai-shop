# AI 聊天式电商项目 API 契约文档

## 1. 文档目的

本文档作为 `docs/03-api-spec.md` 当前阶段初稿，用于冻结 AI 聊天式电商项目的接口协议，作为前端、后端、AI 模块联调的统一依据。

本文档目标：

- 冻结当前阶段对外 API
- 统一请求与响应结构
- 统一前端消费字段命名
- 冻结 AI 聊天返回结构
- 为前后端分离和多 Agent 并行开发提供稳定契约

说明：

- 对外接口字段统一使用驼峰命名
- 数据库存储字段允许使用下划线命名，但对外响应必须转换
- 本文档不包含后端实现代码，仅描述契约

---

## 2. 全局约定

## 2.1 Base URL

开发阶段统一前缀：

```text
/api
```

示例：

- `/api/auth/login`
- `/api/products`
- `/api/cart`
- `/api/orders`
- `/api/payments/create`
- `/api/chat`

## 2.2 鉴权方式

需要登录态的接口统一使用 JWT Bearer Token：

```http
Authorization: Bearer <token>
```

## 2.3 统一响应结构

所有接口统一返回以下结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | `number` | 业务状态码，`0` 表示成功，非 `0` 表示失败 |
| `message` | `string` | 响应消息 |
| `data` | `object` / `array` / `null` | 业务数据主体 |

## 2.4 通用错误示例

```json
{
  "code": 400,
  "message": "request parameter invalid",
  "data": null
}
```

```json
{
  "code": 401,
  "message": "unauthorized",
  "data": null
}
```

```json
{
  "code": 404,
  "message": "resource not found",
  "data": null
}
```

```json
{
  "code": 500,
  "message": "internal server error",
  "data": null
}
```

## 2.5 命名约定

- 对外字段统一使用驼峰命名
- 典型映射关系固定为：
  - `image_url` -> `imageUrl`
  - `shop_name` -> `shopName`
  - `order_no` -> `orderNo`
  - `pay_status` -> `payStatus`
  - `created_at` -> `createdAt`
  - `updated_at` -> `updatedAt`

---

## 3. 通用对象定义

## 3.1 User 对象

```json
{
  "id": 1,
  "username": "orion",
  "email": "orion@example.com",
  "nickname": "Orion"
}
```

### 关键字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `number` | 用户 ID |
| `username` | `string` | 用户名 |
| `email` | `string` | 邮箱 |
| `nickname` | `string` | 昵称，可为空字符串 |

## 3.2 Product 对象

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

### 关键字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `number` | 商品 ID |
| `category` | `string` | 商品分类 |
| `title` | `string` | 商品标题 |
| `price` | `number` | 商品价格 |
| `sales` | `string` | 商品销量展示文本 |
| `imageUrl` | `string` | 商品主图地址 |
| `shopName` | `string` | 店铺名称 |

## 3.3 CartItem 对象

```json
{
  "id": 501,
  "productId": 10001,
  "title": "轻薄通勤双肩包",
  "category": "箱包",
  "price": 199.00,
  "sales": "2万+人付款",
  "imageUrl": "https://example.com/p10001.jpg",
  "shopName": "城市轻选店",
  "quantity": 2,
  "checked": true
}
```

### 关键字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `number` | 购物车项 ID |
| `productId` | `number` | 商品 ID |
| `quantity` | `number` | 数量 |
| `checked` | `boolean` | 是否勾选 |

## 3.4 OrderItem 对象

```json
{
  "productId": 10001,
  "title": "轻薄通勤双肩包",
  "category": "箱包",
  "price": 199.00,
  "quantity": 2,
  "amount": 398.00,
  "imageUrl": "https://example.com/p10001.jpg",
  "shopName": "城市轻选店"
}
```

### 关键字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `productId` | `number` | 商品 ID |
| `price` | `number` | 下单时商品单价快照 |
| `quantity` | `number` | 购买数量 |
| `amount` | `number` | 当前商品行金额，等于 `price * quantity` |

## 3.5 Order 对象

```json
{
  "orderNo": "ORD202603130001",
  "totalAmount": 398.00,
  "status": "CREATED",
  "payStatus": "UNPAID",
  "createdAt": "2026-03-13T16:00:00+08:00",
  "items": [
    {
      "productId": 10001,
      "title": "轻薄通勤双肩包",
      "category": "箱包",
      "price": 199.00,
      "quantity": 2,
      "amount": 398.00,
      "imageUrl": "https://example.com/p10001.jpg",
      "shopName": "城市轻选店"
    }
  ]
}
```

### 关键字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `orderNo` | `string` | 订单号，对外唯一标识 |
| `totalAmount` | `number` | 订单总金额 |
| `status` | `string` | 订单状态 |
| `payStatus` | `string` | 支付状态 |
| `createdAt` | `string` | ISO 8601 时间 |
| `items` | `array` | 由 `items_json` 转换后的订单商品列表 |

## 3.6 ChatAction 对象

```json
{
  "type": "ADD_TO_CART",
  "label": "加入购物车",
  "targetId": "10001"
}
```

### 关键字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `type` | `string` | 动作类型 |
| `label` | `string` | 前端按钮文案 |
| `targetId` | `string` | 动作目标 ID，商品、订单或空字符串 |

## 3.7 ChatResponse 对象

```json
{
  "intent": "SEARCH_PRODUCT",
  "message": "我帮你找到 2 个偏轻便的箱包商品。",
  "products": [
    {
      "id": 10001,
      "category": "箱包",
      "title": "轻薄通勤双肩包",
      "price": 199.00,
      "sales": "2万+人付款",
      "imageUrl": "https://example.com/p10001.jpg",
      "shopName": "城市轻选店"
    }
  ],
  "orders": [],
  "actions": [
    {
      "type": "ADD_TO_CART",
      "label": "加入购物车",
      "targetId": "10001"
    }
  ]
}
```

### 冻结字段

- `intent`
- `message`
- `products`
- `orders`
- `actions`

---

## 4. 状态枚举约定

## 4.1 用户状态

- `ACTIVE`
- `DISABLED`

## 4.2 订单状态

- `CREATED`
- `PAID`
- `CANCELLED`

## 4.3 支付状态

- `UNPAID`
- `PAID`
- `FAILED`

## 4.4 AI Intent

- `SEARCH_PRODUCT`
- `RECOMMEND_PRODUCT`
- `ADD_TO_CART`
- `VIEW_CART`
- `BUY_NOW`
- `CREATE_ORDER`
- `VIEW_ORDER`
- `PAY_GUIDE`
- `GENERAL_QA`

## 4.5 Chat Action Type

- `ADD_TO_CART`
- `BUY_NOW`
- `GO_CART`
- `PAY_NOW`
- `VIEW_ORDER`
- `GO_HOME`

---

## 5. Auth 模块

## 5.1 注册

### URL

```http
POST /api/auth/register
```

### Method

`POST`

### 是否需要 JWT

否

### Request 示例

```json
{
  "username": "orion",
  "email": "orion@example.com",
  "password": "12345678"
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "register success",
  "data": {
    "id": 1,
    "username": "orion",
    "email": "orion@example.com",
    "nickname": "orion"
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `username` | 用户名，要求唯一 |
| `email` | 邮箱，要求唯一 |
| `password` | 明文密码，仅出现在请求中，服务端存储为哈希 |

## 5.2 登录

### URL

```http
POST /api/auth/login
```

### Method

`POST`

### 是否需要 JWT

否

### Request 示例

```json
{
  "account": "orion",
  "password": "12345678"
}
```

或：

```json
{
  "account": "orion@example.com",
  "password": "12345678"
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "login success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xxx.yyy",
    "user": {
      "id": 1,
      "username": "orion",
      "email": "orion@example.com",
      "nickname": "Orion"
    }
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `account` | 用户名或邮箱 |
| `password` | 登录密码 |
| `token` | JWT，前端后续放入 `Authorization` 请求头 |

## 5.3 获取当前用户信息

### URL

```http
GET /api/auth/me
```

### Method

`GET`

### 是否需要 JWT

是

### Request 示例

无请求体。

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "orion",
    "email": "orion@example.com",
    "nickname": "Orion"
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `id` | 当前登录用户 ID |
| `username` | 当前登录用户名 |
| `email` | 当前登录邮箱 |

---

## 6. Product 模块

## 6.1 商品列表

### URL

```http
GET /api/products
```

### Method

`GET`

### 是否需要 JWT

否

### Request 示例

```json
{
  "page": 1,
  "pageSize": 20,
  "category": "箱包",
  "sortBy": "price",
  "sortOrder": "asc"
}
```

说明：该示例表示 query 参数语义，不是实际 GET 请求体。

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "page": 1,
    "pageSize": 20,
    "total": 120,
    "list": [
      {
        "id": 10001,
        "category": "箱包",
        "title": "轻薄通勤双肩包",
        "price": 199.00,
        "sales": "2万+人付款",
        "imageUrl": "https://example.com/p10001.jpg",
        "shopName": "城市轻选店"
      }
    ]
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `page` | 页码，默认 `1` |
| `pageSize` | 每页条数，默认 `20` |
| `category` | 分类过滤，可选 |
| `sortBy` | 当前阶段支持 `price` |
| `sortOrder` | `asc` 或 `desc` |

## 6.2 商品搜索

### URL

```http
GET /api/products/search
```

### Method

`GET`

### 是否需要 JWT

否

### Request 示例

```json
{
  "keyword": "双肩包",
  "page": 1,
  "pageSize": 20,
  "sortBy": "price",
  "sortOrder": "asc"
}
```

说明：该示例表示 query 参数语义，不是实际 GET 请求体。

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "page": 1,
    "pageSize": 20,
    "total": 8,
    "list": [
      {
        "id": 10001,
        "category": "箱包",
        "title": "轻薄通勤双肩包",
        "price": 199.00,
        "sales": "2万+人付款",
        "imageUrl": "https://example.com/p10001.jpg",
        "shopName": "城市轻选店"
      }
    ]
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `keyword` | 搜索关键词，必填 |
| `list` | 返回的商品列表 |
| `imageUrl` | 前端展示主图字段 |
| `shopName` | 前端展示店铺名字段 |

## 6.3 商品轻量详情

### URL

```http
GET /api/products/{id}
```

### Method

`GET`

### 是否需要 JWT

否

### Request 示例

路径参数：

```json
{
  "id": 10001
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10001,
    "category": "箱包",
    "title": "轻薄通勤双肩包",
    "price": 199.00,
    "sales": "2万+人付款",
    "imageUrl": "https://example.com/p10001.jpg",
    "shopName": "城市轻选店"
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `id` | 商品 ID |
| `imageUrl` | 对外统一使用驼峰 |
| `shopName` | 对外统一使用驼峰 |

说明：

- 当前阶段没有独立复杂详情字段
- 详情接口只返回冻结商品字段

---

## 7. Cart 模块

## 7.1 加入购物车

### URL

```http
POST /api/cart/add
```

### Method

`POST`

### 是否需要 JWT

是

### Request 示例

```json
{
  "productId": 10001,
  "quantity": 2
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "add to cart success",
  "data": {
    "cartItemId": 501
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `productId` | 商品 ID |
| `quantity` | 加购数量，必须大于等于 `1` |
| `cartItemId` | 返回购物车项 ID，便于前端后续操作 |

## 7.2 查询购物车

### URL

```http
GET /api/cart
```

### Method

`GET`

### 是否需要 JWT

是

### Request 示例

无请求体。

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 501,
      "productId": 10001,
      "title": "轻薄通勤双肩包",
      "category": "箱包",
      "price": 199.00,
      "sales": "2万+人付款",
      "imageUrl": "https://example.com/p10001.jpg",
      "shopName": "城市轻选店",
      "quantity": 2,
      "checked": true
    }
  ]
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `id` | 购物车项 ID |
| `productId` | 商品 ID |
| `checked` | 结算勾选状态 |
| `quantity` | 数量 |

## 7.3 更新数量

### URL

```http
PUT /api/cart/{id}/quantity
```

### Method

`PUT`

### 是否需要 JWT

是

### Request 示例

```json
{
  "quantity": 3
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "cart quantity updated",
  "data": null
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `id` | 路径中的购物车项 ID |
| `quantity` | 更新后的数量，必须大于等于 `1` |

## 7.4 更新勾选状态

### URL

```http
PUT /api/cart/{id}/checked
```

### Method

`PUT`

### 是否需要 JWT

是

### Request 示例

```json
{
  "checked": true
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "cart checked status updated",
  "data": null
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `id` | 路径中的购物车项 ID |
| `checked` | 勾选状态，`true` 表示参与结算 |

## 7.5 删除购物车商品

### URL

```http
DELETE /api/cart/{id}
```

### Method

`DELETE`

### 是否需要 JWT

是

### Request 示例

路径参数：

```json
{
  "id": 501
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "cart item deleted",
  "data": null
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `id` | 购物车项 ID |

---

## 8. Order 模块

## 8.1 立即购买创建订单

### URL

```http
POST /api/orders/buy-now
```

### Method

`POST`

### 是否需要 JWT

是

### Request 示例

```json
{
  "productId": 10001,
  "quantity": 1
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "order created",
  "data": {
    "orderNo": "ORD202603130001",
    "totalAmount": 199.00,
    "status": "CREATED",
    "payStatus": "UNPAID",
    "createdAt": "2026-03-13T16:00:00+08:00",
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
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `productId` | 直接购买的商品 ID |
| `quantity` | 购买数量 |
| `items` | 从 `items_json` 映射后的订单项 |

## 8.2 购物车勾选结算创建订单

### URL

```http
POST /api/orders/checkout
```

### Method

`POST`

### 是否需要 JWT

是

### Request 示例

```json
{
  "cartItemIds": [501, 502]
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "order created",
  "data": {
    "orderNo": "ORD202603130002",
    "totalAmount": 258.00,
    "status": "CREATED",
    "payStatus": "UNPAID",
    "createdAt": "2026-03-13T16:05:00+08:00",
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
      },
      {
        "productId": 10002,
        "title": "便携咖啡随行杯",
        "category": "家居",
        "price": 59.00,
        "quantity": 1,
        "amount": 59.00,
        "imageUrl": "https://example.com/p10002.jpg",
        "shopName": "慢饮生活馆"
      }
    ]
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `cartItemIds` | 参与结算的购物车项 ID 列表 |
| `orderNo` | 新创建订单号 |
| `payStatus` | 初始通常为 `UNPAID` |

## 8.3 查询订单列表

### URL

```http
GET /api/orders
```

### Method

`GET`

### 是否需要 JWT

是

### Request 示例

```json
{
  "page": 1,
  "pageSize": 10,
  "status": "CREATED",
  "payStatus": "UNPAID"
}
```

说明：该示例表示 query 参数语义，不是实际 GET 请求体。

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "page": 1,
    "pageSize": 10,
    "total": 2,
    "list": [
      {
        "orderNo": "ORD202603130001",
        "totalAmount": 199.00,
        "status": "CREATED",
        "payStatus": "UNPAID",
        "createdAt": "2026-03-13T16:00:00+08:00",
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
    ]
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `page` | 页码 |
| `pageSize` | 每页条数 |
| `status` | 订单状态过滤，可选 |
| `payStatus` | 支付状态过滤，可选 |

## 8.4 查询订单详情

### URL

```http
GET /api/orders/{orderNo}
```

### Method

`GET`

### 是否需要 JWT

是

### Request 示例

路径参数：

```json
{
  "orderNo": "ORD202603130001"
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderNo": "ORD202603130001",
    "totalAmount": 199.00,
    "status": "CREATED",
    "payStatus": "UNPAID",
    "createdAt": "2026-03-13T16:00:00+08:00",
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
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `orderNo` | 路径中的订单号 |
| `items` | 订单商品快照列表 |
| `payStatus` | 当前支付状态 |

---

## 9. Payment 模块

## 9.1 创建支付

### URL

```http
POST /api/payments/create
```

### Method

`POST`

### 是否需要 JWT

是

### Request 示例

```json
{
  "orderNo": "ORD202603130001"
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderNo": "ORD202603130001",
    "payType": "ALIPAY_PC",
    "payForm": "<form id=\"alipay-form\">...</form>"
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `orderNo` | 需要支付的订单号 |
| `payType` | 当前阶段固定为 `ALIPAY_PC` |
| `payForm` | 支付表单 HTML，前端写入后自动提交跳转支付宝沙箱 |

## 9.2 支付回调

### URL

```http
POST /api/payments/callback/alipay
```

### Method

`POST`

### 是否需要 JWT

否

### Request 示例

说明：该接口由支付宝沙箱异步回调，示例字段仅表示回调核心语义。

```json
{
  "out_trade_no": "ORD202603130001",
  "trade_no": "2026031322001499999999999999",
  "trade_status": "TRADE_SUCCESS",
  "total_amount": "199.00"
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

说明：

- 实际对接支付宝时，回调响应体可能需要返回支付宝要求的约定字符串
- 本文档仅冻结项目内部接口语义，不扩展第三方 SDK 实现细节

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `out_trade_no` | 商户订单号，对应系统 `orderNo` |
| `trade_no` | 支付宝交易号 |
| `trade_status` | 支付宝交易状态 |
| `total_amount` | 支付金额 |

## 9.3 查询支付状态

### URL

```http
GET /api/payments/{orderNo}/status
```

### Method

`GET`

### 是否需要 JWT

是

### Request 示例

路径参数：

```json
{
  "orderNo": "ORD202603130001"
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderNo": "ORD202603130001",
    "payStatus": "PAID"
  }
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `orderNo` | 订单号 |
| `payStatus` | 当前支付状态 |

---

## 10. Chat 模块

## 10.1 AI 聊天接口

### URL

```http
POST /api/chat
```

### Method

`POST`

### 是否需要 JWT

是

### Request 示例

```json
{
  "sessionId": "chat-session-001",
  "message": "帮我找一款便宜一点的双肩包"
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "intent": "SEARCH_PRODUCT",
    "message": "我帮你找到 2 个价格更友好的双肩包商品。",
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
}
```

### 关键字段说明

| 字段 | 说明 |
|---|---|
| `sessionId` | 聊天会话 ID，用于上下文延续 |
| `message` | 用户输入文本 |
| `intent` | AI 识别出的业务意图 |
| `products` | AI 返回的真实商品卡片列表 |
| `orders` | AI 返回的订单摘要列表 |
| `actions` | 前端可直接渲染的动作按钮列表 |

## 10.2 ChatResponse 冻结说明

`ChatResponse` 必须包含以下字段，不允许删除：

- `intent`
- `message`
- `products`
- `orders`
- `actions`

附加约束：

- `products` 中商品必须来自真实数据库查询结果
- `orders` 中订单必须属于当前登录用户
- `actions` 只能返回已冻结动作类型

## 10.3 AI 加入购物车示例

### Request 示例

```json
{
  "sessionId": "chat-session-001",
  "message": "把第一个加入购物车"
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "intent": "ADD_TO_CART",
    "message": "已将第一个商品加入购物车。",
    "products": [],
    "orders": [],
    "actions": [
      {
        "type": "GO_CART",
        "label": "查看购物车",
        "targetId": ""
      }
    ]
  }
}
```

## 10.4 AI 立即购买示例

### Request 示例

```json
{
  "sessionId": "chat-session-001",
  "message": "第二个直接买"
}
```

### Response 示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "intent": "BUY_NOW",
    "message": "已为你创建订单，可以继续支付。",
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
      }
    ]
  }
}
```

---

## 11. 前端动作约定

## 11.1 Action 类型

| `type` | 说明 |
|---|---|
| `ADD_TO_CART` | 加入购物车 |
| `BUY_NOW` | 立即购买 |
| `GO_CART` | 跳转购物车 |
| `PAY_NOW` | 发起支付 |
| `VIEW_ORDER` | 查看订单 |
| `GO_HOME` | 返回首页 |

## 11.2 前端行为映射

| Action | 前端行为 |
|---|---|
| `ADD_TO_CART` | 调用加购接口 |
| `BUY_NOW` | 调用立即购买接口 |
| `GO_CART` | 跳转 `/cart` |
| `PAY_NOW` | 调用创建支付接口 |
| `VIEW_ORDER` | 跳转 `/orders` |
| `GO_HOME` | 跳转 `/home` |

---

## 12. 联调冻结说明

以下内容属于联调冻结项，不允许擅自修改：

- 统一响应结构：`code`、`message`、`data`
- 对外字段统一驼峰命名
- 商品对象中的 `imageUrl`、`shopName`
- 订单对象中的 `orderNo`、`payStatus`
- ChatResponse 顶级字段：`intent`、`message`、`products`、`orders`、`actions`

以下内容必须先更新文档，再改实现：

- URL 路径
- 请求字段
- 响应字段
- 枚举值
- Action 类型

---

## 13. 版本记录

### v1.0

- 冻结 Auth、Product、Cart、Order、Payment、Chat 六大模块接口
- 冻结统一响应结构
- 冻结驼峰命名输出规则
- 冻结 ChatResponse 结构与前端动作协议
