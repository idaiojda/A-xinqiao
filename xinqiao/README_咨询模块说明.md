# 咨询模块集成说明

本说明概述了在现有应用中启用 Jetpack Compose、集成“咨询”模块（聚合专业咨询入口与 AI 浮窗对话）的关键点与使用方法。

## 1. Gradle 配置与依赖
- 在 `app/build.gradle.kts` 中启用 Compose：
  - `buildFeatures { compose = true }`
  - 引入 Compose BOM 与相关依赖：`compose.ui`、`compose.material3`、`activity.compose`、`lifecycle.viewmodel.compose` 等。
- 版本管理在 `gradle/libs.versions.toml` 中新增：`composeBom`、`activityCompose`、`lifecycleCompose`、`kotlin` 等。

## 2. 主要类与职责
- `ConsultationView`（Java）：
  - 位于 `com.example.xinqiao.view`，默认加载占位布局 `consultation_placeholder_layout.xml`。
  - 通过 `FloatingAiWindowManager` 挂载 AI 悬浮按钮与对话浮窗。
  - 公开 `showView()` 与 `hideView()` 以在主界面切换时保留/隐藏浮窗。
- `FloatingAiWindowManager`（Kotlin）：
  - 位于 `com.example.xinqiao.consultation`。
  - 使用 `WindowManager + Compose` 实现悬浮按钮与对话窗，支持拖拽吸附、底部安全区限制（96dp）、手势缩放（0.8x~1.4x）。
  - 通过 `attach(stateVM, chatVM)` 显示悬浮按钮；`detach()` 可移除。
- `AiFloatingStateViewModel`：
  - 使用 `SavedStateHandle` 持久化窗口状态（展开/最小化、位置、缩放、尺寸）。
- `ChatViewModel`：
  - 封装 `DeepSeekClient` 调用，提供会话消息流与加载态，支持“结束咨询”后 3 秒跟进提示。

## 3. 主界面集成
- `MainActivity` 将原“AI助手”入口替换为“咨询”入口：
  - 导航标题由 `AI助手` 改为 `咨询`。
  - `case 4` 加载 `ConsultationView` 并将其视图添加至 `FrameLayout` 容器。
  - 隐藏逻辑调用 `ConsultationView.hideView()`，以保留浮窗状态。

## 4. 布局文件
- 新增 `app/src/main/res/layout/consultation_placeholder_layout.xml`：
  - 简单的标题与预约按钮占位，后续可替换为真实咨询师列表、预约入口等。

## 5. 使用建议
- 若需要在非“咨询”页面隐藏悬浮窗，可在页面切换时调用 `floatingManager.detach()`（当前设计为保留状态，体验更连贯）。
- 网络调用与重试逻辑复用 `DeepSeekClient`，如需切换模型或优化提示词，请集中修改该类。

## 6. 注意事项
- Compose 相关依赖需与 AGP/Kotlin 版本兼容，若构建报版本冲突，请检查 BOM 与插件版本。
- 在部分设备上，`WindowManager` 叠加层的权限或表现可能不同；当前方案使用 `TYPE_APPLICATION`，避免系统级权限需求。