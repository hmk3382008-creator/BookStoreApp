package com.example.bookstoreapp.Firbase

import com.example.bookstoreapp.Seller.Product
import com.example.bookstoreapp.User.FavoriteItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FirebaseFavoriteRepository {

    private val db = FirebaseFirestore.getInstance()
    private val favoritesCollection = db.collection("favorites")

    /**
     * 加入收藏（如果已存在會直接回傳 false）
     */
    fun addFavorite(
        userId: String,
        product: Product,
        onResult: (Boolean) -> Unit
    ) {
        if (userId.isBlank() || product.id.isBlank()) {
            onResult(false)
            return
        }

        favoritesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("productId", product.id)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    // 已經收藏過了
                    onResult(false)
                    return@addOnSuccessListener
                }

                val data = hashMapOf(
                    "userId" to userId,
                    "productId" to product.id,
                    "name" to product.name,
                    "description" to product.description,
                    "price" to product.price,
                    "stock" to product.stock,
                    "sellerId" to product.sellerId,
                    "imageUrl" to product.imageUrl
                )

                favoritesCollection
                    .add(data)
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    /**
     * 取消收藏（用 userId + productId 刪除所有符合的收藏）
     */
    fun removeFavorite(
        userId: String,
        productId: String,
        onResult: (Boolean) -> Unit
    ) {
        favoritesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("productId", productId)
            .get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                for (doc in snap.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    /**
     * 檢查某商品是否為收藏
     */
    fun isFavorite(
        userId: String,
        productId: String,
        onResult: (Boolean) -> Unit
    ) {
        if (userId.isBlank() || productId.isBlank()) {
            onResult(false)
            return
        }

        favoritesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("productId", productId)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                onResult(!snap.isEmpty)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    /**
     * 一顆按鈕做「切換收藏」：
     * - 目前沒收藏 → 幫你新增收藏 → callback(isSuccess = true, isFavoriteNow = true)
     * - 目前有收藏 → 幫你取消收藏 → callback(isSuccess = true, isFavoriteNow = false)
     */
    fun toggleFavorite(
        userId: String,
        product: Product,
        onResult: (isSuccess: Boolean, isFavoriteNow: Boolean) -> Unit
    ) {
        if (userId.isBlank() || product.id.isBlank()) {
            onResult(false, false)
            return
        }

        favoritesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("productId", product.id)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    // 沒有 → 加入收藏
                    val data = hashMapOf(
                        "userId" to userId,
                        "productId" to product.id,
                        "name" to product.name,
                        "description" to product.description,
                        "price" to product.price,
                        "stock" to product.stock,
                        "sellerId" to product.sellerId,
                        "imageUrl" to product.imageUrl
                    )

                    favoritesCollection
                        .add(data)
                        .addOnSuccessListener { onResult(true, true) }
                        .addOnFailureListener { onResult(false, false) }
                } else {
                    // 已經有 → 全部刪掉
                    val batch = db.batch()
                    for (doc in snap.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit()
                        .addOnSuccessListener { onResult(true, false) }
                        .addOnFailureListener { onResult(false, true) }
                }
            }
            .addOnFailureListener {
                onResult(false, false)
            }
    }

    /**
     * 取得某個會員的收藏清單 → 轉成 Product List
     */
    fun getFavoritesByUser(
        userId: String,
        onSuccess: (List<Product>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        favoritesCollection
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.map { doc ->
                    Product(
                        id = doc.getString("productId") ?: "",
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = (doc.getLong("price") ?: 0L).toInt(),
                        stock = (doc.getLong("stock") ?: 0L).toInt(),
                        sellerId = doc.getString("sellerId") ?: "",
                        imageUrl = doc.getString("imageUrl")
                    )
                }
                onSuccess(list)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}
