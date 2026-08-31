package com.example.birthdaycountdown.update

object UpdatePackageFileName {
    fun forVersion(versionName: String): String =
        "time-planning-${versionName.removePrefix("v")}.apk"
}
