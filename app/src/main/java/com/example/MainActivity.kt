package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.configureAppCheck
import com.example.auth.signOut
import com.example.data.repository.MazaganRepository
import com.example.ui.screens.BlueprintInspectorScreen
import com.example.ui.screens.ReceiptGeneratorScreen
import com.example.ui.screens.SmartEmployeeScreen
import com.example.ui.screens.StorefrontScreen
import com.example.ui.theme.MazaganEmerald
import com.example.ui.theme.MazaganGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerracottaOrange
import com.example.viewmodel.MazaganViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MazaganRootApp()
                }
            }
        }
    }
}

enum class MazaganTab(
    val labelRes: Int,
    val icon: ImageVector,
    val tag: String
) {
    STOREFRONT(R.string.tab_storefront, Icons.Default.Storefront, "nav_storefront"),
    SMART_EMPLOYEE(R.string.tab_smart_employee, Icons.Default.AutoAwesome, "nav_smart_employee"),
    RECEIPTS(R.string.tab_receipts, Icons.AutoMirrored.Filled.ReceiptLong, "nav_receipts"),
    BLUEPRINT(R.string.tab_blueprint, Icons.Default.Code, "nav_blueprint")
}

@Composable
fun MazaganRootApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        AuthenticatedMazaganShell(
            currentUserId = user.uid,
            onSignedOut = { currentUser = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedMazaganShell(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MazaganViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MazaganViewModel(MazaganRepository(db))
            }
        }
    )

    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedProductModal by viewModel.selectedProductModal.collectAsStateWithLifecycle()
    val isCheckoutModalOpen by viewModel.isCheckoutModalOpen.collectAsStateWithLifecycle()
    val lastConfirmedOrder by viewModel.lastConfirmedOrder.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MazaganTab.STOREFRONT) }

    if (currentTab != MazaganTab.STOREFRONT) {
        BackHandler {
            currentTab = MazaganTab.STOREFRONT
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "مازاغان بروكسي • الجديدة (El Jadida)",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                signOut(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onSignedOut,
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "تسجيل الخروج",
                                tint = MazaganGold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MazaganEmerald,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = MazaganEmerald,
                        contentColor = Color.White
                    ) {
                        MazaganTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = MazaganGold,
                                    indicatorColor = TerracottaOrange,
                                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = MazaganEmerald,
                        contentColor = Color.White
                    ) {
                        MazaganTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        MazaganTab.STOREFRONT -> StorefrontScreen(
                            productsState = productsState,
                            cartItems = cartItems,
                            selectedProductModal = selectedProductModal,
                            isCheckoutModalOpen = isCheckoutModalOpen,
                            lastConfirmedOrder = lastConfirmedOrder,
                            actionError = actionError,
                            onClearError = viewModel::clearActionError,
                            onProductClick = viewModel::openProductFullScreenModal,
                            onCloseProductModal = viewModel::closeProductFullScreenModal,
                            onAddToCart = viewModel::addToCart,
                            onRemoveFromCart = viewModel::removeFromCart,
                            onOpenCheckout = viewModel::openCheckoutFullScreenModal,
                            onCloseCheckout = viewModel::closeCheckoutFullScreenModal,
                            onSubmitWhatsAppOrder = { name, phone, address, onLaunch ->
                                viewModel.submitOrderAndBuildWhatsApp(
                                    customerName = name,
                                    customerPhone = phone,
                                    customerAddress = address,
                                    onLaunchWhatsApp = onLaunch
                                )
                            }
                        )

                        MazaganTab.SMART_EMPLOYEE -> SmartEmployeeScreen(
                            aiState = aiState,
                            onRunAiLocalizer = viewModel::localizeProductWithGeminiAi,
                            onPublishToCatalog = viewModel::publishLocalizedProductToCatalog
                        )

                        MazaganTab.RECEIPTS -> ReceiptGeneratorScreen(
                            ordersState = ordersState,
                            onMarkOrderStatus = viewModel::markOrderStatus,
                            onCreateSampleDeliveredOrder = viewModel::createSampleDeliveredOrderForReceipt
                        )

                        MazaganTab.BLUEPRINT -> BlueprintInspectorScreen()
                    }
                }
            }
        }
    }
}
