# SpringBoot3 + Vue3 校园教务系统（含校园 Agent 助手）

一个前后端分离的**校园教务 / 学生成绩管理**教学项目：从零脚手架开始带做，覆盖登录注册、双端权限、教务主数据、成绩规则、公告、统计，以及基于 Tool Calling + RAG + Spring AI 的**校园 Agent 助手**。

仓库同时提供**完整的中文教学文档**（约 40 万字），按章节讲解每一页的后端代码、前端代码、数据表与联调方式，适合初学者跟做。

## 项目说明

本仓库以「可运行的脚手架」为起点：

- **已实现并可运行**：用户/管理员双端登录注册、JWT 鉴权与路由守卫、管理员管理、用户管理、菜单管理、轮播图管理、系统日志、系统设置、管理端首页统计、个人中心、文件上传，以及**学院管理**业务模块（完整的 CRUD 示例）。
- **教学文档规划**：专业 → 班级 → 学期 → 课程 → 开课 → 成绩录入 → 用户端成绩/课表/成绩单 → 公告 → 首页聚合 → 文档库 → 校园助手 → 学习计划。

各模块的开发顺序、完整代码与数据库设计见 [docs/校园教务系统从0教学.md](docs/校园教务系统从0教学.md)；Agent 助手部分见 [docs/校园助手Agent集成文档.md](docs/校园助手Agent集成文档.md)。

## 功能一览

| 端 | 地址 | 角色 | 功能 |
| --- | --- | --- | --- |
| 用户端 | `/user/home` | 学生 | 首页、公告、个人中心（文档规划：成绩、课表、成绩单、学习计划、校园助手） |
| 管理端 | `/admin` | 管理员 | 首页统计、管理员/用户/菜单/轮播图/日志/系统设置、学院管理 |
| 后端 API | `http://localhost:8080/api` | 前端调用 | 认证、权限、系统管理、教务业务、文件上传 |

界面特性：Element Plus、多标签页导航、明暗主题切换、图片上传、ECharts 图表。

## 技术栈

**后端**

- Spring Boot 3.3.7（Java 17）
- MyBatis-Plus 3.5.9
- MySQL 8+（驱动 MySQL Connector/J）
- JWT 鉴权（jjwt 0.12.6）
- SpringDoc OpenAPI 3（Swagger UI）
- Hutool 工具库

**前端**

- Vue 3.5 + Vite 4
- Vue Router 4 + Pinia 2
- Element Plus 2.9 + Element Plus Icons
- ECharts 6（图表）
- Axios（统一请求封装与拦截器）

## 目录结构

```
.
├── docs/                          # 教学文档（主教程 + Agent 助手专册）
├── sql/                           # 参考 SQL
├── springboot-vue-scaffold/
│   ├── backend/                   # Spring Boot 3 后端
│   │   ├── src/main/java/com/scaffold/
│   │   │   ├── common/            # 统一返回、异常、分页、鉴权上下文
│   │   │   ├── config/            # CORS、MyBatis-Plus、建表与初始化数据
│   │   │   ├── controller/        # 通用接口 + admin/ + user/
│   │   │   ├── dto/               # 请求/响应对象
│   │   │   ├── edu/               # 教务公共逻辑（学期解析、成绩规则）
│   │   │   ├── entity/ mapper/    # 实体与 Mapper
│   │   │   ├── security/          # JWT 工具与拦截器
│   │   │   └── service/           # 接口 IXxxService + 实现 impl/
│   │   └── src/main/resources/application.yml
│   └── frontend/                  # Vue 3 前端
│       └── src/
│           ├── api/               # 接口封装与请求拦截
│           ├── components/        # 公共组件（上传、主题、标签页等）
│           ├── layouts/           # 管理端 / 用户端布局
│           ├── router/            # 路由与登录、角色守卫
│           ├── stores/            # Pinia 状态
│           └── views/             # 页面（含 views/edu、views/user）
```

## 快速开始

### 1. 环境要求

| 软件 | 版本 |
| --- | --- |
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |

### 2. 数据库

无需手动建库建表：后端首次启动时 `SchemaRunner` 会自动创建 `school_edu` 库、基础表并写入初始化数据。

默认连接配置（见 `springboot-vue-scaffold/backend/src/main/resources/application.yml`）：

```yaml
url: jdbc:mysql://localhost:3306/school_edu?...
username: root
password: ${DB_PASSWORD:20120104}
```

请按自己的 MySQL 账号密码修改，或通过环境变量注入：

```powershell
$env:DB_PASSWORD="你的数据库密码"
```

### 3. 启动后端

```bash
cd springboot-vue-scaffold/backend
mvn spring-boot:run
```

后端地址：`http://localhost:8080/api`，健康检查：`/api/health`，接口文档：`http://localhost:8080/api/swagger-ui.html`。

### 4. 启动前端

```bash
cd springboot-vue-scaffold/frontend
npm install
npm run dev
```

前端地址：`http://127.0.0.1:5173`（`/api`、`/uploads` 已代理到 8080）。

### 5. 演示账号

| 角色 | 账号 | 密码 | 入口 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `123456` | `http://127.0.0.1:5173/admin/login` |
| 普通用户 | `user` | `123456` | `http://127.0.0.1:5173/login` |

> 教学文档中出现的 `eduadmin` / `linxiao` 属于教务业务演示账号，需按文档完成对应章节、生成教务数据后才会存在。

## 说明

- 修改 Java 代码后需重启后端。
- 上传文件保存在后端工作目录下的 `uploads/`，通过 `/uploads/**` 静态映射访问。
- 生产环境请务必替换 `app.jwt.secret` 与数据库密码。
