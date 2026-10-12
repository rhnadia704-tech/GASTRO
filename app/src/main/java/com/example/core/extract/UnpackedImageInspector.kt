package com.example.core.extract

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class UnpackedImageDna(
    val folderName: String,
    val detectedPartition: String, // system, vendor, product, etc.
    val androidRelease: String,
    val sdkInt: Int,
    val buildFingerprint: String,
    val buildPropsSummary: Map<String, String>,
    val totalFiles: Int,
    val totalSizeMb: Double
)

@Serializable
data class UnpackedFodSupportDetails(
    val hasFodHalBlob: Boolean,
    val detectedFodBlobs: List<String>,
    val hasFodKeylayout: Boolean,
    val detectedKeylayouts: List<String>,
    val hasUdfpsOverlay: Boolean,
    val detectedOverlays: List<String>,
    val hasFodInitService: Boolean,
    val detectedInitServices: List<String>,
    val missingFodComponentsFromHost: List<String>,
    val portingReadinessScore: Int // 0 to 100%
)

@Serializable
data class UnpackedImageInspectionResult(
    val imageDna: UnpackedImageDna,
    val fodSupport: UnpackedFodSupportDetails,
    val totalBlobsFound: Int,
    val blobsList: List<String>,
    val destinationFolder: String
)

object UnpackedImageInspector {

    private val jsonHelper = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun inspectUnpackedDirectory(
        unpackedFolder: File,
        destinationDataImgstract: File,
        onLog: (String) -> Unit = {}
    ): UnpackedImageInspectionResult {
        destinationDataImgstract.mkdirs()
        onLog("[IMGSTRACT-START] Analyse de l'image unpackée : ${unpackedFolder.name} vers ${destinationDataImgstract.absolutePath}")

        // 1. Detect partition type & scan build.prop
        var detectedPart = "unknown"
        val folderLower = unpackedFolder.name.lowercase()
        when {
            folderLower.contains("vendor") -> detectedPart = "vendor"
            folderLower.contains("product") -> detectedPart = "product"
            folderLower.contains("system_ext") -> detectedPart = "system_ext"
            folderLower.contains("system") -> detectedPart = "system"
            folderLower.contains("odm") -> detectedPart = "odm"
            else -> {
                if (File(unpackedFolder, "build.prop").exists()) {
                    val content = File(unpackedFolder, "build.prop").readText()
                    if (content.contains("ro.vendor.")) detectedPart = "vendor"
                    else if (content.contains("ro.system.")) detectedPart = "system"
                }
            }
        }

        val allProps = mutableMapOf<String, String>()
        val propFiles = unpackedFolder.walkTopDown()
            .filter { it.isFile && (it.name.endsWith(".prop") || it.name == "build.prop") }
            .take(10)
            .toList()

        for (pf in propFiles) {
            runCatching {
                pf.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotBlank() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                        val key = trimmed.substringBefore("=").trim()
                        val value = trimmed.substringAfter("=").trim()
                        allProps[key] = value
                    }
                }
            }
        }

        val androidRel = allProps["ro.build.version.release"]
            ?: allProps["ro.system.build.version.release"]
            ?: allProps["ro.vendor.build.version.release"]
            ?: "GSI/AOSP"

        val sdkInt = (allProps["ro.build.version.sdk"]
            ?: allProps["ro.system.build.version.sdk"]
            ?: "34").toIntOrNull() ?: 34

        val fingerprint = allProps["ro.build.fingerprint"]
            ?: allProps["ro.system.build.fingerprint"]
            ?: allProps["ro.vendor.build.fingerprint"]
            ?: "Generic GSI / Unpacked Partition"

        onLog("[IMGSTRACT-INFO] Partition détectée : $detectedPart | Android $androidRel (API $sdkInt)")

        // 2. Scan blobs and libraries
        val allElfAndSoFiles = unpackedFolder.walkTopDown()
            .filter { it.isFile && (it.name.endsWith(".so") || it.parentFile?.name == "bin" || it.parentFile?.name == "hw") }
            .toList()

        val blobsList = allElfAndSoFiles.map { it.name }.distinct()
        val totalFiles = unpackedFolder.walkTopDown().filter { it.isFile }.count()
        val totalSize = unpackedFolder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val sizeMb = totalSize / (1024.0 * 1024.0)

        // 3. Scan FOD components in the unpacked image
        val detectedFodBlobs = blobsList.filter {
            val l = it.lowercase()
            l.contains("fingerprint") || l.contains("goodix") || l.contains("fpc") ||
                    l.contains("displayfeature") || l.contains("gf_hal") || l.contains("fod")
        }

        val keylayoutFiles = unpackedFolder.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kl") }
            .map { it.name }
            .toList()
        val hasFodKl = keylayoutFiles.any { it.contains("goodix") || it.contains("fpc") || it.contains("fingerprint") }

        val overlayFiles = unpackedFolder.walkTopDown()
            .filter { it.isFile && (it.name.endsWith(".apk") && (it.name.contains("overlay", ignoreCase = true) || it.name.contains("udfps", ignoreCase = true))) }
            .map { it.name }
            .toList()
        val hasUdfpsOverlay = overlayFiles.any { it.contains("udfps", ignoreCase = true) || it.contains("fod", ignoreCase = true) }

        val initServices = unpackedFolder.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".rc") }
            .filter {
                runCatching {
                    val txt = it.readText()
                    txt.contains("fingerprint") || txt.contains("goodix")
                }.getOrDefault(false)
            }
            .map { it.name }
            .toList()

        val missingComponents = mutableListOf<String>()
        if (detectedFodBlobs.isEmpty()) missingComponents.add("Blobs HAL d'empreinte sous l'écran manquants")
        if (!hasFodKl) missingComponents.add("Keylayout spécifique au capteur Goodix/FPC absent (uinput-goodix.kl)")
        if (!hasUdfpsOverlay) missingComponents.add("Overlay de coordonnées UDFPS (Rayon, X, Y) et HBM absent du Framework")
        if (initServices.isEmpty()) missingComponents.add("Déclaration de service Init RC pour le démon biométrique absente")

        var score = 100
        score -= missingComponents.size * 25
        score = score.coerceIn(5, 100)

        onLog("[IMGSTRACT-FOD] Diagnostic FOD Image : ${detectedFodBlobs.size} blobs détectés, Keylayout: ${if (hasFodKl) "OUI" else "NON"}, Overlay: ${if (hasUdfpsOverlay) "OUI" else "NON"} -> Score: $score%")

        val imageDna = UnpackedImageDna(
            folderName = unpackedFolder.name,
            detectedPartition = detectedPart,
            androidRelease = androidRel,
            sdkInt = sdkInt,
            buildFingerprint = fingerprint,
            buildPropsSummary = allProps.entries.take(50).associate { it.key to it.value },
            totalFiles = totalFiles,
            totalSizeMb = String.format("%.2f", sizeMb).toDoubleOrNull() ?: sizeMb
        )

        val fodSupport = UnpackedFodSupportDetails(
            hasFodHalBlob = detectedFodBlobs.isNotEmpty(),
            detectedFodBlobs = detectedFodBlobs,
            hasFodKeylayout = hasFodKl,
            detectedKeylayouts = keylayoutFiles,
            hasUdfpsOverlay = hasUdfpsOverlay,
            detectedOverlays = overlayFiles,
            hasFodInitService = initServices.isNotEmpty(),
            detectedInitServices = initServices,
            missingFodComponentsFromHost = missingComponents,
            portingReadinessScore = score
        )

        // Write to FORGER/Data/IMGSTRACT/
        File(destinationDataImgstract, "unpacked_image_dna.json").writeText(jsonHelper.encodeToString(imageDna))
        File(destinationDataImgstract, "unpacked_blobs_inventory.json").writeText(jsonHelper.encodeToString(blobsList))
        File(destinationDataImgstract, "unpacked_fod_support.json").writeText(jsonHelper.encodeToString(fodSupport))

        onLog("[IMGSTRACT-DONE] Fichiers sauvegardés avec succès dans : ${destinationDataImgstract.absolutePath}")

        return UnpackedImageInspectionResult(
            imageDna = imageDna,
            fodSupport = fodSupport,
            totalBlobsFound = blobsList.size,
            blobsList = blobsList,
            destinationFolder = destinationDataImgstract.absolutePath
        )
    }
}
