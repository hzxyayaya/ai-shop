# alipay-demo

独立的支付宝沙箱购买演示前端，只保留最小链路：

- 登录
- 读取商品列表
- 立即购买创建订单
- 拉起支付宝沙箱支付
- 手动刷新订单状态
- 支付回跳结果确认

## 启动

```powershell
cd alipay-demo
npm install
npm run dev
```

默认地址：

- `http://localhost:5174`

默认会把 `/api` 代理到：

- `http://localhost:8080`

如果你的后端不是这个地址，可以这样启动：

```powershell
cd alipay-demo
$env:VITE_API_TARGET="http://127.0.0.1:8080"
npm run dev
```

## 依赖的后端接口

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/products`
- `POST /api/orders/buy-now`
- `GET /api/orders`
- `GET /api/orders/{orderNo}`
- `POST /api/payments/create`
- `GET /api/payments/{orderNo}/status`

## 说明

- 购买后会自动发起支付，不再保留购物车和 AI 逻辑。
- “刷新订单”会同时请求订单详情和支付状态接口，用于确认最新支付结果。
- 如果支付拉起失败，先检查后端支付宝沙箱环境变量是否已经配置完成。
