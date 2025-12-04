package com.example.bookstoreapp.Member

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.bookstoreapp.Seller.Product

class MyDBHelper(context: Context) : SQLiteOpenHelper(context, "shop.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // 建立使用者表格
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account TEXT,
                password TEXT,
                name TEXT,
                email TEXT,
                role TEXT
            )
        """.trimIndent()
        )
        // 建立商品表格
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS products(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT,
            description TEXT,
            price REAL,
            stock INTEGER,
            sellerId INTEGER,
            imageUri TEXT
        )
        """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 如果要升級 DB，先刪掉舊表格再重建
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    // 新增使用者
    fun insert(user: User) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("account", user.account)
            put("password", user.password)
            put("name", user.name)
            put("email", user.email)
            put("role", user.role)
        }
        db.insert("users", null, values)
    }
}
    // 登入查詢   (已改雲端存取)
//    fun login(account: String, password: String): User? {
//        val db = readableDatabase
//        val cursor = db.rawQuery(
//            "SELECT * FROM users WHERE account=? AND password=?",
//            arrayOf(account, password)
//        )
//        cursor.use {
//            if (it.moveToFirst()) {
//                return User(
//                    id = it.getString(it.getColumnIndexOrThrow("id")),
//                    account = it.getString(it.getColumnIndexOrThrow("account")),
//                    password = it.getString(it.getColumnIndexOrThrow("password")),
//                    name = it.getString(it.getColumnIndexOrThrow("name")),
//                    email = it.getString(it.getColumnIndexOrThrow("email")),
//                    role = it.getString(it.getColumnIndexOrThrow("role"))
//                )
//            }
//        }
//        return null // 查無此帳號
//    }

    // 新增會員（含 id 回寫）
    // ======================
//    fun register(user: User): String {
//        val db = writableDatabase
//
//        // 檢查管理者是否已存在
//        if (user.role == "系統管理者") {
//            val checkAdmin = db.rawQuery("SELECT * FROM users WHERE role='系統管理者'", null)
//            checkAdmin.use {
//                if (it.moveToFirst()) return "EXISTS_ADMIN"
//            }
//        }
//
//        // 檢查帳號或 Email 是否已存在
//        val cursor = db.rawQuery(
//            "SELECT * FROM users WHERE account=? OR email=?",
//            arrayOf(user.account, user.email)
//        )
//        cursor.use {
//            if (it.moveToFirst()) return "EXISTS_ACCOUNT"
//        }
//
//        val values = ContentValues().apply {
//            put("account", user.account)
//            put("password", user.password)
//            put("name", user.name)
//            put("email", user.email)
//            put("role", user.role)
//        }
//
//        val newId = db.insert("users", null, values)
//        if (newId == -1L) return "FAIL"
//
//        user.id = newId.toString()  // ⚡ 重要：把資料庫生成 id 寫回 user
//
//        return "SUCCESS"
//    }
//
//    // ======================
//    // 刪除會員
//    // ======================
//    fun deleteUser(userId: String): Boolean {
//        if (userId <= "") return false
//        val db = writableDatabase
//        val rows = db.delete("users", "id=?", arrayOf(userId.toString()))
//        return rows > 0
//    }
//
//    // ======================
//    // 修改會員
//    // ======================
//    fun updateUser(user: User): Boolean {
//        val db = writableDatabase
//        val values = ContentValues().apply {
//            put("account", user.account)
//            put("password", user.password)
//            put("name", user.name)
//            put("email", user.email)
//            put("role", user.role)
//        }
//        val rows = db.update("users", values, "id=?", arrayOf(user.id.toString()))
//        return rows > 0
//    }
//
//    // ======================
//    // 取得所有會員
//    // ======================
//    fun getAllUsers(): MutableList<User> {
//        val db = readableDatabase
//        val cursor = db.rawQuery("SELECT * FROM users WHERE role!='系統管理者'", null)
//        val users = mutableListOf<User>()
//        cursor.use {
//            if (it.moveToFirst()) {
//                do {
//                    users.add(
//                        User(
//                            id = it.getString(it.getColumnIndexOrThrow("id")),
//                            account = it.getString(it.getColumnIndexOrThrow("account")),
//                            password = it.getString(it.getColumnIndexOrThrow("password")),
//                            name = it.getString(it.getColumnIndexOrThrow("name")),
//                            email = it.getString(it.getColumnIndexOrThrow("email")),
//                            role = it.getString(it.getColumnIndexOrThrow("role"))
//                        )
//                    )
//                } while (it.moveToNext())
//            }
//        }
//        return users
//    }
//    // 新增商品
//    fun insertProduct(product: Product): Boolean {
//        val db = writableDatabase
//        val values = ContentValues().apply {
//            put("name", product.name)
//            put("description", product.description)
//            put("price", product.price)
//            put("stock", product.stock)
//            put("sellerId", product.sellerId)
//            put("imageUri", product.imageUri)
//        }
//        val result = db.insert("products", null, values)
//        return result != -1L
//    }
//
//    // 取得商家資料
//    fun getUserById(userId: Int): User? {
//        val db = readableDatabase
//        val cursor = db.rawQuery("SELECT * FROM users WHERE id=?", arrayOf(userId.toString()))
//        cursor.use {
//            if (it.moveToFirst()) {
//                return User(
//                    id = it.getString(it.getColumnIndexOrThrow("id")),
//                    account = it.getString(it.getColumnIndexOrThrow("account")),
//                    password = it.getString(it.getColumnIndexOrThrow("password")),
//                    name = it.getString(it.getColumnIndexOrThrow("name")),
//                    email = it.getString(it.getColumnIndexOrThrow("email")),
//                    role = it.getString(it.getColumnIndexOrThrow("role"))
//                )
//            }
//        }
//        return null
//    }
//    //取得商品列表
//    fun getProductsBySellerId(sellerId: Int): List<Product> {
//        val db = readableDatabase
//        val cursor = db.rawQuery(
//            "SELECT * FROM products WHERE sellerId=?",
//            arrayOf(sellerId.toString())
//        )
//        val list = mutableListOf<Product>()
//        cursor.use { c ->
//            if (c.moveToFirst()) {
//                do {
//                    list.add(
//                        Product(
//                            id = c.getInt(c.getColumnIndexOrThrow("id")),
//                            name = c.getString(c.getColumnIndexOrThrow("name")),
//                            description = c.getString(c.getColumnIndexOrThrow("description")),
//                            price = c.getInt(c.getColumnIndexOrThrow("price")),
//                            stock = c.getInt(c.getColumnIndexOrThrow("stock")),
//                            sellerId = c.getString(c.getColumnIndexOrThrow("sellerId")),
//                            imageUri = c.getString(c.getColumnIndexOrThrow("imageUri"))
//                        )
//                    )
//                } while (c.moveToNext())
//            }
//        }
//        return list
//    }
//    // 更新商品（僅允許更新自己名下的商品）
//    fun updateProduct(product: Product, sellerId: Int): Boolean {    // 需要商品內容與賣家ID做權限驗證
//        val db = writableDatabase                                    // 取得可寫資料庫
//        val values = ContentValues().apply {                          // 建立內容集合
//            put("name", product.name)                                 // 更新名稱
//            put("description", product.description)                   // 更新描述
//            put("price", product.price)                               // 更新價格
//            put("stock", product.stock)                               // 更新庫存
//            put("imageUri", product.imageUri)                         // 更新圖片URI
//        }
//        val rows = db.update(                                         // 執行更新
//            "products",                                               // 表格名稱
//            values,                                                   // 要更新的欄位
//            "id=? AND sellerId=?",                                    // 條件：商品ID且賣家ID相符
//            arrayOf(product.id.toString(), sellerId.toString())       // 參數陣列
//        )
//        return rows > 0                                               // 回傳是否有影響到列（成功）
//    }
//
//    // 刪除商品（僅允許刪除自己名下的商品）
//    fun deleteProduct(productId: Int, sellerId: Int): Boolean {       // 傳入商品ID與賣家ID
//        val db = writableDatabase                                     // 取得可寫資料庫
//        val rows = db.delete(                                         // 執行刪除
//            "products",                                               // 表格名稱
//            "id=? AND sellerId=?",                                    // 條件：商品ID且賣家ID相符
//            arrayOf(productId.toString(), sellerId.toString())        // 參數陣列
//        )
//        return rows > 0                                               // 有刪到列代表成功
//    }
//    fun getAllProducts():List<Product> {
//        val db = readableDatabase
//        val cursor = db.rawQuery("SELECT * FROM products", null)
//        val list = mutableListOf<Product>()
//        cursor.use { c ->
//            if (c.moveToFirst()) {
//                do {
//                    list.add(
//                        Product(
//                            id = c.getInt(c.getColumnIndexOrThrow("id")),                 // 商品 id
//                            name = c.getString(c.getColumnIndexOrThrow("name")),          // 商品名稱
//                            description = c.getString(c.getColumnIndexOrThrow("description")), // 商品描述
//                            price = c.getInt(c.getColumnIndexOrThrow("price")),           // 價格（用 Int 讀）
//                            stock = c.getInt(c.getColumnIndexOrThrow("stock")),           // 庫存
//                            sellerId = c.getString(c.getColumnIndexOrThrow("sellerId")),     // 所屬商家 id
//                            imageUri = c.getString(c.getColumnIndexOrThrow("imageUri"))   // 圖片 Uri 字串
//                        )
//                    )
//                }
//                while (c.moveToNext())
//            }
//        }
//        return list
//    }
//}
//
