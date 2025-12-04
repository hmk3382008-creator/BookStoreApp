package com.example.bookstoreapp.View

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.example.bookstoreapp.Firbase.FirebaseUserRepository
import com.example.bookstoreapp.Member.User
import com.example.bookstoreapp.R
import com.example.bookstoreapp.User.EditProfileDialog
import com.example.bookstoreapp.User.MyOrdersActivity

class MemberFragment : Fragment() {

    private lateinit var tvMemberTitle: TextView      // 上方標題「會員中心」
    private lateinit var tvMemberName: TextView       // 顯示「您好，XXX」
    private lateinit var btnEditProfile: Button       // 修改資料 / 密碼
    private lateinit var btnMyOrders: Button          // 我的訂單

    private var memberId: String = ""                 // 目前登入會員 Firebase id
    private var memberName: String = "會員"           // 初始名稱，載完 User 再更新

    private val userRepo = FirebaseUserRepository()
    private var currentUser: User? = null             // 從 Firebase 載回來的完整 User 物件

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 從 arguments 拿到登入者資訊（由 MainActivity 傳進來）
        memberId = arguments?.getString("userId") ?: ""
        memberName = arguments?.getString("userName") ?: "會員"

        // 安全檢查
        if (memberId.isBlank()) {
            Toast.makeText(requireContext(), "會員資訊有誤，請重新登入", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_member, container, false)

        tvMemberTitle = v.findViewById(R.id.tvMemberTitle)
        tvMemberName = v.findViewById(R.id.tvMemberName)
        btnEditProfile = v.findViewById(R.id.btnEditProfile)
        btnMyOrders = v.findViewById(R.id.btnMyOrders)

        // 先用登入傳進來的名字顯示
        tvMemberName.text = "您好，$memberName"

        // 讀取 Firebase 裡最新的會員資料（例如 Email、目前密碼等等）
        if (memberId.isNotBlank()) {
            userRepo.getUserById(
                userId = memberId,
                onSuccess = { user ->
                    currentUser = user
                    memberName = user.name
                    tvMemberName.text = "您好，${user.name}"
                },
                onFailure = {
                    Toast.makeText(requireContext(), "載入會員資料失敗", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 修改個人資料 / 密碼
        btnEditProfile.setOnClickListener {
            val u = currentUser
            if (u == null) {
                Toast.makeText(requireContext(), "尚未載入會員資料，請稍後再試", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialog = EditProfileDialog(
                user = u
            ) { updatedUser ->
                // 這裡會在更新成功後被呼叫
                currentUser = updatedUser
                memberName = updatedUser.name
                tvMemberName.text = "您好，${updatedUser.name}"
            }

            dialog.show(parentFragmentManager, "EditProfileDialog")
        }

        // 我的訂單 / 歷史訂單 → 開啟訂單列表 Activity
        btnMyOrders.setOnClickListener {
            if (memberId.isBlank()) {
                Toast.makeText(requireContext(), "會員資訊有誤，請重新登入", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ctx = requireContext()
            val intent = Intent(ctx, MyOrdersActivity::class.java)
            intent.putExtra("userId", memberId)
            intent.putExtra("userName", memberName)
            startActivity(intent)
        }

        return v
    }

    companion object {
        fun newInstance(userId: String, userName: String): MemberFragment {
            val f = MemberFragment()
            f.arguments = bundleOf(
                "userId" to userId,
                "userName" to userName
            )
            return f
        }
    }
}