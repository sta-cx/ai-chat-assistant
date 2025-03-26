# AI Chat Assistant

一个基于Vue 3的AI聊天助手前端界面。

## 功能特点

- 实时对话
- Markdown格式消息渲染
- 代码高亮显示
- 思考过程显示/隐藏
- 聊天历史本地加密存储
- 可折叠侧边栏设计
- 流式打字效果
- 优雅的过渡动画

## 安装

```bash
# 克隆项目
git clone https://github.com/your-username/ai-chat-assistant.git

# 进入项目目录
cd ai-chat-assistant

# 安装依赖
npm install
```

## 配置

1. 复制环境变量示例文件：
```bash
cp .env.example .env
```

2. 修改`.env`文件中的配置：
```
# API配置
VITE_API_BASE_URL=你的API地址
VITE_API_TIMEOUT=30000

# 安全配置
VITE_STORAGE_ENCRYPTION_KEY=你的加密密钥

# 功能配置
VITE_MAX_HISTORY_MESSAGES=50
VITE_ENABLE_THINKING_MODE=true
```

## 开发

```bash
# 启动开发服务器
npm run dev
```

## 构建

```bash
# 构建生产版本
npm run build
```

## 安全注意事项

1. **环境变量**
   - 不要在代码中硬编码敏感信息
   - 不要提交`.env`文件到版本控制系统
   - 使用`.env.example`作为配置模板

2. **数据存储**
   - 本地存储的数据会进行加密
   - 定期清理敏感数据
   - 可以通过界面清除聊天历史

3. **API安全**
   - 确保API端点使用HTTPS
   - 实现适当的认证机制
   - 添加请求频率限制

## 后端API要求

需要实现以下API端点：

1. POST `/api/ai/chat`
   - 普通对话接口
   - 需要认证
   - 返回JSON格式数据

2. POST `/api/ai/typingAsk`
   - 流式对话接口
   - 支持SSE (Server-Sent Events)
   - 需要认证

## 许可证

[MIT](LICENSE)

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交改动
4. 推送到分支
5. 创建Pull Request

## 免责声明

本项目仅供学习和参考使用，在生产环境中使用时请确保：

1. 实现了适当的用户认证
2. 使用更强的加密算法
3. 添加了必要的安全措施
4. 遵守相关的数据保护法规 