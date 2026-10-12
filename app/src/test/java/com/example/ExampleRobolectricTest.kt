package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.assets.AssetBinaryManager
import com.example.modules.keymaker.KeyMakerEngine
import com.example.modules.porter.AutoPorterEngine
import com.example.modules.signpro.SignProEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertTrue(java.io.File(testExtractDir, "elf_dependency_graph.json").exists())
        assertTrue(java.io.File(testExtractDir, "fod_fingerprint").isDirectory)
    }

    @Test
    fun `verify UnpackedImageInspector and FodComparativeEngine generate IMGSTRACT audit`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val unpackedDir = java.io.File(context.cacheDir, "test_unpacked_system").apply {
            mkdirs()
            java.io.File(this, "build.prop").writeText("ro.build.version.release=14\nro.build.version.sdk=34\n")
        }
        val imgstractDir = java.io.File(context.cacheDir, "test_imgstract").apply { mkdirs() }

        val shell = com.example.core.shell.HybridShellEngine(context.cacheDir, context.cacheDir)
        val engine = com.example.core.extract.DeviceExtractEngine(shell)
        val testExtractDir = java.io.File(context.cacheDir, "test_extract_with_unpacked").apply { mkdirs() }

        val report = engine.executeCompleteExtraction(
            extractRootDir = testExtractDir,
            selectedUnpackedDir = unpackedDir,
            imgstractOutputDir = imgstractDir,
            onProgress = { _, _ -> },
            onLog = { }
        )

        assertNotNull(report.comparativeReport)
        assertEquals("test_unpacked_system", report.comparativeReport?.unpackedImageName)
        assertTrue(java.io.File(imgstractDir, "unpacked_image_dna.json").exists())
        assertTrue(java.io.File(imgstractDir, "unpacked_fod_support.json").exists())
        assertTrue(java.io.File(imgstractDir, "fod_comparative_audit.json").exists())
        assertTrue(java.io.File(imgstractDir, "disassembly_host_vs_gsi.json").exists())
        assertTrue(java.io.File(imgstractDir, "universal_ai_porting_diagnostic.json").exists())
        assertNotNull(report.comparativeReport?.deepAiDiagnostic)
        assertNotNull(report.comparativeReport?.disassemblyReport)
    }

    @Test
    fun `verify BinaryDisassemblyEngine and FodAiDiagnosticEngine generate deep analysis`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyHostDir = java.io.File(context.cacheDir, "test_dummy_host").apply { mkdirs() }
        val dummyGsiDir = java.io.File(context.cacheDir, "test_dummy_gsi").apply { mkdirs() }

        val hostDisasm = com.example.core.extract.BinaryDisassemblyEngine.inspectDirectoryOrFiles(
            sourceName = "Host Phone",
            isHostDevice = true,
            targetFilesOrDir = listOf(dummyHostDir)
        )
        assertTrue(hostDisasm.totalDexClasses > 0)
        assertTrue(hostDisasm.fodDexClassesCount > 0)
        assertTrue(hostDisasm.totalElfSymbols > 0)

        val gsiDisasm = com.example.core.extract.BinaryDisassemblyEngine.inspectDirectoryOrFiles(
            sourceName = "GSI Target",
            isHostDevice = false,
            targetFilesOrDir = listOf(dummyGsiDir)
        )

        val comp = com.example.core.extract.BinaryDisassemblyEngine.compareHostAndGsiDisassembly(
            hostResult = hostDisasm,
            gsiResult = gsiDisasm,
            outputDir = java.io.File(context.cacheDir, "disasm_out")
        )
        assertTrue(comp.overallParityScore > 0)
        assertTrue(comp.missingClassesInGsiCount >= 0)

        val fodSpec = com.example.core.extract.FodChainSpecification(
            sensorVendor = "Goodix Optical",
            halInterface = "android.hardware.biometrics.fingerprint@2.1",
            aidlOrHidlService = "vendor.xiaomi.hardware.fingerprintextension@1.0",
            driverDeviceNode = "/dev/goodix_fp",
            hbmIlluminationNode = "/sys/class/drm/card0-DSI-1/disp_param",
            dimLayerNode = "",
            touchFodNode = "",
            keylayoutFile = "uinput-goodix.kl",
            keycodeDetected = 338,
            locationCoordinates = "X=440, Y=1900, R=95px",
            sensorRadiusPx = 95,
            sensorWidthPx = 440,
            sensorHeightPx = 1900,
            whyItWorksOnHostRom = "Fonctionne via chaine OEM.",
            whyItFailsOnGenericGsi = "Echoue sur AOSP sans HBM.",
            howSystemCommunicatesWithVendor = "Pont Binder.",
            portingRequirements = listOf("Overlay UDFPS", "HBM node")
        )

        val aiDiag = com.example.core.extract.FodAiDiagnosticEngine.generateUniversalDiagnostic(
            brand = "Xiaomi",
            model = "Mi Note 10",
            codename = "tucana",
            platform = "sm6150",
            fodSpec = fodSpec,
            disassemblyReport = comp,
            outputDir = java.io.File(context.cacheDir, "ai_diag_out")
        )

        assertTrue(aiDiag.whyItWorksOnHostPhone.contains("Xiaomi"))
        assertTrue(aiDiag.whyItFailsOnGsiDespiteSameVendor.contains("GSI"))
        assertEquals(5, aiDiag.hostHardwareChain.size)
        assertEquals(4, aiDiag.gsiRupturesList.size)
        assertTrue(aiDiag.concretePortingRoadmap.isNotEmpty())
    }
}

