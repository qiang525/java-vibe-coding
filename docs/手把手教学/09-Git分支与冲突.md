# 第 ⑨ 册 · Git 与 GitHub 进阶（下篇：分支、协作、冲突）

> **这一册解决三个你点名说过的问题**：
> ① 本地分支和远程分支到底什么关系
> ② 标准协作流程长什么样
> ③ 冲突为什么会发生、怎么解
>
> 如果你现在只想"自己一个人改东西、推上去"，上篇（第 ⑧ 册）就够了。
> 这一册等你需要开分支、或者遇到冲突时再看。

```handbook-nav
上一册：[Git 入门](08-Git与GitHub入门.md) ｜ 索引：[README.md](README.md) ｜ 下一册：无（这是最后一册）
```

---

## 1. 分支：给项目开一个"平行世界"

### 1.1 为什么需要

假设现在网站跑得好好的，你想加一个"个人中心"页面。
但这个功能要改 8 个文件，中间可能有几天是**半成品状态**（功能没做完，网站可能报错）。

如果直接在 `main` 上改：
- 这几天网站是坏的，没法演示
- 改失败了想退回，得一个个文件手动还原

**分支的解法**：复制一个平行世界，在里面随便折腾，`main` 保持完好。

```
main:     ●───●───●───────────────●   ← 一直保持能跑
                   \             /
feature:            ●───●───●───●      ← 在这里做个人中心
```

### 1.2 两个必须记住的概念

| 名字 | 是什么 |
|---|---|
| **main** | 主分支，项目的主时间线。**永远保持能跑** |
| **feature 分支** | 从 main 上开出来的支线，做完再合回去 |

> 以前叫 `master`，2020 年 GitHub 改成 `main` 了。看到老教程说 master，是同一个东西。
> 你的项目已经叫 `main` 了（我之前改过）。

---

## 2. 本地分支 vs 远程分支（你最困惑的地方）

### 2.1 三个"main"不是同一个东西

这是新手最容易懵的。**你的电脑上其实同时有 3 个"main"的概念**：

```
┌─────────────────────────────────────────────────────────────┐
│  GitHub 上的 main                                            │
│  （真正的远程，别人看到的）                                     │
└───────────────────────┬─────────────────────────────────────┘
                        │  git fetch / git pull
                        │  （必须你主动敲，它不会自动同步）
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  origin/main                                                 │
│  （你本机里的一份"远程快照"，只读，你不能直接改它）                 │
└───────────────────────┬─────────────────────────────────────┘
                        │  git merge（pull 会自动做）
                        ↓
┌─────────────────────────────────────────────────────────────┐
│  本地 main                                                   │
│  （你真正在改的那个，git branch 里带 * 的那个）                   │
└─────────────────────────────────────────────────────────────┘
```

**关键认知**：
- `origin/main` 是"上次你联网时，远程长什么样"的**照片**，它不会自己变
- 你不 `fetch`，就算 GitHub 上被别人改了 100 次，你的 `origin/main` 还是旧的
- 所以你看到的 `ahead of 'origin/main' by 4 commits` 是**跟照片比**，不是跟实时远程比

### 2.2 一条命令看懂全部

```
git branch -vv
```

**你会看到**：

```
* main 6cbfd47 [origin/main: ahead 4] docs(teaching): 新增零基础学习路线图...
  ↑     ↑        ↑                    ↑
  │     │        │                    └─ 最新提交的说明
  │     │        └─ 追踪关系 + 领先/落后几个提交
  │     └─ 这个分支指向的最新提交
  └─ 星号 = 你当前在这
```

`[origin/main: ahead 4]` 读作：**"本地 main 追踪远程 origin/main，领先它 4 个提交"**。

可能出现的三种状态：

| 显示 | 意思 | 该做什么 |
|---|---|---|
| `ahead 4` | 本地多了 4 个提交 | `git push` |
| `behind 2` | 远程多了 2 个提交 | `git pull` |
| `ahead 2, behind 1` | 两边都有新东西（**要合并**） | `git pull`（可能冲突） |
| 什么都不显示 | 完全同步 | 什么都不用做 |

### 2.3 `fetch` 和 `pull` 的区别

| 命令 | 干什么 | 类比 |
|---|---|---|
| `git fetch` | 只更新 `origin/main` 这张照片，**不动你的文件** | 去邮局把信取回来，但不拆 |
| `git pull` | fetch + 把远程的改动合并进你本地 | 取信 + 拆开 + 把内容抄进你的本子 |

**稳妥做法**：先 `git fetch`，再 `git status` 看看差多少，确认没问题再 `git pull`。

---

## 3. 分支的 5 条命令

### 3.1 看有哪些分支

```
git branch          ← 本地分支（带 * 的是当前）
git branch -a       ← 本地 + 远程
```

**你会看到**：

```
* main
  remotes/origin/main
```

`remotes/origin/main` 就是那个"远程快照"。

### 3.2 创建并切换到一个新分支

```
git switch -c feature/profile
```

- `-c` = create（创建）
- `feature/profile` 是分支名，习惯用 `类型/简短说明`

**成功后**：

```
Switched to a new branch 'feature/profile'
```

再 `git branch` 会看到：

```
  main
* feature/profile      ← 星号跑过来了
```

> 老教程可能写 `git checkout -b feature/profile`，功能一样。
> `switch` 是 2019 年新增的，专门用于切分支，更不容易搞错。

### 3.3 切换分支

```
git switch main
git switch feature/profile
```

**切换前必须提交或清理改动**，否则会报错（见第 8 节报错 20）。

> ⚠️ **切分支会真实改变你硬盘上的文件**！
> 切到 `feature/profile` 时，文件会变成那个分支的样子；切回 `main` 又变回来。
> 这不是"复制文件"，是"Git 把文件内容换掉"。第一次见会觉得神奇，属正常。

### 3.4 合并分支

**先把目标分支的最新代码拿到手，再合并**：

```
git switch main              ← 站到 main 上
git pull                     ← 确保 main 是最新的
git merge feature/profile    ← 把 feature 合并进 main
```

**成功的两种形态**：

**① Fast-forward（快进）** —— 最理想

```
Updating 6cbfd47..a1b2c3d
Fast-forward
 src/views/Profile.vue | 120 ++++++++++++++
 1 file changed, 120 insertions(+)
```

意思是"main 没动过，直接往前挪一格就到了"。

**② Merge made by the 'recursive' strategy** —— 产生了合并提交

```
Merge made by the 'recursive' strategy.
 src/views/Profile.vue | 120 ++++++++++++++
 1 file changed, 120 insertions(+)
```

意思是"两边都动过，Git 帮你合了一次，自动生成一个合并提交"。

### 3.5 删掉用完的分支

```
git branch -d feature/profile
```

`-d` 只在"已经合并过"时才允许删（安全）。
真删不掉又确定不要了，用 `-D`（大写，强制）。

---

## 4. 标准协作流程（团队里的规范动作）

### 4.1 单人项目（你现在的情况）

```
每天开始：
  git pull                      ← 先同步
改代码...
  git status                    ← 看改了什么
  git add .
  git commit -m "feat: xxx"
  git push                      ← 结束
```

**就这么简单。单人项目不需要分支。**

### 4.2 团队项目 / 自己做一个大功能

```
① git switch main              ← 站到主分支
② git pull                     ← 拉最新的
③ git switch -c feature/xxx    ← 开新分支
④ 改代码...
⑤ git add . && git commit -m "feat: xxx"
⑥ git push -u origin feature/xxx   ← 首次推送，-u 建立追踪
⑦ 去 GitHub 网页开 Pull Request
⑧ 同事 review → 点 Merge
⑨ 本地 git switch main && git pull
⑩ git branch -d feature/xxx    ← 删掉本地分支
```

**为什么要走 PR 而不是直接推 main？**

因为 PR 给了"看一眼再合"的机会：能看到改了哪些行、能让别人评论、能自动跑测试。
直接把半成品推到 main，是团队协作里最招人恨的行为。

### 4.3 `-u` 是什么意思

```
git push -u origin feature/profile
```

- `origin`：推到哪个远程
- `feature/profile`：推哪个分支
- `-u`（= `--set-upstream`）：**建立追踪关系**

建立之后，这个分支以后直接 `git push` / `git pull` 就行，不用再写全。
第一次推新分支必须加，否则会报错（见第 8 节报错 9）。

---

## 5. Pull Request（在 GitHub 网页上点，不用敲命令）

### 5.1 什么是 PR

**PR = Pull Request = "请求把我的分支合并进 main"。**

它是一个**网页上的讨论区 + 合并按钮**，不是 Git 命令。

### 5.2 怎么开

1. 先 `git push -u origin feature/profile` 把分支推上去
2. 打开 `https://github.com/qiang525/java-vibe-coding`
3. 页面顶部会出现一条黄色横条：**"feature/profile had recent pushes"** → 点右边的 **Compare & pull request**
   （没有横条就点 **Pull requests** 标签 → **New pull request**）
4. 填两个框：
   - **title**：`feat: 新增个人中心页面`
   - **description**：写改了什么、为什么改、怎么测试的
5. 点绿色按钮 **Create pull request**

### 5.3 页面上能看到什么

| 标签 | 内容 |
|---|---|
| **Conversation** | 讨论区 + 合并按钮 |
| **Commits** | 这个 PR 包含哪些提交 |
| **Files changed** | **逐行对比**（绿色是新增，红色是删除）—— 最重要 |

### 5.4 怎么合并

1. 确认 `Files changed` 里没有意外的东西（比如密码文件）
2. 点 **Merge pull request** → **Confirm merge**
3. 出现 **Delete branch** 按钮 → 点它（远程分支用完就删）

### 5.5 合并后本地要做什么

```bash
git switch main
git pull                      ← 把刚才合并的结果拉下来
git branch -d feature/profile ← 删本地分支
```

> ⚠️ 别忘了这一步，否则你本地 main 会落后于远程。

---

## 6. 冲突：为什么会发生、怎么解

### 6.1 本质：Git 不知道该听谁的

**冲突只在一个情况下发生**：同一个文件的**同一区域**，被两边改成了**不同的内容**。

| 情况 | 会不会冲突 |
|---|---|
| A 改了第 10 行，B 改了第 50 行 | ❌ 不冲突，自动合并 |
| A 改了第 10 行，B 也改了第 10 行 | ✅ **冲突** |
| A 新建一个文件，B 也新建同名文件 | ✅ 冲突 |
| A 删了文件，B 改了它 | ✅ 冲突 |

**Git 的原则**：能自动合就自动合，合不了就**停下来问你**。它不会瞎猜。

### 6.2 单人项目为什么也会冲突（你的情况）

三种常见场景：

**① 你在 GitHub 网页上直接改过文件，本地也改了同一行**
GitHub 网页上点铅笔图标编辑 → 相当于远程改了一次 → 你本地也改 → `pull` 就冲突。

**② 换电脑工作，忘了先 `pull`**
公司/学校电脑改了并推上去，回家直接改，`push` 被拒，`pull` 就冲突。

**③ 两个分支改了同一处**
`feature` 分支改了 `Home.vue` 第 20 行，`main` 上也改了第 20 行，合并时冲突。

### 6.3 冲突长什么样（真实样子）

你会在文件里看到这些奇怪的标记：

```
<<<<<<< HEAD
<h1>让课程资料 有序、可信、可追溯</h1>
=======
<h1>让课程资料 整理得明明白白</h1>
>>>>>>> feature/profile
```

逐段解释：

| 标记 | 意思 |
|---|---|
| `<<<<<<< HEAD` | 从这里开始，是**你当前分支**的版本 |
| `=======` | 分隔线，下面是**对方**的版本 |
| `>>>>>>> feature/profile` | 到这里结束，是 `feature/profile` 分支的版本 |

**Git 在问你**：这两段你要哪个？还是都要？还是都不要？

### 6.4 解决冲突：命令行四步法

**第 1 步：找出哪些文件冲突了**

```
git status
```

**你会看到**：

```
Unmerged paths:
  (use "git add <file>..." to mark resolution)
	both modified:   src/views/Home.vue
```

`both modified` = 两边都改了。**记住这些文件名。**

**第 2 步：打开文件，手工改**

用记事本 / IDEA 打开那个文件，找到 `<<<<<<<` 标记，**做决定**：

| 你想要 | 怎么改 |
|---|---|
| 保留你的 | 删掉 `=======` 到 `>>>>>>>` 之间的内容，以及三个标记行 |
| 保留对方的 | 删掉 `<<<<<<<` 到 `=======` 之间的内容，以及三个标记行 |
| 两个都要 | 把两段都留着（按你想要的顺序），删掉三个标记行 |
| 都不要 | 整段删掉 |

**目标：文件里不能再有任何 `<<<<<<<`、`=======`、`>>>>>>>` 标记。**

**第 3 步：标记为"已解决"**

```
git add src/views/Home.vue
```

（`git add` 在这里的意思不是"加入暂存区"，而是"我处理完了"。）

**第 4 步：完成合并**

```
git commit -m "merge: 解决 Home.vue 冲突，保留新的标题文案"
```

> 如果是 `git pull` 引起的冲突，这一步就是"完成这次拉取"。

### 6.5 图形化解决（推荐零基础用这个）

**IDEA 的冲突解决器比手动改文件安全得多**，因为它会高亮显示，还能一键选左/选右。

**步骤**：

1. 冲突发生后，IDEA 底部会弹出提示：**"Merge Conflicts"** / **"文件有冲突"** → 点 **Resolve**
2. 或者在左侧文件树里，冲突文件是**红色**的，右键 → **Git** → **Resolve Conflicts...**
3. 弹出三栏窗口：
   - 左栏：你的版本（Local）
   - 右栏：对方的版本（Incoming）
   - 中间：结果（Result）
4. 每处冲突有三个小按钮：
   - **`>>`** 用左边的 → 放到中间
   - **`<<`** 用右边的 → 放到中间
   - **`X`** 两个都不要
5. 处理完所有冲突（左上角会显示 `0 of N` 全部解决）→ 点 **Apply**

**VS Code 更简单**：冲突文件里会直接显示 `Accept Current Change` / `Accept Incoming Change` / `Accept Both Changes` 三个可点的链接，点一下就行。

### 6.6 什么时候必须找人商量

**技术能解决 ≠ 应该自己决定。**

如果冲突出现在这些地方，**必须问写那段代码的人**：

- 业务逻辑（比如"这个价格到底该不该打折"）
- 别人正在做的功能的核心代码
- 你不理解的代码

删掉别人的代码是最容易结仇的操作。**看不懂就问，别硬删。**

### 6.7 冲突演练：10 分钟在临时文件夹里亲眼看一次

**这个演练不会碰你的项目**，在 `D:\` 下建个临时文件夹做。

打开命令行：

```
mkdir D:\git-conflict-demo
cd /d D:\git-conflict-demo
git init
```

依次执行（一行一行敲，每行回车）：

```
echo 第一行 > a.txt
git add .
git commit -m "init"

git switch -c feature
echo 我是feature分支加的 > a.txt
git commit -am "feature 改动"

git switch main
echo 我是main分支加的 > a.txt
git commit -am "main 改动"

git merge feature
```

**最后一条命令会让你看到**：

```
Auto-merging a.txt
CONFLICT (content): Merge conflict in a.txt
Automatic merge failed; fix conflicts and then commit the result.
```

**这就是冲突。** 然后用记事本打开 `D:\git-conflict-demo\a.txt`，你会看到那三行标记。

按 6.4 的方法改掉它（比如保留两行），然后：

```
git add a.txt
git commit -m "解决冲突"
```

**演练完可以整个删掉**：`rmdir /s /q D:\git-conflict-demo`

> 亲手看过一次，以后遇到就不会慌。这 10 分钟值得花。

---

## 7. 永远不要做的 5 件事

| ❌ 别做 | 为什么 |
|---|---|
| `git push -f` 到 main | **强制覆盖远程**，会把别人的提交直接抹掉，且无法恢复。团队里这是严重事故 |
| 提交密码 / 密钥 / token | 一旦推到 GitHub，即使删掉，历史里还在。你的项目就踩过一次（`verify-*.sh` 里的明文密码） |
| 提交 `node_modules` / `target` / `dist` | 几万个文件、几百 MB，仓库直接卡死。`.gitignore` 就是干这个的 |
| 冲突时随手删掉别人的代码 | 你删的可能正是对方在做的功能。看不懂就问 |
| 在 main 上直接做半成品 | main 应该永远能跑。大改动一律开分支 |

### 关于 `.gitignore`

项目根目录有个 `.gitignore` 文件，里面每一行是一个"不要提交"的规则：

```
node_modules/       ← 前端依赖（几万个文件）
target/             ← 编译产物
application-local.yml  ← 你的数据库密码
upload/             ← 用户上传的文件
```

**规则生效的前提**：文件**从来没被提交过**。
如果已经被提交了，加 `.gitignore` 也没用，要先 `git rm --cached 文件名` 移出追踪。

---

## 8. 报错速查表（按报错原文找）

| # | 报错原文 | 意思 | 怎么修 |
|---|---|---|---|
| 1 | `fatal: not a git repository` | 你不在项目目录里 | `cd` 到项目根目录 |
| 2 | `Permission denied (publickey)` | SSH 密钥不对 | 重新配（上篇 4.2） |
| 3 | `! [rejected] main -> main (fetch first)` | 远程有你没有的提交 | `git pull` 再 `git push` |
| 4 | `CONFLICT (content): Merge conflict in xxx` | 冲突了 | 第 6.4 节 |
| 5 | `Please tell me who you are` | 没配用户名邮箱 | `git config --global user.name/email` |
| 6 | `Your local changes would be overwritten by merge` | 有未提交的改动 | 先 `commit`，或 `git stash` |
| 7 | `fatal: refusing to merge unrelated histories` | 两个无关仓库硬合并 | `git pull --allow-unrelated-histories` |
| 8 | `error: pathspec 'xxx' did not match` | 文件名写错/文件不在这 | `git status` 看准确名字 |
| 9 | `fatal: The current branch has no upstream branch` | 新分支第一次推 | `git push -u origin 分支名` |
| 10 | `You are in 'detached HEAD' state` | 切到了某个提交而非分支 | `git switch main` 回去 |
| 11 | `src refspec main does not match any` | 本地还没提交过 | 先 `commit` 再 `push` |
| 12 | `error: failed to push some refs` | 同 3 | `git pull` |
| 13 | `Could not resolve host: github.com` | 网络/DNS 问题 | 换网络、检查代理 |
| 14 | `Host key verification failed` | known_hosts 里没有 GitHub | `ssh -T git@github.com` 输 yes |
| 15 | `Updates were rejected because the remote contains work...` | 同 3 | `git pull` |
| 16 | `fatal: destination path already exists` | clone 到已有目录 | 换个目录名 |
| 17 | `LF will be replaced by CRLF` | 换行符警告 | **无害，忽略** |
| 18 | `warning: adding embedded git repository` | 目录里有嵌套的 `.git` | 删掉内层 `.git` |
| 19 | `fatal: 'xxx' is not a commit` | 分支名写错 | `git branch -a` 看准确名字 |
| 20 | `Your local changes would be overwritten by checkout` | 切换分支会覆盖你的改动 | 先 `commit` 或 `git stash` |

### 补充：`git stash` 是什么（救命用）

场景：你改了一半，突然要切分支，但不想提交半成品。

```
git stash           ← 把当前改动"收进抽屉"
git switch main     ← 现在可以自由切了
git stash pop       ← 回来把抽屉里的东西拿出来
```

`stash` = 临时藏起来，不产生提交记录。适合"改到一半要紧急处理别的事"。

---

## 9. 不想敲命令？图形化工具

### 9.1 IDEA 自带的 Git 面板（推荐，你已经在用 IDEA）

| 操作 | 在哪 |
|---|---|
| 看改了什么 | 左侧 **Commit** 面板（或 `Ctrl + K`） |
| 提交 | 勾选文件 → 填说明 → 点 **Commit** |
| 提交并推送 | 点 **Commit** 旁边的箭头 → **Commit and Push** |
| 拉取 | 右上角蓝色向下箭头 ↓ |
| 推送 | 右上角绿色向上箭头 ↑ |
| 看历史 | 底部 **Git** 标签 → **Log** |
| 切分支 | 右下角分支名 → 点开选 |

> 图形化的本质跟命令完全一样，只是把命令变成了按钮。
> **但建议先手动敲熟几条命令再用** —— 否则出错了不知道卡在哪。

### 9.2 GitHub Desktop（可选）

官网免费下载，界面最傻瓜：左边显示改动，左下角填说明，右上角一个按钮提交+推送。

---

## 10. 下篇自检

| # | 我能做到 | ✅ |
|---|---|---|
| 1 | 说清"本地 main / origin/main / GitHub 上的 main"三者关系 | ☐ |
| 2 | 用 `git branch -vv` 看出领先/落后几个提交 | ☐ |
| 3 | 独立完成：开分支 → 改 → 提交 → 推送 → 开 PR → 合并 → 删分支 | ☐ |
| 4 | 说清冲突产生的三个条件 | ☐ |
| 5 | 在临时目录跑一遍演练，亲眼看到冲突并解决 | ☐ |
| 6 | 用 IDEA 的图形化界面解决一次冲突 | ☐ |
| 7 | 说出"永远不要做的 5 件事" | ☐ |

---

## 11. 一张图记住全部

```
                    ┌── 平时：改完就 push，完事 ──┐
                    │                            │
  单人小改动 ────────┤                            │
                    │                            │
  大功能 / 团队 ─────┤ 开分支 → 改 → commit → push → PR → merge → 删分支
                    │
  遇到报错 ──────────┤ 先 git status，再查第 8 节速查表
                    │
  遇到冲突 ──────────┤ 打开文件 → 删三个标记选内容 → add → commit
                    └────────────────────────────┘
```

---

**下一步**：Git 这块学完，可以回到 `../零基础学习路线图.md`，
按阶段 5（会改）动手加一个功能 —— 那时候你会真的用到 `git switch -c`。
