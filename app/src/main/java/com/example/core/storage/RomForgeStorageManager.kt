package com.example.core.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StorageStatus(
    val publicRootPath: String,
    val isDirectStorageWritable: Boolean,
    val hasAllFilesAccess: Boolean,
    val customSafTreeUri: String?,
    val extractedImagesFolders: List<String>
)

/**
 * RomForgeStorageManager:
 * Organizes `/sdcard/FORGER/` (`/storage/emulated/0/FORGER/`) into two simple directories:
 * - `UNPACK/` : Unpacked `.img` directories (`sdcard/FORGER/UNPACK/<nom_img>/`)
 * - `PACKED/` : Repacked `.img` files (`sdcard/FORGER/PACKED/`)
 */
class RomForgeStorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("forger_storage_prefs", Context.MODE_PRIVATE)

    fun hasAllFilesAccessPermission(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun getRomForgePublicRoot(): File {
        if (hasAllFilesAccessPermission()) {
            try {
                val directForger = File(Environment.getExternalStorageDirectory(), "FORGER")
                if (directForger.exists() || directForger.mkdirs()) {
                    if (directForger.canWrite()) {
                        ensureSubfoldersExist(directForger)
                        return directForger
                    }
                }
            } catch (_: Exception) {
            }
        }
        val appExternal = context.getExternalFilesDir(null)
        if (appExternal != null) {
            val extRoot = File(appExternal, "FORGER").apply { mkdirs() }
            ensureSubfoldersExist(extRoot)
            return extRoot
        }
        val internalRoot = File(context.filesDir, "FORGER").apply { mkdirs() }
        ensureSubfoldersExist(internalRoot)
        return internalRoot
    }

    private fun ensureSubfoldersExist(root: File) {
        File(root, "UNPACK").mkdirs()
        File(root, "PACKED").mkdirs()
        File(root, "EXTRACT").mkdirs()
        File(root, "Data/IMGSTRACT").mkdirs()
    }

    fun getUserVisibleDisplayRoot(): String {
        return "/sdcard/FORGER"
    }

    // 1. UNPACK folder (sdcard/FORGER/UNPACK)
    fun getUnpackRootDir(): File {
        val unpack = File(getRomForgePublicRoot(), "UNPACK").apply { mkdirs() }
        // Also migrate any existing unpacked folders from previous ROM_FORGE/UNPACK if present
        if (hasAllFilesAccessPermission()) {
            runCatching {
                val oldUnpack = File(Environment.getExternalStorageDirectory(), "ROM_FORGE/UNPACK")
                if (oldUnpack.exists() && oldUnpack.isDirectory) {
                    oldUnpack.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                        val target = File(unpack, dir.name)
                        if (!target.exists()) {
                            dir.copyRecursively(target, overwrite = false)
                        }
                    }
                }
            }
        }
        return unpack
    }

    fun getExtractedImagesRoot(): File = getUnpackRootDir()

    fun getDefaultSystemDecompiledDir(): File = File(getUnpackRootDir(), "system_ext4").apply { mkdirs() }

    fun getStockVendorDecompiledDir(): File = File(getUnpackRootDir(), "stock_vendor_ref").apply { mkdirs() }

    // 2. PACKED folder (sdcard/FORGER/PACKED)
    fun getPackedOutputImagesDir(): File = File(getRomForgePublicRoot(), "PACKED").apply { mkdirs() }

    fun getCompiledOutputImagesDir(): File = getPackedOutputImagesDir()

    // 3. EXTRACT folder (sdcard/FORGER/EXTRACT)
    fun getExtractRootDir(): File = File(getRomForgePublicRoot(), "EXTRACT").apply { mkdirs() }

    // 4. IMGSTRACT folder (sdcard/FORGER/Data/IMGSTRACT)
    fun getImgstractRootDir(): File = File(getRomForgePublicRoot(), "Data/IMGSTRACT").apply { mkdirs() }

    // Legacy getters mapped cleanly inside FORGER so existing helper classes compile cleanly
    fun getKeyRootDir(): File = File(getRomForgePublicRoot(), "PACKED").apply { mkdirs() }
    fun getKeystorePublicDir(): File = getKeyRootDir()
    fun getKeyDataReportsDir(): File = getKeyRootDir()
    fun getPortWorkspaceRootDir(): File = getUnpackRootDir()
    fun getCreationOutputDir(): File = getPackedOutputImagesDir()
    fun getSingleSignedApksDir(): File = getPackedOutputImagesDir()

    fun listDecompiledImgDirectories(): List<File> {
        val root = getUnpackRootDir()
        val children = root.listFiles()
            ?.filter { it.isDirectory && it.name != "stock_vendor_ref" }
            ?.sortedBy { it.name }
            ?: emptyList()
        return if (children.isEmpty()) listOf(getDefaultSystemDecompiledDir()) else children
    }

    fun saveCustomSafTreeUri(uri: Uri) {
        prefs.edit().putString("saf_tree_uri", uri.toString()).apply()
    }

    fun getStorageStatus(): StorageStatus {
        val hasAccess = hasAllFilesAccessPermission()
        val displayRoot = getUserVisibleDisplayRoot()
        val folders = listDecompiledImgDirectories().map { it.name }
        return StorageStatus(
            publicRootPath = displayRoot,
            isDirectStorageWritable = hasAccess,
            hasAllFilesAccess = hasAccess,
            customSafTreeUri = prefs.getString("saf_tree_uri", null),
            extractedImagesFolders = folders
        )
    }

    /**
     * Copies unpacked folder into `/sdcard/FORGER/UNPACK/<folder>`
     */
    suspend fun mirrorDirectoryToPublicDownloadRomForge(
        sourceDir: File,
        subFolderName: String,
        onLog: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val normalizedSub = subFolderName
            .replace("decompiled_imgs/", "UNPACK/")
            .replace("compiled_imgs", "PACKED")

        if (hasAllFilesAccessPermission()) {
            try {
                val directTarget = File(Environment.getExternalStorageDirectory(), "FORGER/$normalizedSub")
                directTarget.mkdirs()
                if (sourceDir.absolutePath != directTarget.absolutePath) {
                    sourceDir.copyRecursively(directTarget, overwrite = true)
                }
                onLog("[FORGER] Dossier enregistré dans : /sdcard/FORGER/$normalizedSub")
                return@withContext "/sdcard/FORGER/$normalizedSub"
            } catch (_: Exception) {
            }
        }

        val localWorkspaceTarget = File(getRomForgePublicRoot(), normalizedSub).apply { mkdirs() }
        if (sourceDir.absolutePath != localWorkspaceTarget.absolutePath) {
            try {
                sourceDir.copyRecursively(localWorkspaceTarget, overwrite = true)
            } catch (_: Exception) {
            }
        }
        onLog("[FORGER] Dossier disponible dans : ${localWorkspaceTarget.absolutePath}")
        localWorkspaceTarget.absolutePath
    }

    /**
     * Exports a single file to `/sdcard/FORGER/<subFolder>` (`UNPACK` or `PACKED`).
     */
    suspend fun exportSingleFileToPublicRomForge(
        sourceFile: File,
        subFolder: String,
        onLog: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val normalizedSub = if (subFolder.contains("UNPACK", true)) "UNPACK" else "PACKED"

        if (hasAllFilesAccessPermission()) {
            try {
                val directFolder = File(Environment.getExternalStorageDirectory(), "FORGER/$normalizedSub")
                directFolder.mkdirs()
                val dst = File(directFolder, sourceFile.name)
                if (sourceFile.absolutePath != dst.absolutePath) {
                    sourceFile.copyTo(dst, overwrite = true)
                }
                onLog("[FORGER] Fichier enregistré dans : /sdcard/FORGER/$normalizedSub/${dst.name}")
                return@withContext "/sdcard/FORGER/$normalizedSub/${dst.name}"
            } catch (_: Exception) {
            }
        }

        val targetFolder = File(getRomForgePublicRoot(), normalizedSub).apply { mkdirs() }
        val dst = File(targetFolder, sourceFile.name)
        if (sourceFile.absolutePath != dst.absolutePath) {
            try {
                sourceFile.copyTo(dst, overwrite = true)
            } catch (_: Exception) {
            }
        }
        onLog("[FORGER] Fichier enregistré dans : ${dst.absolutePath}")
        dst.absolutePath
    }

    suspend fun resolveOrImportSafDirectory(
        treeUri: Uri,
        fallbackFolderName: String,
        onLog: (String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (hasAllFilesAccessPermission()) {
            try {
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                if (docId.startsWith("primary:")) {
                    val rel = docId.removePrefix("primary:")
                    val realFile = File(Environment.getExternalStorageDirectory(), rel)
                    if (realFile.exists() && realFile.isDirectory) {
                        onLog("[UNPACK] Dossier sélectionné : ${realFile.absolutePath}")
                        return@withContext realFile
                    }
                }
            } catch (_: Exception) {
            }
        }

        val target = File(getUnpackRootDir(), fallbackFolderName).apply { mkdirs() }
        onLog("[UNPACK] Dossier sélectionné : ${target.absolutePath}")
        target
    }
}
