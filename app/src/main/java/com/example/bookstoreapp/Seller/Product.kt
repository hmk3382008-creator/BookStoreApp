package com.example.bookstoreapp.Seller

data class Product(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val stock: Int = 0,
    val sellerId: String = "",
    val imageUrl: String? = null,
    var isFavorite: Boolean = false,
    // 新增：商品狀態（true = 上架中、false = 已下架）
    var isActive: Boolean = true
)