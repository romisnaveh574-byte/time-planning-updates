# Global Audit Fixes Implementation Plan

> **For agentic workers:** Execute this plan task-by-task with verification after each task.

**Goal:** 修复全局审计发现的 12 个 UI、交互、AI 任务和配置存储问题。

**Architecture:** 保留现有 Compose、SharedPreferences、Room 和前台 Service 结构。配置继续使用本地加密 API Key；AI 请求增加明确的生命周期状态与幂等边界；UI 通过可观察状态、滚动容器和统一错误占位改善反馈。

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room, Android Foreground Service, SharedPreferences, JUnit。

**Spec:** `design/2026-09-01-global-audit.md`

## Global Constraints

- 不移除用户已有生日、纪念日、追剧记录或 AI 配置。
- 生图失败不得自动重复提交可能产生费用的请求。
- API Key 继续加密保存，并在永久删除配置时清理对应密钥。
- 保持紫粉渐变和简约卡片视觉风格。

### Task 1: AI 配置弹窗与配置状态

**Files:** `AiScreens.kt`, `AiPreferences.kt`, `AiModels.kt`, tests.

- [ ] 为配置编辑内容增加独立滚动容器，保证 320dp 宽度和大字体可操作。
- [ ] 让 AI 首页在返回页面时重新读取配置，而不是只在首次组合时读取。
- [ ] 添加配置名称编辑字段；保存时保持名称稳定。
- [ ] 保存旧配置迁移后清理旧的单配置 API Key 键。
- [ ] 测试迁移、选择、删除和名称保存。

### Task 2: AI 请求与任务生命周期

**Files:** `OpenAiCompatibleClient.kt`, `AiChatService.kt`, `AiImageGenerationService.kt`, `AiHistory.kt`, tests.

- [ ] 为每次请求建立客户端 request id，并在支持时发送幂等请求头。
- [ ] 服务启动失败或任务超过阈值时持久化失败状态和可操作原因。
- [ ] 保留现有一次性流式降级，但避免收到首段内容后重复发送。
- [ ] 测试任务启动失败/超时状态转换和 request id 生成。

### Task 3: 图片加载、历史滚动与模型过滤

**Files:** `AiScreens.kt`, `AiModels.kt`, tests.

- [ ] 生图结果显示加载中、加载失败和文件缺失状态。
- [ ] 对话发送和流式回复更新时自动滚动到底部；用户手动上滑时不强制抢回。
- [ ] 获取模型后按对话/生图用途过滤，并允许明确查看全部模型。
- [ ] 测试模型过滤和图片加载状态文案。

### Task 4: 卡片交互与验证

**Files:** `AiScreens.kt`, `UiComponents.kt`, optional tests.

- [ ] 完善长地址显示/复制入口和最后一个配置删除的解释。
- [ ] 明确配置卡片“确定”与页面“保存配置”的关系。
- [ ] 检查 AI、追剧和设置页面触控尺寸、深色对比度、返回路径和长文本换行。

### Task 5: Verification

- [ ] Run `:app:compileDebugUnitTestKotlin`.
- [ ] Run `:app:assembleDebug`.
- [ ] Run `git diff --check`.
- [ ] Record device-only verification gaps honestly.
