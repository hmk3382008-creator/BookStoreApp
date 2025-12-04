package com.example.bookstoreapp.User

data class OrderItem(
    var id: String = "",           // Firestore 裡該明細文件的 id
    var productId: String = "",    // 商品 id（如果之後要查商品）
    var productName: String = "",  // 商品名稱
    var price: Int = 0,            // 單價
    var quantity: Int = 0,         // 數量
    var sellerId: String = "" ,// 哪個商家的商品

    var sellerStatus: String = "處理中" //：處理中 / 已接單 / 已出貨 / 已完成 / 已取消
)