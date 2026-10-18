# 第 ⑧ 册 · Git 与 GitHub 入门（上篇：能存档、能上传）

> **这一册只解决两件事**：
> ① 改了东西能"存档"，改坏了能回退
> ② 存档能传到 GitHub，换电脑、给别人看都不怕
>
> 分支、协作、冲突在**第 ⑨ 册（下篇）**。先学会走路，再学跑。

```handbook-nav
上一本：[数据库与接口](05-数据库与接口.md) ｜ 下一本：[Git 进阶](09-Git分支与冲突.md) ｜ 索引：[README.md](README.md) ｜ 完整顺序：[../学习顺序.md](../学习顺序.md)
```

> 📍 **学习顺序：第 8 步 / 共 9 步** —— 学完能做到：提交并推送到 GitHub
> 出发前先看：[../零基础学习路线图.md](../零基础学习路线图.md)


---

## 0. 先看你现在的真实情况（不用从零搭建）

打开命令行，进到项目目录（你项目的地址栏敲 `cmd`）：

```
cd /d D:\桌面\Java项目AIGC
git remote -v
```

**你会看到**：

```
origin  git@github.com:qiang525/java-vibe-coding.git (fetch)
origin  git@github.com:qiang525/java-vibe-coding.git (push)
```

这一行信息量很大，翻译成人话：

| 你看到的 | 意思 |
|---|---|
| `origin` | 给远程仓库起的**代号**（默认就叫 origin，你可以理解成"老家"） |
| `git@github.com:...` | 用的是 **SSH 方式**（比密码安全，不用每次输密码） |
| `(fetch)` / `(push)` | 拉取用这个地址、推送用这个地址（通常一样） |

再看一眼分支和状态：

```
git branch
git status
```

**你会看到**：

```
* main
On branch main
Your branch is ahead of 'origin/main' by 4 commits.
  (use "git push" to publish your local commits)
nothing to commit, working tree clean
```

逐句翻译：

| 英文 | 意思 |
|---|---|
| `* main` | 你现在在 `main` 分支上，星号表示"当前所在" |
| `Your branch is ahead of 'origin/main' by 4 commits` | **你本地比 GitHub 上多 4 个存档** |
| `nothing to commit, working tree clean` | 没有未保存的改动，很干净 |

**结论：你的仓库已经配好了，只差最后一步"推上去"。**
所以这一册不是教你"从零建仓库"，是教你**日常怎么用**。

### 顺便验证一下 SSH 能不能用

```
ssh -T git@github.com
```

第一次会问 `Are you sure you want to continue connecting?` → 输 `yes` 回车。

**成功的样子**：

```
Hi qiang525! You've successfully authenticated, but GitHub does not provide shell access.
```

看到 `Hi 你的用户名!` 就说明密钥是对的。**只需要验证一次**，以后都不用管。

---

## 1. Git 和 GitHub 不是一回事（90% 的人一开始就混）

| | **Git** | **GitHub** |
|---|---|---|
| 是什么 | 一个**装在你电脑上的软件** | 一个**网站** |
| 干什么 | 给文件拍快照、记历史 | 存这些快照，让别人也能看 |
| 类比 | **相机** | **网盘相册** |
| 断网能用吗 | 能，全部功能都在本地 | 不能 |
| 你项目里的体现 | `D:\桌面\Java项目AIGC\.git` 这个隐藏文件夹 | `github.com/qiang525/java-vibe-coding` |

**一句话**：Git 负责"拍"，GitHub 负责"存"。你完全可以只用 Git 不用 GitHub（本地存档），但只用 GitHub 不用 Git 是不存在的。

> 顺带说一句：还有 GitLab、Gitee（码云）也是同类网站。
> 命令一模一样，只是网址不同。

---

## 2. 为什么需要 Git（三个你一定会遇到的场景）

### 场景 1：改坏了，想回到昨天

没用 Git 的你：

```
Java项目AIGC
Java项目AIGC_备份
Java项目AIGC_备份2
Java项目AIGC_最终版
Java项目AIGC_最终版_真的
Java项目AIGC_最终版_这次真的
```

而且你根本记不清"最终版"和"最终版2"差在哪。

用 Git 的你：

```
git log --oneline     ← 看到每次改动的说明和时间
git restore 文件名     ← 恢复成上次存档的样子
```

### 场景 2：想试个新功能，怕弄坏现在能跑的版本

Git 的解法是**分支**（第 ⑨ 册讲）：开一条"平行世界"随便折腾，坏了就丢掉，好了再合并回来。

### 场景 3：换电脑 / 给别人看 / 交作业

代码推到 GitHub，换台电脑 `git clone` 一下就全下来了。老师同学打开网址就能看。

---

## 3. 四个区域：这是 Git 最核心的心智模型

**零基础学 Git 卡住，99% 是因为没搞懂这个。** 请务必看完这一节。

### 3.1 一张图

```
你眼前看到的文件                准备清单            本机存档库           云端
┌──────────────┐   git add   ┌──────────┐  git   ┌──────────┐  git  ┌──────────┐
│   工作区      │ ─────────→ │  暂存区   │ commit→│ 本地仓库  │ push→ │  GitHub  │
│ (Working Dir)│            │ (Index)  │        │ (Local)  │       │ (Remote) │
└──────────────┘            └──────────┘        └──────────┘       └──────────┘
      ↑                                                                     │
      └────────────────────────── git pull ──────────────────────────────────┘
```

### 3.2 用"交作业"来理解

| 区域 | 交作业的类比 | 对应什么 |
|---|---|---|
| **工作区** | 你桌面上正在写的那几页纸 | 你用记事本/IDEA 改的文件 |
| **暂存区** | 挑出来准备交的那几页（装进文件袋） | `git add` 之后的状态 |
| **本地仓库** | 你自己留的一份存档（家里柜子） | `git commit` 之后，在 `.git` 文件夹里 |
| **远程仓库** | 交给老师的档案室 | `git push` 之后，在 GitHub 上 |

### 3.3 为什么非要"两步"（add 再 commit）？

这是新手最常问的："为什么不能直接保存？"

因为**一次提交应该是一件事**。你改了 5 个文件，其中 3 个是修 bug，2 个是加功能 ——
你应该分两次提交，而不是打包成一次乱七八糟的提交。

`git add` 就是"挑"：把这次要交的挑出来。`git commit` 是"封袋"：给这一袋贴个标签。

> 如果只改了一个文件，可以偷懒：
> `git commit -am "说明"` —— `-a` 表示"所有已跟踪的改动都自动 add"。
> 但**新建的文件不行**，新建文件必须先 `git add`。

### 3.4 各个区域怎么看

| 想知道什么 | 命令 |
|---|---|
| 工作区有什么没交 | `git status` |
| 暂存区里挑了什么 | `git status`（绿色那部分就是） |
| 本地存档有哪些 | `git log --oneline` |
| 远程有什么 | `git log origin/main --oneline`（需要先 `git fetch`） |

---

## 4. 一次性配置（你这台电脑已经配好了）

**这一节你只需要"知道怎么查"，不需要重新配。** 但换电脑时要照着做一遍。

### 4.1 身份（每次 commit 都会记上你的名字）

```
git config --global user.name
git config --global user.email
```

**你这台电脑显示**：

```
qiang
2359913441@qq.com
```

改的话：

```
git config --global user.name "你的名字"
git config --global user.email "你的邮箱"
```

> `--global` 表示"这台电脑上所有项目都用这个身份"。
> 去掉 `--global` 就只对当前项目生效。

### 4.2 SSH 密钥（不用每次输密码的秘密）

**原理**：你在电脑上生成一对钥匙——公钥和私钥。
公钥交给 GitHub（相当于把你的锁交给门卫），私钥留在自己电脑。
以后连接时，GitHub 用公钥验证"你手里的私钥对不对得上"。

**你这台电脑已经配好了**（上面的 `ssh -T` 验证通过就是证明）。

#### 如果换电脑，怎么重新配

**第 1 步：生成密钥**（一路回车，不用设密码）

```
ssh-keygen -t ed25519 -C "你的邮箱"
```

默认会生成在 `C:\Users\你的用户名\.ssh\id_ed25519`（私钥）和 `.pub`（公钥）。

**第 2 步：复制公钥内容**

```
type C:\Users\liuwe\.ssh\id_ed25519.pub
```

会打印一串以 `ssh-ed25519` 开头的文字，**整段复制**。

**第 3 步：粘贴到 GitHub**

1. 打开 GitHub → 右上角头像 → **Settings**
2. 左侧最下面 → **SSH and GPG keys**
3. 点 **New SSH key**
4. Title 随便写（比如"我的笔记本"），Key 粘贴刚才复制的内容
5. 点 **Add SSH key**

**第 4 步：验证**

```
ssh -T git@github.com
```

看到 `Hi 你的用户名!` 就成功了。

> ⚠️ 私钥文件（没有 `.pub` 的那个）**绝对不能发给任何人**，也不能提交到仓库。

### 4.3 检查远程地址是不是 SSH

```
git remote -v
```

- 以 `git@github.com:` 开头 → SSH，好
- 以 `https://github.com/` 开头 → HTTPS，每次要输密码/token

**HTTPS 换成 SSH**：

```
git remote set-url origin git@github.com:qiang525/java-vibe-coding.git
```

---

## 5. 日常只用 5 条命令（覆盖 90% 的使用）

### 命令 1：`git status` —— 最重要，没有之一

```
git status
```

**什么时候用**：任何时候。改完文件、提交前、推送前、搞不清状况时。

**它回答三个问题**：
1. 我在哪个分支？
2. 有哪些改动还没提交？
3. 我和远程差几个提交？

**三种典型输出**：

**① 干净（没有改动）**

```
On branch main
Your branch is up to date with 'origin/main'.
nothing to commit, working tree clean
```

**② 有改动（红色 = 没 add，绿色 = 已 add）**

```
Changes not staged for commit:
  modified:   README.md          ← 红色：改了但没 add
Changes to be committed:
  new file:   docs/新文档.md      ← 绿色：已 add，等着 commit
```

**③ 领先远程**

```
Your branch is ahead of 'origin/main' by 2 commits.
```

> **养成习惯：敲任何 git 命令前后都 `git status` 一下。**
> 它是你的"仪表盘"，看一眼就知道该干什么。

---

### 命令 2：`git add` —— 把改动放进"准备清单"

**加一个文件**：

```
git add README.md
```

**加一个文件夹里所有改动**：

```
git add docs/
```

**加所有改动**：

```
git add .
```

> ⚠️ `git add .` 很方便，但**会把你不想提交的东西也加进去**
> （比如临时文件、密码文件）。所以提交前一定 `git status` 看一眼绿色清单。

**加错了想撤回来**：

```
git restore --staged 文件名
```

---

### 命令 3：`git commit` —— 存档

```
git commit -m "docs: 补充 Git 使用说明"
```

- `-m` 后面是**这次改动的说明**，用引号包起来
- 没有 `-m` 会弹出编辑器让你写（对新手很麻烦，别用）

**成功的样子**：

```
[main 6cbfd47] docs: 补充 Git 使用说明
 1 file changed, 20 insertions(+), 3 deletions(-)
```

`6cbfd47` 是这个存档的编号（哈希值），`1 file changed` 是改了几个文件。

**提交说明怎么写**（很重要，见第 7 节）。

---

### 命令 4：`git push` —— 上传到 GitHub

```
git push
```

**成功的样子**：

```
Enumerating objects: 12, done.
Counting objects: 100% (12/12), done.
Writing objects: 100% (7/7), 4.21 KiB | 1.20 MiB/s, done.
To github.com:qiang525/java-vibe-coding.git
   3cfaea7..6cbfd47  main -> main
```

最后那行 `3cfaea7..6cbfd47 main -> main` 表示"把本地 main 推到远程 main"。

**第一次推一个新分支**（第 ⑨ 册会用到）：

```
git push -u origin 分支名
```

---

### 命令 5：`git pull` —— 把 GitHub 上的同步下来

```
git pull
```

**什么时候用**：
- 换电脑开始工作前
- 你在 GitHub 网页上直接改过文件
- 团队里别人推了新东西

**成功的样子**：

```
Already up to date.          ← 没有新东西
```
或
```
Updating 3cfaea7..6cbfd47
Fast-forward
 README.md | 20 ++++++++++++++++----
 1 file changed, 16 insertions(+), 4 deletions(-)
```

> ⚠️ **如果你本地有没提交的改动，`git pull` 可能被拒绝**。
> 先 `git status` 看，该 commit 就 commit，该 stash 就 stash（下册讲）。

---

### 五条命令的关系（背这张图就够）

```
改文件
  ↓  git status   ← 看有什么
git add .         ← 挑出来
  ↓  git status   ← 确认绿色清单对不对
git commit -m "" ← 存档
  ↓  git status   ← 确认领先远程 N 个
git push          ← 上传
  ↓
GitHub 上能看到了
```

---

## 6. 第一次完整走一遍（用你现在的 4 个待推送提交）

**这一节是整册的重点，请真的跟着敲。** 你现在正好有 4 个提交等着推。

### 6.1 先看清楚现状

```
cd /d D:\桌面\Java项目AIGC
git status
```

**你应该看到**：

```
Your branch is ahead of 'origin/main' by 4 commits.
```

记住这个数字 **4**。

### 6.2 看一眼这 4 个提交是什么

```
git log --oneline -5
```

**你应该看到**（最近 5 条）：

```
6cbfd47 docs(teaching): 新增零基础学习路线图 + 第 7 册《电脑基本功》
d95e5aa docs(teaching): 新增第 0 册《先认识这些文件》—— 讲清 .bat/.yml/.md 是什么
6db9bd7 docs(overview): 补充 2026-10-17 教学体系与体检结果
8ee2d22 feat(docs): 新增零基础手把手教学手册（6 册）+ 一键脚本 + 项目体检报告
3cfaea7 chore(repo): 推送前清理 —— 移除明文密码、数据卷移到 F 盘、README 加实现状态说明
```

前 4 条是"还没推的"，第 5 条 `3cfaea7` 是远程已有的。

### 6.3 真的改一个文件（制造一次真实的改动）

用记事本打开 `D:\桌面\Java项目AIGC\README.md`（右键 → 打开方式 → 记事本），
在最末尾加一行：

```
<!-- 这是我用 Git 提交的第一行改动 -->
```

保存关闭。

> 用 `<!-- -->` 包起来是因为这是 HTML 注释，不会显示在 GitHub 页面上。
> 你验证完可以删掉。

### 6.4 看状态（红色出现了）

```
git status
```

**你应该看到**：

```
Changes not staged for commit:
  (use "git add <file>..." to update what will be committed)
	modified:   README.md
```

`modified: README.md` 是**红色**的 —— 说明改了但还没 add。

### 6.5 加进暂存区（变绿）

```
git add README.md
git status
```

**现在它变成绿色**：

```
Changes to be committed:
	new file: ...
	modified:   README.md
```

> 红→绿，就是"放进准备清单"的过程。

### 6.6 存档

```
git commit -m "docs: 第一次手动提交测试"
```

**你应该看到**：

```
[main xxxxxxx] docs: 第一次手动提交测试
 1 file changed, 2 insertions(+)
```

现在 `git status` 会显示 `ahead of 'origin/main' by 5 commits`。

### 6.7 推送（关键一步）

```
git push
```

**第一次可能会问**：

```
The authenticity of host 'github.com' can't be established.
Are you sure you want to continue connecting (yes/no)?
```

输 `yes` 回车（只有第一次问）。

**成功后**：

```
To github.com:qiang525/java-vibe-coding.git
   3cfaea7..xxxxxxx  main -> main
```

### 6.8 去 GitHub 网页验证

浏览器打开：

```
https://github.com/qiang525/java-vibe-coding
```

1. 看页面顶部的提交数和时间 —— 应该刚更新
2. 点 **README.md** —— 拉到最底下，应该能看到你加的那行注释
3. 点右上角的 **commits**（或者地址后面加 `/commits/main`）—— 能看到 5 条新提交

**看到了 = 你真的会用 Git 了。**

### 6.9 清理测试改动（可选）

```
git log --oneline -1        ← 拿到刚才那次提交的编号，比如 a1b2c3d
```

然后用记事本把 README 末尾那行注释删掉，再走一遍 add / commit / push。

---

## 7. 提交说明怎么写（别人（和半年后的你）会感谢你）

### 7.1 格式

```
类型: 一句话说明
```

### 7.2 常用类型

| 类型 | 什么时候用 | 例子 |
|---|---|---|
| `feat` | 加新功能 | `feat: 新增个人中心页面` |
| `fix` | 修 bug | `fix: 修复上传大文件时进度条卡住` |
| `docs` | 只改文档 | `docs: 补充 Git 使用说明` |
| `style` | 只改格式（不影响功能） | `style: 统一缩进为 2 空格` |
| `refactor` | 重构（不改功能） | `refactor: 把上传逻辑抽成独立方法` |
| `chore` | 杂事（依赖、配置） | `chore: 升级 Spring Boot 到 2.7.18` |
| `test` | 加测试 | `test: 补充登录接口测试` |

### 7.3 好坏对比

| ❌ 差 | ✅ 好 |
|---|---|
| `update` | `docs: 补充 Git 入门教程` |
| `改了一下` | `fix: 修复课程列表分页参数错误` |
| `111` | `feat: 新增资料下载次数统计` |
| `提交` | `chore: 更新 .gitignore 忽略日志文件` |

**判断标准**：半年后的你，只看这一行，能不能知道做了什么。

---

## 8. 三个救命操作

### 8.1 改坏了想撤销（还没 commit）

```
git restore 文件名
```

把文件恢复成**上次提交时的样子**。⚠️ 你刚改的内容会**永久丢失**。

**只想撤销 add（保留改动）**：

```
git restore --staged 文件名
```

### 8.2 刚提交完发现说明写错了

```
git commit --amend -m "新的说明"
```

只改说明，不改内容。

### 8.3 想看完整历史

```
git log --oneline --graph --all
```

`--graph` 会画出分支线（第 ⑨ 册会用）。

**只看某个人/某段时间**：

```
git log --author="qiang" --oneline --since="2026-10-01"
```

---

## 9. 懒人版：一键提交并推送

不想每次敲三条命令？项目里有个脚本：

**`D:\桌面\Java项目AIGC\tools\git-push.bat`**

双击它，会：
1. 显示当前有哪些改动（`git status`）
2. 让你输入这次改动的说明
3. 自动执行 add → commit → push

> 脚本只是"把三条命令打包"，**原理跟手动敲完全一样**。
> 建议先手动敲熟 3 次，再用脚本偷懒 —— 否则出错了你不知道卡在哪。

---

## 10. 上篇自检（能做到才算会）

| # | 我能做到 | ✅ |
|---|---|---|
| 1 | 说清 Git 和 GitHub 的区别 | ☐ |
| 2 | 画出四个区域（工作区/暂存区/本地/远程）并说出对应命令 | ☐ |
| 3 | 用 `git status` 判断"我在哪个分支、有没有改动、领先远程几个提交" | ☐ |
| 4 | 独立完成一次 add → commit → push，并在 GitHub 网页上看到结果 | ☐ |
| 5 | 写出符合规范的提交说明 | ☐ |
| 6 | 用 `git restore` 撤销一个改坏的文件 | ☐ |
| 7 | 用 `ssh -T` 验证 SSH 是否可用 | ☐ |

**全打勾** → 日常够用了，可以先用着。
**想学分支和团队协作** → 打开 `09-Git分支与冲突.md`。

---

## 附：上篇最常见的 6 个困惑

| 困惑 | 答案 |
|---|---|
| **`git add .` 和 `git commit -am` 有什么区别？** | `add .` 把所有改动（含新建文件）放进暂存区；`commit -am` 只处理**已跟踪**的文件，新建文件它不管 |
| **为什么 commit 了 GitHub 上看不到？** | commit 只在本地存档。**必须 push** 才会上传 |
| **`origin` 是什么？能改名吗？** | 它是远程仓库的代号，默认叫 origin。能改（`git remote rename`），但没必要 |
| **能不能不写提交说明？** | 能（`git commit --allow-empty-message`），但非常不建议，你会后悔 |
| **`.git` 文件夹能删吗？** | **删了就没有全部历史了**，项目会退化成普通文件夹。别动 |
| **提交了密码怎么办？** | 立刻改密码，然后从历史里清除（很麻烦）。**预防：提交前 `git status` 看一眼** |

---

> **下一步（第 9 步）**：[Git 进阶](09-Git分支与冲突.md) —— 开分支、解冲突
