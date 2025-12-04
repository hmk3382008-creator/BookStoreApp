package com.example.bookstoreapp.Firbase

import com.example.bookstoreapp.Member.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject

class FirebaseUserRepository {
    // 取得 Firestore 實例
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // 指定要操作的 collection 名稱為 "users"
    private val usersCollection = db.collection("users")

    /**
     * 取得所有會員（排除系統管理者）
     *
     * @param onSuccess 把查詢到的會員 List 傳給呼叫者
     * @param onFailure 查詢失敗時，把 Exception 傳給呼叫者
     */
    fun getAllUsers(
        onSuccess: (List<User>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        usersCollection
            .whereNotEqualTo("role", "系統管理者")  // 跟你原本 SQLite 一樣，排除系統管理者
            .get()
            .addOnSuccessListener { snapshot ->
                // 把每一筆 Document 轉成 User 物件
                val list = snapshot.documents.mapNotNull { doc ->
                    // 用 Firestore 的內容填入 User（除了 id）
                    val user = doc.toObject<User>()
                    // ⚠⚠⚠ 這一行非常重要：把 documentId 塞到 user.id 裡 ⚠⚠⚠
                    user?.copy(id = doc.id)
                }
                onSuccess(list)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * 新增會員（管理者新增）
     *
     * @param user 欲新增的使用者（id 先給空字串即可）
     * @param onResult 將結果碼 和 新增完成並帶有 id 的 User 回傳
     *
     * 結果碼：
     *  - "SUCCESS"        新增成功
     *  - "EXISTS_ADMIN"   系統管理者已存在
     *  - "EXISTS_ACCOUNT" 帳號或 Email 已存在
     *  - "FAIL"           其他錯誤
     */
    fun registerUser(
        user: User,
        onResult: (String, User?) -> Unit
    ) {
        // 如果要新增的是系統管理者，先確認是否已經有系統管理者存在
        if (user.role == "系統管理者") {
            usersCollection
                .whereEqualTo("role", "系統管理者")
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        // 已經有系統管理者了
                        onResult("EXISTS_ADMIN", null)
                    } else {
                        // 沒有系統管理者，繼續檢查帳號 / 信箱
                        checkAccountAndEmail(user, onResult)
                    }
                }
                .addOnFailureListener {
                    onResult("FAIL", null)
                }
        } else {
            // 一般會員 / 一般商家 → 直接檢查帳號 / 信箱
            checkAccountAndEmail(user, onResult)
        }
    }

    // 檢查帳號與 Email 是否重複
    private fun checkAccountAndEmail(
        user: User,
        onResult: (String, User?) -> Unit
    ) {
        // Firestore 不支援帳號 OR Email 一次查，所以拆成兩次查詢

        // 先查 account
        usersCollection
            .whereEqualTo("account", user.account)
            .limit(1)
            .get()
            .addOnSuccessListener { accountSnapshot ->
                if (!accountSnapshot.isEmpty) {
                    // 帳號已存在
                    onResult("EXISTS_ACCOUNT", null)
                } else {
                    // 帳號沒重複，再查 Email
                    usersCollection
                        .whereEqualTo("email", user.email)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { emailSnapshot ->
                            if (!emailSnapshot.isEmpty) {
                                // 信箱已存在
                                onResult("EXISTS_ACCOUNT", null)
                            } else {
                                // 帳號與信箱都沒重複 → 實際寫入
                                insertUser(user, onResult)
                            }
                        }
                        .addOnFailureListener {
                            onResult("FAIL", null)
                        }
                }
            }
            .addOnFailureListener {
                onResult("FAIL", null)
            }
    }

    // 真正對 Firestore 新增一筆 user 資料
    private fun insertUser(
        user: User,
        onResult: (String, User?) -> Unit
    ) {
        val data = mapOf(
            "account" to user.account,
            "password" to user.password,
            "name" to user.name,
            "email" to user.email,
            "role" to user.role
        )

        // add()：讓 Firestore 自動產生一個 documentId
        usersCollection
            .add(data)
            .addOnSuccessListener { docRef ->
                // docRef.id 就是這一筆 user 的 documentId
                val newUser = user.copy(id = docRef.id)
                onResult("SUCCESS", newUser)
            }
            .addOnFailureListener {
                onResult("FAIL", null)
            }
    }

    /**
     * 更新會員資料
     */
    fun updateUser(
        user: User,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // ⚠ 這裡一定要有 id，不然不知道要更新哪一筆文件
        if (user.id.isBlank()) {
            onFailure(IllegalArgumentException("User id 不可為空"))
            return
        }

        // 要更新的欄位
        val data = mapOf(
            "account" to user.account,
            "password" to user.password,
            "name" to user.name,
            "email" to user.email,
            "role" to user.role
        )

        // document(user.id)：指定某一筆文件 → 去更新它
        usersCollection
            .document(user.id)
            .update(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * 刪除會員資料
     */
    fun deleteUser(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (userId.isBlank()) {
            onFailure(IllegalArgumentException("User id 不可為空"))
            return
        }

        usersCollection
            .document(userId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
    // 取得單一會員資料（用 documentId）
    fun getUserById(
        userId: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (userId.isBlank()) {
            onFailure(IllegalArgumentException("userId 不可為空"))
            return
        }

        usersCollection
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    // Firestore 裡的資料沒有 id 欄位，所以這裡補上 documentId
                    val userWithId = user.copy(id = doc.id)
                    onSuccess(userWithId)
                } else {
                    onFailure(IllegalStateException("找不到會員資料"))
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}