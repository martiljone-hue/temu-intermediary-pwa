package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.ElJadidaCatalogDefaults
import com.example.data.model.Product
import com.example.data.model.ProxyOrder
import com.example.data.model.RawTemuCandidate
import com.example.data.repository.MazaganRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.random.Random

private const val TAG = "MazaganViewModel"

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface AiLocalizerState {
    data object Idle : AiLocalizerState
    data object Loading : AiLocalizerState
    data class LocalizedSuccess(
        val originalTitle: String,
        val darijaTitle: String,
        val darijaPitch: String,
        val neighborhoodTag: String,
        val categoryDarija: String,
        val priceMad: Long,
        val temuGoodsId: String,
        val salesVolume: String,
        val imageKey: String,
        val savedToCatalog: Boolean = false
    ) : AiLocalizerState
    data class Error(val message: String) : AiLocalizerState
}

data class OrderConfirmationReceipt(
    val orderCode: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val subtotalMad: Long,
    val serviceFeeMad: Long,
    val totalMad: Long,
    val itemsSummaryDarija: List<String>,
    val whatsappUrl: String
)

class MazaganViewModel(
    private val repository: MazaganRepository
) : ViewModel() {

    // Schema.obj for structured Darija localization output via Firebase AI Logic
    private val darijaLocalizationSchema = Schema.obj(
        properties = mapOf(
            "darijaTitle" to Schema.string(
                description = "Natural, engaging Moroccan Darija product title targeting buyers in El Jadida. Avoid formal Arabic or Egyptian dialects."
            ),
            "darijaPitch" to Schema.string(
                description = "2-sentence persuasive Moroccan Darija description tailored to El Jadida daily life."
            ),
            "neighborhoodTag" to Schema.string(
                description = "Local El Jadida delivery highlight in Darija (e.g., سيدي بوزيد • حي المطار)"
            ),
            "categoryDarija" to Schema.string(
                description = "Short category label in Moroccan Darija"
            )
        )
    )

    private val aiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = darijaLocalizationSchema
            }
        )
    }

    val productsState: StateFlow<DataUiState<List<Product>>> = repository.observeProducts()
        .map<List<Product>, DataUiState<List<Product>>> { DataUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing products", rootError)
            emit(DataUiState.Error(rootError.message ?: "تعذر تحميل المنتجات"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    val ordersState: StateFlow<DataUiState<List<ProxyOrder>>> = repository.observeOrders()
        .map<List<ProxyOrder>, DataUiState<List<ProxyOrder>>> { list ->
            DataUiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing orders", rootError)
            emit(DataUiState.Error(rootError.message ?: "تعذر تحميل الطلبات"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _selectedProductModal = MutableStateFlow<Product?>(null)
    val selectedProductModal: StateFlow<Product?> = _selectedProductModal.asStateFlow()

    private val _isCheckoutModalOpen = MutableStateFlow(false)
    val isCheckoutModalOpen: StateFlow<Boolean> = _isCheckoutModalOpen.asStateFlow()

    private val _lastConfirmedOrder = MutableStateFlow<OrderConfirmationReceipt?>(null)
    val lastConfirmedOrder: StateFlow<OrderConfirmationReceipt?> = _lastConfirmedOrder.asStateFlow()

    private val _aiState = MutableStateFlow<AiLocalizerState>(AiLocalizerState.Idle)
    val aiState: StateFlow<AiLocalizerState> = _aiState.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialCatalogIfEmpty(ElJadidaCatalogDefaults.rawTemuBestSellers)
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun openProductFullScreenModal(product: Product) {
        _selectedProductModal.value = product
    }

    fun closeProductFullScreenModal() {
        _selectedProductModal.value = null
    }

    fun openCheckoutFullScreenModal(initialProduct: Product? = null) {
        if (initialProduct != null && _cartItems.value.none { it.product.id == initialProduct.id }) {
            _cartItems.value = _cartItems.value + CartItem(initialProduct, 1)
        }
        _selectedProductModal.value = null
        _lastConfirmedOrder.value = null
        _isCheckoutModalOpen.value = true
    }

    fun closeCheckoutFullScreenModal() {
        _isCheckoutModalOpen.value = false
        _lastConfirmedOrder.value = null
    }

    fun addToCart(product: Product) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            if (item.quantity < 5) {
                current[index] = item.copy(quantity = item.quantity + 1)
            }
        } else if (current.size < 10) {
            current.add(CartItem(product, 1))
        }
        _cartItems.value = current
    }

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == productId }
    }

    fun isValidMoroccanPhone(phone: String): Boolean {
        val cleaned = phone.replace(Regex("[\\s\\-]"), "")
        return Regex("^(?:(?:\\+|00)212|0)[5-7]\\d{8}$").matches(cleaned)
    }

    fun submitOrderAndBuildWhatsApp(
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        whatsappBusinessPhone: String = "212661000000",
        onLaunchWhatsApp: (String) -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            _actionError.value = "المرجو اختيار منتج واحد على الأقل"
            return
        }
        val cleanPhone = customerPhone.replace(Regex("[\\s\\-]"), "")
        if (customerName.trim().length < 2) {
            _actionError.value = "المرجو إدخال الاسم الكامل"
            return
        }
        if (!isValidMoroccanPhone(cleanPhone)) {
            _actionError.value = "رقم الهاتف غير صحيح. مثال: 0661234567 أو +212661234567"
            return
        }
        if (customerAddress.trim().length < 3) {
            _actionError.value = "المرجو كتابة العنوان أو الحي فالجديدة"
            return
        }

        val expandedTitles = items.flatMap { item -> List(item.quantity) { item.product.darijaTitle } }.take(10)
        val expandedPrices = items.flatMap { item -> List(item.quantity) { item.product.priceMad } }.take(10)
        val expandedImages = items.flatMap { item -> List(item.quantity) { item.product.imageKey } }.take(10)

        val subtotal = expandedPrices.sum()
        val serviceFee = ElJadidaCatalogDefaults.FIXED_SERVICE_FEE_MAD
        val finalTotal = subtotal + serviceFee
        val orderCode = "JD-${Random.nextInt(1000, 9999)}"

        val darijaLines = buildString {
            appendLine("السلام عليكم مازاغان بروكسي (الجديدة)! بغيت نأكد هاد الطلبية:")
            appendLine("🔖 رقم الطلب: $orderCode")
            appendLine("👤 الاسم: ${customerName.trim()}")
            appendLine("📱 الهاتف: $cleanPhone")
            appendLine("📍 العنوان فالجديدة: ${customerAddress.trim()}")
            appendLine("📦 السلع المطلوبة:")
            items.forEachIndexed { idx, cartItem ->
                appendLine("${idx + 1}. ${cartItem.product.darijaTitle} (x${cartItem.quantity}) - ${cartItem.product.priceMad * cartItem.quantity} درهم")
            }
            appendLine("🚚 عمولة الخدمة والتوصيل المحلي: +$serviceFee درهم")
            appendLine("💰 المجموع النهائي الواجب أداؤه: $finalTotal درهم")
        }

        val encodedText = URLEncoder.encode(darijaLines, "UTF-8")
        val whatsappUrl = "https://wa.me/$whatsappBusinessPhone?text=$encodedText"

        viewModelScope.launch {
            val result = repository.createProxyOrder(
                orderCode = orderCode,
                customerName = customerName.trim(),
                customerPhone = cleanPhone,
                customerAddress = customerAddress.trim(),
                itemTitlesDarija = expandedTitles,
                itemPricesMad = expandedPrices,
                itemImageKeys = expandedImages,
                status = "pending_whatsapp"
            )
            result.onSuccess {
                _cartItems.value = emptyList()
                _lastConfirmedOrder.value = OrderConfirmationReceipt(
                    orderCode = orderCode,
                    customerName = customerName.trim(),
                    customerPhone = cleanPhone,
                    customerAddress = customerAddress.trim(),
                    subtotalMad = subtotal,
                    serviceFeeMad = serviceFee,
                    totalMad = finalTotal,
                    itemsSummaryDarija = expandedTitles,
                    whatsappUrl = whatsappUrl
                )
                onLaunchWhatsApp(whatsappUrl)
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "تعذر حفظ الطلبية"
            }
        }
    }

    fun localizeProductWithGeminiAi(
        temuGoodsId: String,
        originalTitle: String,
        originalDescription: String,
        regularTemuPriceMad: Long,
        salesVolume: String,
        imageKey: String
    ) {
        if (originalTitle.isBlank()) {
            _aiState.value = AiLocalizerState.Error("المرجو إدخال عنوان المنتج الأصلي من Temu")
            return
        }
        viewModelScope.launch {
            _aiState.value = AiLocalizerState.Loading
            val prompt = """
                Translate and rewrite this product title into natural, engaging, and simplified Moroccan Darija (الدارجة المغربية), targeting buyers in El Jadida. Avoid formal Arabic or Egyptian dialects.
                
                Also provide a short 2-sentence persuasive Moroccan Darija pitch (`darijaPitch`), a local El Jadida delivery tag (`neighborhoodTag`), and a short category in Darija (`categoryDarija`).
                
                Original Product Title: $originalTitle
                Original Description: $originalDescription
                Exact Regular Temu Price (0% markup): $regularTemuPriceMad MAD
            """.trimIndent()

            try {
                val response = withTimeout(60_000L) {
                    aiModel.generateContent(prompt)
                }
                val jsonString = response.text
                if (jsonString.isNullOrBlank()) {
                    _aiState.value = AiLocalizerState.Error("أرجع نموذج الذكاء الاصطناعي استجابة فارغة.")
                    return@launch
                }

                val json = JSONObject(jsonString)
                val darijaTitle = json.getString("darijaTitle")
                val darijaPitch = json.optString("darijaPitch", "منتج عملي بجودة عالية وبثمن تيمو الأصلي فالجديدة.")
                val neighborhoodTag = json.optString("neighborhoodTag", "سيدي بوزيد • وسط الجديدة")
                val categoryDarija = json.optString("categoryDarija", "الأكثر مبيعاً")

                _aiState.value = AiLocalizerState.LocalizedSuccess(
                    originalTitle = originalTitle,
                    darijaTitle = darijaTitle,
                    darijaPitch = darijaPitch,
                    neighborhoodTag = neighborhoodTag,
                    categoryDarija = categoryDarija,
                    priceMad = regularTemuPriceMad,
                    temuGoodsId = temuGoodsId,
                    salesVolume = salesVolume,
                    imageKey = imageKey,
                    savedToCatalog = false
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Darija localization timed out", e)
                _aiState.value = AiLocalizerState.Error("انتهت مهلة الاتصال بالذكاء الاصطناعي (60 ثانية). حاول مرة أخرى.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI Darija localization failed", e)
                _aiState.value = AiLocalizerState.Error(e.localizedMessage ?: "فشل تعريب المنتج بالدارجة.")
            }
        }
    }

    fun publishLocalizedProductToCatalog(state: AiLocalizerState.LocalizedSuccess) {
        viewModelScope.launch {
            val res = repository.upsertLocalizedProduct(
                temuGoodsId = state.temuGoodsId,
                originalTitle = state.originalTitle,
                darijaTitle = state.darijaTitle,
                darijaPitch = state.darijaPitch,
                neighborhoodTag = state.neighborhoodTag,
                priceMad = state.priceMad,
                salesVolume = state.salesVolume,
                imageKey = state.imageKey,
                category = state.categoryDarija
            )
            res.onSuccess {
                _aiState.value = state.copy(savedToCatalog = true)
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "فشل حفظ المنتج في قاعدة البيانات"
            }
        }
    }

    fun markOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
        }
    }

    fun createSampleDeliveredOrderForReceipt(itemCount: Int) {
        viewModelScope.launch {
            val catalog = ElJadidaCatalogDefaults.rawTemuBestSellers.take(itemCount.coerceIn(1, 3))
            repository.createProxyOrder(
                orderCode = "JD-${Random.nextInt(1000, 9999)}",
                customerName = "فاطمة الزهراء الجديدية",
                customerPhone = "0662987654",
                customerAddress = "إقامة النخيل، سيدي بوزيد، الجديدة",
                itemTitlesDarija = catalog.map { it.defaultDarijaTitle },
                itemPricesMad = catalog.map { it.regularTemuPriceMad },
                itemImageKeys = catalog.map { it.imageKey },
                status = "delivered"
            )
        }
    }
}
