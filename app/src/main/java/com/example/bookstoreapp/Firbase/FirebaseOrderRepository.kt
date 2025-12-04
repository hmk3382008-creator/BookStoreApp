package com.example.bookstoreapp.Firbase

import com.example.bookstoreapp.Seller.Product
import com.example.bookstoreapp.User.Order
import com.example.bookstoreapp.User.OrderItem
import com.example.bookstoreapp.View.CartManager
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.security.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class FirebaseOrderRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val ordersCollection = db.collection("orders")

    fun createOrder(
        userId: String,
        userName: String,
        items: List<CartManager.CartItem>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        createOrder(
            userId = userId,
            userName = userName,
            items = items,
            paymentMethod = "",
            receiverName = "",
            receiverPhone = "",
            shippingAddress = "",
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    // 新的 API: 給有結帳頁面要填資料用
    fun createOrder(
        userId: String,
        userName: String,
        items: List<CartManager.CartItem>,
        paymentMethod: String,
        receiverName: String,
        receiverPhone: String,
        shippingAddress: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (items.isEmpty()) {
            onFailure(IllegalArgumentException("購物車是空的"))
            return
        }

        val totalAmount = items.sumOf { it.product.price * it.quantity }
        val orderRef = ordersCollection.document()

        val orderData = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "totalAmount" to totalAmount,
            "status" to "處理中",
            "createdAt" to FieldValue.serverTimestamp(),

            // 新增欄位
            "paymentMethod" to paymentMethod,
            "receiverName" to receiverName,
            "receiverPhone" to receiverPhone,
            "shippingAddress" to shippingAddress
        )

        val batch = db.batch()
        batch.set(orderRef, orderData)

        items.forEach { cartItem ->
            val itemRef = orderRef.collection("items").document()
            val p: Product = cartItem.product

            val itemData = hashMapOf(
                "productId" to p.id,
                "productName" to p.name,
                "price" to p.price,
                "quantity" to cartItem.quantity,
                "sellerId" to p.sellerId,
                "sellerStatus" to "處理中"
            )

            batch.set(itemRef, itemData)
        }

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getOrdersByUser(
        userId: String,
        onSuccess: (List<Order>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)

        ordersCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    val ts = doc.getTimestamp("createdAt")
                    val timeText = if (ts != null) {
                        val date = ts.toDate()
                        sdf.format(date)
                    } else {
                        "未知時間"
                    }

                    Order(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        totalAmount = (doc.getLong("totalAmount") ?: 0L).toInt(),
                        status = doc.getString("status") ?: "",
                        createdAtText = timeText
                    )
                }
                onSuccess(list)
            }
            .addOnFailureListener(onFailure)
    }

    //  一般會員用：讀整張訂單 + 所有明細（多商家都看得到）
    fun getOrderDetail(
        orderId: String,
        onSuccess: (Order, List<OrderItem>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val orderRef = ordersCollection.document(orderId)
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)

        orderRef.get().addOnSuccessListener { doc ->
            val order = doc.toObject(Order::class.java)
            if (order == null) {
                onFailure(Exception("找不到訂單"))
                return@addOnSuccessListener
            }

            order.id = doc.id

            val ts = doc.getTimestamp("createdAt")
            order.createdAtText = if (ts != null) {
                sdf.format(ts.toDate())
            } else {
                "未知時間"
            }

            orderRef.collection("items")
                .get()
                .addOnSuccessListener { snap ->
                    val list = snap.documents.mapNotNull { d ->
                        val item = d.toObject(OrderItem::class.java)
                        item?.apply { id = d.id }
                    }
                    onSuccess(order, list)
                }
                .addOnFailureListener(onFailure)

        }.addOnFailureListener(onFailure)
    }

    //  商家專用：只讀該商家自己的明細 + 該商家自己應得的金額
    fun getOrderDetailForSeller(
        orderId: String,
        sellerId: String,
        onSuccess: (Order, List<OrderItem>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val orderRef = ordersCollection.document(orderId)
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)

        orderRef.get()
            .addOnSuccessListener { doc ->
                val order = doc.toObject(Order::class.java)
                if (order == null) {
                    onFailure(Exception("找不到訂單"))
                    return@addOnSuccessListener
                }

                order.id = doc.id

                val ts = doc.getTimestamp("createdAt")
                order.createdAtText = if (ts != null) {
                    sdf.format(ts.toDate())
                } else {
                    "未知時間"
                }

                orderRef.collection("items")
                    .whereEqualTo("sellerId", sellerId)
                    .get()
                    .addOnSuccessListener { snap ->
                        val list = snap.documents.mapNotNull { d ->
                            val item = d.toObject(OrderItem::class.java)
                            item?.apply { id = d.id }
                        }

                        // 這個商家的金額
                        val sellerTotal = list.sumOf { it.price * it.quantity }
                        order.totalAmount = sellerTotal

                        //  商家在這張訂單的狀態：這裡簡單用第一筆 item 的 sellerStatus
                        val sellerStatus = list.firstOrNull()?.sellerStatus ?: "處理中"
                        order.sellerStatus = sellerStatus   // 下面會在 Order 裡加這個欄位

                        onSuccess(order, list)
                    }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }

    fun deleteOrderItem(
        orderId: String,
        itemId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        ordersCollection
            .document(orderId)
            .collection("items")
            .document(itemId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    //  商家訂單列表：只抓有賣家是 sellerId 的訂單，金額顯示該商家的 subtotal
    fun getOrdersBySeller(
        sellerId: String,
        onSuccess: (List<Order>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)

        FirebaseFirestore.getInstance()
            .collectionGroup("items")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener { itemsSnap ->

                if (itemsSnap.isEmpty) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                // 依 orderId 分組這個商家的 items
                val itemsByOrderId = itemsSnap.documents.groupBy { d ->
                    d.reference.parent?.parent?.id!!
                }

                // 每張訂單這個商家的金額
                val sellerAmountByOrderId: Map<String, Int> =
                    itemsByOrderId.mapValues { (_, docs) ->
                        docs.sumOf { d ->
                            val price = (d.getLong("price") ?: 0L).toInt()
                            val qty = (d.getLong("quantity") ?: 0L).toInt()
                            price * qty
                        }
                    }

                // 🔥 每張訂單這個商家的狀態（先用第一筆 item 的 sellerStatus）
                val sellerStatusByOrderId: Map<String, String> =
                    itemsByOrderId.mapValues { (_, docs) ->
                        val first = docs.firstOrNull()
                        first?.getString("sellerStatus") ?: "處理中"
                    }

                val orderIds = itemsByOrderId.keys.toList()

                db.collection("orders")
                    .whereIn(FieldPath.documentId(), orderIds)
                    .get()
                    .addOnSuccessListener { orderSnap ->

                        val tempList: List<Pair<Order, Long>> =
                            orderSnap.documents.mapNotNull { doc ->
                                val order = doc.toObject(Order::class.java) ?: return@mapNotNull null
                                order.id = doc.id

                                val ts = doc.getTimestamp("createdAt")
                                order.createdAtText = if (ts != null) {
                                    sdf.format(ts.toDate())
                                } else {
                                    "未知時間"
                                }

                                // 金額換成該商家的小計
                                order.totalAmount = sellerAmountByOrderId[doc.id] ?: 0
                                // 🔥 填入這個商家的狀態
                                order.sellerStatus = sellerStatusByOrderId[doc.id] ?: "處理中"

                                val millis = ts?.toDate()?.time ?: 0L
                                order to millis
                            }

                        val sortedOrders = tempList
                            .sortedByDescending { it.second }
                            .map { it.first }

                        onSuccess(sortedOrders)
                    }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }
    //管理者用：更新訂單
    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        ordersCollection.document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    //管理者用：取得所有訂單（依建立時間由新到舊）
    fun getAllOrders(
        onSuccess: (List<Order>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)

        ordersCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    val ts = doc.getTimestamp("createdAt")
                    val timeText = if (ts != null) {
                        sdf.format(ts.toDate())
                    } else {
                        "未知時間"
                    }

                    Order(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        totalAmount = (doc.getLong("totalAmount") ?: 0L).toInt(),
                        status = doc.getString("status") ?: "",
                        createdAtText = timeText
                    )
                }
                onSuccess(list)
            }
            .addOnFailureListener(onFailure)
    }
        //管理者用：刪除整張訂單（包含 items 子集合）
    fun deleteOrderWithItems(
        orderId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val orderRef = ordersCollection.document(orderId)

        orderRef.collection("items")
            .get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()

                // 先刪除所有明細
                snap.documents.forEach { d ->
                    batch.delete(d.reference)
                }

                // 再刪除訂單本身
                batch.delete(orderRef)

                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }
    fun cancelOrder(
        orderId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        deleteOrderWithItems(
            orderId = orderId,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
    // 一般會員用：修改某一筆明細的數量
    fun updateOrderItemQuantity(
        orderId: String,
        itemId: String,
        newQuantity: Int,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        ordersCollection
            .document(orderId)
            .collection("items")
            .document(itemId)
            .update("quantity", newQuantity)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }
    //商家用的更新狀態
    fun updateSellerStatus(
        orderId: String,
        sellerId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val orderRef = ordersCollection.document(orderId)

        orderRef.collection("items")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    onFailure(Exception("找不到此商家的明細"))
                    return@addOnSuccessListener
                }

                val batch = db.batch()

                snap.documents.forEach { d ->
                    batch.update(d.reference, "sellerStatus", newStatus)
                }

                batch.commit()
                    .addOnSuccessListener {
                        //  明細更新成功後，重算整張訂單的 status（給買家看的總狀態）
                        recomputeOrderStatus(
                            orderId = orderId,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }
    private fun calculateOverallStatus(statuses: List<String>): String {
        if (statuses.isEmpty()) return "已取消"

        //  如果全部狀態都一樣，就直接顯示那個狀態本身
        val distinct = statuses.toSet()
        if (distinct.size == 1) {
            return distinct.first()   // 可能是：處理中 / 已接單 / 已出貨 / 已完成 / 已取消
        }

        //  混合狀態 → 判斷要顯示哪一種「部分 XX」
        val hasProcessing = distinct.contains("處理中")
        val hasAccepted   = distinct.contains("已接單")
        val hasShipped    = distinct.contains("已出貨")
        val hasDone       = distinct.contains("已完成")
        val hasCanceled   = distinct.contains("已取消")

        // 優先順序：
        // 有出貨 → 部分出貨
        if (hasShipped) {
            return "部分出貨"
        }

        // 沒出貨，有完成 → 部分完成
        if (hasDone) {
            return "部分完成"
        }

        // 沒出貨沒完成，有接單 → 部分接單
        if (hasAccepted) {
            return "部分接單"
        }

        // 沒出貨沒完成沒接單，有取消 → 部分取消（例如：處理中 + 已取消）
        if (hasCanceled) {
            return "部分取消"
        }

        // 其他奇怪組合（通常是多種「處理中」狀態混在一起）→ 統一視為「部分處理中」
        return "部分處理中"
    }
    // 重新讀取整張訂單的 items，算出總狀態後寫回 orders.status
    private fun recomputeOrderStatus(
        orderId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val orderRef = ordersCollection.document(orderId)

        orderRef.collection("items")
            .get()
            .addOnSuccessListener { snap ->
                val statuses = snap.documents.mapNotNull { d ->
                    d.getString("sellerStatus")
                }

                val overallStatus = calculateOverallStatus(statuses)

                orderRef.update("status", overallStatus)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }
}