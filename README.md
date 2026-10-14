# 天云外卖 · AI 智能客服系统

> 毕业设计项目：在天云外卖（Spring Boot 版）基础上，基于 Spring AI 接入通义千问大模型，构建外卖平台的 **AI 智能客服** 与 **商家 AI 经营助手**。

一个仓库包含两个服务：
- **`backend/`** —— 天云外卖主项目（管理端 + 用户端 REST API）
- **`ai-service/`** —— AI 客服子服务（大模型对话、RAG 知识库检索、业务工具调用）

三大终端均已打通：**Vue3 管理后台、微信小程序（uni-app）、AI 客服聊天页（商家端 / 用户端）**。

---

## 功能特性

### 主项目（backend，端口 8080）
- 员工登录 / 注册 / 管理 / **修改密码**（JWT 鉴权，旧密码校验）
- 分类、菜品、套餐的增删改查与起售停售
- 购物车、地址簿、下单、支付回调、订单管理全流程
- 数据统计：营业额、订单、用户、销量 Top10 报表（Excel 导出）
- 工作台：今日经营概况、订单状态概览
- WebSocket 订单状态实时推送

### AI 客服（ai-service，端口 8081）
- 基于 **Spring AI + 阿里云百炼（通义千问 qwen-plus）** 的 SSE 流式对话
- **工具调用（Function Calling）**：
  - 查订单详情 / 查我的订单列表（通过 RestTemplate 对接主项目接口，JWT token 透传，数据实时真实）
  - 售后知识库检索（**RAG 向量检索**，内存向量库 SimpleVectorStore）
- **RAG 知识库自动初始化**：项目启动时自动导入 3 条售后规则文档（退款流程 / 投诉流程 / 配送说明），无需手动初始化
- 统一会话入口：一条消息自动路由到「业务查询」或「知识库检索」，单窗口返回
- 会话历史记忆（Redis，按 sessionId 隔离）
- **商家端 AI 经营助手**：查今日营业额 / 订单概况 / 销量 Top10 / 营业状态

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.5.5、MyBatis、Druid、PageHelper、Knife4j、JWT |
| AI | Spring AI 1.1.8、阿里云百炼 DashScope（qwen-plus）、Function Calling、RAG（SimpleVectorStore） |
| 数据 | MySQL、Redis |
| 管理端 | Vue3 + Vite + TypeScript + Element Plus + Pinia |
| 小程序 | uni-app（微信开发者工具） |
| 部署 | Nginx（反向代理 + gzip + SSE 不缓冲） |

---

## 项目结构

```
sky-take-out-ai/
├── backend/                      # 天云外卖主项目
│   ├── sky-common/               # 公共模块（工具、常量、异常）
│   ├── sky-pojo/                 # 实体/DTO/VO（含 EmployeeFixPwdDTO 改密）
│   └── sky-server/               # 启动模块（controller / service / mapper）
│       └── src/main/resources/
│           ├── application.yml        # 主配置（端口 8080）
│           ├── application-dev.yml    # 开发配置（数据库、Redis、环境变量占位符）
│           └── mapper/*.xml           # MyBatis SQL
└── ai-service/                   # AI 客服子服务（端口 8081）
    └── src/main/
        ├── java/com/sky/aicustomerservice/
        │   ├── controller/ChatController      # /ai 系列接口（登录、对话、经营助手）
        │   ├── function/                      # 大模型工具
        │   │   ├── OrderQueryFunction.java    # 订单查询工具（token 透传）
        │   │   ├── KnowledgeBaseFunction.java # 售后知识库 RAG 工具
        │   │   └── BusinessFunction.java      # 商家经营数据工具
        │   ├── config/
        │   │   └── RagDataInitializer.java   # 启动时自动初始化 RAG 知识库
        │   ├── service/                       # 对话 / RAG 服务
        │   └── interceptor/TokenInterceptor    # JWT 拦截器
        └── resources/
            ├── application.yml                # 配置（端口 8081、模型、环境变量）
            └── static/index.html              # AI 客服前端页面（管理端内嵌）
```

---

## 快速开始

### 环境要求
- JDK 17+（主项目）/ JDK 21（AI 服务）
- Maven 3.6+
- MySQL 8.x、Redis 6.x
- Node.js 16+（前端）
- 阿里云百炼 API Key（通义千问）

### 1. 数据库初始化
执行 `backend` 下的 `sky_take_out.sql`（主项目数据库脚本，含测试数据），数据库名 `sky_take_out`。

### 2. 配置环境变量（敏感信息不入库）

| 变量 | 说明 | 使用方 |
|---|---|---|
| `DASHSCOPE_API_KEY` | 阿里云百炼 API Key | 两个服务 |
| `WECHAT_SECRET` | 微信小程序 AppSecret | 主项目 |

IDEA 中：`Run → Edit Configurations → Environment variables` 配置后启动。

### 3. 启动主项目（8080）
```
cd backend
mvn spring-boot:run -pl sky-server
```
或 IDEA 直接运行 `SkyApplication`。

### 4. 启动 AI 客服（8081）
```
cd ai-service
mvn spring-boot:run
```
或 IDEA 直接运行 `AiCustomerServiceApplication`。启动后自动初始化 RAG 知识库，控制台可见 `RAG知识库文档初始化完成，共 3 条`。

### 5. 前端
- **管理端**：`hanye-take-out-vue3` 项目 `npm run dev`（开发），生产部署见下文 Nginx
- **小程序**：`hanye-take-out-uniapp` 项目，用微信开发者工具导入 `dist/build/mp-weixin`，勾选「不校验合法域名」

### 演示账号

| 端 | 账号 | 密码 |
|---|---|---|
| 管理端 | admin | 123456 |
| 小程序（模拟登录） | 用户 id=4 | 自动登录 |

---

## AI 客服对话流程

```
用户输入一句话
        │
        ▼
统一会话入口（/ai 单窗口对话，SSE 流式返回）
        │
        ▼
AI 判断意图（大模型 + 工具路由）
├─ 查订单/催单/订单列表   → 调用 OrderQueryFunction（RestTemplate 调主项目，JWT 透传）
├─ 问退款/售后/投诉规则   → 调用 KnowledgeBaseFunction（SimpleVectorStore 向量检索后回答）
└─ 闲聊/平台介绍          → 直接自然回答
        │
        ▼
两种能力的结果在同一个会话里自然返回
```

商家端进入管理后台 →「AI 经营助手」，可对话式查询今日营业额、订单概况、销量 Top10。

---

## 部署（Nginx）

前端打包后部署到 Nginx，反向代理后端两个服务：

```nginx
server {
    listen 80;
    root html/sky;                 # 前端静态资源
    index index.html;
    # Vue Router history 模式
    location / {
        try_files $uri $uri/ /index.html;
    }
    # 管理端接口 → 主项目 8080
    location /api/ {
        proxy_pass http://localhost:8080/admin/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    # 用户端接口 → 主项目 8080
    location /user/ {
        proxy_pass http://localhost:8080/user/;
        proxy_set_header Host $host;
    }
    # AI 客服 → 8081（SSE 流式输出必须关闭缓冲）
    location /ai/ {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_buffering off;
        proxy_cache off;
        chunked_transfer_encoding on;
    }
    gzip on;
}
```

---

## 项目亮点（答辩要点）

1. **AI 能力产品化**：不是简单"接入大模型"，而是把外卖真实业务做成 Function Calling 工具（订单查询、RAG 检索），AI 回答基于实时数据库数据，不编造。
2. **RAG 知识库自动初始化**：内存向量库 + 启动时自动导入售后规则文档，重启即用，无需额外维护。
3. **JWT 透传设计**：AI 服务通过前端传入的 token 调主项目接口，用户数据隔离，不做全局身份假设。
4. **统一会话入口**：业务查询 + 知识库问答在同一个对话窗口自然路由，接近真实客服体验。
5. **双端 AI**：用户端 AI 客服 + 商家端 AI 经营助手，一套框架两种角色。
6. **全流程贯通**：管理端 / 小程序 / AI 客服三大终端全部联调可用。

---

## 说明
- 前端管理端（Vue3）与小程序（uni-app）基于开源项目二次适配，接口与字段已对齐本项目后端。
- 数据库脚本、微信支付等配置为本地开发用途，生产部署请自行替换密钥。
