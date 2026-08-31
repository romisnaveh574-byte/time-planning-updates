package com.example.birthdaycountdown.update

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import java.security.MessageDigest

object ApkVerifier {
    fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    fun verify(context: Context, file: File, expected: ReleaseInfo): Boolean {
        if (!file.exists() || sha256(file) != expected.sha256) return false
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES) ?: return false
        if (info.packageName != context.packageName || (info.longVersionCode <= context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode)) return false
        val current = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners ?: return false
        return info.signingInfo?.apkContentsSigners?.contentEquals(current) == true
    }
}
