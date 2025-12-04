package com.example.bookstoreapp.User

import com.example.bookstoreapp.Seller.Product

data class CartItem(                         // 購物車裡的一項商品
    val product: Product,                    // 對應的商品物件
    var quantity: Int                        // 此商品選購的數量
)