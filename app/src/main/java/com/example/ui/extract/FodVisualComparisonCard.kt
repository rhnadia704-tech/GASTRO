package com.example.ui.extract

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.extract.DeepFodDiagnosticReport
import com.example.core.extract.DisassemblyComparativeReport
import com.example.core.extract.FodComparativeAnalysisReport
import com.example.core.extract.FodDependencyDiffItem
import com.example.core.extract.FodDiffSeverity
import com.example.core.extract.HardwareChainStep

@Composable
fun FodVisualComparisonCard(
    report: FodComparativeAnalysisReport,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Couverture & Écarts, 1: Autopsie IA, 2: Désassemblage DEX/ELF
    var isDetailsExpanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
            .fillMaxWidth()
            .testTag("fod_visual_comparison_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Comparaison",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Synthèse Comparative FOD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Téléphone hôte ⟷ Image '${report.unpackedImageName}'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Global Score Gauge Badge
                val scoreColor = when {
                    report.overallCompatibilityScore >= 75 -> Color(0xFF10B981)
                    report.overallCompatibilityScore >= 45 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }

                Surface(
                    color = scoreColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${report.overallCompatibilityScore}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = scoreColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Compatibilité",
                            style = MaterialTheme.typography.labelSmall,
                            color = scoreColor
                        )
                    }
                }
            }

            // Navigation Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TabPillButton(
                    selected = selectedTab == 0,
                    label = "Couverture & Liens",
                    icon = Icons.Default.Analytics,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabPillButton(
                    selected = selectedTab == 1,
                    label = "Autopsie IA",
                    icon = Icons.Default.Psychology,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1.1f)
                )
                TabPillButton(
                    selected = selectedTab == 2,
                    label = "Désassemblage",
                    icon = Icons.Default.DataObject,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.weight(1.1f)
                )
            }

            HorizontalDivider()

            when (selectedTab) {
                0 -> {
                    // TAB 0: Visual Coverage Bar Chart & Dependency Diff Matrix
                    Text(
                        text = "Graphique de Couverture des Composants FOD (Hôte vs Image) :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FodMetricComparisonBar(
                            label = "Blobs & HALs (.so)",
                            hostPercent = 100,
                            unpackedPercent = report.blobsCoveragePercent
                        )
                        FodMetricComparisonBar(
                            label = "Services Init & Daemons (.rc)",
                            hostPercent = 100,
                            unpackedPercent = report.initServicesCoveragePercent
                        )
                        FodMetricComparisonBar(
                            label = "Propriétés Système (build.prop)",
                            hostPercent = 100,
                            unpackedPercent = report.propertiesCoveragePercent
                        )
                        FodMetricComparisonBar(
                            label = "Overlays UDFPS & Framework",
                            hostPercent = 100,
                            unpackedPercent = report.overlaysCoveragePercent
                        )
                    }

                    // Synthèse Rapide Hôte vs Image
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFF0F291E),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "OK",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sur Votre Téléphone",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = report.whyItWorksOnHostRom,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF2B1214),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Erreur",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sur l'Image Unpackée",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = report.whyItFailsOnUnpackedImage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Matrice des dépendances
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Matrice des Dépendances & Liens Manquants (${report.diffItems.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(onClick = { isDetailsExpanded = !isDetailsExpanded }) {
                            Text(if (isDetailsExpanded) "Réduire" else "Détailler")
                        }
                    }

                    AnimatedVisibility(
                        visible = isDetailsExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            report.diffItems.forEach { item ->
                                FodDiffItemRow(item = item)
                            }
                        }
                    }

                    // Plan d'action rapide
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Conseils",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Directives de Résolution pour l'Automatisation du Portage :",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            report.portingActionPlan.forEach { action ->
                                Text(
                                    text = "• $action",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Deep AI Autopsy: Why it works on Phone and why it breaks on GSI
                    val aiReport = report.deepAiDiagnostic ?: buildFallbackAiReport(report)
                    FodDeepAiAutopsyView(aiReport = aiReport)
                }

                2 -> {
                    // TAB 2: Disassembly & Bytecode DEX/ELF Explorer
                    val disasmReport = report.disassemblyReport ?: buildFallbackDisasmReport(report)
                    FodDisassemblyExplorerView(disasmReport = disasmReport)
                }
            }
        }
    }
}

@Composable
private fun TabPillButton(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(38.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FodMetricComparisonBar(
    label: String,
    hostPercent: Int,
    unpackedPercent: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Hôte: $hostPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00E676),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Image: $unpackedPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unpackedPercent >= 70) Color(0xFF10B981) else if (unpackedPercent >= 40) Color(0xFFF59E0B) else Color(0xFFEF4444),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val w = size.width
            val h = size.height

            val unpackedWidth = (w * (unpackedPercent / 100f)).coerceIn(0f, w)
            val barColor = when {
                unpackedPercent >= 70 -> Color(0xFF10B981)
                unpackedPercent >= 40 -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }

            drawRect(
                color = barColor,
                topLeft = Offset(0f, 0f),
                size = Size(unpackedWidth, h)
            )

            drawLine(
                color = Color(0xFF00E676),
                start = Offset(w - 2f, 0f),
                end = Offset(w - 2f, h),
                strokeWidth = 4f
            )
        }
    }
}

@Composable
fun FodDiffItemRow(item: FodDependencyDiffItem) {
    val statusColor = when (item.severity) {
        FodDiffSeverity.COMPATIBLE -> Color(0xFF10B981)
        FodDiffSeverity.MINOR -> Color(0xFF3B82F6)
        FodDiffSeverity.MAJOR -> Color(0xFFF59E0B)
        FodDiffSeverity.CRITICAL -> Color(0xFFEF4444)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.componentName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hôte : ${item.hostStatus}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Text(
                    text = "Image : ${item.unpackedStatus}",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }

            if (item.severity != FodDiffSeverity.COMPATIBLE) {
                Text(
                    text = "↳ Remède : ${item.remedyAction}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun buildFallbackAiReport(report: FodComparativeAnalysisReport): DeepFodDiagnosticReport {
    return DeepFodDiagnosticReport(
        deviceBrand = "Xiaomi",
        deviceModel = "Mi Note 10 (tucana)",
        deviceCodename = "sm6150",
        socPlatform = "Qualcomm sm6150",
        sensorVendor = report.hostDeviceModel,
        sensorType = "Optique sous l'écran (OLED/UDFPS)",
        whyItWorksOnHostPhone = report.whyItWorksOnHostRom,
        hostHardwareChain = listOf(
            HardwareChainStep(1, "Pilote Noyau Goodix", "/dev/goodix_fp", "FONCTIONNEL_OEM", "Interruption tactile et liaison SPI active."),
            HardwareChainStep(2, "HAL Vendor & Extension", "vendor.xiaomi.hardware.fingerprintextension@1.0", "FONCTIONNEL_OEM", "Service d'extension propriétaire chargé au démarrage."),
            HardwareChainStep(3, "services.jar Framework", "BiometricService (OEM patché)", "FONCTIONNEL_OEM", "Gestion de l'appui et liaison avec SystemUI."),
            HardwareChainStep(4, "Illumination OLED HBM", "/sys/class/drm/card0-DSI-1/disp_param", "FONCTIONNEL_OEM", "Ioctl d'activation du mode haute luminosité à 1000 nits sans dimlayer.")
        ),
        whyItFailsOnGsiDespiteSameVendor = report.whyItFailsOnUnpackedImage,
        gsiRupturesList = listOf(
            HardwareChainStep(1, "AOSP ignore l'extension vendor", "AOSP Fingerprint21", "RUPTURE_GSI", "AOSP ne déclenche pas extTouchDown()."),
            HardwareChainStep(2, "Pas d'illumination HBM (Cécité)", "/sys/class/drm/card0-DSI-1/disp_param", "RUPTURE_GSI", "Le capteur prend une photo dans le noir."),
            HardwareChainStep(3, "Classes de pont manquantes", "services.jar GSI", "RUPTURE_GSI", "Classes constructeur introuvables."),
            HardwareChainStep(4, "Blocage SELinux", "plat_file_contexts", "RUPTURE_GSI", "Droits d'accès au sysfs refusés.")
        ),
        disassemblyGapsSummary = "L'inspection binaire montre l'absence des hooks d'extension constructeur dans le framework de la GSI.",
        concretePortingRoadmap = report.portingActionPlan,
        trebleOverlaySnippet = "<bool name=\"config_supportsInDisplayFingerprint\">true</bool>",
        smaliHookSnippet = "invoke-static {}, Lcom/android/server/biometrics/FodHbmHelper;->turnOnHbm()V",
        sepolicyRulesSnippet = "allow system_server sysfs_graphics:file rw_file_perms;"
    )
}

private fun buildFallbackDisasmReport(report: FodComparativeAnalysisReport): DisassemblyComparativeReport {
    return DisassemblyComparativeReport(
        hostSourceName = "Téléphone Hôte",
        gsiSourceName = report.unpackedImageName,
        overallParityScore = 65,
        totalHostClasses = 1240,
        totalGsiClasses = 980,
        missingClassesInGsiCount = 3,
        missingClassesList = listOf(
            "vendor.xiaomi.hardware.fingerprintextension.V1_0.IXiaomiFingerprint",
            "vendor.goodix.hardware.biometrics.fingerprint.V2_1.IGoodixFingerprintDaemon",
            "com.android.systemui.biometrics.FodCircleView"
        ),
        totalHostSymbols = 850,
        totalGsiSymbols = 620,
        missingSymbolsInGsiCount = 2,
        missingSymbolsList = listOf(
            "Java_vendor_xiaomi_hardware_fingerprintextension_extTouchDown",
            "gf_hal_authenticate"
        ),
        aidlHidlInterfaceGaps = listOf("vendor.xiaomi.hardware.fingerprintextension@1.0"),
        jniBridgeGaps = listOf("Java_vendor_xiaomi_hardware_fingerprintextension_extTouchDown"),
        portingPatchRecommendations = report.portingActionPlan
    )
}
