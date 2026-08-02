package com.nanji.lootarchive.data.repository

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BackupRepositoryZipSlipTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `normal entry resolves under target dir`() {
        val target = tmp.newFolder("target")
        val file = resolveSafeFile(target, "photos/a.jpg")
        assertNotNull(file)
        assertTrue(file!!.parentFile.isDirectory) // 父目录已创建
    }

    @Test
    fun `parent-dotdot traversal is rejected`() {
        val target = tmp.newFolder("target")
        assertNull(resolveSafeFile(target, "../../evil.txt"))
    }

    @Test
    fun `absolute path is rejected`() {
        val target = tmp.newFolder("target")
        assertNull(resolveSafeFile(target, "/etc/passwd"))
    }

    @Test
    fun `windows backslash traversal is rejected`() {
        val target = tmp.newFolder("target")
        assertNull(resolveSafeFile(target, "..\\..\\evil.txt"))
    }
}
