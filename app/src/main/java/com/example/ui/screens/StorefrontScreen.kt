package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.ElJadidaCatalogDefaults
import com.example.data.model.Product
import com.example.ui.receipt.PdfReceiptExporter
import com.example.ui.theme.MazaganEmerald
import com.example.ui.theme.MazaganEmeraldLight
import com.example.ui.theme.MazaganGold
import com.example.ui.theme.TerracottaOrange
import com.example.ui.theme.WhatsAppGreen
import com.example.viewmodel.DataUiState
import com.example.viewmodel.OrderConfirmationReceipt

@Composable
fun StorefrontScreen(
    productsState: DataUiState<List<Product>>,
    cartItems: List<CartItem>,
    selectedProductModal: Product?,
    isCheckoutModalOpen: Boolean,
    lastConfirmedOrder: OrderConfirmationReceipt?,
    actionError: String?,
    onClearError: () -> Unit,
    onProductClick: (Product) -> Unit,
    onCloseProductModal: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onOpenCheckout: (Product?) -> Unit,
    onCloseCheckout: () -> Unit,
    onSubmitWhatsAppOrder: (name: String, phone: String, address: String, onLaunchWhatsApp: (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var selectedNeighborhoodFilter by rememberSaveable { mutableStateOf("الكل") }
    val neighborhoodFilters = remember {
        listOf("الكل", "سيدي بوزيد", "حي المطار", "القلعة", "حي السلام", "وسط المدينة")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("storefront_list"),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner
            item {
                Card(
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(215.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_el_jadida_hero_1793928385328),
                            contentDescription = "توصيل طلبات تيمو بمدينة الجديدة",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MazaganEmerald.copy(alpha = 0.35f),
                                            MazaganEmerald.copy(alpha = 0.92f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TerracottaOrange
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ثمن تيمو الأصلي 0% زيادة + 15 درهم خدمة التوصيل",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "مازاغان بروكسي • طلبات Temu فالجديدة",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "عناوين مترجمة بالدارجة المغربية عبر الموظف الذكي والطلب فخطوة وحدة عبر واتساب.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Neighborhood Filter Bar
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "التوصيل السريع لجميع أحياء الجديدة:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        neighborhoodFilters.forEach { chip ->
                            val selected = selectedNeighborhoodFilter == chip
                            FilterChip(
                                selected = selected,
                                onClick = { selectedNeighborhoodFilter = chip },
                                label = {
                                    Text(
                                        text = chip,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MazaganEmerald,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            when (productsState) {
                is DataUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TerracottaOrange)
                        }
                    }
                }

                is DataUiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = productsState.message,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                is DataUiState.Success -> {
                    val filtered = productsState.data.filter { prod ->
                        selectedNeighborhoodFilter == "الكل" ||
                            prod.neighborhoodTag.contains(selectedNeighborhoodFilter)
                    }
                    items(filtered, key = { it.id }) { product ->
                        ProductDarijaCard(
                            product = product,
                            onCardClick = { onProductClick(product) },
                            onQuickOrder = { onOpenCheckout(product) },
                            onAddToCart = { onAddToCart(product) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // Floating Cart Bar if items in cart
        if (cartItems.isNotEmpty()) {
            val subtotal = cartItems.sumOf { it.product.priceMad * it.quantity }
            val finalTotal = subtotal + ElJadidaCatalogDefaults.FIXED_SERVICE_FEE_MAD
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MazaganEmerald,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .widthIn(max = 580.dp)
                    .clickable { onOpenCheckout(null) }
                    .testTag("floating_checkout_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TerracottaOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${cartItems.sumOf { it.quantity }}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "المجموع النهائي (+15 درهم خدمة)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "$finalTotal درهم (MAD)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MazaganGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { onOpenCheckout(null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إتمام الطلب",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    // CRUCIAL RULE: Full-Screen Modal (100% viewport height/width, NO bottom sheets) for Product Details
    if (selectedProductModal != null) {
        FullScreenProductModal(
            product = selectedProductModal,
            onDismiss = onCloseProductModal,
            onOrderNow = { onOpenCheckout(selectedProductModal) },
            onAddToCart = {
                onAddToCart(selectedProductModal)
                onCloseProductModal()
            }
        )
    }

    // CRUCIAL RULE: Full-Screen Modal (100% viewport height/width, NO bottom sheets) for Single-Step Checkout
    if (isCheckoutModalOpen) {
        FullScreenCheckoutModal(
            cartItems = cartItems,
            confirmedOrder = lastConfirmedOrder,
            actionError = actionError,
            onClearError = onClearError,
            onRemoveItem = onRemoveFromCart,
            onDismiss = onCloseCheckout,
            onSubmitOrder = { name, phone, address ->
                onSubmitWhatsAppOrder(name, phone, address) { whatsappUrl ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // On emulator without browser/WhatsApp, local confirmation screen still shows
                    }
                }
            }
        )
    }
}

@Composable
private fun ProductDarijaCard(
    product: Product,
    onCardClick: () -> Unit,
    onQuickOrder: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("product_card_${product.temuGoodsId}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                Image(
                    painter = painterResource(id = PdfReceiptExporter.drawableResForImageKey(product.imageKey)),
                    contentDescription = product.darijaTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top-Right Badge: 0% Markup Temu Exact Price
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MazaganEmerald.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = MazaganGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ثمن Temu الأصلي (0% زيادة)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                // Bottom-Left Sales Volume Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TerracottaOrange,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.salesVolume,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TerracottaOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.neighborhoodTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = TerracottaOrange,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = product.darijaTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = product.darijaPitch,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${product.priceMad} درهم",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TerracottaOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+ 15 درهم خدمة التوصيل للطلبية كاملة",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onAddToCart,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "أضف للسلة",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Button(
                            onClick = onQuickOrder,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MazaganEmerald,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("quick_order_${product.temuGoodsId}")
                        ) {
                            Text(
                                text = "اطلب دابا",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenProductModal(
    product: Product,
    onDismiss: () -> Unit,
    onOrderNow: () -> Unit,
    onAddToCart: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler { onDismiss() }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("full_screen_product_modal"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Full-screen Modal Top Bar
                Surface(
                    color = MazaganEmerald,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "تفاصيل السلعة (شاشة كاملة 100dvh)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_product_modal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        Image(
                            painter = painterResource(id = PdfReceiptExporter.drawableResForImageKey(product.imageKey)),
                            contentDescription = product.darijaTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TerracottaOrange.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = product.salesVolume,
                                style = MaterialTheme.typography.labelLarge,
                                color = TerracottaOrange,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MazaganEmerald.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = product.neighborhoodTag,
                                style = MaterialTheme.typography.labelLarge,
                                color = MazaganEmerald,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = product.darijaTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = product.darijaPitch,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "شفافية الأثمنة 100% فالجديدة:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MazaganEmerald
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ثمن السلعة فـ Temu (0% زيادة):")
                                Text(
                                    text = "${product.priceMad} درهم",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("خدمة الطلب والتوصيل المحلي فالجديدة:")
                                Text(
                                    text = "+15 درهم",
                                    fontWeight = FontWeight.Bold,
                                    color = TerracottaOrange
                                )
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "المجموع النهائي لهاد السلعة بوحدها:",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${product.priceMad + 15} درهم",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TerracottaOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "العنوان الأصلي للمورد (للشفافية): ${product.originalTitle}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bottom Action Bar inside Full-Screen Modal
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddToCart,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text(
                                text = "أضف للسلة",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Button(
                            onClick = onOrderNow,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(54.dp)
                                .testTag("modal_order_now_button")
                        ) {
                            Text(
                                text = "اطلب دابا (${product.priceMad + 15} درهم)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenCheckoutModal(
    cartItems: List<CartItem>,
    confirmedOrder: OrderConfirmationReceipt?,
    actionError: String?,
    onClearError: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmitOrder: (name: String, phone: String, address: String) -> Unit
) {
    val context = LocalContext.current
    var fullName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }

    val subtotalMad = cartItems.sumOf { it.product.priceMad * it.quantity }
    val finalTotalMad = subtotalMad + ElJadidaCatalogDefaults.FIXED_SERVICE_FEE_MAD

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler { onDismiss() }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("full_screen_checkout_modal"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Surface(
                    color = MazaganEmerald,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (confirmedOrder != null) "تأكيد الطلبية فالجديدة" else "إتمام الطلب فخطوة وحدة (بدون حساب)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_checkout_modal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (confirmedOrder != null) {
                    // Local Order Confirmation Screen (prevents duplicate submissions when navigating back from WhatsApp)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "تم تأكيد الطلب",
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "تم تسجيل طلبيتك بنجاح!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MazaganEmerald
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "رقم الطلب: ${confirmedOrder.orderCode} • تم قفل الاستمارة محلياً لمنع إرسال الطلب مرتين عند الرجوع من واتساب.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ملخص الطلبية المرسلة لواتساب:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MazaganEmerald
                                )
                                Text("الزبون: ${confirmedOrder.customerName}")
                                Text("الهاتف: ${confirmedOrder.customerPhone}")
                                Text("العنوان فالجديدة: ${confirmedOrder.customerAddress}")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                Text(
                                    text = "المجموع النهائي الواجب أداؤه عند الاستلام: ${confirmedOrder.totalMad} درهم",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TerracottaOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(confirmedOrder.whatsappUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فتح رسالة واتساب مرة أخرى")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("الرجوع للمتجر")
                        }
                    }
                } else {
                    // Single-Step Frictionless Checkout Form
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cart Items List
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "السلع المختارة:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MazaganEmerald
                                )
                                cartItems.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.product.darijaTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.product.priceMad} درهم × ${item.quantity}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = TerracottaOrange
                                            )
                                        }
                                        if (cartItems.size > 1) {
                                            IconButton(onClick = { onRemoveItem(item.product.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (actionError != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = actionError,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }

                        // Field 1: Full Name
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                onClearError()
                            },
                            label = { Text("الاسم الكامل (Full Name)") },
                            placeholder = { Text("مثال: أمين الجديدي") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_name_input")
                        )

                        // Field 2: Moroccan Phone Number
                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                onClearError()
                            },
                            label = { Text("رقم الهاتف المغربي (06 / 07 / +212)") },
                            placeholder = { Text("0661234567") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_phone_input")
                        )

                        // Field 3: Address in El Jadida + Quick Neighborhood Chips
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                onClearError()
                            },
                            label = { Text("العنوان الكامل فالجديدة (Address)") },
                            placeholder = { Text("الحي، الزنقة، أو علامة قريبة فالجديدة...") },
                            minLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_address_input")
                        )

                        Text(
                            text = "اختر الحي فالجديدة بسرعة:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ElJadidaCatalogDefaults.neighborhoods.forEach { hood ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            address = if (address.isBlank()) hood else "$hood - $address"
                                        }
                                ) {
                                    Text(
                                        text = hood,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Final Total Banner (Showing the Final Total including Fixed 15 MAD Service Fee)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MazaganEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "المجموع النهائي الواجب أداؤه",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "شامل 15 درهم خدمة الطلب والتوصيل المحلي فالجديدة",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Text(
                                    text = "$finalTotalMad درهم",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MazaganGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Surface(
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onSubmitOrder(fullName, phone, address) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(56.dp)
                                .testTag("submit_whatsapp_order_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "أكد الطلب دابا عبر واتساب ($finalTotalMad درهم)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
