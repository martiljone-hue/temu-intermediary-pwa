package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.blueprint.AutomationBlueprintCode
import com.example.ui.theme.MazaganEmerald
import com.example.ui.theme.MazaganGold
import com.example.ui.theme.TerracottaOrange

@Composable
fun BlueprintInspectorScreen() {
    val context = LocalContext.current
    val artifacts = AutomationBlueprintCode.allArtifacts
    var selectedArtifactId by rememberSaveable { mutableStateOf(artifacts.first().id) }
    val activeArtifact = artifacts.firstOrNull { it.id == selectedArtifactId } ?: artifacts.first()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("blueprint_inspector_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MazaganEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MazaganGold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ملفات الكود والأتمتة الكاملة (Python + Supabase + PWA)",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "جميع الملفات المطلوبة جاهزة للنسخ الفوري: سكريبت الموظف الذكي بـ Python، قاعدة بيانات Supabase SQL، ملف GitHub Actions (Cron 2:00 AM)، ومكونات React + TypeScript PWA.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        // File selector chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                artifacts.forEach { item ->
                    val isSelected = item.id == activeArtifact.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) TerracottaOrange else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedArtifactId = item.id }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = item.titleDarija,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.relativePath,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Active File Code Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeArtifact.titleDarija,
                                style = MaterialTheme.typography.titleMedium,
                                color = MazaganEmerald
                            )
                            Text(
                                text = activeArtifact.descriptionDarija,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(activeArtifact.relativePath, activeArtifact.content)
                                )
                                Toast.makeText(context, "تم نسخ ${activeArtifact.relativePath}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MazaganEmerald,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ الكود")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ")
                        }
                    }

                    // Render source code in LTR block for clean code indentation
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0B1D18),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "// File: ${activeArtifact.relativePath} (${activeArtifact.language})",
                                    color = MazaganGold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = activeArtifact.content,
                                    color = Color(0xFFE6F4EE),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
