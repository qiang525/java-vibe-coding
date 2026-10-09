# CourseHub 项目交付总览

> 更新时间：2026-10-09
> 项目位置：`D:\桌面\Java项目AIGC`
> **项目目标**：通过亲手推进一个真实的 Web 项目，走完整开发流程、补上知识盲区。

---

## 一、项目进度

```
M0 项目骨架    ✅ 完成  ── 前后端跑通，数据库建好
M1 用户与课程  ✅ 完成  ── 能登录、能建课、越权会被拦
M2 资料核心    🔄 进行中
   ├─ M2-1 文件存储与上传   ✅ 完成  ← 本次交付
   ├─ M2-2 资料管理 + 三级可见性
   ├─ M2-3 分片上传（秒传 / 断点续传 / 合并复验）
   ├─ M2-4 中文全文检索
   └─ M2-5 前端资料页与上传组件
M3 协作与治理  ⏳
M4 评分与上线  ⏳
```

**当前规模**：后端 53 个类 / 约 4000 行，前端 15 个文件 / 约 1900 行，文档 5 份。

---

## 二、本次交付：M2-1 文件存储与上传

### 做了什么

| 模块 | 文件 | 能力 |
|---|---|---|
| **存储抽象** | `storage/FileStorage` + `LocalFileStorage` | 面向接口编程，换 MinIO 不改业务代码 |
| **文件工具** | `common/util/FileUtil` | MD5、扩展名、UUID 重命名、文件头校验表 |
| **数据层** | `entity/FileInfo` + `FileMapper` + XML | 按 MD5 查（秒传）、引用计数原地增减 |
| **业务层** | `FileService` + `FileServiceImpl` | **9 步上传流程 + 五重安全校验** |
| **接口层** | `FileController` | 上传 / 元信息 / 下载 / 删除 |

### 五重安全校验（每一步防一类问题）

| 校验 | 防什么 |
|---|---|
| ① 空文件 | 前端漏传、请求截断 |
| ② 大小限制 | 上传超大文件占满磁盘 |
| ③ **扩展名白名单** | 上传 `.exe`/`.jsp` 等可执行文件（白名单优于黑名单） |
| ④ **文件头（魔术数字）** | **改扩展名的伪装文件**（`.pdf` 其实是木马） |
| ⑤ **UUID 重命名 + 路径穿越防护** | `../../windows/system32` 这类攻击、同名覆盖、特殊字符问题 |

### 两个值得理解的设计

**1. 秒传**：上传前先算 MD5，数据库里已有同 MD5 的文件就不重复存储，只把引用计数 +1。500MB 的课件也能"瞬间传完"。

**2. 引用计数**：同一份文件可能被多份资料引用（秒传导致）。删除时必须计数减到 0 才真正删物理文件，否则会连累其他还在用它的资料。

---

## 三、验收证据（实测）

### M2-1 文件模块：25 / 25 通过

| 场景 | 结果 |
|---|---|
| 未登录上传 | ✅ 40101 |
| 正常上传 PDF | ✅ 成功，`instantUpload=false` |
| 同一内容换名再传 | ✅ **秒传命中**，返回同一个文件 ID |
| 上传 `.exe` | ✅ 41003（扩展名白名单拦截） |
| **伪装文件**（`.pdf` 但内容是文本） | ✅ 41003（文件头校验拦截） |
| 无扩展名文件 | ✅ 41003 |
| 查询元信息 | ✅ 不暴露服务器存储路径 |
| 下载比对 | ✅ 内容 MD5 一致、中文文件名正确编码 |
| 删除一次（引用 2→1） | ✅ 文件保留（这正是引用计数的意义） |
| 再删一次（1→0） | ✅ 记录与物理文件彻底清理 |

### 历史验收（未回归但保持）

| 模块 | 结果 |
|---|---|
| M1 认证 | 14 / 14 |
| M1 课程 | 34 / 34 |

### 磁盘实证

```
backend/upload/
├── 2026/10/09/<uuid>.pdf    ← UUID 文件名（用户看不到）
└── temp/                     ← 正常应为空，说明临时文件被正确清理
```

---

## 四、怎么自己跑起来

```bash
# 1. 启动 Redis
双击 E:\java\redis\start.bat

# 2. 启动后端
cd backend
mvn spring-boot:run

# 3. 启动前端（另开窗口）
cd frontend
npm run dev
```

### 一键跑文件模块测试

```bash
cd backend
bash verify-file.sh
```

它会自动：清空测试数据 → 启动 Redis → 启动后端 → 跑 25 项测试 → 清理进程。

### 手工试上传

1. 打开 `http://localhost:8080/doc.html`
2. 用 `admin` / `admin123` 登录，把 Token 填到「全局参数」
3. 找到「04-文件」→「上传文件」
4. 传一个 PDF，观察返回的 `instantUpload` 字段
5. **同一个文件再传一次**，看它变成 `true` —— 这就是秒传

---

## 五、目录结构

```
D:\桌面\Java项目AIGC\
├── README.md  CONTRIBUTING.md  LICENSE  .editorconfig  .gitignore
│
├── backend/
│   ├── pom.xml
│   ├── sql/schema.sql  init_data.sql
│   ├── test-auth.py   + verify-m1.sh        ← M1 认证测试
│   ├── test-course.py + verify-course.sh    ← M1 课程测试
│   ├── test-file.py   + verify-file.sh      ← M2 文件测试
│   ├── upload/                              ← 上传的文件（不入 Git）
│   └── src/main/
│       ├── java/com/nnuist/coursehub/
│       │   ├── common/        result / exception / page / constant / util / annotation
│       │   ├── config/        Cors / Redis / Knife4j / WebMvc
│       │   ├── controller/    Health / Auth / College / Course / File
│       │   ├── service/       User / College / Course / File (+ impl)
│       │   ├── mapper/        User / College / Course / File
│       │   ├── entity/ dto/ vo/
│       │   ├── interceptor/   AuthInterceptor
│       │   ├── storage/       FileStorage / LocalFileStorage   ← M2-1 新增
│       │   └── aspect/ scheduler/   ← 待 M3/M4 填充
│       └── resources/
│           ├── application.yml  -dev.yml  -prod.yml  -local.yml(不传)
│           └── mapper/  UserMapper.xml  CollegeMapper.xml  CourseMapper.xml  FileMapper.xml
│
├── frontend/src/
│   ├── api/ request.js health.js auth.js course.js college.js
│   ├── store/user.js
│   ├── components/AppHeader.vue
│   ├── router/index.js
│   └── views/ Home.vue CourseList.vue Login.vue NotFound.vue
│
├── docs/
│   ├── 项目文件说明.md
│   ├── 技术架构与中间件全景.md
│   ├── 数据库表设计详解.md
│   ├── 接口清单.md
│   └── 文件上传与存储设计.md      ← 本次新增
│
└── tools/fix-java-path/
```

---

## 六、已知问题与技术债

审查时发现的、**尚未处理**的问题（记录在案，后续按优先级处理）：

| # | 问题 | 影响 | 计划 |
|---|---|---|---|
| 1 | 前端课程卡片点不进去（无课程详情页） | 功能不完整 | M2-5 |
| 2 | `/api/courses/mine` 已实现但前端未调用 | 功能闲置 | M2-5 |
| 3 | 前端无课程编辑/删除入口 | 功能不完整 | M2-5 |
| 4 | **Redis 装了但业务代码尚未使用** | 资源闲置 | M2-3（分片上传进度） |
| 5 | 无单元测试（只有接口测试脚本） | 回归靠手工 | M4 |
| 6 | README 还是初期规划版，里程碑编号与实际不符 | 文档失真 | 待修 |
| 7 | `docs/` 缺 需求规格说明、部署说明 | 文档不全 | M4 |
| 8 | 文件 IO 放在 `@Transactional` 内 | 事务偏长 | 已在文档标注权衡 |

---

## 七、下一步

**M2-2：资料管理 + 三级可见性**

要做的事：
1. `Resource` 实体 + Mapper（把上传的文件"挂"到课程/章节下）
2. 资料列表（走 `idx_course_status_score` 组合索引，验证索引效果）
3. 三级可见性：`PUBLIC` 全平台 / `COURSE_ONLY` 仅本课程 / `PRIVATE` 仅自己
4. **数据权限**：改 URL 里的资料 ID 看不到别人的私密资料

做完你能看到：**上传文件 → 发布成资料 → 按课程浏览 → 私密资料被正确保护**。

---

## 八、需要你配合的事

1. **Java PATH 修复脚本**还没执行（不影响 Maven/IDEA，但命令行 `java` 会崩）
   → 双击 `tools\fix-java-path\run-fix.bat`
2. **GitHub 仓库**还没推（本地已有 8 个提交）
   → 建好仓库把 URL 给我即可
