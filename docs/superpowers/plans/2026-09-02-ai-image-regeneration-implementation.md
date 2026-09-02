# AI Image Regeneration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Let users regenerate a completed AI image as a new task while preserving the original result and all generation inputs.

**Architecture:** Add a repository operation that clones a completed image message into a new PENDING message in the same conversation, validates any saved reference image before insertion, and starts the existing image service for the new message. The UI exposes this action only for completed results and observes the same Flow so progress and completion update without leaving the page.

**Tech Stack:** Kotlin, Jetpack Compose, Room, coroutines/Flow, existing AI image service and history repository.

**Spec:** docs/superpowers/specs/2026-09-01-watch-status-management-image-regeneration-design.md

## Global Constraints

- Completed regeneration always inserts a new message; it never overwrites the old result.
- Copy prompt, reference image path, requested size, and quality from the source message.
- Use the current image relay configuration when the new service call starts.
- If a referenced image is missing, do not insert a task and show the exact message: 原参考图已不存在，请重新上传参考图.
- Failed-task retry keeps its existing reset-in-place behavior.

---

### Task 1: Add repository cloning and validation tests

**Files:** modify data/AiHistory.kt; test app/src/test/java/com/example/birthdaycountdown/data/AiHistoryRepositoryTest.kt.

**Interfaces:** Add suspend fun regenerateCompletedImage(messageId: Long): Result<AiMessageEntity>; return the newly inserted PENDING entity, preserve the original entity, and return a typed validation error when a required reference file is absent.

- [ ] Add failing tests asserting a completed source remains unchanged, the new row copies text/referenceImagePath/size/quality, and missing reference files create no row with the required error.
- [ ] Run ./gradlew :app:testDebugUnitTest --tests '*AiHistoryRepositoryTest*' and verify failure.
- [ ] Implement source lookup, completed-state guard, reference-file existence check, clone insert, and Result errors using existing DAO/database patterns.
- [ ] Re-run focused tests and verify pass.
- [ ] Commit with message feat: clone completed image tasks.

### Task 2: Start regenerated tasks through the existing image service

**Files:** modify ai/AiImageGenerationService.kt and data/AiHistory.kt; test AiImageGenerationServiceTest.kt.

**Interfaces:** Add a single entry point that accepts the new message ID, reads its persisted inputs, invokes the currently selected image endpoint, and updates progress/status exactly like initial generation. Do not reuse the failed retry path.

- [ ] Add a failing service test that verifies regenerated message execution uses current configuration and updates the new row independently of the source row.
- [ ] Run ./gradlew :app:testDebugUnitTest --tests '*AiImageGenerationServiceTest*' and verify failure.
- [ ] Implement the minimal launch method by reusing existing request construction and status updates.
- [ ] Re-run focused tests and verify pass.
- [ ] Commit with message feat: run regenerated image task.

### Task 3: Add completed-card UI action and feedback

**Files:** modify ui/AiScreens.kt; test app/src/test/java/com/example/birthdaycountdown/ui/AiImageSemanticsTest.kt.

**Interfaces:** Completed image cards expose a visible 再次生成 action. Clicking it launches the clone/start flow, keeps the old card visible, shows progress for the new card, and surfaces the exact missing-reference error without navigation.

- [ ] Add failing semantics tests for visible action on completed cards, source-card preservation, and missing-reference error text.
- [ ] Run focused UI tests and verify failure.
- [ ] Implement the action alongside existing FAILED retry, using stable message IDs and existing snackbar/dialog patterns.
- [ ] Re-run focused tests and verify pass.
- [ ] Commit with message feat: add completed image regeneration action.

### Task 4: Verify cross-screen state and release build

**Files:** modify none unless verification finds a defect.

- [ ] Run ./gradlew :app:testDebugUnitTest.
- [ ] Run ./gradlew :app:connectedDebugAndroidTest for image-history UI coverage.
- [ ] Run ./gradlew :app:assembleRelease.
- [ ] Run git diff --check and inspect that old completed results and generated files remain intact.

