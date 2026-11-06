package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ElJadidaCatalogDefaults
import com.example.data.model.OperationType
import com.example.data.model.Product
import com.example.data.model.ProxyOrder
import com.example.data.model.RawTemuCandidate
import com.example.data.model.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MazaganRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProducts(): Flow<List<Product>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/products"
        emitAll(
            db.collection("users").document(uid).collection("products")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeOrders(): Flow<List<ProxyOrder>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/orders"
        emitAll(
            db.collection("users").document(uid).collection("orders")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ProxyOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertLocalizedProduct(
        temuGoodsId: String,
        originalTitle: String,
        darijaTitle: String,
        darijaPitch: String,
        neighborhoodTag: String,
        priceMad: Long,
        salesVolume: String,
        imageKey: String,
        category: String,
        documentId: String = "prod_${temuGoodsId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}_${UUID.randomUUID().toString().take(6)}"
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(documentId)
        val payload = mapOf(
            "userId" to uid,
            "temuGoodsId" to temuGoodsId.take(80),
            "originalTitle" to originalTitle.take(300),
            "darijaTitle" to darijaTitle.take(300),
            "darijaPitch" to darijaPitch.take(600),
            "neighborhoodTag" to neighborhoodTag.take(100),
            "priceMad" to priceMad.coerceIn(1L, 50000L),
            "salesVolume" to salesVolume.take(60),
            "imageKey" to imageKey.take(60),
            "category" to category.take(60),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getProductById(targetUserId: String, productId: String): Result<Product?> {
        val docRef = db.collection("users").document(targetUserId).collection("products").document(productId)
        return try {
            val snap = docRef.get().await()
            Result.success(snap.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createProxyOrder(
        orderCode: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        itemTitlesDarija: List<String>,
        itemPricesMad: List<Long>,
        itemImageKeys: List<String>,
        status: String = "pending_whatsapp"
    ): Result<String> {
        val uid = requireUserId()
        val docId = "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid).collection("orders").document(docId)
        val subtotal = itemPricesMad.sum().coerceIn(1L, 100000L)
        val serviceFee = ElJadidaCatalogDefaults.FIXED_SERVICE_FEE_MAD
        val total = subtotal + serviceFee

        val payload = mapOf(
            "userId" to uid,
            "orderCode" to orderCode.take(40),
            "customerName" to customerName.trim().take(120),
            "customerPhone" to customerPhone.trim().take(20),
            "customerAddress" to customerAddress.trim().take(250),
            "subtotalMad" to subtotal,
            "serviceFeeMad" to serviceFee,
            "totalMad" to total,
            "status" to status,
            "itemTitlesDarija" to itemTitlesDarija.take(10).map { it.take(300) },
            "itemPricesMad" to itemPricesMad.take(10),
            "itemImageKeys" to itemImageKeys.take(10).map { it.take(60) },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        val uid = requireUserId()
        require(orderId.isNotBlank()) { "Order ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("orders").document(orderId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedInitialCatalogIfEmpty(candidates: List<RawTemuCandidate>) {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("products")
        try {
            val existing = colRef.whereEqualTo("userId", uid).limit(1).get().await()
            if (existing.isEmpty) {
                for (item in candidates) {
                    upsertLocalizedProduct(
                        temuGoodsId = item.temuGoodsId,
                        originalTitle = item.originalTitle,
                        darijaTitle = item.defaultDarijaTitle,
                        darijaPitch = item.defaultDarijaPitch,
                        neighborhoodTag = item.neighborhoodTag,
                        priceMad = item.regularTemuPriceMad,
                        salesVolume = item.salesVolume,
                        imageKey = item.imageKey,
                        category = item.categoryDarija,
                        documentId = "seed_${item.temuGoodsId}"
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
        }
    }
}
