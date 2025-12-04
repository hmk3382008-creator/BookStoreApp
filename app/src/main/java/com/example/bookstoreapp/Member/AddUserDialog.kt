package com.example.bookstoreapp.Member

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.bookstoreapp.R
import com.example.bookstoreapp.Firbase.FirebaseUserRepository

class AddUserDialog(
    private val onUserAdded: (User) -> Unit   //  新增成功後要做的事，由外面決定
) : DialogFragment() {
    private val userRepo = FirebaseUserRepository()

    // UI 元件宣告
    private lateinit var etAccount: EditText
    private lateinit var etPassword: EditText
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var spRole: Spinner
    private lateinit var btnAdd: Button
    private lateinit var btnCancel: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.dialog_add_user, container, false)

        etAccount = view.findViewById(R.id.etAccount)
        etPassword = view.findViewById(R.id.etPassword)
        etName = view.findViewById(R.id.etName)
        etEmail = view.findViewById(R.id.etEmail)
        spRole = view.findViewById(R.id.spRole)
        btnAdd = view.findViewById(R.id.btnAddUser)
        btnCancel = view.findViewById(R.id.btnCancelUser)

        // 設定下拉選單內容：一般會員 / 一般商家
        val roles = arrayOf("一般會員", "一般商家")
        spRole.adapter = ArrayAdapter(
            view.context,
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )

        //  新增按鈕：開始新增會員流程
        btnAdd.setOnClickListener {

            val account = etAccount.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val role = spRole.selectedItem.toString()

            // 簡單檢查是否有空白欄位
            if (account.isEmpty() || password.isEmpty() ||
                name.isEmpty() || email.isEmpty()
            ) {
                Toast.makeText(context, "請填寫完整資料", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
                if (!isValidEmail(email)) {
                Toast.makeText(context, "Email格式錯誤", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }

            // 建立 User 物件（此時 id 先給空字串，Firebase 新增完會給我們）
            val user = User(
                id = "",
                account = account,
                password = password,
                name = name,
                email = email,
                role = role
            )

            // 改成呼叫 Firebase 版本的新增
            userRepo.registerUser(user) { result, newUser ->
                when (result) {
                    "SUCCESS" -> {
                        // newUser 不會是 null，因此用 ?: user 保險
                        val u = newUser ?: user
                        Toast.makeText(context, "新增成功", Toast.LENGTH_SHORT).show()
                        onUserAdded(u)  // 把新增好的 User 回傳給外面
                        dismiss()        // 關閉 Dialog
                    }
                    "EXISTS_ACCOUNT" -> {
                        Toast.makeText(context, "帳號或Email已存在", Toast.LENGTH_SHORT).show()
                    }
                    "EXISTS_ADMIN" -> {
                        Toast.makeText(context, "系統管理者已存在", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, "新增失敗，請稍後再試", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 取消按鈕：直接關閉 Dialog
        btnCancel.setOnClickListener { dismiss() }

        return view
    }

    override fun onStart() {
        super.onStart()
        //  調整 Dialog 寬度 = 螢幕寬度的 90%，高度自動
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}