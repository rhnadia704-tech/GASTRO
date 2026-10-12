package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.assets.ExtractedBinary
import com.example.core.shell.ExecutionMode
import com.example.core.verifier.CrossVerificationSummary
import com.example.data.local.VerificationAlertEntity
import com.example.ui.TerminalLogEntry

@Composable
fun TopSystemStatusBar(
    executionMode: ExecutionMode,
    isRootAvailable: Boolean,
    isBusy: Boolean,
    activeTaskTitle: String,
    romForgePublicPath: String,
    hasAllFilesAccess: Boolean,
    availableDecompiledImgs: List<String>,
    selectedDecompiledImgName: String,
    binaries: List<ExtractedBinary>,
    verificationSummary: CrossVerificationSummary?,
    alerts: List<VerificationAlertEntity>,
    logsCount: Int,
    latestLogEntry: TerminalLogEntry?,
    isConsoleTabSelected: Boolean,
    onOpenDrawer: () -> Unit,
    onToggleConsoleTab: () -> Unit,
    onToggleMode: (ExecutionMode) -> Unit,
    onSelectDecompiledImg: (String) -> Unit,
    onRunVerifier: () -> Unit,
    onFixAllCoherence: () -> Unit,
    onRefreshStorage: () -> Unit
) {
    val context = LocalContext.current

    val allFilesSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshStorage()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FORGER • UNPACK & REPACK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "sdcard/FORGER/UNPACK  •  sdcard/FORGER/PACKED",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleConsoleTab() }
                    .testTag("top_console_tab_button"),
                color = if (isConsoleTabSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isBusy) Color(0xFFF59E0B) else Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Journal",
                        tint = if (isConsoleTabSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Journal ($logsCount)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isConsoleTabSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasAllFilesAccess) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Autoriser l'accès au dossier sdcard/FORGER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AssistChip(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                allFilesSettingsLauncher.launch(intent)
                            } catch (_: Exception) {
                                val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                allFilesSettingsLauncher.launch(fallback)
                            }
                        },
                        label = { Text("Autoriser", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.testTag("grant_all_files_chip")
                    )
                }
            }
        }

        if (isBusy) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            val statusText = latestLogEntry?.message ?: activeTaskTitle
            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
fun DecompiledImgTargetSelectorCard(
    title: String,
    availableFolders: List<String>,
    selectedFolderName: String,
    selectedFullPath: String,
    onSelectFolder: (String) -> Unit,
    onPickExternalSafTree: (Uri?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val folderTreePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        onPickExternalSafTree(uri)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalButton(
                    onClick = { folderTreePicker.launch(null) },
                    modifier = Modifier.testTag("pick_decompiled_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Autre dossier", style = MaterialTheme.typography.labelSmall)
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .testTag("unpack_dropdown_selector"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UNPACK / $selectedFolderName",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "sdcard/FORGER/UNPACK/$selectedFolderName",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Choisir le dossier dans UNPACK",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.88f)
                ) {
                    availableFolders.forEach { folder ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "UNPACK / $folder",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (folder == selectedFolderName) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "sdcard/FORGER/UNPACK/$folder",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (folder == selectedFolderName) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                expanded = false
                                onSelectFolder(folder)
                            },
                            modifier = Modifier.testTag("decompiled_img_chip_$folder")
                        )
                    }
                }
            }
        }
    }
}
