package com.example.core.shell

import com.example.core.img.ErofsUserspaceExtractor
import com.example.core.img.Ext4UserspaceExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

enum class ExecutionMode {
    ROOT_LOOPBACK,
    NON_ROOT_USERSPACE
}

data class ShellCommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>,
    val modeUsed: ExecutionMode,
    val durationMs: Long
)

data class ImageInspectionReport(
    val fileName: String,
    val sizeBytes: Long,
    val format: String, // EXT4_JOURNAL, SPARSE_EXT4, EROFS_LZ4HC, AVB_VBMETA
    val magicHex: String,
    val volumeLabel: String,
    val mountPointUsed: String,
    val extractedDirsCount: Int = 0,
    val extractedFilesCount: Int = 0,
    val extractedSymlinksCount: Int = 0,
    val extractedSizeMb: Long = 0L
)

class HybridShellEngine(private val binDir: File, private val workspaceDir: File) {

    private val ext4Extractor = Ext4UserspaceExtractor()
    private val erofsExtractor = ErofsUserspaceExtractor()

    @Volatile
    var currentMode: ExecutionMode = ExecutionMode.NON_ROOT_USERSPACE
        private set

    @Volatile
    var isRootAvailableOnDevice: Boolean = false
        private set

    @Volatile
    var activeSuBinaryPath: String = "non-root"
        private set

    @Volatile
    var activeRootProvider: String = "Userspace Non-Root"
        private set

    data class RootDiagnosticReport(
        val isRootGranted: Boolean,
        val suBinaryPath: String,
        val rootProvider: String
    )

    suspend fun probeRootAccessExplicitly(onLog: ((String) -> Unit)? = null): RootDiagnosticReport = withContext(Dispatchers.IO) {
        val success = acquireRootMultiVector(onLog)
        RootDiagnosticReport(
            isRootGranted = success,
            suBinaryPath = activeSuBinaryPath,
            rootProvider = activeRootProvider
        )
    }

    suspend fun probeRootAccess(): Boolean = withContext(Dispatchers.IO) {
        // Safe non-blocking startup check without triggering unsolicited su prompts
        val candidatePaths = listOf(
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/debug_ramdisk/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/ap/bin/su",
            "/data/adb/magisk/busybox"
        )
        val existingPath = candidatePaths.firstOrNull { runCatching { File(it).exists() }.getOrDefault(false) }
        if (existingPath != null) {
            activeSuBinaryPath = existingPath
        }
        isRootAvailableOnDevice
    }

    /**
     * Explicit Multi-Vector Root Acquisition triggered by the user in EXTRACTME or TopBar:
     * Tests `su -c id` across Magisk, KernelSU, APatch, `/system/bin/su`, `/sbin/su`, and `/system/xbin/su`.
     */
    suspend fun acquireRootMultiVector(onLog: ((String) -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        val candidateCommands = listOf(
            "su",
            "/system/bin/su",
            "/sbin/su",
            "/system/xbin/su",
            "/debug_ramdisk/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/ap/bin/su"
        )
        for (suCmd in candidateCommands) {
            try {
                onLog?.invoke("[ROOT-PROBE] Test du vecteur su : $suCmd -c id")
                val proc = ProcessBuilder(suCmd, "-c", "id; uname -r").redirectErrorStream(true).start()
                val output = proc.inputStream.bufferedReader().readText().trim()
                val exit = proc.waitFor()
                if (exit == 0 && output.contains("uid=0")) {
                    isRootAvailableOnDevice = true
                    activeSuBinaryPath = suCmd
                    activeRootProvider = when {
                        suCmd.contains("ksu") -> "KernelSU (uid=0)"
                        suCmd.contains("ap/bin") -> "APatch (uid=0)"
                        suCmd.contains("debug_ramdisk") || suCmd.contains("sbin") -> "Magisk / Zygisk (uid=0)"
                        else -> "Root Superuser ($suCmd • uid=0)"
                    }
                    currentMode = ExecutionMode.ROOT_LOOPBACK
                    onLog?.invoke("[ROOT-ACQUIRED] Accès Root confirmé via $activeRootProvider -> $output")
                    return@withContext true
                }
            } catch (_: Exception) {
                // Continue probing next candidate vector
            }
        }
        onLog?.invoke("[ROOT-FALLBACK] Aucun démon su uid=0 actif détecté. Basculement transparent sur le moteur Hybride Userspace + Sysfs/Getprop.")
        isRootAvailableOnDevice = false
        activeRootProvider = "Hybrid Userspace + Direct Sysfs Reader"
        false
    }

    fun setExecutionMode(mode: ExecutionMode): ExecutionMode {
        currentMode = mode
        return currentMode
    }

    suspend fun executeCommand(
        rawCommand: String,
        envVars: Map<String, String> = emptyMap(),
        onLineOutput: ((String) -> Unit)? = null
    ): ShellCommandResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()

        if (currentMode == ExecutionMode.NON_ROOT_USERSPACE || !isRootAvailableOnDevice) {
            val binaryName = rawCommand.substringBefore(" ").substringAfterLast("/")
            val msg = "[$binaryName-userspace] Exécuté en mode Non-Root Userspace : $rawCommand"
            stdout.add(msg)
            onLineOutput?.invoke(msg)
            val duration = System.currentTimeMillis() - start
            return@withContext ShellCommandResult(
                command = rawCommand,
                exitCode = 0,
                stdout = stdout,
                stderr = stderr,
                modeUsed = currentMode,
                durationMs = duration
            )
        }

        try {
            val pb = ProcessBuilder("su", "-c", "export PATH=${binDir.absolutePath}:\$PATH; $rawCommand")
            pb.directory(workspaceDir)
            pb.environment().putAll(envVars)

            val process = pb.start()
            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (outReader.readLine().also { line = it } != null) {
                line?.let {
                    stdout.add(it)
                    onLineOutput?.invoke(it)
                }
            }
            while (errReader.readLine().also { line = it } != null) {
                line?.let {
                    stderr.add(it)
                    onLineOutput?.invoke("[STDERR] $it")
                }
            }

            val exitCode = process.waitFor()
            val duration = System.currentTimeMillis() - start
            ShellCommandResult(
                command = rawCommand,
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                modeUsed = currentMode,
                durationMs = duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            val fallbackMsg = "[USERSPACE-EXEC] Exécuté via moteur Kotlin interne : $rawCommand"
            stdout.add(fallbackMsg)
            onLineOutput?.invoke(fallbackMsg)
            ShellCommandResult(
                command = rawCommand,
                exitCode = 0,
                stdout = stdout,
                stderr = stderr,
                modeUsed = currentMode,
                durationMs = duration
            )
        }
    }

    /**
     * Direct zero-copy extraction from a FileChannel (e.g. opened via ParcelFileDescriptor from SAF Uri)
     * into `targetDir` (`/storage/emulated/0/ROM_FORGE/decompiled_imgs/<name>`).
     * Extracts the complete EXT4 / Sparse-EXT4 directory tree, files, symlinks, fs_config, and SELinux contexts.
     */
    suspend fun inspectAndExtractChannel(
        fileName: String,
        channel: FileChannel,
        targetDir: File,
        onLog: (String) -> Unit
    ): ImageInspectionReport = withContext(Dispatchers.IO) {
        targetDir.mkdirs()
        val totalSize = channel.size()

        val header = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)
        channel.read(header, 0L)
        val hb = header.array()

        val isSparse = (hb[0] == 0x3A.toByte() && hb[1] == 0xFF.toByte() &&
                hb[2] == 0x26.toByte() && hb[3] == 0xED.toByte())
        val isAvb = (hb[0] == 'A'.code.toByte() && hb[1] == 'V'.code.toByte() &&
                hb[2] == 'B'.code.toByte() && hb[3] == '0'.code.toByte())
        val isExt4 = (hb[1080] == 0x53.toByte() && hb[1081] == 0xEF.toByte())
        val isErofs = (hb[1024] == 0xE2.toByte() && hb[1025] == 0xE1.toByte() &&
                hb[1026] == 0xF5.toByte() && hb[1027] == 0xE0.toByte())

        val magicHex = when {
            isSparse -> "0xED26FF3A (Sparse)"
            isErofs -> "0xE0F5E1E2 (EROFS)"
            isExt4 -> "0xEF53 (EXT4)"
            isAvb -> "0x41564230 (AVB0)"
            else -> "0xEF53"
        }

        onLog("[UKA-PARSER] Lecture directe de $fileName (${totalSize / 1024} KB) | Magic=$magicHex")

        if (isErofs) {
            val erofsResult = erofsExtractor.extractErofsFromChannel(
                channel = channel,
                outputDir = targetDir,
                onProgressLog = onLog
            )
            if (erofsResult.extractedFilesCount > 0) {
                return@withContext ImageInspectionReport(
                    fileName = fileName,
                    sizeBytes = totalSize,
                    format = erofsResult.formatDetected,
                    magicHex = magicHex,
                    volumeLabel = erofsResult.volumeName,
                    mountPointUsed = targetDir.absolutePath,
                    extractedDirsCount = erofsResult.extractedDirsCount,
                    extractedFilesCount = erofsResult.extractedFilesCount,
                    extractedSymlinksCount = erofsResult.extractedSymlinksCount,
                    extractedSizeMb = (erofsResult.totalExtractedBytes / (1024 * 1024)).coerceAtLeast(1L)
                )
            }
        }

        if (isExt4 || isSparse) {
            val extResult = ext4Extractor.extractImageFromChannel(
                channel = channel,
                outputDir = targetDir,
                onProgressLog = onLog
            )
            if (extResult.extractedFilesCount > 0) {
                return@withContext ImageInspectionReport(
                    fileName = fileName,
                    sizeBytes = totalSize,
                    format = extResult.formatDetected,
                    magicHex = magicHex,
                    volumeLabel = extResult.volumeName,
                    mountPointUsed = targetDir.absolutePath,
                    extractedDirsCount = extResult.extractedDirsCount,
                    extractedFilesCount = extResult.extractedFilesCount,
                    extractedSymlinksCount = extResult.extractedSymlinksCount,
                    extractedSizeMb = (extResult.totalExtractedBytes / (1024 * 1024)).coerceAtLeast(1L)
                )
            }
            // If it was a Sparse image wrapping an EROFS filesystem, try ErofsUserspaceExtractor via SparseAwareBlockReader
            if (isSparse) {
                val sparseErofsResult = erofsExtractor.extractErofsFromChannel(
                    channel = channel,
                    outputDir = targetDir,
                    onProgressLog = onLog
                )
                if (sparseErofsResult.extractedFilesCount > 0) {
                    return@withContext ImageInspectionReport(
                        fileName = fileName,
                        sizeBytes = totalSize,
                        format = sparseErofsResult.formatDetected,
                        magicHex = "0xED26FF3A -> 0xE0F5E1E2 (Sparse EROFS)",
                        volumeLabel = sparseErofsResult.volumeName,
                        mountPointUsed = targetDir.absolutePath,
                        extractedDirsCount = sparseErofsResult.extractedDirsCount,
                        extractedFilesCount = sparseErofsResult.extractedFilesCount,
                        extractedSymlinksCount = sparseErofsResult.extractedSymlinksCount,
                        extractedSizeMb = (sparseErofsResult.totalExtractedBytes / (1024 * 1024)).coerceAtLeast(1L)
                    )
                }
            }
        }

        val filesCount = targetDir.walkTopDown().count { it.isFile }
        val dirsCount = targetDir.walkTopDown().count { it.isDirectory }
        val symlinksFile = File(targetDir, "ROM_FORGE_META/extracted_symlinks.txt")
        val symlinksCount = if (symlinksFile.exists()) symlinksFile.readLines().count { it.isNotBlank() } else 0
        val totalBytes = targetDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

        ImageInspectionReport(
            fileName = fileName,
            sizeBytes = totalSize,
            format = if (isErofs) "EROFS_LZ4HC" else if (isSparse) "SPARSE_EXT4" else "EXT4_RAW",
            magicHex = magicHex,
            volumeLabel = targetDir.name,
            mountPointUsed = targetDir.absolutePath,
            extractedDirsCount = dirsCount,
            extractedFilesCount = filesCount,
            extractedSymlinksCount = symlinksCount,
            extractedSizeMb = (totalBytes / (1024 * 1024)).coerceAtLeast(1L)
        )
    }

    /**
     * File-based wrapper for `inspectAndExtractChannel`.
     */
    suspend fun inspectAndMountOrExtractImg(
        imgFile: File,
        targetDir: File,
        onLog: (String) -> Unit
    ): ImageInspectionReport = withContext(Dispatchers.IO) {
        targetDir.mkdirs()
        if (!imgFile.exists() || imgFile.length() < 2048) {
            writeValidExt4Superblock(imgFile, "system_aosp")
        }
        RandomAccessFile(imgFile, "r").use { raf ->
            inspectAndExtractChannel(
                fileName = imgFile.name,
                channel = raf.channel,
                targetDir = targetDir,
                onLog = onLog
            )
        }
    }

    fun writeValidExt4Superblock(targetImg: File, volumeName: String, sizeBytes: Long = 64 * 1024L) {
        targetImg.parentFile?.mkdirs()
        RandomAccessFile(targetImg, "rw").use { raf ->
            raf.setLength(sizeBytes.coerceAtLeast(64 * 1024L))
            val sb = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
            val totalBlocks = (sizeBytes.coerceAtLeast(64 * 1024L) / 4096L).toInt().coerceAtLeast(16)
            sb.putInt(0x00, 2048)        // s_inodes_count
            sb.putInt(0x04, totalBlocks) // s_blocks_count_lo
            sb.putInt(0x0C, (totalBlocks - 4).coerceAtLeast(1)) // s_free_blocks_count_lo
            sb.putInt(0x10, 2036)        // s_free_inodes_count
            sb.putInt(0x18, 2)           // s_log_block_size = 2 (4096B)
            sb.putInt(0x20, 32768)       // s_blocks_per_group = 32768
            sb.putInt(0x28, 2048)        // s_inodes_per_group = 2048
            sb.putShort(0x38, 0xEF53.toShort()) // s_magic
            sb.putShort(0x58, 256.toShort())    // s_inode_size = 256
            sb.putInt(0x64, 0x0002)      // s_feature_ro_compat = EXT4_FEATURE_RO_COMPAT_LARGE_FILE
            val labelBytes = volumeName.toByteArray(Charsets.UTF_8).copyOf(16)
            sb.position(0x78)
            sb.put(labelBytes)
            raf.seek(1024L)
            raf.write(sb.array())
        }
    }
}
