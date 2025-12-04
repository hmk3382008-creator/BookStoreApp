package com.example.bookstoreapp.Member

data class User(
    var id: String = "",     // 使用 Firebase 的 document ID
    var account: String = "", // 帳號
    var password: String = "",// 密碼
    var name: String = "",    // 姓名
    var email: String = "",   // 電子郵件
    var role: String = ""     // 身分（例如 一般會員 / 商家 / 系統管理者）
)