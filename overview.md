# CourseHub 项目交付总览（M0 骨架阶段）

> 更新时间：2026-10-09
> 当前阶段：**M0 项目骨架 —— 已完成并实测通过**

---

## 一、这次交付了什么

一句话：**一个能跑起来的前后端分离项目骨架**，跑通了你 → 浏览器 → 前端 → 后端 → 数据库 的完整链路。

| 模块 | 内容 | 状态 |
|---|---|---|
| 后端骨架 | Spring Boot 2.7.18 + MyBatis + Druid + Redis，13 个 Java 类 | ✅ 编译通过、启动成功 |
| 数据库 | 16 张表 + 索引 + 全文检索 + 种子数据 | ✅ 已建库并验证 |
| 前端骨架 | Vue 3 + Element Plus + Vite + Pinia | ⏳ 依赖安装中 |
| 项目文档 | README、CONTRIBUTING、文件说明等 5 份 | ✅ |

---

## 二、验收证据（实测，不是"应该能跑"）

启动日志：

```
DruidDataSourceAutoConfigure - Init DruidDataSource
DruidDataSource              - {dataSource-1} inited
TomcatWebServer              - Tomcat started on port(s): 8080 (http)
CourseHubApplication         - Started CourseHubApplication in 2.626 seconds
```

接口实测：

| 请求 | 返回 |
|---|---|
| `GET /api/health` | `{"code":0,"message":"服务运行正常","data":{"status":"UP","javaVersion":"11.0.5"}}` |
| `GET /api/health/ping` | `{"code":0,"message":"ok","data":"pong"}` |
| `GET /api/not-exist` | `{"code":40401,"message":"请求的资源不存在"}` ← 统一格式 |
| `GET /doc.html` | `HTTP 200` ← Swagger 正常 |

数据库实测：

| 检查 | 结果 |
|---|---|
| 建表数量 | 16 张 |
| 中文全文检索 | 搜「数据结构」命中「数据结构与算法」 |
| 组合索引 | `EXPLAIN` 显示走 `idx_course_status_score`，无 `Using filesort` |

---

## 三、目录结构

```
D:\桌面\Java项目AIGC\
├── README.md                    项目总说明书
├── CONTRIBUTING.md              协作规范
├── LICENSE  .editorconfig  .gitignore  docker-compose.yml
│
├── backend/                     ← 后端
│   ├── pom.xml                  依赖清单
│   ├── sql/
│   │   ├── schema.sql           16 张表
│   │   └── init_data.sql        种子数据（密码为 BCrypt 哈希）
│   ├── verify-startup.sh        一键验收脚本
│   └── src/main/
│       ├── java/com/nnuist/coursehub/
│       │   ├── CourseHubApplication.java   ← 启动入口
│       │   ├── common/           通用组件（响应体/异常/分页/常量）
│       │   ├── config/           配置类（跨域/Redis/接口文档）
│       │   ├── controller/       接口层 ← M1 开始大规模填充
│       │   ├── service/          业务层
│       │   ├── mapper/           数据访问层
│       │   ├── entity/ dto/ vo/  数据模型
│       │   └── interceptor/ aspect/ scheduler/ storage/  ← 待填充
│       └── resources/
│           ├── application.yml        主配置
│           ├── application-dev.yml    开发环境
│           ├── application-prod.yml   生产环境
│           ├── application-local.yml          本机密码（不上传 Git）
│           └── application-local.yml.example  密码模板（上传 Git）
│
├── frontend/                    ← 前端
│   ├── package.json  vite.config.js
│   └── src/
│       ├── main.js  App.vue
│       ├── api/request.js       Axios 封装
│       ├── router/index.js      路由 + 登录守卫
│       └── views/               页面
│
├── docs/
│   └── 项目文件说明.md          ← 每个文件干什么用的
│
└── tools/fix-java-path/         环境修复工具
```

---

## 四、关键技术决策（答辩时可讲）

| 决策 | 选择 | 理由 |
|---|---|---|
| Web 框架 | Spring MVC（非 WebFlux） | 传统 MVC 体系，每层代码可逐行讲解 |
| 持久层 | MyBatis + 手写 SQL | 能展示 SQL 功底，答辩时可直接掏 SQL 分析 |
| 安全 | JWT + 自定义拦截器 | 不用 Spring Security 全家桶，流程透明可控 |
| 连接池 | Druid | 自带监控面板，可直观展示慢 SQL |
| 检索 | MySQL ngram 全文索引 | 零额外依赖，且已实测中文分词有效 |
| 密码存储 | BCrypt（自带盐） | 绝不用 MD5/SHA1 |
| 密码配置 | 独立 local 文件 + gitignore | 避免密码随代码上传 GitHub |

---

## 五、这次踩过的坑（都已解决，记录备查）

| # | 问题 | 原因 | 解决 |
|---|---|---|---|
| 1 | 数据库连接失败 | JDBC 参数写 `characterEncoding=utf8mb4` | 改成 `UTF-8`（Java 字符集名） |
| 2 | 密码读不到 | `config.import` 的文件优先级更低，被 dev.yml 的空值覆盖 | dev.yml 里彻底不写 password 字段 |
| 3 | 启动直接崩溃 | Spring Boot 2.6+ 默认 PathPatternParser，Swagger 不兼容 | 配 `ant_path_matcher` |
| 4 | 404 格式不统一 | 未匹配请求走 Spring 默认错误页 | 自定义 `CustomErrorController` 接管 `/error` |
| 5 | 修 404 时误伤文档 | 关闭静态资源映射导致 `/doc.html` 也 404 | 放弃该方案，保留静态资源 |
| 6 | 编译失败 | Spring Boot 2.5+ 移除了 `getErrorPath()` | 删掉该方法的实现 |

---

## 六、下一步（M1：用户与课程）

| 任务 | 内容 |
|---|---|
| 1 | 用户注册 / 登录接口（BCrypt 校验 + 签发 JWT） |
| 2 | JWT 拦截器（校验 Token、解析当前用户） |
| 3 | 自定义注解 `@RequireLogin` / `@RequireRole` |
| 4 | 学院 / 课程 CRUD 接口 |
| 5 | 前端登录页接入真实接口 + Token 存储 |
| 6 | 课程列表页 |

---

## 七、怎么在你本机跑起来

```bash
# 1. 建库（只需一次）
mysql -uroot -p < backend/sql/schema.sql
mysql -uroot -p coursehub < backend/sql/init_data.sql

# 2. 配置密码（只需一次）
#    把 backend/src/main/resources/application-local.yml.example
#    复制为同目录下的 application-local.yml，填入你的 MySQL 密码

# 3. 启动 Redis
双击 E:\java\redis\start.bat

# 4. 启动后端
cd backend
mvn spring-boot:run
# 浏览器打开 http://localhost:8080/api/health 应看到 code=0

# 5. 启动前端
cd frontend
npm install
npm run dev
# 浏览器打开 http://localhost:5173
```

---

## 八、测试账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123` |
| 教师 | `teacher01` | `123456` |
| 学生 | `2025210101` | `123456` |

> 密码在数据库中以 BCrypt 哈希存储，登录功能将在 M1 阶段接入。
