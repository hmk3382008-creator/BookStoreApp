package com.example.bookstoreapp.View

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.Firbase.FirebaseFavoriteRepository
import com.example.bookstoreapp.Firebase.FirebaseProductRepository
import com.example.bookstoreapp.Member.LoginActivity
import com.example.bookstoreapp.R
import com.example.bookstoreapp.User.MemberProductAdapter

class UserHomeFragment : Fragment() {

    // 負責從 Firebase 抓商品
    private val productRepo = FirebaseProductRepository()

    // 收藏功能的 Repository
    private val favoriteRepo = FirebaseFavoriteRepository()

    // 畫面元件
    private lateinit var rv: RecyclerView
    private lateinit var adapter: MemberProductAdapter
    private lateinit var tvWelcome: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var btnCart: ImageButton   // 你原本用來做登出

    // 目前登入會員資料（全部用「字串」）
    private var memberId: String = ""          // Firebase 的 user.id
    private var memberName: String = "會員"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 從 arguments 取得登入會員資訊（由 MainActivity 傳入）
        arguments?.let { bundle ->
            memberId = bundle.getString("userId", "")
            memberName = bundle.getString("userName", "會員")
        }

        // 如果真的少了 userId，就先吐個 Toast（不會崩潰）
        if (memberId.isBlank()) {
            Toast.makeText(requireContext(), "會員資訊有誤，請重新登入", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_user_home, container, false)

        // 綁定 View
        tvWelcome = v.findViewById(R.id.tvWelcome)
        tvSubtitle = v.findViewById(R.id.tvSubtitle)
        btnCart = v.findViewById(R.id.btnCart)
        rv = v.findViewById(R.id.rvMemberProducts)

        // 顯示歡迎文字
        tvWelcome.text = "歡迎回來，$memberName"
        tvSubtitle.text = "瀏覽所有上架的二手書"

        // 設定 RecyclerView
        rv.layoutManager = LinearLayoutManager(requireContext())

        // 建立 Adapter（改用新的建構子）
        adapter = MemberProductAdapter(
            userId = memberId,
            favoriteRepo = favoriteRepo,
            items = mutableListOf(),

            // 加入購物車
            onAddToCart = { product ->
                CartManager.addProduct(product, 1)
                Toast.makeText(
                    requireContext(),
                    "已加入購物車：${product.name}",
                    Toast.LENGTH_SHORT
                ).show()
            },

            // 查看詳情（之後可以改成跳轉商品詳情頁）
            onDetail = { product ->
                Toast.makeText(
                    requireContext(),
                    "查看詳情：${product.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        rv.adapter = adapter

        // 登出功能（btnCart）
        btnCart.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("確認登出")
                .setMessage("確定要登出嗎？")
                .setPositiveButton("登出") { dialog, _ ->
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    dialog.dismiss()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        // 一進來就從 Firebase 抓商品 + 收藏狀態
        loadProductsFromFirebase()

        return v
    }

    // ----------------- 從 Firebase 載入商品 + 收藏狀態 -----------------

    private fun loadProductsFromFirebase() {
        // ★★★ 修改這一行：改成只從 Firebase 抓「isActive = true」的商品 ★★★
        productRepo.getActiveProducts(
            onSuccess = { list ->

                // ★ list 這裡已經是「只有上架中的商品」了，不需要再 filter 一次
                val productList = list.map { p ->
                    p.copy(isFavorite = false)
                }

                if (memberId.isBlank()) {
                    adapter.submitList(productList)
                    return@getActiveProducts   // ★ 記得這裡方法名也要改
                }

                // 再抓該會員的收藏清單
                favoriteRepo.getFavoritesByUser(
                    userId = memberId,
                    onSuccess = { favoriteList ->
                        val favoriteIds = favoriteList.map { it.id }.toSet()

                        val merged = productList.map { p ->
                            p.copy(isFavorite = favoriteIds.contains(p.id))
                        }
                        adapter.submitList(merged)
                    },
                    onFailure = { _ ->
                        // 收藏載入失敗就當成全部沒收藏
                        adapter.submitList(productList)
                    }
                )
            },
            onFailure = { e ->
                Toast.makeText(
                    requireContext(),
                    "載入商品失敗：${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // 提供給 MainActivity 建立這個 Fragment
    companion object {
        fun newInstance(userId: String, memberName: String): UserHomeFragment {
            val f = UserHomeFragment()
            f.arguments = bundleOf(
                "userId" to userId,
                "userName" to memberName
            )
            return f
        }
    }
}