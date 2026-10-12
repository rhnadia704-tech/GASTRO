package com.example.ui.extract

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.extract.DisassemblyComparativeReport

@Composable
fun FodDisassemblyExplorerView(
    disasmReport: DisassemblyComparativeReport,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, CLASSES, JNI, AIDL

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Header Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DataObject,
                            contentDescription = "Disassembly",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Désassemblage & Parité Binaire DEX / ELF",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Score : ${disasmReport.overallParityScore}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DisasmMetricPill(
                        label = "Classes Hôte",
                        value = "${disasmReport.totalHostClasses}",
                        color = Color(0xFF10B981)
                    )
                    DisasmMetricPill(
                        label = "Classes GSI",
                        value = "${disasmReport.totalGsiClasses}",
                        color = Color(0xFF6B7280)
                    )
                    DisasmMetricPill(
                        label = "Classes Manquantes",
                        value = "${disasmReport.missingClassesInGsiCount}",
                        color = Color(0xFFEF4444)
                    )
                    DisasmMetricPill(
                        label = "Gaps Symboles",
                        value = "${disasmReport.missingSymbolsInGsiCount}",
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("Tous les écarts", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "CLASSES",
                onClick = { selectedFilter = "CLASSES" },
                label = { Text("Classes (${disasmReport.missingClassesInGsiCount})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "JNI",
                onClick = { selectedFilter = "JNI" },
                label = { Text("JNI (${disasmReport.jniBridgeGaps.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "AIDL",
                onClick = { selectedFilter = "AIDL" },
                label = { Text("AIDL/HIDL (${disasmReport.aidlHidlInterfaceGaps.size})", fontSize = 11.sp) }
            )
        }

        // Search textfield
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filtrer une classe ou symbole...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            singleLine = true
        )

        // Filtered elements list
        val displayItems = remember(searchQuery, selectedFilter, disasmReport) {
            val list = mutableListOf<DisassemblyGapDisplayItem>()

            if (selectedFilter == "ALL" || selectedFilter == "CLASSES") {
                disasmReport.missingClassesList.forEach { cls ->
                    list.add(
                        DisassemblyGapDisplayItem(
                            name = cls,
                            type = "DEX_CLASS",
                            missingLocation = "services.jar / framework",
                            advice = "Smali patch ou overlay requis"
                        )
                    )
                }
            }

            if (selectedFilter == "ALL" || selectedFilter == "JNI") {
                disasmReport.jniBridgeGaps.forEach { sym ->
                    list.add(
                        DisassemblyGapDisplayItem(
                            name = sym,
                            type = "JNI_SYMBOL",
                            missingLocation = "libandroid_runtime.so",
                            advice = "Shim natif libfod_shim.so"
                        )
                    )
                }
            }

            if (selectedFilter == "ALL" || selectedFilter == "AIDL") {
                disasmReport.aidlHidlInterfaceGaps.forEach { iface ->
                    list.add(
                        DisassemblyGapDisplayItem(
                            name = iface,
                            type = "AIDL_HIDL",
                            missingLocation = "VINTF compatibility_matrix.xml",
                            advice = "Déclaration VINTF requise"
                        )
                    )
                }
            }

            if (searchQuery.isNotBlank()) {
                list.filter { it.name.contains(searchQuery, ignoreCase = true) }
            } else {
                list
            }
        }

        if (displayItems.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Aucun écart binaire correspondant au filtre.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                displayItems.take(25).forEach { item ->
                    DisassemblyGapRow(item = item)
                }
            }
        }
    }
}

private data class DisassemblyGapDisplayItem(
    val name: String,
    val type: String, // DEX_CLASS, JNI_SYMBOL, AIDL_HIDL
    val missingLocation: String,
    val advice: String
)

@Composable
private fun DisasmMetricPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DisassemblyGapRow(item: DisassemblyGapDisplayItem) {
    val badgeColor = when (item.type) {
        "DEX_CLASS" -> Color(0xFFEF4444)
        "JNI_SYMBOL" -> Color(0xFF3B82F6)
        else -> Color(0xFFF59E0B)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "↳ ${item.advice}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9.5.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
