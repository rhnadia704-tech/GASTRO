package com.example.core.extract

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
enum class FodDiffSeverity {
    CRITICAL,
    MAJOR,
    MINOR,
    COMPATIBLE
}

@Serializable
data class FodDependencyDiffItem(
    val componentName: String,
    val category: String, // BLOBS, INIT_RC, PROPERTIES, KEYLAYOUT, HBM_SYSFS, OVERLAYS
    val hostStatus: String,
    val unpackedStatus: String,
    val isPresentInUnpacked: Boolean,
    val severity: FodDiffSeverity,
    val remedyAction: String
)

@Serializable
data class FodComparativeAnalysisReport(
    val hostDeviceModel: String,
    val unpackedImageName: String,
    val overallCompatibilityScore: Int, // 0 to 100
    val blobsCoveragePercent: Int,
    val initServicesCoveragePercent: Int,
    val propertiesCoveragePercent: Int,
    val overlaysCoveragePercent: Int,
    val whyItWorksOnHostRom: String,
    val whyItFailsOnUnpackedImage: String,
    val differentialSummary: String,
    val diffItems: List<FodDependencyDiffItem>,
    val portingActionPlan: List<String>
)

object FodComparativeEngine {

    private val jsonHelper = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun compareHostAndUnpacked(
        hostSpec: FodChainSpecification,
        unpackedResult: UnpackedImageInspectionResult,
        outputDir: File
    ): FodComparativeAnalysisReport {
        outputDir.mkdirs()

        val items = mutableListOf<FodDependencyDiffItem>()

        // 1. Blobs comparison
        val hasHalBlob = unpackedResult.fodSupport.hasFodHalBlob
        items.add(
            FodDependencyDiffItem(
                componentName = "HAL Empreinte (${hostSpec.halInterface})",
                category = "BLOBS",
                hostStatus = "Actif (${hostSpec.sensorVendor})",
                unpackedStatus = if (hasHalBlob) "Détecté dans l'image" else "Absent de l'image unpackée",
                isPresentInUnpacked = hasHalBlob,
                severity = if (hasHalBlob) FodDiffSeverity.COMPATIBLE else FodDiffSeverity.CRITICAL,
                remedyAction = if (hasHalBlob) "Vérifier la compatibilité ABI" else "Transplanter les bibliothèques vendor/lib64/hw/biometrics.fingerprint*.so"
            )
        )

        val hasVendorExt = unpackedResult.blobsList.any { it.contains("fingerprintextension") || it.contains("displayfeature") }
        items.add(
            FodDependencyDiffItem(
                componentName = "Extensions d'Affichage & HBM (vendor.*.displayfeature)",
                category = "BLOBS",
                hostStatus = "Présent sur l'hôte (contrôle de la couche d'illumination)",
                unpackedStatus = if (hasVendorExt) "Présent" else "Manquant dans l'image cible",
                isPresentInUnpacked = hasVendorExt,
                severity = if (hasVendorExt) FodDiffSeverity.COMPATIBLE else FodDiffSeverity.CRITICAL,
                remedyAction = "Injecter vendor.xiaomi.hardware.displayfeature@1.0.so et ses dépendances"
            )
        )

        // 2. Keylayout
        val hasKl = unpackedResult.fodSupport.hasFodKeylayout
        items.add(
            FodDependencyDiffItem(
                componentName = "Disposition Touches (${hostSpec.keylayoutFile})",
                category = "KEYLAYOUT",
                hostStatus = "Configuré pour le réveil (Keycode ${hostSpec.keycodeDetected})",
                unpackedStatus = if (hasKl) "Keylayout présent" else "Absent (Écran ne détecte pas l'appui du doigt)",
                isPresentInUnpacked = hasKl,
                severity = if (hasKl) FodDiffSeverity.COMPATIBLE else FodDiffSeverity.MAJOR,
                remedyAction = "Copier ${hostSpec.keylayoutFile} dans /system/usr/keylayout/ ou /vendor/usr/keylayout/"
            )
        )

        // 3. Init Services
        val hasInit = unpackedResult.fodSupport.hasFodInitService
        items.add(
            FodDependencyDiffItem(
                componentName = "Démon de Service Init (android.hardware.biometrics.fingerprint*.rc)",
                category = "INIT_RC",
                hostStatus = "Démarré automatiquement au boot (${hostSpec.aidlOrHidlService})",
                unpackedStatus = if (hasInit) "Service déclaré" else "Non déclaré dans les scripts .rc",
                isPresentInUnpacked = hasInit,
                severity = if (hasInit) FodDiffSeverity.COMPATIBLE else FodDiffSeverity.CRITICAL,
                remedyAction = "Ajouter la strophe 'service vendor.fps_hal' dans un script init.target.rc"
            )
        )

        // 4. Overlays UDFPS
        val hasOverlay = unpackedResult.fodSupport.hasUdfpsOverlay
        items.add(
            FodDependencyDiffItem(
                componentName = "Overlay UDFPS (Coordonnées ${hostSpec.locationCoordinates})",
                category = "OVERLAYS",
                hostStatus = "Câblé dans le Framework & SystemUI (${hostSpec.sensorRadiusPx}px)",
                unpackedStatus = if (hasOverlay) "Overlay détecté" else "Absent (SystemUI ignore où afficher le cercle)",
                isPresentInUnpacked = hasOverlay,
                severity = if (hasOverlay) FodDiffSeverity.COMPATIBLE else FodDiffSeverity.CRITICAL,
                remedyAction = "Générer un RRO Overlay ciblant framework-res avec config_udfpsLocation et dimensions"
            )
        )

        // 5. Driver Node & HBM
        items.add(
            FodDependencyDiffItem(
                componentName = "Liaison Driver Sysfs (${hostSpec.hbmIlluminationNode})",
                category = "HBM_SYSFS",
                hostStatus = "Accès direct au DRM disp_param",
                unpackedStatus = "Nécessite droits SELinux & injection des props",
                isPresentInUnpacked = true,
                severity = FodDiffSeverity.MAJOR,
                remedyAction = "Définir les règles sepolicy 'allow system_server sysfs_graphics:file rw_file_perms'"
            )
        )

        // Scores calculation
        val blobsCoverage = if (hasHalBlob && hasVendorExt) 100 else if (hasHalBlob || hasVendorExt) 50 else 10
        val initCoverage = if (hasInit) 100 else 15
        val propsCoverage = if (unpackedResult.imageDna.buildPropsSummary.any { it.key.contains("fingerprint") }) 80 else 25
        val overlayCoverage = if (hasOverlay) 100 else 5
        val overallScore = ((blobsCoverage * 0.35) + (initCoverage * 0.2) + (propsCoverage * 0.15) + (overlayCoverage * 0.3)).toInt()

        val report = FodComparativeAnalysisReport(
            hostDeviceModel = hostSpec.sensorVendor,
            unpackedImageName = unpackedResult.imageDna.folderName,
            overallCompatibilityScore = overallScore,
            blobsCoveragePercent = blobsCoverage,
            initServicesCoveragePercent = initCoverage,
            propertiesCoveragePercent = propsCoverage,
            overlaysCoveragePercent = overlayCoverage,
            whyItWorksOnHostRom = "Sur votre téléphone hôte, la ROM constructeur dispose de l'ensemble de la chaîne fermée : le service SystemUI connaît exactement les coordonnées de l'écran (${hostSpec.locationCoordinates}), dialogue avec le HAL via l'interface Binder '${hostSpec.aidlOrHidlService}', et déclenche directement l'illumination HBM (lumière verte haute intensité) sur ${hostSpec.hbmIlluminationNode} grâce à des appels spécifiques du kernel.",
            whyItFailsOnUnpackedImage = "Sur l'image unpackée sélectionnée (${unpackedResult.imageDna.folderName}), le capteur ne fonctionne pas car : 1) Les overlays Framework UDFPS de géométrie du capteur sont absents ou génériques ; 2) Les bibliothèques spécifiques au fournisseur (${hostSpec.sensorVendor}) manquent ou sont privées de leurs dépendances d'affichage ; 3) Le démon d'empreinte n'a pas les permissions SELinux pour commander l'illumination de la dalle OLED.",
            differentialSummary = "Score de compatibilité FOD : $overallScore% • ${items.count { it.isPresentInUnpacked }} composants validés sur ${items.size}. Des adaptations sont indispensables pour activer l'empreinte sous l'écran sur cette image.",
            diffItems = items,
            portingActionPlan = listOf(
                "1. Transplanter les bibliothèques matérielles extraites du dossier fod_fingerprint/ vers l'image.",
                "2. Installer le keylayout ${hostSpec.keylayoutFile} dans /system/usr/keylayout/.",
                "3. Compiler un overlay RRO UDFPS avec les dimensions exactes X=${hostSpec.sensorWidthPx}, Y=${hostSpec.sensorHeightPx}, Rayon=${hostSpec.sensorRadiusPx}px.",
                "4. Injecter les propriétés système persistantes dans build.prop pour router les commandes d'illumination.",
                "5. Vérifier les contextes SELinux sur le node d'illumination ${hostSpec.hbmIlluminationNode}."
            )
        )

        File(outputDir, "fod_comparative_audit.json").writeText(jsonHelper.encodeToString(report))

        return report
    }
}
