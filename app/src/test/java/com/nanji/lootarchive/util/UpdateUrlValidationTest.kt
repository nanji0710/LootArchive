package com.nanji.lootarchive.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateUrlValidationTest {

    @Test
    fun `accepts github release download url`() {
        assertTrue(
            isValidDownloadUrl(
                "https://github.com/nanji0710/LootArchive/releases/download/V6.6.4/LootArchive-release-v6.6.4.apk"
            )
        )
    }

    @Test
    fun `accepts githubusercontent raw url`() {
        assertTrue(
            isValidDownloadUrl(
                "https://raw.githubusercontent.com/nanji0710/LootArchive/main/version.json"
            )
        )
    }

    @Test
    fun `rejects unknown host`() {
        assertFalse(isValidDownloadUrl("https://evil.example.com/LootArchive.apk"))
    }

    @Test
    fun `rejects non-https`() {
        assertFalse(isValidDownloadUrl("http://github.com/foo.apk"))
    }

    @Test
    fun `rejects path traversal versionName`() {
        assertFalse(isValidVersionName("../../etc/passwd"))
        assertFalse(isValidVersionName("6.6.4;rm -rf /"))
    }

    @Test
    fun `accepts normal versionName`() {
        assertTrue(isValidVersionName("6.6.4"))
        assertTrue(isValidVersionName("v6.6.4"))
    }
}
