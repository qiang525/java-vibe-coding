# CourseHub 项目交付总览

> 更新时间：2026-10-09
> 当前阶段：**M1 用户与课程 —— 已完成并实测通过**
> 项目位置：`D:\桌面\Java项目AIGC`

---

## 一、现在项目到什么程度了

```
M0 项目骨架    ✅ 完成   ── 能跑起来
M1 用户与课程  ✅ 完成   ── 能登录、能建课、能越权拦截
M2 资料核心    ⏳ 下一步 ── 上传下载、分片、秒传、检索
M3 协作与治理  ⏳
M4 评分与上线  ⏳
```

一句话：**现在可以真正登录进系统、浏览课程、创建课程了**，而且越权访问会被拦截。

---

## 二、M1 交付了什么

### 2.1 后端（29 → 43 个类）

| 模块 | 文件 | 能力 |
|---|---|---|
| **认证** | `AuthController` + `UserService` + `JwtUtil` + `UserContext` | 登录、注册、获取当前用户、退出 |
| **鉴权** | `AuthInterceptor` + `@RequireLogin` + `@RequireRole` + `WebMvcConfig` | 拦截器统一校验，注解式声明权限 |
| **学院** | `CollegeController` + `CollegeService` + `CollegeMapper` | 学院列表（免登录） |
| **课程** | `CourseController` + `CourseService` + `CourseMapper` | 列表/详情/增删改/加入/退出 |

### 2.2 前端（9 → 17 个文件）

| 文件 | 作用 |
|---|---|
| `store/user.js` | **Pinia 全局登录状态**（新增） |
| `api/auth.js` `course.js` `college.js` | 业务接口封装（新增） |
| `components/AppHeader.vue` | 公用顶部导航，登录状态自动同步（新增） |
| `views/CourseList.vue` | **课程库页面**：列表 + 搜索 + 筛选 + 分页 + 新建（新增） |
| `views/Login.vue` | 改造为登录/注册双 tab，接真实接口 |
| `views/Home.vue` | 改用公用导航组件 |
| `router/index.js` | 新增 `/courses` 路由 |

### 2.3 文档（4 → 7 份）

| 文档 | 内容 |
|---|---|
| `docs/技术架构与中间件全景.md` | 每个技术组件发挥什么作用（零基础友好） |
| `docs/数据库表设计详解.md` | **16 张表逐字段说明 + ER 图**（新增） |
| `docs/接口清单.md` | **15 个接口的参数/返回/错误码**（新增） |
| `docs/项目文件说明.md` | 每个文件干什么（已同步更新） |

---

## 三、验收证据（实测）

### 3.1 M1 认证模块：14 / 14 通过

| 测试 | 结果 |
|---|---|
| 正确账号登录 → 返回 Token | ✅ |
| 用户信息**不含密码** | ✅ |
| 密码错误 / 账号不存在 → 同一提示（防枚举） | ✅ |
| 参数校验、未登录拦截、伪造 Token 拦截 | ✅ |
| 带 Token 访问、退出登录 | ✅ |

### 3.2 M1 课程模块：34 / 34 通过

| 测试 | 结果 |
|---|---|
| 学院列表、课程列表（免登录浏览） | ✅ |
| 创建课程（自动成为负责人 + 成员数=1） | ✅ |
| 课程详情（学院名 JOIN 带出） | ✅ |
| 中文关键词搜索 | ✅ |
| **学生越权改别人的课程 → 40301** | ✅ |
| **学生越权删别人的课程 → 40301** | ✅ |
| **重复加入 → 40906** | ✅ |
| 负责人退出自己的课程 → 40904 | ✅ |
| 删除后查详情 → 40403 | ✅ |

### 3.3 前端构建

```
✓ 1681 modules transformed
✓ built in 5.36s
dist/assets/CourseList-*.js        6.53 kB  ← 路由懒加载生效
dist/assets/AppHeader-*.js         2.20 kB  ← 组件独立分块
dist/assets/user-*.js              0.91 kB  ← Pinia store 独立分块
```

---

## 四、两个值得写进答辩的设计点

### 1. 防用户枚举

登录失败时，**"账号不存在"和"密码错误"返回完全相同的提示**。

如果两者提示不同，攻击者就能靠提示差异批量试探出"哪些账号真实存在"，进而做定向爆破。统一提示后，他无法区分。

### 2. 功能权限 vs 数据权限（本次重点）

| 类型 | 管什么 | 实现位置 |
|---|---|---|
| **功能权限** | 你能不能进这个接口 | 拦截器 + `@RequireLogin` / `@RequireRole` |
| **数据权限** | 你能不能动**这条**数据 | **Service 层**按数据归属判断 |

**为什么缺一不可**：改课程接口只要求"登录"，任何登录用户都能调。如果不在 Service 校验 `course.ownerId == 当前用户`，用户只要改一下 URL 里的课程 ID，就能改掉别人的课程 —— 这是典型的**水平越权漏洞**。

本次已实测：学生账号尝试修改/删除管理员的课程，**返回 40301，数据未被改动**。

---

## 五、本次踩的坑（都已解决）

| # | 问题 | 原因 | 解决 |
|---|---|---|---|
| 1 | 应用启动失败 | `type-aliases-package` 只注册了 `entity` 包，XML 里 `resultType="CourseVO"` 解析不到 | 改成 `entity,vo` 两个包（逗号分隔） |
| 2 | 修改课程返回 40001 | PUT 是全量更新语义，测试只传了部分字段，`name` 的 `@NotBlank` 校验未过 | 测试脚本补上必填字段；**前端编辑时也要传完整字段** |
| 3 | 中文搜索测试失败 | 测试脚本里中文未做 URL 编码 | 用 `urllib.parse.quote` 编码 |
| 4 | 测试时后端进程被回收 | 后台任务结束后子进程被清理 | 改为**同一条命令内**完成"启动→测试→清理" |

> 前 3 个是代码/测试问题，第 4 个是环境问题。M0 阶段的 6 个坑记录在本文件历史版本中。

---

## 六、目录结构（当前）

```
D:\桌面\Java项目AIGC\
├── README.md  CONTRIBUTING.md  LICENSE  .editorconfig  .gitignore
│
├── backend/
│   ├── pom.xml
│   ├── sql/schema.sql  init_data.sql
│   ├── test-auth.py  verify-m1.sh        ← 认证模块测试
│   ├── test-course.py  verify-course.sh  ← 课程模块测试
│   └── src/main/
│       ├── java/com/nnuist/coursehub/
│       │   ├── CourseHubApplication.java
│       │   ├── common/        result / exception / page / constant / util / annotation
│       │   ├── config/        Cors / Redis / Knife4j / WebMvc
│       │   ├── controller/    Health / Auth / College / Course
│       │   ├── service/       User / College / Course (+ impl)
│       │   ├── mapper/        User / College / Course
│       │   ├── entity/ dto/ vo/
│       │   ├── interceptor/   AuthInterceptor
│       │   └── aspect/ scheduler/ storage/   ← 待 M2-M4 填充
│       └── resources/
│           ├── application.yml  -dev.yml  -prod.yml  -local.yml(不传)
│           └── mapper/  UserMapper.xml  CollegeMapper.xml  CourseMapper.xml
│
├── frontend/
│   └── src/
│       ├── api/         request.js  health.js  auth.js  course.js  college.js
│       ├── store/       user.js
│       ├── components/  AppHeader.vue
│       ├── router/      index.js
│       └── views/       Home.vue  CourseList.vue  Login.vue  NotFound.vue
│
├── docs/
│   ├── 项目文件说明.md
│   ├── 技术架构与中间件全景.md
│   ├── 数据库表设计详解.md
│   └── 接口清单.md
│
└── tools/fix-java-path/
```

---

## 七、怎么在本机跑起来

```bash
# 1. 启动 Redis
双击 E:\java\redis\start.bat

# 2. 启动后端
cd backend
mvn spring-boot:run
# 看到 "Started CourseHubApplication" 即成功

# 3. 启动前端（另开一个窗口）
cd frontend
npm run dev
```

浏览器打开 `http://localhost:5173`：

1. 首页能看到"后端连接正常"绿色卡片
2. 点右上角「登录」→ 用 `admin` / `admin123`
3. 登录后导航栏变成用户菜单
4. 点「课程」→ 看到课程列表，可以搜索、筛选、新建课程
5. 打开 `http://localhost:8080/doc.html` 可以直接调试所有接口

**测试账号**

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123` |
| 教师 | `teacher01` | `123456` |
| 学生 | `2025210101` | `123456` |

---

## 八、下一步（M2：资料核心）

| 任务 | 说明 |
|---|---|
| 1 | `FileStorage` 接口 + 本地实现（预留 MinIO） |
| 2 | 文件上传（单文件 + 校验类型/大小/魔术数字） |
| 3 | **分片上传三件套**：秒传（MD5） / 断点续传（Redis 记分片） / 合并后全文件哈希复验 |
| 4 | 资料 CRUD + 三级可见性（PUBLIC / COURSE_ONLY / PRIVATE） |
| 5 | 资料列表（走 `idx_course_status_score` 组合索引） |
| 6 | 中文全文检索（`MATCH ... AGAINST`） |
| 7 | 前端：资料列表页 + 上传组件 |

完成后，你就能**真正上传课件、搜索资料、下载文件**了。

---

## 九、需要你配合的事

1. **Java PATH 修复脚本**还没执行（不影响 Maven/IDEA，但命令行 `java` 命令会崩）
   → 双击 `tools\fix-java-path\run-fix.bat`
2. **GitHub 仓库**还没推（本地已有 6 个提交）
   → 建好仓库把 URL 给我即可
