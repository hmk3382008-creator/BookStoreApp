package com.example.bookstoreapp.Firebase

import android.net.Uri
import com.example.bookstoreapp.Seller.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class FirebaseProductRepository {

    // Firestore 實例
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // 指向 "products" 這個 collection
    private val productsCollection = db.collection("products")

    // Firebase Storage 實例（用來放圖片）
    private val storage = FirebaseStorage.getInstance()
    private val storageRootRef = storage.reference  // 根節點

    /**
     * 上架商品：同時處理「上傳圖片到 Storage」＋「在 Firestore 建立商品文件」
     */
    fun addProductWithImage(
        product: Product,
        imageUri: Uri,
        onSuccess: (Product) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fileName = "product_${System.currentTimeMillis()}.jpg"
        val imageRef = storageRootRef.child("product_images/$fileName")

        imageRef.putFile(imageUri)
            .continueWithTask { uploadTask ->
                if (!uploadTask.isSuccessful) {
                    throw uploadTask.exception ?: Exception("圖片上傳失敗")
                }
                imageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val imageUrl = downloadUri.toString()

                val data = mapOf(
                    "name" to product.name,
                    "description" to product.description,
                    "price" to product.price,
                    "stock" to product.stock,
                    "sellerId" to product.sellerId,
                    "imageUrl" to imageUrl,
                    "isActive" to product.isActive
                )

                productsCollection
                    .add(data)
                    .addOnSuccessListener { docRef ->
                        val saved = product.copy(
                            id = docRef.id,
                            imageUrl = imageUrl
                        )
                        onSuccess(saved)
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * 取得所有商品（管理員 / 一般使用者都可用）
     */
    fun getAllProducts(
        onSuccess: (List<Product>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        productsCollection
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<Product>()
                for (doc in snapshot.documents) {

                    // ★ 先用 Firestore 原本的方式轉成 Product
                    val p = doc.toObject(Product::class.java)

                    if (p != null) {
                        // ★ 一定要把 documentId 塞到 id
                        p.id = doc.id

                        // 如果沒有這個欄位（舊資料），就預設 true（= 上架中）
                        val activeFromDb = doc.getBoolean("isActive") ?: true
                        p.isActive = activeFromDb

                        list.add(p)
                    }
                }
                onSuccess(list)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * 取得「某個商家」的所有商品
     */
    fun getProductsBySellerId(
        sellerId: String,
        onSuccess: (List<Product>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        productsCollection
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<Product>()
                for (doc in snapshot.documents) {
                    val p = doc.toObject(Product::class.java)
                    if (p != null) {
                        p.id = doc.id
                        list.add(p)
                    }
                }
                onSuccess(list)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * 更新商品（可選擇是否更換圖片）
     */
    fun updateProduct(
        product: Product,          // 含 id、已改好的 name/desc/price/stock
        newImageUri: Uri?,         // 若有選新圖就不是 null
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (product.id.isBlank()) {
            onFailure(IllegalArgumentException("product.id 不可為空"))
            return
        }

        fun updateFirestore(imageUrl: String?) {
            val data = mutableMapOf<String, Any>(
                "name" to product.name,
                "description" to product.description,
                "price" to product.price,
                "stock" to product.stock,
                "sellerId" to product.sellerId,
                "isActive" to product.isActive
            )

            if (imageUrl != null) {
                data["imageUrl"] = imageUrl
            }

            productsCollection
                .document(product.id)
                .update(data)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(e) }
        }

        // 沒有選新圖片 → 只更新文字欄位
        if (newImageUri == null) {
            updateFirestore(null)
            return
        }

        // 有選新圖片 → 先上傳 Storage，再更新 imageUrl
        val fileName = "product_${System.currentTimeMillis()}.jpg"
        val imageRef = storageRootRef.child("product_images/$fileName")

        imageRef.putFile(newImageUri)
            .continueWithTask { uploadTask ->
                if (!uploadTask.isSuccessful) {
                    throw uploadTask.exception ?: Exception("圖片上傳失敗")
                }
                imageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val url = downloadUri.toString()
                updateFirestore(url)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * 只更新商品「上架狀態」
     */
    fun updateProductActive(
        productId: String,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (productId.isBlank()) {
            onFailure(IllegalArgumentException("productId 不可為空"))
            return
        }

        productsCollection
            .document(productId)
            .update("isActive", isActive)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * 刪除商品
     */
    fun deleteProduct(
        productId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (productId.isBlank()) {
            onFailure(IllegalArgumentException("productId 不可為空"))
            return
        }

        productsCollection
            .document(productId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
    //只抓「上架中(isActive = true)」的商品，給一般使用者用
    fun getActiveProducts(
        onSuccess: (List<Product>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        productsCollection
            .whereEqualTo("isActive", true)   // 只抓上架中的
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<Product>()
                for (doc in snapshot.documents) {
                    val p = doc.toObject(Product::class.java)
                    if (p != null) {
                        p.id = doc.id
                        list.add(p)
                    }
                }
                onSuccess(list)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}
