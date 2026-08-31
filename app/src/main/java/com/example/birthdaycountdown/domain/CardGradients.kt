package com.example.birthdaycountdown.domain

data class CardGradient(val id: String, val name: String, val colors: List<Int>)

object CardGradients {
    val all = listOf(
        CardGradient("solid", "纯色", emptyList()),
        CardGradient("blue_cyan", "电光蓝青", listOf(0xFF2563EB.toInt(), 0xFF06B6D4.toInt())),
        CardGradient("blue_violet", "蓝紫高亮", listOf(0xFF3B82F6.toInt(), 0xFF8B5CF6.toInt())),
        CardGradient("mint", "青绿薄荷", listOf(0xFF0F766E.toInt(), 0xFF34D399.toInt())),
        CardGradient("purple_pink", "紫红霓虹", listOf(0xFF7C3AED.toInt(), 0xFFEC4899.toInt())),
        CardGradient("coral", "珊瑚橙粉", listOf(0xFFF97316.toInt(), 0xFFFB7185.toInt())),
        CardGradient("deep_ocean", "深海蓝紫", listOf(0xFF111827.toInt(), 0xFF312E81.toInt(), 0xFF4F46E5.toInt())),
        CardGradient("berry", "莓果红橙", listOf(0xFF9F1239.toInt(), 0xFFEA580C.toInt())),
        CardGradient("olive_gold", "橄榄沙金", listOf(0xFF3F6212.toInt(), 0xFFCA8A04.toInt())),
        CardGradient("aurora", "Aurora", listOf(0xFF22D3EE.toInt(), 0xFFA78BFA.toInt(), 0xFF172554.toInt()))
    )

    fun find(id: String): CardGradient = all.firstOrNull { it.id == id } ?: all.first()
}
