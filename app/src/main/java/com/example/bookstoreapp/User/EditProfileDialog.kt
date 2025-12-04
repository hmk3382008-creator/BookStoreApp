package com.example.bookstoreapp.User

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.bookstoreapp.Firbase.FirebaseUserRepository
import com.example.bookstoreapp.Member.User
import com.example.bookstoreapp.R

class EditProfileDialog(
    private val user: User,                        // 目前登入這位會員
    private val onUpdated: (User) -> Unit          // 更新成功後，要把新 User 回傳給外面
) : DialogFragment() {

    private val userRepo = FirebaseUserRepository()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_profile, null)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etOldPassword = view.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = view.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = view.findViewById<EditText>(R.id.etConfirmPassword)

        // 先把現在的資料塞進去（密碼不顯示）
        etName.setText(user.name)
        etEmail.setText(user.email)

        builder.setView(view)
            .setTitle("修改個人資料 / 密碼")
            // 這裡先給空的 Listener，等一下在 onStart 自己接 click，
            // 這樣可以避免按一下就一定關掉 Dialog
            .setPositiveButton("儲存", null)
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }

    override fun onStart() {
        super.onStart()

        // 這裡可以取得真正的 AlertDialog，然後自己接「儲存」按鈕的點擊事件
        val dialog = dialog as? AlertDialog ?: return

        // 取得正向按鈕（「儲存」）
        val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

        // 重新設定「儲存」的點擊邏輯
        positiveButton.setOnClickListener {
            // Dialog 還沒關，在這裡做欄位檢查、呼叫 Firebase
            handleSaveClicked()
        }
    }

    private fun handleSaveClicked() {
        val dialogView = dialog?.findViewById<View>(android.R.id.content)?.rootView
            ?: return

        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etOldPassword = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        val newName = etName.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()
        val oldPasswordInput = etOldPassword.text.toString()
        val newPasswordInput = etNewPassword.text.toString()
        val confirmPasswordInput = etConfirmPassword.text.toString()

        // 1 基本欄位檢查
        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(requireActivity(), "姓名與 Email 不可空白", Toast.LENGTH_SHORT).show()
            return  //  不關 Dialog，只是提醒
        }

        //  預設沿用原本的密碼
        var finalPassword = user.password

        // 只要三個密碼欄位有任何一個有填，就視為「有要改密碼」
        if (
            oldPasswordInput.isNotEmpty() ||
            newPasswordInput.isNotEmpty() ||
            confirmPasswordInput.isNotEmpty()
        ) {
            // 檢查舊密碼是否正確
            if (oldPasswordInput != user.password) {
                Toast.makeText(requireActivity(), "原密碼錯誤", Toast.LENGTH_SHORT).show()
                return
            }

            // 新密碼不可空
            if (newPasswordInput.isEmpty()) {
                Toast.makeText(requireActivity(), "請輸入新密碼", Toast.LENGTH_SHORT).show()
                return
            }

            // 新密碼需與確認密碼一致
            if (newPasswordInput != confirmPasswordInput) {
                Toast.makeText(requireActivity(), "新密碼與確認密碼不一致", Toast.LENGTH_SHORT).show()
                return
            }

            // 通過檢查 → 使用新密碼
            finalPassword = newPasswordInput
        }

        //  建立更新後的 User 物件
        val updatedUser = user.copy(
            name = newName,
            email = newEmail,
            password = finalPassword
        )

        //  呼叫 Firebase 更新
        userRepo.updateUser(
            user = updatedUser,
            onSuccess = {

                Toast.makeText(requireActivity(), "更新成功", Toast.LENGTH_SHORT).show()

                // 通知外面的 Fragment / Activity 更新畫面
                onUpdated(updatedUser)

                // 關掉 Dialog
                dismissAllowingStateLoss()
            },
            onFailure = { e ->
                Toast.makeText(
                    requireActivity(),
                    "更新失敗：${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}