# AI智能客服系统

## 项目介绍

AI智能客服系统是一个基于Spring AI框架开发的外卖平台智能客服解决方案。该系统集成了大语言模型，能够自动理解用户意图，查询订单信息，并提供智能回复。系统支持流式对话，能够实时响应用户请求，同时保留了对话历史，支持多轮对话。

### 主要功能

1. **智能对话**：基于大语言模型的自然语言交互，能够理解用户意图并作出适当回应
2. **订单查询**：集成订单查询工具，可根据订单号查询订单详情，或查询用户所有订单列表
3. **会话记忆**：保存对话历史，支持多轮对话和上下文理解
4. **流式响应**：基于Server-Sent Events(SSE)的流式对话，提供实时响应体验
5. **安全认证**：基于JWT的用户身份认证，确保系统安全

## 技术栈

- **后端框架**：Spring Boot 3.x
- **AI框架**：Spring AI
- **大语言模型**：集成了OpenAI或其他兼容的LLM服务
- **数据库**：MySQL（存储对话历史）
- **缓存**：Redis（可选，用于缓存会话数据）
- **认证**：JWT（JSON Web Token）
- **前端**：HTML5 + JavaScript（示例）
- **构建工具**：Maven
- **Java版本**：Java 17+

## 启动方式

### 环境准备

1. **JDK 17+**：确保安装Java 17或更高版本
2. **Maven 3.6+**：用于构建和管理项目依赖
3. **MySQL 8.0+**：用于存储对话历史记录
4. **Redis（可选）**：用于缓存会话数据

### 配置步骤

1. **克隆项目**
   ```bash
   git clone [项目地址]
   cd ai-customer-service
   ```

2. **配置数据库**
   - 创建数据库：`CREATE DATABASE ai_customer_service;`
   - 修改配置文件：`src/main/resources/application.yml`，配置数据库连接信息

3. **配置AI服务**
   - 在`application.yml`中配置AI服务的相关参数，包括API密钥、模型等

4. **配置JWT**
   - 在`application.yml`中配置JWT密钥和过期时间

5. **构建项目**
   ```bash
   mvn clean package
   ```

6. **启动服务**
   ```bash
   java -jar target/ai-customer-service.jar
   ```

### 访问服务

启动成功后，服务将在默认端口8080上运行，可以通过以下地址访问：
- 服务地址：http://localhost:8080
- API文档：http://localhost:8080/swagger-ui.html（如果集成了Swagger）

## 接口说明

### 1. 用户认证接口

#### 登录接口
- **路径**：`/ai/login`
- **方法**：GET
- **参数**：
  - `userId` (Long, 必填)：用户ID
- **返回**：JWT令牌字符串
- **说明**：根据用户ID生成JWT令牌，用于后续身份认证（演示用，正式项目应使用微信登录等安全认证方式）

### 2. 对话接口

#### 非流式对话接口
- **路径**：`/ai/generate`
- **方法**：GET
- **参数**：
  - `message` (String, 可选，默认"你好")：用户输入的消息
- **返回**：AI的回复内容
- **说明**：提供简单的AI对话功能，用于测试和调试。此接口不保存会话历史，每次都是全新对话

#### 流式对话接口
- **路径**：`/ai/generateStream`
- **方法**：GET
- **参数**：
  - `message` (String, 可选，默认"你好")：用户输入的消息
  - `sessionId` (String, 可选，默认"1")：会话ID，用于区分不同对话会话
- **返回**：Server-Sent Events流，实时返回AI的回复内容
- **说明**：提供基于Server-Sent Events的流式AI对话功能，支持实时响应和会话记忆。这是主要的AI客服交互接口

#### RAG增强流式对话接口
- **路径**：`/ai/ragStream`
- **方法**：GET
- **参数**：
  - `message` (String, 可选，默认"你好")：用户输入的消息
  - `sessionId` (String, 可选，默认"rag001")：会话ID，用于区分不同对话会话
- **返回**：Server-Sent Events流，实时返回AI的回复内容
- **说明**：基于检索增强生成(RAG)技术的流式对话接口，能够结合知识库提供更准确的回答

### 3. 会话管理接口

#### 查询会话历史
- **路径**：`/ai/history`
- **方法**：GET
- **参数**：
  - `sessionId` (String, 可选，默认"1")：会话ID
- **返回**：包含历史对话记录的Result对象
- **说明**：根据会话ID和当前用户ID查询该会话的历史对话记录，用于前端展示对话历史或实现会话恢复功能

## 系统架构

系统采用前后端分离架构，后端提供RESTful API和SSE流式接口，前端通过HTTP请求和SSE连接与后端交互。

### 核心组件

1. **ChatController**：处理HTTP请求，提供API接口
2. **OrderQueryFunction**：订单查询工具，供大模型调用
3. **TokenInterceptor**：JWT令牌拦截器，处理用户认证
4. **ChatService**：处理会话相关业务逻辑
5. **RagService**：处理检索增强生成相关逻辑

## 使用示例

### 1. 用户登录

```bash
curl "http://localhost:8080/ai/login?userId=12345"
```

返回示例：
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEyMzQ1LCJpYXQiOjE2ODc2ODk3MDksImV4cCI6MTY4Nzg5ODMwOX0.abcdef123456
```

### 2. 流式对话

```bash
curl "http://localhost:8080/ai/generateStream?message=我的订单号是12345&sessionId=123&token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEyMzQ1LCJpYXQiOjE2ODc2ODk3MDksImV4cCI6MTY4Nzg5ODMwOX0.abcdef123456"
```

### 3. 查询历史

```bash
curl "http://localhost:8080/ai/history?sessionId=123&token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEyMzQ1LCJpYXQiOjE2ODc2ODk3MDksImV4cCI6MTY4Nzg5ODMwOX0.abcdef123456"
```

## 注意事项

1. 本项目包含本地调试功能，通过URL参数`debugUserId`可以直接指定用户ID，上线前务必移除此功能
2. 流式对话接口需要支持SSE的前端，如EventSource
3. 系统集成了订单查询工具，需要正确配置主项目的访问地址
4. 系统提示词定义了AI的行为规则，可根据实际业务需求调整

## 开发指南

如需扩展功能或修改现有功能，请参考以下指南：

1. **添加新工具**：实现新功能工具，并添加`@Tool`注解，在系统提示词中说明工具用途
2. **修改AI行为**：调整`BASE_SYS_PROMPT`常量中的系统提示词
3. **扩展会话功能**：修改`ChatService`中的相关方法
4. **添加新的认证方式**：扩展`TokenInterceptor`，添加新的认证逻辑

## 联系方式

如有问题或建议，请联系开发团队。
