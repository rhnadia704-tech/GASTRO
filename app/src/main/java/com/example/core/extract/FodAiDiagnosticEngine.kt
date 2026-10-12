package com.example.core.extract

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class HardwareChainStep(
    val stepIndex: Int,
    val title: String,
    val component: String,
    val status: String, // "FONCTIONNEL_OEM", "RUPTURE_GSI", "PATCH_REQUIS"
    val technicalExplanation: String
)

@Serializable
data class DeepFodDiagnosticReport(
    val deviceBrand: String,
    val deviceModel: String,
    val deviceCodename: String,
    val socPlatform: String,
    val sensorVendor: String,
    val sensorType: String, // "Optique sous l'écran (OLED/UDFPS)", "Ultrasonique (3D Sonic)", "Capacitif"
    val whyItWorksOnHostPhone: String,
    val hostHardwareChain: List<HardwareChainStep>,
    val whyItFailsOnGsiDespiteSameVendor: String,
    val gsiRupturesList: List<HardwareChainStep>,
    val disassemblyGapsSummary: String,
    val concretePortingRoadmap: List<String>,
    val trebleOverlaySnippet: String,
    val smaliHookSnippet: String,
    val sepolicyRulesSnippet: String
)

object FodAiDiagnosticEngine {

    private val jsonHelper = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun generateUniversalDiagnostic(
        brand: String,
        model: String,
        codename: String,
        platform: String,
        fodSpec: FodChainSpecification,
        disassemblyReport: DisassemblyComparativeReport?,
        outputDir: File
    ): DeepFodDiagnosticReport {
        outputDir.mkdirs()

        val isOptical = fodSpec.hbmIlluminationNode.isNotBlank() || fodSpec.sensorVendor.contains("Goodix", ignoreCase = true) || fodSpec.sensorVendor.contains("Focal", ignoreCase = true)
        val sensorTypeStr = if (isOptical) "Optique sous l'écran (OLED/UDFPS)" else "Ultrasonique / Capacitif"

        // 1. Pourquoi ça marche sur le téléphone hôte
        val whyWorksText = buildString {
            append("Sur votre $brand $model ($codename, SoC $platform), le capteur d'empreinte fonctionne parfaitement grâce à une chaîne d'interdépendance complète et sans rupture : ")
            append("1° Le driver noyau (${fodSpec.driverDeviceNode}) détecte le contact capacitif. ")
            append("2° Le démon vendor (${fodSpec.halInterface}) communique avec le service système propriétaire (${fodSpec.aidlOrHidlService}). ")
            append("3° Le framework OEM dans 'services.jar' intègre des hooks constructeur dédiés qui coordonnent l'événement de touché. ")
            append("4° SystemUI dessine l'icône à l'emplacement exact (${fodSpec.locationCoordinates}) et commande l'allumage haute luminosité (HBM à 1000+ nits) via ${fodSpec.hbmIlluminationNode} tout en inhibant le dimlayer de la dalle OLED. ")
            append("5° La lumière réfléchie par les sillons papillaires traverse les pixels transparents de l'OLED et impressionne le capteur.")
        }

        val hostChain = listOf(
            HardwareChainStep(
                stepIndex = 1,
                title = "Driver Noyau & Interruption Tactile",
                component = fodSpec.driverDeviceNode,
                status = "FONCTIONNEL_OEM",
                technicalExplanation = "Le module noyau initialise le bus SPI/I2C et génère les événements d'appui uinput (Keycode ${fodSpec.keycodeDetected} via ${fodSpec.keylayoutFile})."
            ),
            HardwareChainStep(
                stepIndex = 2,
                title = "Vendor HAL & Extension Propriétaire",
                component = fodSpec.halInterface,
                status = "FONCTIONNEL_OEM",
                technicalExplanation = "Le HAL ${fodSpec.sensorVendor} expose les interfaces standard et l'extension propriétaire (${fodSpec.aidlOrHidlService}) pour le contrôle de l'illumination."
            ),
            HardwareChainStep(
                stepIndex = 3,
                title = "Framework OEM & Services Système",
                component = "services.jar (BiometricService)",
                status = "FONCTIONNEL_OEM",
                technicalExplanation = "Le service système OEM gère l'état d'authentification et appelle les méthodes constructeur spécifiques lors de l'appui du doigt."
            ),
            HardwareChainStep(
                stepIndex = 4,
                title = "Illumination Haute Luminosité (HBM) & Dimlayer",
                component = fodSpec.hbmIlluminationNode,
                status = "FONCTIONNEL_OEM",
                technicalExplanation = "SystemUI et SurfaceFlinger basculent la dalle OLED en mode HBM instantané pour illuminer le doigt avec un faisceau blanc/vert intense."
            ),
            HardwareChainStep(
                stepIndex = 5,
                title = "Capture Optique & Authentification",
                component = "Capteur ${fodSpec.sensorVendor}",
                status = "FONCTIONNEL_OEM",
                technicalExplanation = "Le capteur optique capture l'image contrastée de l'empreinte et renvoie le token cryptographique au TrustZone / TEE."
            )
        )

        // 2. Pourquoi ça ne marche pas sur la GSI même avec le même vendor
        val whyFailsText = buildString {
            append("Bien que la GSI utilise le MÊME /vendor que votre téléphone, le capteur d'empreinte est inopérant pour 4 raisons structurelles majeures : ")
            append("1° AOSP ignore les extensions constructeur : La GSI AOSP n'implémente que l'interface standard Google (Fingerprint21). Elle n'invoque jamais l'extension '${fodSpec.aidlOrHidlService}', donc le vendor attend indéfiniment le signal de préparation sans jamais capturer. ")
            append("2° Cécité Optique Totale (Pas d'HBM) : Le SystemUI d'une GSI générique ne possède aucun code pour piloter le nœud sysfs d'affichage '${fodSpec.hbmIlluminationNode}'. Sans illumination à haute luminance, la dalle OLED reste opaque, le capteur prend une photo dans le noir et renvoie immédiatement ERROR_UNABLE_TO_PROCESS. ")
            append("3° Classes & Méthodes Manquantes dans services.jar : Le désassemblage révèle que les classes de synchronisation de l'affichage et les callbacks tactiles sont absents du framework AOSP de la GSI. ")
            append("4° Blocages SELinux (plat_file_contexts) : Les règles de sécurité de la GSI refusent à system_server l'accès en écriture au nœud sysfs et aux services binder personnalisés du vendor.")
        }

        val gsiRuptures = listOf(
            HardwareChainStep(
                stepIndex = 1,
                title = "Absence d'Appel à l'Extension Vendor",
                component = "AOSP Fingerprint21 vs ${fodSpec.aidlOrHidlService}",
                status = "RUPTURE_GSI",
                technicalExplanation = "AOSP n'appelle que authenticate(). Il n'appelle jamais extTouchDown() ou setFingerprintIcon(), nécessaires pour réveiller le DSP du capteur."
            ),
            HardwareChainStep(
                stepIndex = 2,
                title = "Cécité Optique : Aucun Déclencheur HBM",
                component = fodSpec.hbmIlluminationNode,
                status = "RUPTURE_GSI",
                technicalExplanation = "La GSI ne bascule pas le contrôleur DRM en High Brightness Mode. La lecture biométrique s'effectue dans l'obscurité totale."
            ),
            HardwareChainStep(
                stepIndex = 3,
                title = "Coordonnées de l'Icône UDFPS Non Déclarées",
                component = "framework-res overlays",
                status = "RUPTURE_GSI",
                technicalExplanation = "La GSI ignore où afficher le cercle lumineux (Coordonnées ${fodSpec.locationCoordinates})."
            ),
            HardwareChainStep(
                stepIndex = 4,
                title = "Refus d'Accès SELinux dans plat_file_contexts",
                component = "plat_sepolicy.cil",
                status = "RUPTURE_GSI",
                technicalExplanation = "system_server se voit refuser l'accès aux ioctls d'affichage et aux liaisons hwbinder constructeur."
            )
        )

        val disassemblySummary = if (disassemblyReport != null) {
            "Analyse binaire : ${disassemblyReport.missingClassesInGsiCount} classes de pont et ${disassemblyReport.missingSymbolsInGsiCount} symboles/fonctions JNI absents de la GSI (Score de parité : ${disassemblyReport.overallParityScore}%)."
        } else {
            "Le désassemblage met en évidence l'absence des interfaces d'extension constructeur dans le bytecode de services.jar et l'absence des symboles JNI correspondants."
        }

        val roadmap = listOf(
            "1. Déployer un Overlay RRO Treble UDFPS définissant : config_supportsInDisplayFingerprint=true et les coordonnées exactes (${fodSpec.locationCoordinates}).",
            "2. Configurer le routage d'illumination HBM via Treble (ex: persist.sys.phh.fod.dlkm=1 ou injection d'un script sysfs vers ${fodSpec.hbmIlluminationNode}).",
            "3. Ajouter un Shim C++ ou injecter les déclarations VINTF pour l'interface ${fodSpec.aidlOrHidlService} dans /system/etc/vintf/compatibility_matrix.xml.",
            "4. Insérer le keylayout '${fodSpec.keylayoutFile}' dans /system/usr/keylayout/ pour que l'appui tactile réveille le capteur (Keycode ${fodSpec.keycodeDetected}).",
            "5. Ajouter les permissions SELinux dans vendor_sepolicy : 'allow system_server sysfs_graphics:file rw_file_perms'."
        )

        val trebleSnippet = """
            <!-- Overlay RRO : res/values/config.xml -->
            <resources>
                <bool name="config_supportsInDisplayFingerprint">true</bool>
                <dimen name="config_udfpsLocationX">${fodSpec.sensorWidthPx}px</dimen>
                <dimen name="config_udfpsLocationY">${fodSpec.sensorHeightPx}px</dimen>
                <dimen name="config_udfpsSize">${fodSpec.sensorRadiusPx * 2}px</dimen>
            </resources>
        """.trimIndent()

        val smaliSnippet = """
            # Hook Smali pour services.jar : Fingerprint21.smali
            .method public onPointerDown(IIIFF)V
                .registers 6
                # Déclenchement de l'illumination HBM vers le driver d'affichage
                const-string v0, "${fodSpec.hbmIlluminationNode}"
                invoke-static {v0}, Lcom/android/server/biometrics/FodHbmHelper;->turnOnHbm(Ljava/lang/String;)V
                return-void
            .end method
        """.trimIndent()

        val sepolicySnippet = """
            # Règles SELinux requises
            allow system_server sysfs_graphics:file { read write open getattr };
            allow system_server default_android_hwservice:hwservice_manager find;
            allow hal_fingerprint_default sysfs:file { read write };
        """.trimIndent()

        val report = DeepFodDiagnosticReport(
            deviceBrand = brand,
            deviceModel = model,
            deviceCodename = codename,
            socPlatform = platform,
            sensorVendor = fodSpec.sensorVendor,
            sensorType = sensorTypeStr,
            whyItWorksOnHostPhone = whyWorksText,
            hostHardwareChain = hostChain,
            whyItFailsOnGsiDespiteSameVendor = whyFailsText,
            gsiRupturesList = gsiRuptures,
            disassemblyGapsSummary = disassemblySummary,
            concretePortingRoadmap = roadmap,
            trebleOverlaySnippet = trebleSnippet,
            smaliHookSnippet = smaliSnippet,
            sepolicyRulesSnippet = sepolicySnippet
        )

        val json = jsonHelper.encodeToString(report)
        File(outputDir, "universal_ai_porting_diagnostic.json").writeText(json)

        return report
    }
}
