package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MazaganRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertAndObserveProducts_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = MazaganRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertLocalizedProduct(
                temuGoodsId = "TM-9901",
                originalTitle = "Portable USB Smoothie Blender",
                darijaTitle = "خلاط عصير محمول كيتشارجا بـ USB",
                darijaPitch = "طحن العصير ديالك فين ما كنتي فالجديدة.",
                neighborhoodTag = "سيدي بوزيد",
                priceMad = 89L,
                salesVolume = "+4,800 مبيعة",
                imageKey = "blender",
                category = "الكوزينة"
            )
        }
        assertTrue(createResult.isSuccess)
        val productId = createResult.getOrThrow()

        val emittedProducts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProducts().first { list -> list.any { it.id == productId } }
        }
        assertTrue(emittedProducts.any { it.id == productId && it.priceMad == 89L })
    }

    @Test
    fun createProxyOrder_enforces15MadServiceFee_andUpdatesStatus() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = MazaganRepository(firestore)

        val orderResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProxyOrder(
                orderCode = "JD-5001",
                customerName = "ياسين الجديدي",
                customerPhone = "0661234567",
                customerAddress = "حي المطار، الجديدة",
                itemTitlesDarija = listOf("خلاط عصير محمول", "مصباح كريستال"),
                itemPricesMad = listOf(89L, 95L),
                itemImageKeys = listOf("blender", "lamp")
            )
        }
        assertTrue(orderResult.isSuccess)
        val orderId = orderResult.getOrThrow()

        val orders = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeOrders().first { list -> list.any { it.id == orderId } }
        }
        val createdOrder = orders.first { it.id == orderId }
        assertEquals(184L, createdOrder.subtotalMad)
        assertEquals(15L, createdOrder.serviceFeeMad)
        assertEquals(199L, createdOrder.totalMad)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateOrderStatus(orderId, "delivered")
        }
        assertTrue(updateResult.isSuccess)
    }

    @Test
    fun crossUserProductAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = MazaganRepository(firestore)
        val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertLocalizedProduct(
                temuGoodsId = "TM-9902",
                originalTitle = "Crystal Table Lamp",
                darijaTitle = "فيوزة كريستال",
                darijaPitch = "ضو مذهب للصالون.",
                neighborhoodTag = "القلعة",
                priceMad = 95L,
                salesVolume = "+3,000 مبيعة",
                imageKey = "lamp",
                category = "ديكور"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = MazaganRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProductById(aliceUid, productId)
        }
        assertTrue(crossReadResult.isFailure)
        val ex = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun observeProducts_unauthenticatedUser_fails() = runBlocking {
        auth.signOut()
        val repository = MazaganRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProducts().first()
            }
            fail("Expected exception when observing products unauthenticated")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException || e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@mazagan.ma"
        const val BOB_EMAIL = "bob@mazagan.ma"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
