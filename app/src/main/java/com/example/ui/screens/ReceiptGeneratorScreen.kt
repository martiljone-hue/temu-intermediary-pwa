package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ProxyOrder
import com.example.ui.receipt.PdfReceiptExporter
import com.example.ui.theme.MazaganEmerald
import com.example.ui.theme.MazaganGold
import com.example.ui.theme.TerracottaOrange
import com.example.ui.theme.WhatsAppGreen
import com.example.viewmodel.DataUiState

@Composable
fun ReceiptGeneratorScreen(
    ordersState: DataUiState<List<ProxyOrder>>,
    onMarkOrderStatus: (orderId: String, status: String) -> Unit,
    onCreateSampleDeliveredOrder: (itemCount: Int) -> Unit
) {
    val context = LocalContext.current
    var selectedOrderIndex by remember { mutableIntStateOf(0) }
    var pdfExportError by remember { mutableStateOf<String?>(null) }

    val orders = (ordersState as? DataUiState.Success)?.data.orEmpty()
    val activeOrder: ProxyOrder? = orders.getOrNull(selectedOrderIndex.coerceIn(0, (orders.size - 1).coerceAtLeast(0)))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("receipt_generator_screen"),
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
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MazaganGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "مولد تواصيل التسليم A4 (White-Label PDF)",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "• إزالة تامة لأي علامة تجارية أو شعار أو عنوان أصلي لـ Temu.\n• تحجيم وتوزيع تلقائي للصور (1 أو 2 أو 3 بطاقات صور أنيقة) داخل صفحة A4 واحدة فقط.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Dynamic Scaling Test Order Generator Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "توليد طلبية تجريبية لمعاينة تحجيم الصور التلقائي في صفحة A4 واحدة:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedOrderIndex = 0
                            onCreateSampleDeliveredOrder(1)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("بطاقة 1 صورة")
                    }
                    OutlinedButton(
                        onClick = {
                            selectedOrderIndex = 0
                            onCreateSampleDeliveredOrder(2)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("بطاقتان (2 صور)")
                    }
                    OutlinedButton(
                        onClick = {
                            selectedOrderIndex = 0
                            onCreateSampleDeliveredOrder(3)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("3 بطاقات صور")
                    }
                }
            }
        }

        if (orders.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اختر طلبية لمعاينة وتصدير توصيل A4 PDF:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        orders.forEachIndexed { index, ord ->
                            val isSelected = index == selectedOrderIndex
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) TerracottaOrange else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedOrderIndex = index }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = "#${ord.orderCode} • ${ord.customerName}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${ord.itemTitlesDarija.size} سلع • ${ord.totalMad} درهم (${ord.status})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (pdfExportError != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pdfExportError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (activeOrder != null) {
            // Order Status Action Bar & Export PDF Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeOrder.status != "delivered") {
                        OutlinedButton(
                            onClick = { onMarkOrderStatus(activeOrder.id, "delivered") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تأكيد التسليم")
                        }
                    }

                    Button(
                        onClick = {
                            pdfExportError = null
                            PdfReceiptExporter.exportAndShareWhiteLabelA4Pdf(
                                context = context,
                                order = activeOrder,
                                onError = { pdfExportError = it }
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerracottaOrange,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp)
                            .testTag("export_a4_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تصدير ومشاركة A4 PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Single-Page A4 Visual Preview Component (AspectRatio 1 : 1.414)
            item {
                SinglePageA4ReceiptPreviewCard(order = activeOrder)
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لا توجد طلبيات حالياً. اضغط على أحد الأزرار أعلاه لتوليد توصيل A4 تجريبي بـ 1 أو 2 أو 3 صور.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SinglePageA4ReceiptPreviewCard(order: ProxyOrder) {
    val imageKeys = order.itemImageKeys.ifEmpty { listOf("blender") }.take(3)
    val cardCount = imageKeys.size

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f / 1.414f)
            .border(1.5.dp, Color(0xFFE2DACB), RoundedCornerShape(18.dp))
            .testTag("a4_single_page_preview")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. White-Label Header (Strictly NO Temu mentions)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MazaganEmerald,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MAZAGAN EXPRESS • الجديدة",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "توصيل تسليم رسمي (White-Label Receipt)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MazaganGold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "#${order.orderCode}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (order.status == "delivered") "مسلّمة ✓" else "قيد التسليم",
                            style = MaterialTheme.typography.labelMedium,
                            color = WhatsAppGreen
                        )
                    }
                }
            }

            // 2. Customer Info Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFAF7F2),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2DACB), RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "الزبون: ${order.customerName}   |   الهاتف: ${order.customerPhone}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF141D1A)
                    )
                    Text(
                        text = "عنوان التسليم بمدينة الجديدة: ${order.customerAddress}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF53635E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Dynamic Image Scaling Cards (1, 2, or 3 cards scaled neatly onto the single A4 page)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "صور السلع المسلمة (${cardCount} بطاقات صور موزعة تلقائياً فصفحة وحدة):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MazaganEmerald,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 0 until cardCount) {
                        val key = imageKeys[i]
                        val caption = order.itemTitlesDarija.getOrNull(i) ?: "سلعة #${i + 1}"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFAF7F2))
                                .border(1.dp, Color(0xFFE2DACB), RoundedCornerShape(10.dp))
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = PdfReceiptExporter.drawableResForImageKey(key)),
                                contentDescription = caption,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (cardCount == 1) 105.dp else if (cardCount == 2) 86.dp else 72.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = caption,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF141D1A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 4. Compact White-Labeled Itemized Lines
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "جدول السلع (عناوين بالدارجة بدون اسم المورد الأصلي):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MazaganEmerald,
                    fontWeight = FontWeight.Bold
                )
                order.itemTitlesDarija.take(4).forEachIndexed { idx, title ->
                    val price = order.itemPricesMad.getOrNull(idx) ?: 0L
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAF7F2), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}. $title",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF141D1A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$price درهم",
                            style = MaterialTheme.typography.labelLarge,
                            color = MazaganEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 5. Single-Page A4 Footer with Transparent 15 MAD Service Fee
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF3EFE6),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2DACB), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "مجموع السلع الأصلي (0% زيادة): ${order.subtotalMad} درهم",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF53635E)
                        )
                        Text(
                            text = "خدمة التوصيل: +${order.serviceFeeMad} درهم",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF53635E)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المجموع النهائي المؤدى:",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF141D1A),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${order.totalMad} درهم (MAD)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TerracottaOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
