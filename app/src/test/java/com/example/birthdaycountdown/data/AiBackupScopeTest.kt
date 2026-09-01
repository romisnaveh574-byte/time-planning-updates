package com.example.birthdaycountdown.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AiBackupScopeTest {
    @Test
    fun backupScopeClearlyExcludesAiHistoryAndLocalImages() {
        assertEquals("备份包含生日、纪念日和追剧记录；不包含 AI 对话、生图记录、图片与参考图。", backupScopeDescription())
    }
}
