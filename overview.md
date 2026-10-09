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
   ├─ M2-1 文件存储与上传   ✅ 完成
   ├─ M2-2 资料管理 + 三级可见性  ✅ 完成
   ├─ M2-3 分片上传 + 断点续传   ✅ 完成  ← 本次交付
   ├─ M2-4 中文全文检索
   └─ M2-5 前端资料页与上传组件
M3 协作与治理  ⏳
M4 评分与上线  ⏳
```

**当前规模**：后端 69 个类 / 约 6300 行，前端 15 个文件 / 约 1900 行，文档 7 份。

---

## 二、M2-1 文件存储与上传（已交付）

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

## 三、M2-3 分片上传 + 断点续传（本次交付）

### 做了什么

| 模块 | 文件 | 能力 |
|---|---|---|
| **业务层** | `ChunkUploadService` + `ChunkUploadServiceImpl` | Redis 记进度、合并复验、并发去重 |
| **接口层** | `FileChunkController` | check / upload / merge 三个接口 |

### 三个接口的配合

```
① GET  /api/files/chunk/check    → 是否秒传 / 拿已传分片列表
② POST /api/files/chunk/upload   → 循环上传每个分片（5MB 一片）
③ POST /api/files/chunk/merge    → 合并 + 全文件 MD5 复验
```

### Redis 终于派上用场了

在这之前，项目的 Redis 只有配置类、没有一行业务代码用它。现在它承担了：

```
key:   coursehub:upload:chunk:{文件MD5}
type:  Set（天然去重，重传同一分片不会重复计数）
TTL:   24 小时（用户传一半走了，自动清理）
```

**为什么放 Redis 不放数据库**：上传进度是临时状态、写入极频繁（500MB 文件要写 100 次）、
而且需要自动过期。这三点都指向 Redis 而不是 MySQL。

### 三个关键设计

**1. 断点续传**：传了一半关掉浏览器，第二天打开 → 前端先调 `check`
拿到 `uploadedChunks=[0,1,2]` → 只补传 3 号之后的分片。

**2. 合并后的 MD5 复验**（最不该省的一步）
前端算的是"原始文件"的 MD5，服务端算的是"合并后文件"的 MD5。
两者不一致 → 说明有分片损坏 / 乱序 / 截断 → **拒绝入库**。
不做这一步，用户会拿到一个"看着正常但打不开"的坏文件。

**3. hash 格式校验其实是安全防线**
`fileHash` 会被拼进临时目录路径，限定成 32 位十六进制后，
`../../windows/system32` 这类路径穿越攻击就彻底不可能了。

---

## 四、M2-2 资料管理 + 三级可见性

### 做了什么

| 模块 | 文件 | 能力 |
|---|---|---|
| **数据层** | `Resource` + `ResourceVersion` + 两个 Mapper + XML | 多表 JOIN、**可见性过滤写在 SQL 里** |
| **业务层** | `ResourceService` + `ResourceServiceImpl` | 三级可见性校验、修改/删除权限判断、N+1 优化 |
| **接口层** | `ResourceController` | 列表 / 详情 / 发布 / 修改 / 删除 / 下载 |

### 三级可见性（本模块核心）

| 级别 | 谁能看 |
|---|---|
| `PUBLIC` | 所有人，含未登录游客 |
| `COURSE_ONLY` | 本课程成员 + 课程负责人 |
| `PRIVATE` | 仅上传者本人 |

另外：**上传者本人和管理员**对所有级别都有访问权。

### 三个值得理解的设计

**1. 权限过滤写在 SQL 里**
不是"查出来再在 Java 里过滤"。这样数据库直接少返回数据；
而且万一业务代码漏写判断，SQL 这层还兜着。

**2. `currentUserId` 由服务端注入**
```java
query.setCurrentUserId(UserContext.getUserId());   // 前端传什么都会被覆盖
```
否则用户传个 `currentUserId=999` 就能看别人的资料。

**3. 发布资料是"两步走"**
先上传文件拿 `fileId`，再发布资料。这样大文件可以先慢慢传，
秒传时第二步只需毫秒；文件模块和资料模块的职责也清晰。

### 顺带修掉一个性能问题（N+1）

资料列表要显示"你能不能改这条"，需要知道课程负责人是谁。
循环里逐条查课程 → 一页 10 条就是 10 次查询，这是 **N+1 问题**。
改成**一次批量查询 + 内存映射**后，不管一页多少条都只有 2 次数据库往返。

---

## 五、验收证据（实测）

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

### M2-2 资料模块：30 / 30 通过

| 场景 | 结果 |
|---|---|
| 未登录看 PUBLIC 资料 | ✅ 可见 |
| **学生看别人的 PRIVATE 资料** | ✅ 40303 拦截 |
| **列表里不出现别人的 PRIVATE** | ✅ |
| **非课程成员看 COURSE_ONLY** | ✅ 40304 拦截 |
| 加入课程后再看 COURSE_ONLY | ✅ 可见 |
| **学生改 / 删管理员的资料** | ✅ 40301 拦截，数据未被改动 |
| 发布 3 份后课程资料数 | ✅ 3 |
| 删除后资料数 | ✅ 2 |

### M2-3 分片上传：26 / 26 通过（使用真实 50MB 文件）

测试文件：`E://文件办公//PPT//文体活动.pptx`（47.95 MB）

| 场景 | 结果 |
|---|---|
| 上传前检查（全新上传） | ✅ exists=false |
| **先传 3 片 → 查进度** | ✅ 返回 `[0,1,2]`（**断点续传生效**） |
| 补传剩余 7 片 | ✅ 耗时 2.7 秒 |
| 合并 + MD5 复验 | ✅ 成功 |
| **下载回来比对 MD5** | ✅ 与原文件完全一致 |
| 再次检查（秒传） | ✅ exists=true，返回同一 fileId |
| 用该文件发布资料 | ✅ 串通 M2-2 |
| 非法哈希（路径穿越） | ✅ 40001 拦截 |
| 临时分片目录 | ✅ 合并后自动清理 |

### 历史验收（未回归但保持）

| 模块 | 结果 |
|---|---|
| M1 认证 | 14 / 14 |
| M1 课程 | 34 / 34 |
| M2-1 文件 | 25 / 25 |

### 磁盘实证

```
backend/upload/
├── 2026/10/09/<uuid>.pdf    ← UUID 文件名（用户看不到）
└── temp/                     ← 正常应为空，说明临时文件被正确清理
```

---

## 六、怎么自己跑起来

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

## 七、目录结构

```
D:\桌面\Java项目AIGC\
├── README.md  CONTRIBUTING.md  LICENSE  .editorconfig  .gitignore
│
├── backend/
│   ├── pom.xml
│   ├── sql/schema.sql  init_data.sql
│   ├── test-auth.py   + verify-m1.sh        ← M1 认证测试
│   ├── test-course.py + verify-course.sh    ← M1 课程测试
│   ├── test-file.py   + verify-file.sh      ← M2-1 文件测试
│   ├── test-resource.py + verify-resource.sh ← M2-2 资料测试
│   ├── test-chunk.py  + verify-chunk.sh     ← M2-3 分片上传测试（用真实 50MB 文件）
│   ├── upload/                              ← 上传的文件（不入 Git）
│   └── src/main/
│       ├── java/com/nnuist/coursehub/
│       │   ├── common/        result / exception / page / constant / util / annotation
│       │   ├── config/        Cors / Redis / Knife4j / WebMvc
│       │   ├── controller/    Health / Auth / College / Course / File / FileChunk / Resource
│       │   ├── service/       User / College / Course / File / ChunkUpload / Resource (+ impl)
│       │   ├── mapper/        User / College / Course / File / Resource / ResourceVersion
│       │   ├── entity/ dto/ vo/
│       │   ├── interceptor/   AuthInterceptor
│       │   ├── storage/       FileStorage / LocalFileStorage
│       │   └── aspect/ scheduler/   ← 待 M3/M4 填充
│       └── resources/
│           ├── application.yml  -dev.yml  -prod.yml  -local.yml(不传)
│           └── mapper/  UserMapper.xml  CollegeMapper.xml  CourseMapper.xml
│                        FileMapper.xml  ResourceMapper.xml  ResourceVersionMapper.xml
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
│   ├── 文件上传与存储设计.md
│   ├── 资料管理与权限设计.md
│   └── 分片上传设计.md            ← 本次新增
│
└── tools/fix-java-path/
```

---

## 八、已知问题与技术债

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

## 九、下一步

**M2-4：中文全文检索**

要做的事：
1. 用 MySQL 的 **ngram 全文索引**实现资料搜索
2. 对比 `MATCH ... AGAINST` 和 `LIKE '%关键词%'` 的性能差距（用 EXPLAIN 看）
3. 搜索结果按可信度评分排序
4. 加搜索热词缓存（Redis）

做完你能看到：**搜「数据结构」能命中「数据结构与算法」**，
而且数据量大时不会像 `LIKE` 那样全表扫描。

---
