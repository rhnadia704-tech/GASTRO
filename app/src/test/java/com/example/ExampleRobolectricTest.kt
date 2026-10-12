package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.assets.AssetBinaryManager
import com.example.modules.keymaker.KeyMakerEngine
import com.example.modules.porter.AutoPorterEngine
import com.example.modules.signpro.SignProEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `verify app name and core AOSP reverse compiler modules`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FORGER", appName)

        // 1. Verify AssetBinaryManager extracts all 8 native binaries & JNI Rust libraries
        val binaryManager = AssetBinaryManager(context)
        val extracted = binaryManager.extractAndVerifyBinaries {}
        assertTrue(extracted.size >= 7)
        assertTrue(extracted.any { it.name == "libgastro_engine_jni.so" })

        // 2. Verify KeyMakerEngine generates RSA-2048 keys & manifest.json ("Clé Note")
        val keyMaker = KeyMakerEngine(context.filesDir)
        val keys = keyMaker.generateFullKeySuite("LineageForge", "AOSP-Test", "FR", 25) {}
        assertEquals(4, keys.size)
        assertTrue(keyMaker.getManifestJsonFile().exists())

        // 3. Verify SignProEngine signs system APKs in-memory and injects plat_mac_permissions.xml
        val signPro = SignProEngine(binaryManager.getWorkspaceDir(), keyMaker)
        val batch = signPro.signAllApksInMemoryAndPatchMacPermissions(keys, true) {}
        assertTrue(batch.signedSuccess >= 4)
        assertTrue(batch.macPermissionsUpdated)

        // 4. Verify AutoPorterEngine transplants proprietary blobs, RRO overlay & UDFPS/FOD HBM shim
        val porter = AutoPorterEngine(binaryManager.getWorkspaceDir())
        val portRes = porter.executeFullGsiPortingPipeline {}
        assertTrue(portRes.fodDiagnostics.detected)
        assertTrue(portRes.fodDiagnostics.systemUiOverlayInjected)
    }

    @Test
    fun `verify DeviceExtractEngine json serialization succeeds without Any serializer errors`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shell = com.example.core.shell.HybridShellEngine(context.cacheDir, context.cacheDir)
        val engine = com.example.core.extract.DeviceExtractEngine(shell)
        val testExtractDir = java.io.File(context.cacheDir, "test_extract").apply { mkdirs() }

        val report = engine.executeCompleteExtraction(
            extractRootDir = testExtractDir,
            onProgress = { _, _ -> },
            onLog = { }
        )

        assertTrue(report.jsonFilesGenerated.isNotEmpty())
        assertTrue(java.io.File(testExtractDir, "device_dna.json").exists())
        assertTrue(java.io.File(testExtractDir, "hals_and_blobs.json").exists())
        assertTrue(java.io.File(testExtractDir, "fod_chain.json").exists())
        assertTrue(java.io.File(testExtractDir, "partition_layout.json").exists())
        assertTrue(java.io.File(testExtractDir, "system_vendor_bridge.json").exists())
    }
}

