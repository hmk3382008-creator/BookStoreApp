package com.example.bookstoreapp.User

import java.security.Timestamp


data class Order(
    var id: String = "",                 // Firestore documentId
    val userId: String = "",             // 哪個會員
    val userName: String = "",           // 會員名稱
    var totalAmount: Int = 0,            // 總金額
    var status: String = "",             // 狀態（處理中 / 已出貨...）
    var createdAtText: String = ""  ,     // 直接存「已格式化好的日期字串
    var sellerStatus: String = "",
    var paymentMethod: String = "",      // 付款方式
    var receiverName: String = "",       // 收件人姓名
    var receiverPhone: String = "",      // 收件人電話
    var shippingAddress: String = ""     // 收件地址
)