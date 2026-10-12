package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
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
    val steps: List<String>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HelpScreen(
    romForgeRootPath: String,
    themePreference: AppThemePreference = AppThemePreference.SYSTEM,
    onSelectThemePreference: (AppThemePreference) -> Unit = {},
    onNavigateToTab: (KitchenTab) -> Unit = {},
    onRunCommandInTerminal: (String) -> Unit = {},
    isBusy: Boolean = false,
    lastGeneratedPdfDoc: GeneratedPdfDocResult? = null,
    onGenerateTechnicalPdf: (((File) -> Unit)?) -> Unit = {},
    onSaveTechnicalPdfToUri: (Uri) -> Unit = {}
) {
    val guideSections = listOf(
        HelpGuideSection(
            title = "1. Dossiers sdcard/FORGER",
            subtitle = "Emplacement des fichiers",
            icon = Icons.Default.Folder,
            steps = listOf(
                "• sdcard/FORGER/UNPACK/ : Contient les dossiers extraits après un UNPACK d'une image .img.",
                "• sdcard/FORGER/PACKED/ : Contient les fichiers .img reconstruits après un REPACK Simple."
            )
        ),
        HelpGuideSection(
            title = "2. Utilisation UNPACK & REPACK Simple",
            subtitle = "Fonctionnement direct",
            icon = Icons.Default.Build,
            steps = listOf(
                "• UNPACK : Choisissez un fichier .img pour extraire son contenu dans sdcard/FORGER/UNPACK/<nom_img>/.",
                "• Sélection : Choisissez le dossier extrait dans la liste déroulante.",
                "• REPACK Simple : Reconstruit le dossier sélectionné en fichier .img dans sdcard/FORGER/PACKED/."
            )
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thème de l'application",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemePreference.entries.forEach { pref ->
                            val icon = when (pref) {
                                AppThemePreference.SYSTEM -> Icons.Default.SettingsBrightness
                                AppThemePreference.LIGHT -> Icons.Default.LightMode
                                AppThemePreference.DARK -> Icons.Default.DarkMode
                            }
                            FilterChip(
                                selected = themePreference == pref,
                                onClick = { onSelectThemePreference(pref) },
                                label = { Text(pref.label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("theme_chip_${pref.name}")
                            )
                        }
                    }
                }
            }
        }

        items(guideSections) { section ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = section.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    section.steps.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
