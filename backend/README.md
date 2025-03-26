# AI聊天助手 - 后端

这是AI聊天助手项目的后端部分，使用Spring Boot框架开发。

## 技术栈

- Java 17
- Spring Boot
- Maven

## 项目结构

- `src/main/java/com/example/demo/` - 主要源代码
  - `controller/` - REST API控制器
  - `service/` - 业务逻辑服务
  - `dto/` - 数据传输对象
  - `vo/` - 视图对象
  - `config/` - 配置类

## 开发环境设置

1. 安装JDK 17或更高版本
2. 安装Maven
3. 克隆仓库：`git clone https://github.com/sta-cx/ai-chat-assistant.git`
4. 进入后端目录：`cd ai-chat-assistant/backend`
5. 运行项目：`mvn spring-boot:run`

## API文档

项目启动后，可以通过访问 `http://localhost:8080/swagger-ui.html` 查看API文档。

## 部署

1. 打包应用：`mvn clean package`
2. 运行JAR文件：`java -jar target/demo-0.0.1-SNAPSHOT.jar` 