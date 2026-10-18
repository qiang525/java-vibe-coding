# frontend —— Vue 3 前端（Vite + Element Plus + Pinia）

> 前端就干两件事：**画界面** + **发请求拿数据**。
> 已经打包好放进后端了，所以**不改前端的话根本不用管这个目录**。

---

## 一、什么时候需要动它

| 我想 | 需要动前端吗 |
|---|---|
| 看网站效果 | ❌ 不用，后端起起来就有界面 |
| 改页面上的文字/颜色/布局 | ✅ 要 |
| 只在本地临时看效果 | ✅ 跑开发模式（热更新） |
| 让 8080 也变成新样子 | ✅ 打包（双击 `build-to-backend.bat`） |

---

## 二、两个命令

### 开发模式（改代码自动刷新）

```
cd /d D:\桌面\Java项目AIGC\frontend
npm run dev
```

→ 浏览器开 `http://localhost:5173`
→ 改 `src/` 下的文件，保存后页面**自动变**（不用手动刷新）

**前提**：后端也要同时跑着（5173 的 `/api` 请求会转发给 8080）。

### 打包（让 8080 也用新代码）

**推荐双击 `build-to-backend.bat`**，它会自动：
1. `npm run build` 编译压缩
2. 删掉旧的 `backend/src/main/resources/static`
3. 把新的 `dist` 复制过去

手动做的话：

```
npm run build
xcopy /e /i /y dist "..\backend\src\main\resources\static"
```

> ⚠️ 打包完**必须重启后端** + 浏览器 `Ctrl + F5`，否则看到的还是旧页面。

---

## 三、目录分工

```
src/
├── main.js              入口：创建 Vue 应用，注册路由/状态/样式
├── App.vue              根组件：顶部导航 + 内容区 + 页脚
├── router/index.js      ⭐ 路由表：哪个网址对应哪个页面
├── store/               Pinia 状态（登录用户信息存在这）
├── api/                 ⭐ 所有后端请求都在这
│   ├── request.js       Axios 封装：自动加 token、统一处理错误
│   ├── auth.js          登录/注册/我的信息
│   ├── course.js        课程
│   ├── resource.js      资料（含版本、下载）
│   ├── comment.js       评论
│   ├── collection.js    收藏
│   ├── report.js        举报
│   └── user.js          用户
├── views/               ⭐ 一个个页面（9 个）
│   ├── Home.vue             首页
│   ├── Login.vue            登录 / 注册
│   ├── CourseList.vue       课程库
│   ├── CourseDetail.vue     课程详情（含"上传资料"对话框）
│   ├── ResourceList.vue     资料库（全文检索）
│   ├── ResourceDetail.vue   资料详情（下载/版本/评论/举报）
│   ├── MyCollections.vue    我的收藏
│   ├── ReportManage.vue     举报处理（仅管理员）
│   └── NotFound.vue         404
├── components/          可复用组件
│   ├── AppHeader.vue        顶部导航
│   ├── AppFooter.vue        页脚 + 用户下拉菜单
│   ├── UploadDialog.vue     上传资料（选文件 → 上传 → 填信息 三步）
│   ├── VersionDialog.vue    上传新版本
│   ├── VersionHistory.vue   版本树时间线
│   ├── CommentSection.vue   讨论区（两级评论）
│   └── ReportDialog.vue     举报对话框
├── styles/main.css      ⭐ 设计系统：颜色/圆角/阴影/通用类
└── utils/               工具函数
```

---

## 四、三个最常改的地方

| 想改什么 | 改哪个文件 |
|---|---|
| 页面上的文字、按钮、布局 | `src/views/xxx.vue` |
| 颜色、字体、圆角、阴影 | `src/styles/main.css` 顶部的 `:root` 变量 |
| 新增一个页面 | 新建 `views/Xxx.vue` + 在 `router/index.js` 加一条路由 |

### 改主题色（最快见效的实验）

打开 `src/styles/main.css`，最上面：

```css
:root {
  --ch-brand: #4f46e5;      /* 改成 #0ea5e9 就是天蓝色 */
  --ch-brand-2: #7c3aed;
  --ch-brand-3: #a855f7;
}
```

改完保存 → 开发模式（5173）立刻变色。

> ⚠️ **不要在页面里写死颜色**（如 `color: #4f46e5`），
> 要用变量 `color: var(--ch-brand)`，否则换主题时会漏改。

---

## 五、几个必须知道的约定

### 1. 样式导入顺序不能反

`src/main.js` 里必须是：

```js
import 'element-plus/dist/index.css'   // 先：组件库默认样式
import '@/styles/main.css'             // 后：我们的覆盖样式
```

反了的话，主题色覆盖就失效，界面会退回 Element Plus 默认蓝。

### 2. 路由必须用 hash 模式

`router/index.js` 里是 `createWebHashHistory()`，所以网址带 `#`（如 `/#/courses`）。

**这是刻意的**：hash 模式刷新页面不会 404，部署到任何静态服务器都零配置。

### 3. `.env` 文件里 `VITE_` 开头才会被前端读到

```
.env.development   → VITE_API_BASE_URL=（留空，走 proxy）
.env.production    → VITE_API_BASE_URL=（留空，同源）
```

改成后端完整地址时才能跨域部署。**留空 = 请求发到当前域名**。

### 4. 打包产物不要手动改

`dist/` 和 `../backend/src/main/resources/static/` 都是生成物。
你改了源码，重新打包会全部覆盖。

---

## 六、首次使用前

如果 `node_modules` 不存在（或装坏了）：

```
npm install
```

国内建议先换镜像再装（会快很多）：

```
npm config set registry https://registry.npmmirror.com
```

---

## 七、下一步

- 想改东西 → `../docs/手把手教学/04-修改与打包.md`
- 想懂设计系统 → `../docs/前端设计升级说明.md`
- 打包报错 → `../docs/手把手教学/06-故障排查.md` 的 F16 / F18
