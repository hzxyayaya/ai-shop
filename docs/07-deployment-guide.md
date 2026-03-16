# AI Shop 部署文档

## 1. 文档目的

本文档用于让新同学在不熟悉项目的情况下，按步骤完成部署并成功联调前后端、AI 聊天与支付链路。

适用范围：
- 本地部署（Windows / PowerShell）
- 单机演示部署（同机前后端）

## 2. 项目结构

- frontend：Vue 3 前端
- backend/mall-backend：Spring Boot 3 后端
- docker-compose.yml：PostgreSQL 与 pgvector
- docker/postgres/init：数据库初始化脚本

## 3. 环境要求

- JDK 17
- Maven 3.9+
- Node.js 18+
- npm 9+
- Docker Desktop（需支持 docker compose）

建议先确认版本：
- java -version
- mvn -v
- node -v
- npm -v
- docker -v
- docker compose version

## 4. 第一次部署（本地完整链路）

### 4.1 启动数据库

在仓库根目录执行：

- docker compose up -d

默认数据库：
- Host: localhost
- Port: 5432
- DB: shop
- User: postgres
- Password: postgres

### 4.2 启动后端

在仓库根目录执行：

- cd backend/mall-backend
- mvn spring-boot:run

默认后端地址：
- http://localhost:8080

### 4.3 启动前端

在仓库根目录执行：

- cd frontend
- npm install
- npm run dev

默认前端地址：
- http://localhost:5173

## 5. 部署前必配环境变量

说明：不要把真实密钥写进仓库文件，统一使用环境变量。

### 5.1 后端数据库

- DB_R2DBC_URL
- DB_USERNAME
- DB_PASSWORD

默认本地可不配，直接使用 application.yml 默认值。

### 5.2 DeepSeek（聊天模型）

- DEEPSEEK_ENABLED=true
- DEEPSEEK_BASE_URL=https://api.deepseek.com
- DEEPSEEK_API_KEY=你的密钥
- DEEPSEEK_MODEL=deepseek-chat

### 5.3 智谱（embedding 模型）

- ZHIPU_EMBEDDING_ENABLED=true
- ZHIPU_EMBEDDING_BASE_URL=https://open.bigmodel.cn/api/paas/v4
- ZHIPU_API_KEY=你的密钥
- ZHIPU_EMBEDDING_MODEL=embedding-3

## 6. 冒烟验证清单

### 6.1 服务可用性

- 打开前端首页 http://localhost:5173
- 登录后访问商品列表
- 可正常加载商品

### 6.2 AI 聊天基础链路

在聊天页发送：
- 我想买手机

预期：
- 返回商品卡片
- 可点击加入购物车或购买

### 6.3 embedding 真实可用验证

项目已提供测试类：
- src/test/java/com/mall/embedding/ZhipuEmbeddingSmokeTest.java

执行命令：
- mvn -f backend/mall-backend/pom.xml -Dtest=com.mall.embedding.ZhipuEmbeddingSmokeTest test

判定标准：
- 测试通过：说明智谱 embedding API 可连通且可返回向量
- 测试跳过：通常是未设置 ZHIPU_API_KEY
- 测试失败：通常是 key 无效、模型名不支持或网络问题

### 6.4 支付链路

如需验证真实沙箱回调，需要可公网访问的 notify 地址。

推荐流程（Windows / PowerShell）：

1. 启动本地穿透（推荐）

- npx localtunnel --port 8080 --subdomain ai-shop-check-fixed

预期公网地址：
- https://ai-shop-check-fixed.loca.lt

可选：也可以使用仓库脚本自动启动并写入输出文件

- powershell -ExecutionPolicy Bypass -File .\scripts\start-localtunnel.ps1
- 结果会写入 output/localtunnel.url.txt

2. 配置支付回调地址并启动后端（两种方式任选其一）

方式 A：直接用封装脚本（推荐）

- powershell -ExecutionPolicy Bypass -File .\start-backend.ps1 -NotifyBaseUrl https://ai-shop-check-fixed.loca.lt

方式 B：手动设置环境变量后启动

- $env:ALIPAY_NOTIFY_URL = "https://ai-shop-check-fixed.loca.lt/api/payments/callback/alipay"
- cd backend/mall-backend
- mvn spring-boot:run

3. 前端保持本地启动

- cd frontend
- npm run dev

4. 验证回调是否配置成功

- 后端启动日志中应能看到 notify URL 已被覆盖
- 发起支付后，订单状态应能从未支付更新为已支付（或可通过支付状态查询接口确认）

注意：

- 每次重启本地穿透，公网域名可能变化，需同步更新 ALIPAY_NOTIFY_URL
- ALIPAY_NOTIFY_URL 必须是公网可访问地址，不能使用 localhost 或 127.0.0.1

## 7. 常见问题

### 7.1 聊天返回商品不相关

优先检查：
- 是否已设置 ZHIPU_API_KEY
- 智谱 embedding 烟雾测试是否通过
- 后端日志是否出现 embedding 失败后回退关键词检索

### 7.2 前端能打开但接口 401

检查：
- 是否从同一个域名访问前端（避免 localhost 与 127.0.0.1 混用）
- 是否已重新登录获取 token

### 7.3 数据库启动了但后端连不上

检查：
- docker compose ps
- 5432 端口是否被占用
- DB_R2DBC_URL 是否与当前数据库地址一致

## 8. 面向新同学的最短路径

按顺序执行：

1. docker compose up -d
2. 启动后端 mvn spring-boot:run
3. 启动前端 npm run dev
4. 登录后打开聊天页
5. 跑智谱 embedding 烟雾测试
6. 再验证聊天推荐与下单链路

完成以上 6 步，即可视为部署成功。
