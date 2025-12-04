package com.example.bookstoreapp.Member

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookstoreapp.View.MainActivity
import com.example.bookstoreapp.R
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var etAccount: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private val firestore = FirebaseFirestore.getInstance()   // 連線到 Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etAccount = findViewById(R.id.etAccount)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)

        btnLogin.setOnClickListener {
            val account = etAccount.text.toString().trim()     // 輸入的帳號
            val password = etPassword.text.toString().trim()   // 輸入的密碼

            if (account.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "請輸入帳號密碼", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 到 Firestore 查 users collection 中符合帳號＋密碼的那一筆
            firestore.collection("users")
                .whereEqualTo("account", account)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener { snapshots ->
                    if (snapshots.isEmpty) {
                        Toast.makeText(this, "帳號密碼錯誤", Toast.LENGTH_SHORT).show()
                    } else {
                        // 取出第一筆文件
                        val doc = snapshots.documents[0]

                        // 把欄位轉成 User 物件
                        val user = doc.toObject(User::class.java)

                        if (user != null) {
                            //  把 Firestore 的 documentId 塞回 user.id
                            user.id = doc.id

                            Toast.makeText(this, "登入成功：${user.name}", Toast.LENGTH_SHORT).show()

                            // 依照角色導向 MainActivity，讓 MainActivity 再決定要去哪一頁
                            val intent = Intent(this, MainActivity::class.java)

                            intent.putExtra("role", user.role)    // 身分：系統管理者 / 一般商家 / 一般會員
                            intent.putExtra("userId", user.id)    // Firebase 的 documentId（字串）
                            intent.putExtra("userName", user.name)// 顯示用名稱

                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "登入資料格式錯誤", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "登入發生錯誤：${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}