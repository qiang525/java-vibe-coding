# 贡献指南

感谢你愿意为 CourseHub 出一份力。这份文档说明如何规范地提交代码。

## 1. 开发环境

| 软件 | 版本 |
|---|---|
| JDK | 11 |
| Maven | 3.6+ |
| MySQL | 8.0+（FULLTEXT ngram 依赖 8.0） |
| Redis | 5.0+ |
| Node.js | 16+ |

## 2. 分支模型

```
main        生产分支，只接受合并，每次打 tag
dev         集成分支，所有 PR 的目标分支
feature/xx  功能分支，如 feature/upload-chunk
fix/xx      修复分支，如 fix/search-empty-result
```

**规则**
- 禁止直接向 `main` 提交
- 新分支从 `dev` 切出
- 分支命名用英文小写 + 连字符

## 3. 提交信息规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**type 取值**

| type | 用途 |
|---|---|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档变更 |
| `style` | 格式调整（不改变逻辑） |
| `refactor` | 重构（既非 feat 也非 fix） |
| `perf` | 性能优化 |
| `test` | 测试 |
| `chore` | 构建/工具/杂项 |

**示例**

```
feat(resource): 新增资料分片上传与秒传接口

- 实现 /file/upload/init、/chunk、/merge 三个接口
- 基于 MD5 哈希实现秒传，Redis 记录已上传分片索引
- 合并后重新校验全文件哈希，防止静默损坏

Closes #23
```

**注意**：subject 用中文或英文都可以，但**必须是祈使句、首字母小写、结尾不加句号**。

## 4. 提交频率

- 每天至少 push 一次，避免本地积累大量改动
- 一个 commit 只做一件事，不要把"加功能"和"改格式"混在一起
- 上课/实验当天的工作当天提交，**不要攒到周末**

## 5. 提交流程

1. 同步远端：`git checkout dev && git pull`
2. 建分支：`git checkout -b feature/你的模块`
3. 开发 + 自测（`mvn clean package` 必须通过）
4. 提交：`git add . && git commit -m "feat(xxx): ..."`
5. 推送：`git push origin feature/你的模块`
6. 创建 PR 到 `dev`，描述里写清：改了什么、为什么改、怎么测
7. 至少 1 人 Review 通过后合并

## 6. 代码规范

### 6.1 编码格式

由 `.editorconfig` 统一，IDEA 建议开启 `File → Settings → Editor → Code Style → Enable EditorConfig support`。

- 缩进 4 空格
- 文件编码 UTF-8
- 行尾 LF
- 文件末尾保留一个空行

### 6.2 分层约束（不可违反）

| 层 | 可以做 | 不可以做 |
|---|---|---|
| Controller | 收参、`@Valid` 校验、调 Service、返回 `Result` | 写业务逻辑、直连 Mapper |
| Service | 业务逻辑、事务、权限校验、跨表编排 | 拼 SQL、碰 HttpServlet |
| Mapper | 单表 CRUD、复杂查询 | 写业务判断 |
| Entity | 仅数据库映射 | 出现在 Controller 返回值中 |

**Entity 不得直接返回给前端**——必须转成 VO，否则会泄露 `password`、`is_deleted` 等字段。

### 6.3 注释要求

- 类注释：说明这个类解决什么问题
- 方法注释：说明**为什么**这么做，而不是复述代码
- 关键算法（如可信度评分公式）必须写清计算逻辑与权重来源
- 被注释掉的代码一律删掉，不要留着

**反例**：
```java
// 遍历列表
for (Resource r : list) {
    // 计数加一
    count++;
}
```

**正例**：
```java
// 下载量采用 Redis 累加而非直接更新数据库：
// 热门资料的 download_count 是单行热点，并发直接 UPDATE 会造成行锁竞争，
// 极端情况下拖垮整个资源表。改为内存累加 + 定时批量落库。
redisTemplate.opsForValue().increment(DOWNLOAD_COUNT_KEY + resourceId);
```

### 6.4 异常处理

- 业务异常抛 `BizException`，由 `GlobalExceptionHandler` 统一转成响应体
- **禁止** `try-catch` 后 `e.printStackTrace()` 了事
- **禁止**把异常堆栈直接返回前端
- 日志用 `log.error("上下文描述, resourceId={}", id, e)`，不要裸打印

### 6.5 SQL 规范

- MyBatis 中用户输入一律用 `#{}`，**严禁 `${}`**（SQL 注入）
- 单表查询字段不超过 8 个时可直接写，多表/带条件优先写 XML
- 列表查询必须带 `LIMIT`，且总数上限 20 页
- 涉及多表写入的方法必须加 `@Transactional`

## 7. 测试要求

- 新增/修改 `Service` 层核心逻辑，需补充单元测试
- 目标覆盖率：核心 Service ≥ 50%
- 提交前本地跑通：`mvn clean package`（含测试）

```bash
mvn clean package
java -jar backend/target/coursehub-1.0.0.jar
```

## 8. 文档更新

以下情况**必须**同步更新文档：

- 新增接口 → 更新 `docs/API文档.md`
- 修改表结构 → 更新 `docs/数据库设计.md`
- 新增功能模块 → 更新 `README.md` 的功能模块章节
- 修改启动方式 → 更新 `README.md` 的快速开始

**文档是代码的一部分**，不是事后补的。

## 9. 提问

遇到不确定的先查：
1. 项目 `docs/` 目录下的三份文档
2. 代码里相似功能的现有实现
3. 搜索引擎 + 官方文档

仍未解决再问，问的时候**把你已经试过什么一起说清楚**。

---

再次感谢贡献！
