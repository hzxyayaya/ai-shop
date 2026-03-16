# AI Shop

AI 聊天式电商 Demo。当前项目目标是在单仓内完成“商品浏览 + AI 导购 + 下单 + 支付”的可演示闭环。

## 当前状态

- 前后端分离：`frontend` + `backend/mall-backend`
- 真实链路已跑通：
  - 注册 / 登录 / `/api/auth/me`
  - 商品列表 / 搜索
  - 加入购物车 / 查询购物车
  - 购物车结算创建订单
  - 创建支付 / 支付回调 / 支付状态查询 / 订单状态更新
- 支付已切到真实支付宝沙箱 Java SDK
- `/api/chat` 已接入真实商品、购物车、订单服务
- AI 会话上下文 `chat_session_context` 已落库，支持“第一个 / 第二个 / 第三个”商品引用解析

已实测支付订单：

- `ORD20260314000730147844`

已实测 AI 聊天链路：

- `帮我找逗猫棒` -> `第二个直接买`
- 实测创建订单：`ORD20260314005155628874`

## 技术栈

前端：

- Vue 3
- Vite
- Pinia
- Axios

后端：

- Java 17
- Spring Boot 3
- Spring WebFlux
- Spring Data R2DBC
- PostgreSQL
- JWT

数据库 / 基础设施：

- PostgreSQL 16
- pgvector 镜像
- Docker Compose

## 目录结构

```text
ai-shop/
├─ frontend/                   # Vue 3 前端
├─ backend/mall-backend/       # Spring Boot WebFlux 后端
├─ docker/                     # PostgreSQL 初始化脚本
├─ docs/                       # 架构、数据库、API、联调文档
├─ docker-compose.yml
└─ shop_dump.sql               # 当前本地 shop 库导出
```

## 核心文档

建议接手前先读：

- [docs/00-project-overview.md](/Users/Orion/Desktop/ai-shop/docs/00-project-overview.md)
- [docs/01-architecture-and-contract.md](/Users/Orion/Desktop/ai-shop/docs/01-architecture-and-contract.md)
- [docs/02-database-design.md](/Users/Orion/Desktop/ai-shop/docs/02-database-design.md)
- [docs/03-api-spec.md](/Users/Orion/Desktop/ai-shop/docs/03-api-spec.md)
- [docs/03a-ai-chat-response-contract.md](/Users/Orion/Desktop/ai-shop/docs/03a-ai-chat-response-contract.md)
- [docs/04-agent-task-board.md](/Users/Orion/Desktop/ai-shop/docs/04-agent-task-board.md)
- [docs/05-integration-plan.md](/Users/Orion/Desktop/ai-shop/docs/05-integration-plan.md)
- [docs/06-ui-ux-notes.md](/Users/Orion/Desktop/ai-shop/docs/06-ui-ux-notes.md)

## 快速启动

### 1. 启动数据库

在仓库根目录执行：

```powershell
docker compose up -d
```

默认数据库配置：

- Host: `localhost`
- Port: `5432`
- DB: `shop`
- User: `postgres`
- Password: `postgres`

初始化脚本：

- [docker/postgres/init/02-create-core-tables.sql](/Users/Orion/Desktop/ai-shop/docker/postgres/init/02-create-core-tables.sql)

### 2. 启动后端

```powershell
cd backend/mall-backend
mvn spring-boot:run
```

默认地址：

- `http://localhost:8080`
- 项目默认启用 Spring `local` profile
- 日常开发不再依赖启动脚本
- 如需覆盖本地配置，直接设置同名环境变量即可

后端测试：

```powershell
cd backend/mall-backend
mvn test
```

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

默认地址：

- `http://localhost:5173`

如果要把前端联调目标切到公网后端，例如 `http://170.106.147.199:8080`，可以这样启动：

```powershell
cd frontend
$env:VITE_API_TARGET="http://170.106.147.199:8080"
npm run dev
```

默认代理目标定义见：

- [frontend/.env.example](/Users/Orion/Desktop/ai-shop/frontend/.env.example)
- [frontend/vite.config.js](/Users/Orion/Desktop/ai-shop/frontend/vite.config.js)

前端构建验证：

```powershell
cd frontend
npm run build
```

## 环境变量

后端数据库支持环境变量覆盖：

- `DB_R2DBC_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

支付宝相关环境变量：

- `ALIPAY_GATEWAY_URL`
- `ALIPAY_NOTIFY_URL`
- `ALIPAY_RETURN_URL`
- `ALIPAY_APP_ID`
- `ALIPAY_SELLER_ID`
- `ALIPAY_PRIVATE_KEY`
- `ALIPAY_PUBLIC_KEY`
- `ALIPAY_FORMAT`
- `ALIPAY_CHARSET`
- `ALIPAY_SIGN_TYPE`

默认配置文件：

- [backend/mall-backend/src/main/resources/application.yml](/Users/Orion/Desktop/ai-shop/backend/mall-backend/src/main/resources/application.yml)
- [backend/mall-backend/src/main/resources/application-local.yml](/Users/Orion/Desktop/ai-shop/backend/mall-backend/src/main/resources/application-local.yml)

## 前后端联调约定

- 前端开发地址默认 `http://localhost:5173`
- 后端默认 `http://localhost:8080`
- Vite 已配置 `/api` 代理到后端 `8080`
- API、数据库结构、页面范围都受 `docs/` 冻结文档约束

支付联调补充约定：

- 本地后端做真实支付宝沙箱联调时，`ALIPAY_NOTIFY_URL` 不能指向本机 `localhost`
- 本地支付闭环需要先起公网穿透，再把穿透地址注入本地后端的 `ALIPAY_NOTIFY_URL`
- 前端发起支付、登录、支付结果页回跳必须全程使用同一个 origin，避免 `localhost` / `127.0.0.1` 混用导致 token 丢失

推荐启动方式：

```powershell
cd backend/mall-backend
mvn spring-boot:run
```

```powershell
cd frontend
npm run dev
```

如果只是日常开发，到这里就够了。

如果要调真实支付宝异步回调，推荐使用“无脚本流程”：

1. 先启动一个公网穿透，把 `8080` 暴露出去

示例：

```powershell
npx localtunnel --port 8080 --subdomain ai-shop-test
```

2. 再把公网地址写入 `ALIPAY_NOTIFY_URL`

```powershell
$env:ALIPAY_NOTIFY_URL = "https://ai-shop-test.loca.lt/api/payments/callback/alipay"
cd backend/mall-backend
mvn spring-boot:run
```

3. 前端仍然正常启动

```powershell
cd frontend
npm run dev
```

说明：

- 现在主推荐方式是不依赖任何启动脚本
- 旧的 `scripts/run-payment-debug.ps1` 和 `scripts/stop-payment-debug.ps1` 只作为可选辅助工具保留
- 即使不用脚本，支付功能和真实回调仍然可用，前提是你自己提供公网 `notify` 地址

当前冻结限制包括：

- 禁止擅自修改数据库冻结结构
- 禁止擅自修改 API 路径
- 禁止擅自新增未规划模块
- 如需变更，先更新冻结文档，再改代码

## 当前已完成页面

- `/login`
- `/register`
- `/home`
- `/chat`
- `/cart`
- `/orders`
- `/payment/result`

## AI 聊天能力现状

已支持：

- 商品搜索
- 加入购物车
- 立即购买
- 查看购物车引导
- 查看订单
- 支付引导
- 基于 `sessionId` 的商品序号引用解析

暂未支持：

- DeepSeek 模型接入
- embedding 语义检索
- 更复杂的多轮记忆和引用扩展

## 常用接口

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/products`
- `GET /api/products/search`
- `GET /api/cart`
- `POST /api/cart/add`
- `POST /api/orders/buy-now`
- `POST /api/orders/checkout`
- `GET /api/orders`
- `POST /api/payments/create`
- `POST /api/payments/callback/alipay`
- `GET /api/payments/{orderNo}/status`
- `POST /api/chat`

详细契约见：

- [docs/03-api-spec.md](/Users/Orion/Desktop/ai-shop/docs/03-api-spec.md)
- [docs/03a-ai-chat-response-contract.md](/Users/Orion/Desktop/ai-shop/docs/03a-ai-chat-response-contract.md)

## 已验证的回归项

- `mvn test`
- `npm run build`
- 真实支付宝沙箱支付闭环
- `/api/chat` 真实 HTTP 烟测

## 当前支付联调状态

- 已修复：支付结果页因回跳 origin 不一致导致的 token 丢失与“支付失败”误判
- 已修复：支付表单提交前按当前前端 origin 重写 `return_url`
- 已补齐：本地后端查询支付状态时，若异步回调未先落库，可主动向支付宝查单并回写本地订单状态
- 已补齐：本地支付联调脚本，可作为可选辅助工具使用
- 当前建议：本地联调统一使用 `http://localhost:5173`

## 后续建议

- 继续补齐 AI 模块剩余意图的真实联调
- 做聊天页与支付结果页的展示回归
- 视需求决定是否继续接入 DeepSeek 与 embedding 检索
