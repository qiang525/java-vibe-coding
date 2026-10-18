# backend —— Java 后端（Spring Boot 2.7 + MyBatis）

> 一个后端就干三件事：**接收请求 → 处理业务 → 读写数据库**。
> 这个目录里的 99 个 `.java` 文件，都是围绕这三件事分工的。

---

## 一、怎么启动

```
双击  ..\tools\start-backend.bat
```

或者用 IDEA 打开**这个文件夹**（不是外层），运行 `CourseHubApplication`。

**只启动后端（不启前端）时**，浏览器打开 `http://localhost:8080` 就是完整网站 ——
因为前端已经打包进 `src/main/resources/static/`。

---

## 二、目录分工（按"请求怎么流动"看）

一次请求从浏览器进来，会依次穿过这些包：

```
浏览器  ──→  config/WebMvcConfig     注册拦截器：谁需要登录
             │
             ↓
         interceptor/AuthInterceptor  解析 JWT，把"我是谁"放进 ThreadLocal
             │
             ↓
         controller/  ───────────────  ← 第一站：接收请求、校验参数
             │                            比如 AuthController.login()
             ↓
         service/ + service/impl/  ───  ← 第二站：真正的业务逻辑
             │                            比如校验密码、生成 token
             ↓
         mapper/  ───────────────────  ← 第三站：定义"要查什么数据"（只有接口）
             │
             ↓
    resources/mapper/*.xml  ────────  ← 第四站：真正的 SQL 写在这里
             │
             ↓
           MySQL
```

### 每个包是干什么的

| 包 | 放什么 | 数量 | 你要动它吗 |
|---|---|---|---|
| `controller/` | 接收 HTTP 请求的入口 | 11 | 加接口时动 |
| `service/` | 业务接口（只有方法声明） | 15 | 加功能时动 |
| `service/impl/` | 业务实现（真正的逻辑） | 13 | **最常改这里** |
| `mapper/` | 数据访问接口（只有方法声明） | 15 | 加查询时动 |
| `entity/` | 跟数据库表一一对应的类 | 16 | 改表结构时动 |
| `dto/` | 前端传进来的参数（Data Transfer Object） | 18 | 加接口参数时动 |
| `vo/` | 返回给前端的视图对象（View Object） | 14 | 加返回字段时动 |
| `common/` | 通用能力：统一响应体、异常、工具类、注解 | —— | 一般是读 |
| `config/` | Spring 配置（CORS、Redis、Knife4j、拦截器注册） | 4 | 加中间件时动 |
| `aspect/` | AOP 切面（审计日志） | 1 | 基本不用动 |
| `scheduler/` | 定时任务（每天 03:00 重算可信度评分） | 2 | 改调度时动 |
| `storage/` | 文件存储抽象（本地实现 + 预留 MinIO） | 3 | 换存储时动 |
| `interceptor/` | 登录/权限拦截器 | 1 | 改鉴权规则时动 |

### 三个"长得像"的包，为什么不能合并

| 包 | 长什么样 | 用途 | 举例 |
|---|---|---|---|
| `entity` | 跟表字段**一模一样** | 数据库长什么样 | `SysUser`（含 password 字段） |
| `dto` | 只有前端**该传的**字段 | 收参数 | `RegisterDTO`（不含 id） |
| `vo` | 只有前端**该看的**字段 | 返数据 | `UserVO`（**不含 password**） |

**分开的核心原因**：安全。`entity` 里有密码哈希，如果直接返回给前端就泄露了。
所以所有对外返回都转成 `vo`。

---

## 三、几个关键文件（想读懂就看这几个）

| 文件 | 为什么重要 |
|---|---|
| `CourseHubApplication.java` | 启动入口，所有配置的起点 |
| `config/WebMvcConfig.java` | 拦截器注册 + 登录白名单（哪些接口不用登录） |
| `interceptor/AuthInterceptor.java` | JWT 怎么解析、权限怎么判 |
| `service/impl/FileServiceImpl.java` | 文件上传 9 步流程 + 五重校验 + 秒传 |
| `service/impl/ResourceServiceImpl.java` | 三级可见性校验 + 版本树 |
| `storage/LocalFileStorage.java` | 防路径穿越（`resolveSafely`） |
| `scheduler/ScoreCalculator.java` | 5 因子加权评分公式 |
| `aspect/OperationLogAspect.java` | 审计日志怎么记录的 |

---

## 四、配置文件在哪

```
src/main/resources/
├── application.yml            总配置（端口、上传大小、JWT、评分权重）
├── application-dev.yml        开发环境（数据库地址、Redis 地址、日志级别）
├── application-local.yml      ⚠️ 本地私密配置（数据库密码），不进 Git
├── application-local.yml.example   上面那个的模板
├── application-prod.yml       生产环境
├── mapper/*.xml               ⭐ 全部 SQL 都在这（15 个文件）
└── static/                    前端打包产物（后端负责托管）
```

**想改端口 / 上传大小 / 评分权重** → 改 `application.yml`，改完**重启后端**。

---

## 五、数据库脚本在 `sql/`

| 文件 | 作用 | 会删数据吗 |
|---|---|---|
| `init_all.sql` | **一键建库**（建表 + 基础数据 + 演示课程），630 行 | ⚠️ **会**（开头 `DROP DATABASE`） |
| `schema.sql` | 只建 16 张表 | 不会（`CREATE TABLE IF NOT EXISTS` 风格） |
| `init_data.sql` | 只插基础数据（学院/账号/课程/章节） | 不会 |
| `seed_demo.sql` | 只插演示课程，可重复执行 | 不会 |

**第一次搭建**：跑 `init_all.sql` 一个文件就够。

---

## 六、这些别手动改

| 目录/文件 | 为什么 |
|---|---|
| `target/` | Maven 编译产物，`mvn clean` 会删，改了也会被覆盖 |
| `src/main/resources/static/` | 前端打包产物，会由 `build-to-backend.bat` 整体替换 |
| `upload/`（运行后生成） | 用户上传的物理文件，用 UUID 命名，改名会导致下载 404 |

---

## 七、下一步

- 想启动 → `../docs/手把手教学/02-启动项目.md`
- 想读懂代码 → `../docs/手把手教学/05-数据库与接口.md` + `../docs/技术架构与中间件全景.md`
- 想知道每个文件干什么 → `../docs/项目文件说明.md`
- 报错了 → `../docs/手把手教学/06-故障排查.md`
