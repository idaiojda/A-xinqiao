# 心桥心理管理员端系统

基于 Vue 3 + Element Plus + TypeScript 的心理咨询管理后台系统。

## 技术栈

- **前端框架**: Vue 3.3.8
- **UI 组件库**: Element Plus 2.4.2
- **状态管理**: Pinia 2.1.7
- **路由管理**: Vue Router 4.2.5
- **数据可视化**: ECharts 5.4.3
- **构建工具**: Vite 5.0.0
- **编程语言**: TypeScript 5.2.0
- **样式**: SCSS + Tailwind CSS

## 功能模块

### 核心管理模块
- 👥 **用户管理**: 用户CRUD操作、状态管理、权限控制
- 👨‍⚕️ **咨询师审核**: 咨询师注册审核、资质认证

### 内容管理模块
- 📝 **内容审核**: 心理文章内容审核与管理
- 📊 **测评管理**: 心理测评问卷管理

### 系统维护模块
- 📈 **数据报表**: 数据统计与可视化展示
- 🔍 **系统监控**: 系统运行状态监控
- 📁 **档案管理**: 用户档案与文件管理

## 快速开始

### 安装依赖
```bash
npm install
```

### 开发环境
```bash
npm run dev
```

### 生产构建
```bash
npm run build
```

### 代码检查
```bash
npm run lint
npm run type-check
```

## 项目结构

```
src/
├── api/          # API接口服务
├── assets/       # 静态资源
├── components/   # 公共组件
├── composables/  # Vue组合式函数
├── layouts/      # 布局组件
├── router/       # 路由配置
├── stores/       # Pinia状态管理
├── types/        # TypeScript类型定义
├── utils/        # 工具函数
└── views/        # 页面组件
```

## 默认登录信息

- 用户名: admin
- 密码: 任意密码（演示模式）

## 部署

支持 Vercel 一键部署，已配置好部署文件 `vercel.json`。

## 特性

✅ 响应式设计，支持移动端
✅ TypeScript 类型安全
✅ 组件化开发
✅ 权限管理系统
✅ 数据可视化
✅ 主题切换支持
✅ 国际化支持