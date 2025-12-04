package com.example.bookstoreapp.Member

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.bookstoreapp.R
import com.example.bookstoreapp.Firbase.FirebaseUserRepository

class EditUserDialog(
    private val user: User,                 // 要編輯的那一位會員
    private val onUpdated: (User) -> Unit   // 更新成功後，要通知外面怎麼處理
) : DialogFragment() {

    // Firebase User 資料存取的 Repository
    private val userRepo = FirebaseUserRepository()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_user, null)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val spRole = view.findViewById<Spinner>(R.id.spRole)

        // ===== 初始化原本會員資料到畫面上 =====
        etName.setText(user.name)
        etEmail.setText(user.email)

        val roles = arrayOf("一般會員", "一般商家")
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            roles
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRole.adapter = spinnerAdapter

        val roleIndex = roles.indexOf(user.role).let { index ->
            if (index >= 0) index else 0
        }
        spRole.setSelection(roleIndex)

        // ===== 組裝 Dialog =====
        builder.setView(view)
            .setTitle("編輯會員")
            .setPositiveButton("儲存") { _, _ ->
                val newName = etName.text.toString().trim()
                val newEmail = etEmail.text.toString().trim()
                val newRole = spRole.selectedItem.toString()

                if (newName.isEmpty() || newEmail.isEmpty()) {
                    val ctx = context
                    if (ctx != null) {
                        Toast.makeText(ctx, "姓名與 Email 不可空白", Toast.LENGTH_SHORT).show()
                    }
                    return@setPositiveButton
                }

                val updatedUser = user.copy(
                    name = newName,
                    email = newEmail,
                    role = newRole
                )

                userRepo.updateUser(
                    updatedUser,
                    onSuccess = {
                        // ⚠ 這裡不要再用 `return@updateUser` 提早結束
                        val ctx = context
                        if (ctx != null) {
                            Toast.makeText(ctx, "更新成功", Toast.LENGTH_SHORT).show()
                        }

                        // 👉 就算沒有 context，也要照樣通知外面刷新畫面
                        onUpdated(updatedUser)
                        dismiss()
                    },
                    onFailure = { e ->
                        val ctx = context
                        if (ctx != null) {
                            Toast.makeText(
                                ctx,
                                "更新失敗：${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        // 失敗的情況就不用叫 onUpdated 了，畫面維持原樣即可
                    }
                )
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }
}