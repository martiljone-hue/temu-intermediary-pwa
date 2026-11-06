package com.example.data.model

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val temuGoodsId: String = "",
    val originalTitle: String = "",
    val darijaTitle: String = "",
    val darijaPitch: String = "",
    val neighborhoodTag: String = "",
    val priceMad: Long = 0L,
    val salesVolume: String = "",
    val imageKey: String = "blender",
    val category: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProxyOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderCode: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val subtotalMad: Long = 0L,
    val serviceFeeMad: Long = 15L,
    val totalMad: Long = 15L,
    val status: String = "pending_whatsapp",
    val itemTitlesDarija: List<String> = emptyList(),
    val itemPricesMad: List<Long> = emptyList(),
    val itemImageKeys: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartItem(
    val product: Product,
    val quantity: Int = 1
)

data class RawTemuCandidate(
    val temuGoodsId: String,
    val originalTitle: String,
    val originalDescription: String,
    val regularTemuPriceMad: Long,
    val salesVolume: String,
    val imageKey: String,
    val categoryDarija: String,
    val defaultDarijaTitle: String,
    val defaultDarijaPitch: String,
    val neighborhoodTag: String
)

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

object ElJadidaCatalogDefaults {
    const val FIXED_SERVICE_FEE_MAD = 15L

    val neighborhoods = listOf(
        "سيدي بوزيد (Sidi Bouzid)",
        "حي المطار (Hay El Matar)",
        "القلعة (El Kalaa)",
        "حي السلام (Hay Essalam)",
        "شارع محمد السادس",
        "وسط المدينة • مازاغان"
    )

    val rawTemuBestSellers = listOf(
        RawTemuCandidate(
            temuGoodsId = "TM-882410",
            originalTitle = "Portable 6-Blade USB Rechargeable Personal Smoothie & Juice Blender Cup 380ml",
            originalDescription = "Cordless personal size blender with 6 stainless steel blades, USB-C fast charging, BPA free bottle for gym, beach, and travel.",
            regularTemuPriceMad = 89L,
            salesVolume = "+4,800 مبيعة",
            imageKey = "blender",
            categoryDarija = "الكوزينة والدار",
            defaultDarijaTitle = "خلاط عصير محمول بـ 6 المواس كيتشارجا بـ USB - دي معاك للخدمة ولا بحر سيدي بوزيد",
            defaultDarijaPitch = "طحن العصير ديالك طري فـ 30 ثانية فين ما كنتي فالجديدة. كيتشارجا بالتيليفون وساهل فالتنظيف وبثمن تيمو الأصلي بلا زيادة.",
            neighborhoodTag = "سيدي بوزيد • حي المطار"
        ),
        RawTemuCandidate(
            temuGoodsId = "TM-774192",
            originalTitle = "TWS Matte Black Wireless Earbuds with LED Power Display & ENC Noise Cancelling Mic",
            originalDescription = "Bluetooth 5.3 stereo in-ear headphones with digital battery indicator case, waterproof IPX5, deep bass sound.",
            regularTemuPriceMad = 74L,
            salesVolume = "+9,100 مبيعة",
            imageKey = "earbuds",
            categoryDarija = "إلكترونيات",
            defaultDarijaTitle = "زويجة ليزياتور بلوتوث بشاشة LED وصوت نقي بلا صداع - شارج كيدوم السيمانة",
            defaultDarijaPitch = "سمع للموسيقى وهضر فالتيليفون بصوت صافي واخا تكون فالبرد على الكورنيش. بواطة فيها أفيشور ديال الشارج.",
            neighborhoodTag = "وسط المدينة • الكورنيش"
        ),
        RawTemuCandidate(
            temuGoodsId = "TM-663905",
            originalTitle = "Cordless Acrylic Crystal Diamond Table Lamp with Touch Dimming & 3 Warm Light Modes",
            originalDescription = "Rechargeable decorative bedside crystal lamp projecting rose diamond light patterns, USB charging, touch control.",
            regularTemuPriceMad = 95L,
            salesVolume = "+3,650 مبيعة",
            imageKey = "lamp",
            categoryDarija = "ديكور الصالون",
            defaultDarijaTitle = "فيوزة كريستال بلا خيط باللمس (3 ألوان دافئة) كتعطي ديكور واعر للصالون وبيت النعاس",
            defaultDarijaPitch = "ضو مذهب كيعطي زواقة بحال الزليج العصري فوق الطبلة. كتشارجا وتخدم بلا بريز فين ما حطيتيها.",
            neighborhoodTag = "حي السلام • القلعة"
        ),
        RawTemuCandidate(
            temuGoodsId = "TM-551034",
            originalTitle = "360-Degree Rotating Cosmetics & Skincare Organizer Rack with Gold Accents",
            originalDescription = "Multi-layer countertop storage tray for perfumes, creams, and makeup brushes with smooth 360 rotation.",
            regularTemuPriceMad = 68L,
            salesVolume = "+6,300 مبيعة",
            imageKey = "blender",
            categoryDarija = "تنظيم الحمام والبيت",
            defaultDarijaTitle = "منظم الماكياج والروايح كيدور 360 درجة بلمسة ذهبية - كيجمع كولشي فبلاصة وحدة",
            defaultDarijaPitch = "هني راسك من الروينة فوق الكوافوز! كيهز القراعي الكبار والكريمات وكيدور بسهولة باش تلقاي كل حاجة دغيا.",
            neighborhoodTag = "حي المطار • سيدي بوزيد"
        ),
        RawTemuCandidate(
            temuGoodsId = "TM-449811",
            originalTitle = "Foldable Handheld Garment Steamer 1200W Fast Heat-Up Portable Clothes Iron",
            originalDescription = "Compact travel clothes steamer removes wrinkles in 20 seconds, safe on silk, cotton, djellaba and shirts.",
            regularTemuPriceMad = 119L,
            salesVolume = "+2,900 مبيعة",
            imageKey = "lamp",
            categoryDarija = "الكوزينة والدار",
            defaultDarijaTitle = "مصلوح البخار المحمول كيطوي (1200W) - كيصلح الجلابة والقميجة فـ 20 ثانية",
            defaultDarijaPitch = "كتسخن فالحين وكتصلح الحوايج وهما معلقين بلا ما تحرق الثوب. مثالية للجلابة المغربية والقميجات ديال الخدمة.",
            neighborhoodTag = "القلعة • شارع محمد السادس"
        )
    )
}
