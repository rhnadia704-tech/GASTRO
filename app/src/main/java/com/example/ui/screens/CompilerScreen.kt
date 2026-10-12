package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.modules.compiler.FilesystemFormat
import com.example.ui.KitchenUiState
import com.example.ui.components.DecompiledImgTargetSelectorCard

@Composable
fun CompilerScreen(
    uiState: KitchenUiState,
    onSelectDecompiledImg: (String) -> Unit,
    onPickCustomSafTree: (Uri?) -> Unit,
    onUpdateOptions: (FilesystemFormat, Boolean, Boolean) -> Unit,
    onRunPreFlightAudit: (Boolean) -> Unit,
    onCompileImages: () -> Unit,
    onRepackSimple1To1: () -> Unit = {},
    onImportImgUri: (Uri?) -> Unit
) {
    val safImgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        onImportImgUri(uri)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // 1. UNPACK card -> sdcard/FORGER/UNPACK
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "1. UNPACK (Extraire une image .img)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Extrait le contenu du fichier .img dans sdcard/FORGER/UNPACK/<nom_img>/",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { safImgPicker.launch(arrayOf("*/*")) },
                            enabled = !uiState.isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("saf_import_img_button")
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choisir un fichier .img")
                        }

                        Button(
                            onClick = { onImportImgUri(null) },
                            enabled = !uiState.isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inspect_workspace_img_button")
                        ) {
                            Icon(imageVector = Icons.Default.Storage, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UNPACK")
                        }
                    }

                    uiState.lastMountedImgReport?.let { rep ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "UNPACK terminé : ${rep.fileName} (${rep.format})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${rep.extractedFilesCount} fichiers • ${rep.extractedDirsCount} dossiers • ${rep.extractedSymlinksCount} liens (${rep.extractedSizeMb} MB)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Dossier : sdcard/FORGER/UNPACK/${uiState.selectedDecompiledImgName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Folder Selector inside sdcard/FORGER/UNPACK
        item {
            DecompiledImgTargetSelectorCard(
                title = "2. Dossier sélectionné dans sdcard/FORGER/UNPACK :",
                availableFolders = uiState.availableDecompiledImgs,
                selectedFolderName = uiState.selectedDecompiledImgName,
                selectedFullPath = "sdcard/FORGER/UNPACK/${uiState.selectedDecompiledImgName}",
                onSelectFolder = onSelectDecompiledImg,
                onPickExternalSafTree = onPickCustomSafTree
            )
        }

        // 3. Single Simple REPACK -> sdcard/FORGER/PACKED
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "3. REPACK Simple",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Reconstruit le dossier sélectionné en fichier .img dans sdcard/FORGER/PACKED/",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onRepackSimple1To1,
                        enabled = !uiState.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("repack_simple_1to1_button")
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REPACK Simple (-> sdcard/FORGER/PACKED)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    uiState.lastCompilationOutput?.let { out ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "REPACK terminé (${out.format.name})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Fichier .img : ${out.systemImgPath}\nTaille : ${out.systemImgSizeBytes / 1024} KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
