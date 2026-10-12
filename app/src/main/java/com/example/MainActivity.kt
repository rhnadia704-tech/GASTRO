package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.KitchenTab
import com.example.ui.RomKitchenViewModel
import com.example.ui.components.TopSystemStatusBar
import com.example.ui.screens.CompilerScreen
import com.example.ui.screens.ConsoleHistoryScreen
import com.example.ui.screens.HelpScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RomKitchenViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(themePreference = uiState.themePreference) {
                RomForgeKitchenApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RomForgeKitchenApp(viewModel: RomKitchenViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val alerts by viewModel.alertsFlow.collectAsStateWithLifecycle()
    val portHistory by viewModel.portHistoryFlow.collectAsStateWithLifecycle()

    if (uiState.currentTab != KitchenTab.COMPILER) {
        BackHandler {
            viewModel.selectTab(KitchenTab.COMPILER)
        }
    }

    val isConsolePaneActive = uiState.currentTab == KitchenTab.CONSOLE
    val isSettingsPaneActive = uiState.currentTab == KitchenTab.HELP
    val isExtractorPaneActive = uiState.currentTab == KitchenTab.EXTRACTOR
    val isUnpackRepackActive = uiState.currentTab == KitchenTab.COMPILER

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopSystemStatusBar(
                    executionMode = uiState.executionMode,
                    isRootAvailable = uiState.isRootAvailable,
                    isBusy = uiState.isBusy,
                    activeTaskTitle = uiState.activeTaskTitle,
                    romForgePublicPath = uiState.romForgePublicPath,
                    hasAllFilesAccess = uiState.hasAllFilesAccess,
                    availableDecompiledImgs = uiState.availableDecompiledImgs,
                    selectedDecompiledImgName = uiState.selectedDecompiledImgName,
                    binaries = uiState.extractedBinaries,
                    verificationSummary = uiState.verificationSummary,
                    alerts = alerts,
                    logsCount = uiState.terminalLogs.size,
                    latestLogEntry = uiState.terminalLogs.lastOrNull(),
                    isConsoleTabSelected = isConsolePaneActive,
                    onOpenDrawer = {},
                    onToggleConsoleTab = viewModel::toggleConsoleTab,
                    onToggleMode = viewModel::toggleExecutionMode,
                    onSelectDecompiledImg = viewModel::selectDecompiledImgFolder,
                    onRunVerifier = viewModel::runIntelligentCrossVerifier,
                    onFixAllCoherence = viewModel::fixAllCoherenceAndBootloopRisksForUnpackedImg,
                    onRefreshStorage = viewModel::refreshStorageStatusAndFolders
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = isUnpackRepackActive,
                        onClick = { viewModel.selectTab(KitchenTab.COMPILER) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "UNPACK & REPACK"
                            )
                        },
                        label = {
                            Text(
                                text = "UNPACK / REPACK",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("nav_pane_action")
                    )

                    NavigationBarItem(
                        selected = isExtractorPaneActive,
                        onClick = { viewModel.selectTab(KitchenTab.EXTRACTOR) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "EXTRACT"
                            )
                        },
                        label = {
                            Text(
                                text = "EXTRACT",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_extractor")
                    )

                    NavigationBarItem(
                        selected = isConsolePaneActive,
                        onClick = { viewModel.selectTab(KitchenTab.CONSOLE) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Journal"
                            )
                        },
                        label = {
                            Text(
                                text = "Journal (${uiState.terminalLogs.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_console")
                    )

                    NavigationBarItem(
                        selected = isSettingsPaneActive,
                        onClick = { viewModel.selectTab(KitchenTab.HELP) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Paramètres"
                            )
                        },
                        label = {
                            Text(
                                text = "Paramètres",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRailItem(
                            selected = isUnpackRepackActive,
                            onClick = { viewModel.selectTab(KitchenTab.COMPILER) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "UNPACK & REPACK"
                                )
                            },
                            label = { Text("UNPACK / REPACK") },
                            modifier = Modifier.testTag("nav_rail_compiler")
                        )
                        NavigationRailItem(
                            selected = isExtractorPaneActive,
                            onClick = { viewModel.selectTab(KitchenTab.EXTRACTOR) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "EXTRACT"
                                )
                            },
                            label = { Text("EXTRACT") },
                            modifier = Modifier.testTag("nav_rail_extractor")
                        )
                        NavigationRailItem(
                            selected = isConsolePaneActive,
                            onClick = { viewModel.selectTab(KitchenTab.CONSOLE) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "Journal"
                                )
                            },
                            label = { Text("Journal") },
                            modifier = Modifier.testTag("nav_rail_console")
                        )
                        NavigationRailItem(
                            selected = isSettingsPaneActive,
                            onClick = { viewModel.selectTab(KitchenTab.HELP) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Paramètres"
                                )
                            },
                            label = { Text("Paramètres") },
                            modifier = Modifier.testTag("nav_rail_settings")
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    androidx.compose.animation.Crossfade(
                        targetState = uiState.currentTab,
                        label = "tab_crossfade",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { targetTab ->
                        when (targetTab) {
                            KitchenTab.EXTRACTOR -> com.example.ui.screens.ExtractorScreen(
                                uiState = uiState,
                                onSelectUnpackedTarget = viewModel::selectExtractUnpackedTarget,
                                onExecuteExtractAll = viewModel::executeFullDeviceExtractAndAiDiagnostic,
                                onRequestRootAccess = viewModel::requestRootAccess,
                                onNavigateToConsole = { viewModel.selectTab(KitchenTab.CONSOLE) }
                            )

                            KitchenTab.CONSOLE -> ConsoleHistoryScreen(
                                uiState = uiState,
                                portHistory = portHistory,
                                onClearLogs = viewModel::clearLogs,
                                onExecuteCommand = viewModel::executeInteractiveTerminalCommand,
                                onSaveLogsToTxt = viewModel::exportTerminalLogsToTxtFile,
                                onSelectDecompiledImgAndNavigate = viewModel::selectDecompiledImgAndNavigate
                            )

                            KitchenTab.HELP -> HelpScreen(
                                romForgeRootPath = uiState.romForgePublicPath,
                                themePreference = uiState.themePreference,
                                onSelectThemePreference = viewModel::setThemePreference,
                                onNavigateToTab = viewModel::selectTab,
                                onRunCommandInTerminal = viewModel::executeInteractiveTerminalCommand,
                                isBusy = uiState.isBusy,
                                lastGeneratedPdfDoc = uiState.lastGeneratedPdfDoc,
                                onGenerateTechnicalPdf = viewModel::generateTechnicalManualPdf,
                                onSaveTechnicalPdfToUri = viewModel::saveTechnicalManualPdfToUri
                            )

                            else -> CompilerScreen(
                                uiState = uiState,
                                onSelectDecompiledImg = viewModel::selectDecompiledImgFolder,
                                onPickCustomSafTree = viewModel::selectCustomDecompiledDirectoryUri,
                                onUpdateOptions = viewModel::updateCompilerOptions,
                                onRunPreFlightAudit = viewModel::runPreFlightAudit,
                                onCompileImages = viewModel::compileFullSystemAndVbmeta,
                                onRepackSimple1To1 = viewModel::repackSimpleAndIntelligent1To1,
                                onImportImgUri = viewModel::importAndInspectExternalImg
                            )
                        }
                    }
                }
            }
        }
    }
}
