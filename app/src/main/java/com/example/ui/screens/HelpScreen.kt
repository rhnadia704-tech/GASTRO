package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.docs.GeneratedPdfDocResult
import com.example.ui.KitchenTab
import com.example.ui.theme.AppThemePreference
import java.io.File

data class HelpGuideSection(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val steps: List<String>,
    val commands: List<Pair<String, String>>,
    val targetTab: KitchenTab? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HelpScreen(
    romForgeRootPath: String,
    themePreference: AppThemePreference = AppThemePreference.SYSTEM,
    onSelectThemePreference: (AppThemePreference) -> Unit = {},
    onNavigateToTab: (KitchenTab) -> Unit,
    onRunCommandInTerminal: (String) -> Unit,
    isBusy: Boolean = false,
    lastGeneratedPdfDoc: GeneratedPdfDocResult? = null,
    onGenerateTechnicalPdf: (((File) -> Unit)?) -> Unit = {},
    onSaveTechnicalPdfToUri: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            onSaveTechnicalPdfToUri(uri)
        }
    }
    // 0 = Thème & Aides/Consignes/Astuces, 1 = Commandes & Règles, 2 = Changelog & Features
    var activeSectionTab by remember { mutableIntStateOf(0) }

    val guideSections = listOf(
        HelpGuideSection(
            title = "1. Organisation des Dossiers ROM_FORGE",
            subtitle = "Emplacement : $romForgeRootPath",
            icon = Icons.Default.Folder,
            steps = listOf(
                "• UNPACK/   : Contient vos fichiers .img décompilés (EXT4, EROFS, Sparse) et le dossier UKA config/.",
                "• PACKED/   : Contient les images recompilées (system.img, vbmeta.img) compatibles ZArchiver & 7-Zip.",
                "• KEY/      : Contient vos 4 paires de clés RSA-2048 (.pk8 & .x509.pem) ainsi que manifest.json (Clé Note).",
                "• KEY/Data/ : Contient les rapports complets de vérification des signatures APK en format .json et .txt.",
                "• PORT/     : Espace dédié au portage GSI (fichiers portés, overlays RRO, FODstruct et image .img portée finale)."
            ),
            commands = listOf(
                "ls" to "Lister les dossiers principaux de ROM_FORGE",
                "ls unpack" to "Afficher les systèmes décompilés dans UNPACK"
            ),
            targetTab = KitchenTab.CONSOLE
        ),
        HelpGuideSection(
            title = "2. Key Maker — Chaîne de Confiance RSA-2048",
            subtitle = "Génération des clés AOSP & Clé Note (manifest.json)",
            icon = Icons.Default.Key,
            steps = listOf(
                "• Renseignez votre Organisation (O), Common Name (CN) et Pays (C).",
                "• Appuyez sur « Générer les 4 Paires RSA-2048 » pour créer platform, media, shared et testkey.",
                "• Astuce : Régénérez vos clés avant toute resignature globale pour garantir une chaîne cryptographique unique."
            ),
            commands = listOf(
                "ls key" to "Vérifier les clés RSA-2048 et rapports présents dans KEY/"
            ),
            targetTab = KitchenTab.KEY_MAKER
        ),
        HelpGuideSection(
            title = "3. Sign Pro — Resignature & Clé Entière d'APK",
            subtitle = "Signature d'un système UNPACK, d'un APK unique, et rapport complet",
            icon = Icons.Default.Bolt,
            steps = listOf(
                "• Choisissez le système décompilé via la liste déroulante UNPACK.",
                "• Cliquez sur « Tout Signer » pour resigner les APKs et synchroniser automatiquement plat_mac_permissions.xml, privapp-permissions et build.prop (Zéro Bootloop).",
                "• Cliquez sur « Vérifier (-> KEY/Data) » ou « Clé & Infos » sur un APK pour inspecter sa clé X.509 PEM entière, le modulus RSA hexadécimal complet, SHA-256/SHA-1/MD5 et les permissions."
            ),
            commands = listOf(
                "verify-apks" to "Lancer l'audit complet de la clé et des signatures de tous les APKs",
                "zipalign -v 4 SystemUI.apk SystemUI_aligned.apk" to "Vérifier l'alignement 4K de resources.arsc"
            ),
            targetTab = KitchenTab.SIGN_PRO
        ),
        HelpGuideSection(
            title = "4. Generator — Cache ART (.odex / .vdex) & fs-verity",
            subtitle = "Pré-compilation Ahead-Of-Time via dex2oat ARM64",
            icon = Icons.Default.Memory,
            steps = listOf(
                "• Sélectionnez le système décompilé dans UNPACK.",
                "• Choisissez le filtre de compilation (speed-profile ou speed) et activez fs-verity.",
                "• Astuce : Le cache MD5 évite de recompiler les APKs inchangés et réduit le premier boot de 60%."
            ),
            commands = listOf(
                "dex2oat --dex-file=SystemUI.apk --oat-file=SystemUI.odex --instruction-set=arm64 --compiler-filter=speed-profile" to "Compiler un APK en ODEX/VDEX ARM64"
            ),
            targetTab = KitchenTab.GENERATOR
        ),
        HelpGuideSection(
            title = "5. Compilator — Unpack & Repack UKA (EXT4 / EROFS)",
            subtitle = "Inspiré de blackeangel/UKA, 100% compatible ZArchiver & 7-Zip",
            icon = Icons.Default.Build,
            steps = listOf(
                "• Unpack : Extrait les images EXT4, Sparse-EXT4, EROFS et Sparse-EROFS dans ROM_FORGE/UNPACK/<nom>/ et génère config/<part>_fs_config et config/<part>_file_contexts.",
                "• Repack : Reconstruit une image EXT4 multi-groupes (Superblock 0xEF53, GDT, Bitmaps, Extents, Xattrs SELinux) ou EROFS v1 (0xE0F5E1E2) + vbmeta.img dans ROM_FORGE/PACKED/."
            ),
            commands = listOf(
                "mke2fs -L system -M /system -t ext4 -b 4096 system_packed.img" to "Construire une partition EXT4 4K",
                "avbtool make_vbmeta_image --output vbmeta.img --flags 3" to "Générer une image vbmeta.img"
            ),
            targetTab = KitchenTab.COMPILER
        ),
        HelpGuideSection(
            title = "6. EXTRACTOR — Extraction ADN du Téléphone (-> EXTRACT/)",
            subtitle = "ExtractMe propulsé par GASTROengine (DeviceProfiler + ShellOrchestrator)",
            icon = Icons.Default.Download,
            steps = listOf(
                "• Cliquez sur « ExtractMe (Extraire Téléphone -> Dossier EXTRACT/) » pour extraire en lecture seule l'ADN matériel et logiciel de votre téléphone.",
                "• Isole les partitions, points de montage, services init, symlinks SAR, VINTF et la logique FOD double (/system et /vendor) dans ROM_FORGE/EXTRACT/.",
                "• Déverrouille automatiquement le bouton PORT dans le module PORTER."
            ),
            commands = listOf(
                "ls" to "Vérifier l'espace de travail GASTRO"
            ),
            targetTab = KitchenTab.EXTRACTOR
        ),
        HelpGuideSection(
            title = "7. PORTER — PORT, PORTERPLAN & FOD (SCAN, AISCAN, Fix 1/2/3)",
            subtitle = "Portage GSI complet & Sonde FOD Dynamique (/system + /vendor)",
            icon = Icons.Default.AccountTree,
            steps = listOf(
                "• Bouton « PORT » : Crée le portage complet vers l'OS unpacké sélectionné à partir du dossier EXTRACT/.",
                "• Bouton « PORTERPLAN » : Ouvre la page dédiée comparant l'architecture du GSI et celle du téléphone (pourquoi ça marche sur la ROM hôte et pourquoi ça échoue sur le GSI).",
                "• Bouton « FOD » : Ouvre la page dédiée FOD avec SCAN (/system + /vendor), AISCAN (IA Gemini + Anti-Quota + GASTROengine) et les 3 correctifs : FOD fix 1 (Stock-Grade), FOD fix 2 (Bidouillage sans modif APK) et FOD fix 3 (Assisté par IA + GASTROengine + R.E.C.O.R.E)."
            ),
            commands = listOf(
                "getprop" to "Inspecter les propriétés matérielles de votre téléphone",
                "ls port" to "Vérifier les dossiers et images .img portés dans ROM_FORGE/PORT"
            ),
            targetTab = KitchenTab.AUTO_PORTER
        ),
        HelpGuideSection(
            title = "8. CREATION — Le Graal MAKE IMG & MAKE ROM (De A à Z)",
            subtitle = "Reconstruction d'OS en 12 étapes (Tous moteurs + IA + R.E.C.O.R.E)",
            icon = Icons.Default.Bolt,
            steps = listOf(
                "• Bouton « MAKE IMG » : Reconstruit un OS .img complet de A à Z en enchaînant automatiquement les 12 étapes (EXTRACTOR -> StructureAligner -> IA Gemini -> PORTER -> FOD Fix 3 -> VintfReconciler -> KeyMaker -> SignPro -> Generator -> FsParser -> R.E.C.O.R.E 16 rapports + Z3 -> CrossVerifier).",
                "• Bouton « MAKE ROM » : Exécute les 12 étapes puis assemble une Custom ROM complète en archive .ZIP flashable (dynamic_partitions_op_list, updater-script, system.img, vbmeta.img, ADN EXTRACT)."
            ),
            commands = listOf(
                "recore --smt-z3" to "Vérifier la preuve formelle Z3 SMT et les 16 rapports SCANNER"
            ),
            targetTab = KitchenTab.CREATION
        )
    )

    val commandRules = listOf(
        "Règle 1 • Alignement 4096 octets (Page-Size 4K) : Toute modification d'APK système (framework-res.apk, SystemUI.apk) DOIT conserver resources.arsc et les bibliothèques .so en mode STORED (non compressé) alignés sur 4096 octets.",
        "Règle 2 • Cohérence SELinux & MAC : Après une resignature d'APK système, le certificat hexadécimal X.509 complet DOIT être mis à jour dans etc/selinux/plat_mac_permissions.xml sous peine de rejet par PackageManagerService au boot.",
        "Règle 3 • Permissions Critiques Init : Le binaire /system/bin/init doit obligatoirement avoir l'UID 0, GID 2000 et le mode octal 0750 avec le contexte u:object_r:init_exec:s0 dans config/system_fs_config et system_file_contexts.",
        "Règle 4 • Exclusion des Métadonnées UKA au Repack : Les dossiers de travail config/ et ROM_FORGE_META/ servent à alimenter e2fsdroid / mkfs.erofs mais ne doivent jamais être injectés comme des fichiers APKs à l'intérieur de la racine EXT4.",
        "Règle 5 • Sonde FOD Double Partition (/system + /vendor) : Le FOD nécessite l'alignement simultané de la logique /system (keylayout dynamique, keycode 338/304, coordonnées RRO) et de la logique /vendor (nœud sysfs HBM, DimLayer, Touch et HAL biométrique)."
    )

    val changelogFeatures = listOf(
        "v4.0 — GASTROengine (10 Modules Rust), EXTRACTOR, PORTER (FOD 1/2/3) & CREATION (MAKE A-à-Z)" to listOf(
            "Nouveau module EXTRACTOR avec bouton ExtractMe isolant tout l'ADN du téléphone (/system + /vendor) dans ROM_FORGE/EXTRACT/ via ShellOrchestrator.",
            "Refonte complète de PORTER avec liste déroulante UNPACK, bouton PORT, page dédiée PORTERPLAN et page dédiée FOD (SCAN, AISCAN avec IA Gemini + Anti-Quota, FOD fix 1, FOD fix 2, FOD fix 3).",
            "Sonde dynamique HostSystemAndVendorFodProbe inspectant à la fois /system et /vendor du téléphone pour un fix FOD sur-mesure non-statique.",
            "Nouveau module CREATION (Le Graal) : boutons MAKE IMG et MAKE ROM reconstruisant un OS .img ou une Custom ROM .zip flashable de A à Z en 12 étapes unifiant tous les moteurs et R.E.C.O.R.E (SCANNER 16 rapports + COMPARE + Z3 SMT)."
        ),
        "v3.0 — Reconstruction Complète de l'Interface & Thème Dynamique" to listOf(
            "Barre latérale unifiée et barre de navigation inférieure à 3 volets : Action (module actif), Console (log entier, historique, progression et terminal), Paramètres (thème, PDF technique, aides, règles, changelog).",
            "Support complet du Thème Système (Auto), Mode Clair et Mode Sombre."
        ),
        "v2.8 — Unpacker & Repacker UKA 1:1 (EXT4 / EROFS / DSU Sideloader)" to listOf(
            "Moteur inspiré de blackeangel/UKA avec Repack Simple & Intelligent 1:1 (clonage bit-à-bit ou greffe chirurgicale Copy-On-Write ExactImageCloneEngine)."
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // =========================================================================
        // CARD 1: THEME SETTINGS (LIGHT, DARK, FOLLOW SYSTEM)
        // =========================================================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_theme_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PARAMÈTRES DE L'APPLICATION • THÈME D'AFFICHAGE",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Choisissez entre le mode Clair, le mode Sombre ou le suivi automatique du thème système Android.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemePreference.entries.forEach { pref ->
                            val selected = themePreference == pref
                            val icon = when (pref) {
                                AppThemePreference.SYSTEM -> Icons.Default.SettingsBrightness
                                AppThemePreference.LIGHT -> Icons.Default.LightMode
                                AppThemePreference.DARK -> Icons.Default.DarkMode
                            }
                            val shortTitle = when (pref) {
                                AppThemePreference.SYSTEM -> "Système"
                                AppThemePreference.LIGHT -> "Clair"
                                AppThemePreference.DARK -> "Sombre"
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectThemePreference(pref) },
                                label = {
                                    Text(
                                        text = shortTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = pref.label,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_${pref.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CARD 1.5: DOWNLOADABLE TECHNICAL MANUAL, ARCHITECTURE & AI AUDIT PDF
        // =========================================================================
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_pdf_manual_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MANUEL TECHNIQUE COMPLET, ARBORESCENCE & AUDIT IA (.PDF TÉLÉCHARGEABLE)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Génère un fichier PDF complet contenant la description de chaque fonctionnalité, la liste des outils, le manuel d'utilisation, la structure et l'arborescence complète de l'application (prêt à être partagé auprès d'une autre IA).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "• Section 1 : Description exhaustive de chaque fonctionnalité (Unpack/Repack UKA v5.27, Repack Simple & Intelligent 1:1, FOD Fix Solutions 1 & 2, Sign PRO & Tout Signer Pro, R.E.C.O.R.E 12 Piliers, Régénérateur AOSP OAT/VDEX/ODEX/fsv_meta).\n" +
                                "• Section 2 : Liste complète de tous les outils et moteurs internes qui composent l'application.\n" +
                                "• Section 3 : Manuel d'utilisation complet étape par étape (DSU Sideloader, Unpack/Repack 1:1, FOD Fix, R.E.C.O.R.E, Sign Pro).\n" +
                                "• Section 4 : Structure logicielle et arborescence complète des packages Kotlin et du dossier ROM_FORGE/.\n" +
                                "• Section 5 : Diagnostic technique approfondi des correctifs Anti-Bootloop DSU Sideloader (EXT4 CoW, METADATA_CSUM, xattr e_value_offs, SELinux secilc, VINTF libvintf).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            onGenerateTechnicalPdf { _ ->
                                createPdfLauncher.launch("ROM_Forge_Documentation_Complete_Architecture_IA_Audit.pdf")
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_technical_pdf_saf_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Télécharger / Enregistrer le PDF Complet (Choisir l'emplacement)")
                    }

                    OutlinedButton(
                        onClick = {
                            onGenerateTechnicalPdf { pdfFile ->
                                Toast.makeText(
                                    context,
                                    "PDF généré dans Téléchargements & ${pdfFile.absolutePath}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_technical_pdf_direct_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Générer dans Téléchargements & ROM_FORGE/DOCS/")
                    }

                    if (lastGeneratedPdfDoc != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PDF Prêt (${lastGeneratedPdfDoc.pageCount} pages • ${lastGeneratedPdfDoc.sizeBytes / 1024} KB • ${lastGeneratedPdfDoc.generatedAtLabel})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Téléchargements : ${lastGeneratedPdfDoc.publishedDownloadPath ?: lastGeneratedPdfDoc.pdfFile.absolutePath}\n" +
                                            "ROM_FORGE/DOCS : ${lastGeneratedPdfDoc.pdfFile.absolutePath}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CARD 2: SECTION SELECTOR (AIDES & ASTUCES | COMMANDES & RÈGLES | CHANGELOG)
        // =========================================================================
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = activeSectionTab == 0,
                    onClick = { activeSectionTab = 0 },
                    label = { Text("Aides, Consignes & Astuces", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("settings_subtab_guides")
                )
                FilterChip(
                    selected = activeSectionTab == 1,
                    onClick = { activeSectionTab = 1 },
                    label = { Text("Commandes & Règles", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Rule, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("settings_subtab_rules")
                )
                FilterChip(
                    selected = activeSectionTab == 2,
                    onClick = { activeSectionTab = 2 },
                    label = { Text("Changelog & Features", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("settings_subtab_changelog")
                )
            }
        }

        if (activeSectionTab == 0) {
            items(guideSections, key = { it.title }) { sec ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = sec.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = sec.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = sec.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            if (sec.targetTab != null) {
                                FilledTonalButton(
                                    onClick = { onNavigateToTab(sec.targetTab) }
                                ) {
                                    Text("Ouvrir", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            sec.steps.forEach { step ->
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        } else if (activeSectionTab == 1) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Rule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RÈGLES D'OR SUR LES COMMANDES & LA COHÉRENCE AOSP",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        commandRules.forEach { rule ->
                            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = rule,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            items(guideSections, key = { "cmd_${it.title}" }) { sec ->
                if (sec.commands.isNotEmpty()) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Commandes • ${sec.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            sec.commands.forEach { (cmd, desc) ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "$ $cmd",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Command", cmd))
                                                    Toast.makeText(context, "Commande copiée", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copier la commande",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    onRunCommandInTerminal(cmd)
                                                    onNavigateToTab(KitchenTab.CONSOLE)
                                                },
                                                modifier = Modifier.testTag("run_help_cmd_${cmd.take(8)}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Exécuter dans la Console",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(changelogFeatures, key = { it.first }) { (versionTitle, featuresList) ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = versionTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider()
                        featuresList.forEach { feat ->
                            Text(
                                text = "• $feat",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
