package com.example.bookstoreapp.Member

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookstoreapp.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var etAccount: EditText
    private lateinit var etPassword: EditText
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var spRole: Spinner
    private lateinit var btnRegister: Button
    // Firebase 連線
    private val firestore = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etAccount = findViewById(R.id.etAccount)
        etPassword = findViewById(R.id.etPassword)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        spRole = findViewById(R.id.spRole)
        btnRegister = findViewById(R.id.btnRegister)
        //返回
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarRegister)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 設定下拉選單（會員角色）
        val roles = listOf("一般使用者", "一般商家", "系統管理者")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRole.adapter = adapter

        btnRegister.setOnClickListener {
            val account = etAccount.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val role = spRole.selectedItem.toString()

            // 檢查是否有空值
            if (account.isEmpty() || password.isEmpty() || name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "請完整填寫資料", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Email格式錯誤", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 建立 user 物件（id 先給 0，也可以不給）
            val user = User(
                id = "",
                account = account,
                password = password,
                name = name,
                email = email,
                role = role
            )

            // 開始進行註冊流程 → Firebase 版本
            checkAccountAndEmail(user)
        }
    }


    // 檢查帳號是否存在
    private fun checkAccountAndEmail(user: User) {

        firestore.collection("users")
            .whereEqualTo("account", user.account)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    Toast.makeText(this, "帳號已存在", Toast.LENGTH_SHORT).show()
                } else {
                    checkEmail(user)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "註冊失敗，請稍後再試", Toast.LENGTH_SHORT).show()
            }
    }

    // ========================
    // 檢查 Email 是否存在
    // ========================
    private fun checkEmail(user: User) {

        firestore.collection("users")
            .whereEqualTo("email", user.email)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    Toast.makeText(this, "Email 已存在", Toast.LENGTH_SHORT).show()
                } else {
                    checkAdmin(user)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "註冊失敗，請稍後再試", Toast.LENGTH_SHORT).show()
            }
    }

    // 如果是系統管理者,檢查只能有 1 個
    private fun checkAdmin(user: User) {

        if (user.role != "系統管理者") {
            insertUser(user)
            return
        }

        firestore.collection("users")
            .whereEqualTo("role", "系統管理者")
            .get()
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    Toast.makeText(this, "系統管理者已存在，無法新增", Toast.LENGTH_SHORT).show()
                } else {
                    insertUser(user)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "註冊失敗，請稍後再試", Toast.LENGTH_SHORT).show()
            }
    }


    // 寫入 Firebase
   private fun insertUser(user: User) {

        firestore.collection("users")
            .add(user)
            .addOnSuccessListener { docRef->
                val id = docRef.id
                docRef.update("id", id)
                Toast.makeText(this, "註冊成功，請返回登入", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "註冊失敗，請稍後再試", Toast.LENGTH_SHORT).show()
            }
    }
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}