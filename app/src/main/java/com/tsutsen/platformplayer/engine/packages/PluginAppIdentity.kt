package com.tsutsen.platformplayer.engine.packages

/**
 * Source plugins interpret bridge.buildVersion as the Grayjay engine version,
 * not this fork's release number. Bluejay's semver migration (0df48d24) records
 * 389 as its final legacy version; advertise that conservative baseline even
 * when the successor's independently numbered application reaches 7001+.
 */
object PluginAppIdentity {
    fun buildVersion(versionCode: Int, isThorBuild: Boolean, engineCompatibilityVersion: Int): Int =
        when {
            isThorBuild -> engineCompatibilityVersion
            versionCode == 1 -> Int.MAX_VALUE
            else -> versionCode
        }

    fun buildFlavor(flavor: String, isThorBuild: Boolean): String =
        if (isThorBuild) "stable" else flavor
}
