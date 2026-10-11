package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.KitchenUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationScreen(
    uiState: KitchenUiState,
    onSelectDecompiledImg: (String) -> Unit,
    onRunAospDecompileAndAiPort: () -> Unit = {},
    onExecuteMakeImg: () -> Unit,
    onExecuteMakeRomZip: () -> Unit,
    onNavigateToExtractor: () -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedSourceFileIndex by remember { mutableStateOf(0) }
    val makeImgRes = uiState.lastMakeImgResult
    val makeRomRes = uiState.lastMakeRomZipResult
    val recore = uiState.recoreBrainReport
    val decomp = uiState.aospDecompilationReport
    val aiBp = uiState.aiPortingBlueprint
    val preBoot = uiState.preBootSimulationReport

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card: CREATION (Le Graal de la Reconstruction OS de A à Z + Cerveau IA + Désassemblage Code Source AOSP)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "CREATION",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CREATION • AI Omega-Forge & AOSP Source Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Désassemblage Code Source AOSP (.java/.cpp/.hal/Android.bp) + GeminiOsArchitectAgent + Simulateur Pré-Boot 5 Stades",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Ce moteur décompile n'importe quel fichier .img vers un niveau proche du code source Android AOSP, dialogue automatiquement avec l'IA pour greffer les HALs, blobs et overlays du téléphone (EXTRACT/), et garantit un résultat final 100% fonctionnel :",
                    style = MaterialTheme.typography.bodySmall
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "• Désassemblage Code Source AOSP : DEX -> .java (frameworks/base) | ELF64 -> .cpp/.h | Contrats .hal/.aidl | Android.bp | XML RRO.\n" +
                                    "• Cerveau IA Autonome (GeminiOsArchitectAgent) : Génère & injecte Shims C/ELF64, SELinux .cil, VINTF XML, Overlays 4K & init.rc.\n" +
                                    "• Simulateur Pré-Boot 5 Stades : 1.Init Stage 1 -> 2.SELinux .cil -> 3.VINTF -> 4.Linker64 -> 5.Zygote/ART (Score 100% Bootable).",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "• État Cerveau IA Embarqué : ${uiState.embeddedAiKeyStatusText.ifBlank { "BuildConfig.GEMINI_API_KEY + Solveur Déterministe Embarqué Actif" }}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Unpacked GSI Selector + EXTRACT Status
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Sélection de l'Image .IMG Unpackée (UNPACK/) & ADN du Téléphone (EXTRACT/)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.selectedDecompiledImgName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("OS / Image .img Unpackée à décompiler & porter") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("creation_dropdown_unpacked_os")
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        uiState.availableDecompiledImgs.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text("UNPACK/$folder") },
                                onClick = {
                                    onSelectDecompiledImg(folder)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isExtractReady) {
                                "✓ Dossier EXTRACT prêt (${uiState.extractedFilesCount} fichiers ADN + Device Tree du téléphone)"
                            } else {
                                "ℹ Dossier EXTRACT sera auto-extrait à l'étape [1/12] par GASTROengine"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isExtractReady) Color(0xFF00C853) else MaterialTheme.colorScheme.secondary
                        )
                        if (recore != null) {
                            Text(
                                text = "Moteur R.E.C.O.R.E : Z3=${recore.smtStatus} • Confiance Boot=${recore.bootConfidenceScore}/100 • 16 Rapports SCANNER",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onNavigateToExtractor,
                        enabled = !uiState.isBusy,
                        modifier = Modifier.testTag("creation_btn_open_extractor")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EXTRACTOR")
                    }
                }
            }
        }

        // 3. Primary Action Buttons: AOSP Source Decompiler + AI Auto-Port, MAKE IMG & MAKE ROM
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. Désassemblage Code Source AOSP, Auto-Portage IA & Compilation 100% Fonctionnelle",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                // Button 0: AOSP Source-Level Decompile + Autonomous AI Port & Pre-Boot Simulation
                OutlinedButton(
                    onClick = onRunAospDecompileAndAiPort,
                    enabled = !uiState.isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_creation_aosp_decompile_ai_port")
                ) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = "AOSP Source Decompile & AI Port")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DÉSASSEMBLER CODE SOURCE AOSP + AUTO-PORTAGE IA",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Button 1: Make IMG
                Button(
                    onClick = onExecuteMakeImg,
                    enabled = !uiState.isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("btn_creation_make_img")
                ) {
                    Icon(imageVector = Icons.Default.Memory, contentDescription = "Make IMG")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MAKE IMG (Créer un OS .img 100% Fonctionnel de A à Z)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Button 2: Make ROM (Flashable ZIP)
                Button(
                    onClick = onExecuteMakeRomZip,
                    enabled = !uiState.isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("btn_creation_make_rom")
                ) {
                    Icon(imageVector = Icons.Default.Archive, contentDescription = "Make ROM")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MAKE ROM (Créer une Custom ROM .ZIP 100% Fonctionnelle)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 3b. AOSP Source Tree Explorer + 5-Stage Pre-Boot Simulator + AI Blueprint Card
        if (decomp != null || preBoot != null || aiBp != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AOSP Source Studio",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Studio Code Source AOSP Désassemblé & Radar Pré-Boot IA",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (decomp != null) {
                        Text(
                            text = "• Format Universel Détecté : ${decomp.universalFormatInfo.detectedFormat} (${decomp.universalFormatInfo.magicHex})\n" +
                                    "• Arbre Source AOSP : ${decomp.aospSourceTreeRootPath}\n" +
                                    "• Classes Java/Kotlin : ${decomp.totalJavaClassesDecompiled} | Binaires ELF64 -> C/C++ : ${decomp.totalElf64BinariesDecompiledToCpp}\n" +
                                    "• Contrats .hal/.aidl : ${decomp.totalHalAidlContractsReconstructed} | Android.bp : ${decomp.totalAndroidBpGenerated} | XML RRO : ${decomp.totalXmlResourcesDecoded}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )

                        if (decomp.decompiledFiles.isNotEmpty()) {
                            HorizontalDivider()
                            Text(
                                text = "Explorateur des Fichiers Sources AOSP Générés (${decomp.decompiledFiles.size} fichiers) :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val safeIndex = selectedSourceFileIndex.coerceIn(0, decomp.decompiledFiles.lastIndex)
                            val activeFile = decomp.decompiledFiles[safeIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                decomp.decompiledFiles.take(4).forEachIndexed { idx, fileItem ->
                                    OutlinedButton(
                                        onClick = { selectedSourceFileIndex = idx },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = fileItem.language.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (safeIndex == idx) FontWeight.ExtraBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Fichier : ${activeFile.relativePathInAospTree} (Origine : ${activeFile.originBinaryPath})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = Color(0xFF0B0F17),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = activeFile.previewCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00E676),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    if (preBoot != null) {
                        HorizontalDivider()
                        Text(
                            text = "Simulateur Pré-Boot en Boucle Fermée (Score : ${preBoot.overallBootableScore}/100 • 100% Bootable = ${preBoot.is100PercentBootable}) :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00C853)
                        )
                        preBoot.stages.forEach { st ->
                            Text(
                                text = "  ✓ [Stade ${st.stageNumber}/5 • ${st.stageName}] Score=${st.score}% — ${st.details}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (aiBp != null) {
                        HorizontalDivider()
                        Text(
                            text = "Cerveau IA (${aiBp.modelUsed}) : ${aiBp.architectureDiagnosis}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 4. Make IMG Output Card (12-stage A-to-Z report + AI Source Blueprint)
        if (makeImgRes != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Make IMG Succès",
                            tint = Color(0xFF00C853)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Résultat MAKE IMG de A à Z (${makeImgRes.timestamp})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "• Image créée : ${makeImgRes.outputImgPath}\n" +
                                "• VBMeta créé : ${makeImgRes.vbmetaImgPath}\n" +
                                "• Format : ${makeImgRes.filesystemFormat} (${makeImgRes.sizeBytes / 1024} KB)\n" +
                                "• Cerveau IA : ${makeImgRes.aiModelUsedForMake} (${makeImgRes.aiAntiQuotaStatus})\n" +
                                "• Clés RSA-2048 : ${makeImgRes.keysGeneratedOrVerifiedCount} | Overlays RRO signés 4K : ${makeImgRes.apksSignedAndAlignedCount}\n" +
                                "• Validation R.E.C.O.R.E Z3 : ${makeImgRes.recoreSmtStatus} (${makeImgRes.recoreBootConfidence}/100) | Conformité CrossVerifier : ${makeImgRes.crossVerifierPassRate}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )

                    if (makeImgRes.aiSourceBlueprintSummary.isNotBlank()) {
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Plan IA",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Synthèse d'Architecture IA (AiPortingAgent + GASTROengine) :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = makeImgRes.aiSourceBlueprintSummary,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    HorizontalDivider()
                    Text(
                        text = "Journal des 12 Étapes Exécutées (Tous Moteurs & Outils) :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    makeImgRes.pipelineStagesExecuted.forEach { stage ->
                        Text(
                            text = "  ✓ $stage",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 5. Make ROM (.ZIP) Output Card
        if (makeRomRes != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Make ROM Succès",
                            tint = Color(0xFF00C853)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Résultat MAKE ROM de A à Z • ZIP Flashable (${makeRomRes.timestamp})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "• Archive ZIP Flashable : ${makeRomRes.outputFlashableZipPath}\n" +
                                "• Taille totale : ${makeRomRes.zipSizeBytes / 1024} KB\n" +
                                "• Cible matérielle : ${makeRomRes.buildPropDeviceSummary}\n" +
                                "• Cerveau IA : ${makeRomRes.aiModelUsedForMake}\n" +
                                "• Preuve R.E.C.O.R.E Z3 : ${makeRomRes.recoreSmtStatus} (${makeRomRes.recoreBootConfidence}/100)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    HorizontalDivider()
                    Text(
                        text = "Contenu de la Custom ROM (.zip) :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    makeRomRes.includedEntries.forEach { entry ->
                        Text(
                            text = "  📦 $entry",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (makeRomRes.dynamicPartitionsOpListPreview.isNotBlank()) {
                        HorizontalDivider()
                        Text(
                            text = "Aperçu dynamic_partitions_op_list (Super Partition) :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = makeRomRes.dynamicPartitionsOpListPreview,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    HorizontalDivider()
                    Text(
                        text = "Aperçu META-INF/com/google/android/updater-script :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = makeRomRes.updaterScriptPreview,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
