package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ElJadidaCatalogDefaults
import com.example.ui.theme.MazaganEmerald
import com.example.ui.theme.MazaganGold
import com.example.ui.theme.TerracottaOrange
import com.example.ui.theme.WhatsAppGreen
import com.example.viewmodel.AiLocalizerState

@Composable
fun SmartEmployeeScreen(
    aiState: AiLocalizerState,
    onRunAiLocalizer: (
        temuGoodsId: String,
        originalTitle: String,
        originalDescription: String,
        regularPriceMad: Long,
        salesVolume: String,
        imageKey: String
    ) -> Unit,
    onPublishToCatalog: (AiLocalizerState.LocalizedSuccess) -> Unit
) {
    var temuGoodsId by rememberSaveable { mutableStateOf("TM-882410") }
    var originalTitle by rememberSaveable {
        mutableStateOf("Portable 6-Blade USB Rechargeable Personal Smoothie & Juice Blender Cup 380ml")
    }
    var originalDescription by rememberSaveable {
        mutableStateOf("Cordless personal size blender with 6 stainless steel blades, USB-C fast charging, BPA free bottle for beach and work.")
    }
    var regularPriceMadText by rememberSaveable { mutableStateOf("89") }
    var salesVolume by rememberSaveable { mutableStateOf("+4,800 مبيعة") }
    var selectedImageKey by rememberSaveable { mutableStateOf("blender") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_employee_screen"),
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
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(TerracottaOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "الموظف الذكي (Smart Employee AI)",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Temu RapidAPI → 0% Markup MAD → Gemini 3.8 Flash Darija",
                                style = MaterialTheme.typography.labelMedium,
                                color = MazaganGold
                            )
                        }
                    }

                    Text(
                        text = "يجلب هذا المحرك منتجات Temu الأعلى تقييماً بثمنها الأصلي بالدرهم المغربي (بدون أي زيادة 0%)، ثم يعيد صياغة العنوان بالدارجة المغربية المبسطة الموجهة لزبناء مدينة الجديدة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        // Preset selector from RapidAPI Temu Best-Sellers feed
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. اختر سلعة من خلاصة RapidAPI Temu Best-Sellers (أو عدّل البيانات):",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ElJadidaCatalogDefaults.rawTemuBestSellers.forEach { candidate ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (temuGoodsId == candidate.temuGoodsId) {
                                TerracottaOrange
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.clickable {
                                temuGoodsId = candidate.temuGoodsId
                                originalTitle = candidate.originalTitle
                                originalDescription = candidate.originalDescription
                                regularPriceMadText = candidate.regularTemuPriceMad.toString()
                                salesVolume = candidate.salesVolume
                                selectedImageKey = candidate.imageKey
                            }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = candidate.temuGoodsId,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (temuGoodsId == candidate.temuGoodsId) Color.White else MazaganEmerald
                                )
                                Text(
                                    text = "${candidate.regularTemuPriceMad} MAD (0% Markup)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (temuGoodsId == candidate.temuGoodsId) {
                                        Color.White.copy(alpha = 0.9f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input fields for AI localization
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = originalTitle,
                        onValueChange = { originalTitle = it },
                        label = { Text("العنوان الأصلي من Temu (Original Title)") },
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_original_title_input")
                    )

                    OutlinedTextField(
                        value = originalDescription,
                        onValueChange = { originalDescription = it },
                        label = { Text("الوصف الأصلي (Original Description)") },
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = regularPriceMadText,
                            onValueChange = { regularPriceMadText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("ثمن Temu الأصلي (MAD)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = salesVolume,
                            onValueChange = { salesVolume = it },
                            label = { Text("حجم المبيعات") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val priceMad = regularPriceMadText.toLongOrNull() ?: 89L
                            onRunAiLocalizer(
                                temuGoodsId,
                                originalTitle,
                                originalDescription,
                                priceMad,
                                salesVolume,
                                selectedImageKey
                            )
                        },
                        enabled = aiState !is AiLocalizerState.Loading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerracottaOrange,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("run_ai_localizer_button")
                    ) {
                        if (aiState is AiLocalizerState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جاري الترجمة للدارجة الجديدية عبر Gemini AI...")
                        } else {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ترجم وصايب العنوان بالدارجة المغربية (Gemini AI)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // AI Result Card
        item {
            when (aiState) {
                is AiLocalizerState.Idle -> Unit
                is AiLocalizerState.Loading -> Unit
                is AiLocalizerState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                is AiLocalizerState.LocalizedSuccess -> {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "نتيجة التعريب بالدارجة الجديدية:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MazaganEmerald
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MazaganEmerald
                                ) {
                                    Text(
                                        text = "${aiState.priceMad} درهم (0% زيادة)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MazaganGold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = aiState.darijaTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = aiState.darijaPitch,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "📍 الحي المستهدف: ${aiState.neighborhoodTag}  |  التصنيف: ${aiState.categoryDarija}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TerracottaOrange
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (aiState.savedToCatalog) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = WhatsAppGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تم حفظ ونشر المنتج فالمتجر بنجاح (Upserted to Cloud DB)!",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WhatsAppGreen
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onPublishToCatalog(aiState) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MazaganEmerald,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("publish_localized_product_button")
                                ) {
                                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("نشر المنتج فمتجر الجديدة (Upsert Product)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
