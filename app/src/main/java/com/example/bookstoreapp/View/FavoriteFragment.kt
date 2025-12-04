package com.example.bookstoreapp.View

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.Firbase.FirebaseFavoriteRepository
import com.example.bookstoreapp.R
import com.example.bookstoreapp.User.FavoriteAdapter
import com.example.bookstoreapp.User.MemberProductAdapter

class FavoriteFragment : Fragment() {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: FavoriteAdapter

    private val favoriteRepo = FirebaseFavoriteRepository()
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_favorite, container, false)

        rvFavorites = v.findViewById(R.id.rvFavorites)
        tvEmpty = v.findViewById(R.id.tvEmpty)

        rvFavorites.layoutManager = LinearLayoutManager(requireContext())

        adapter = FavoriteAdapter(
            items = mutableListOf(),
            onAddToCart = { product ->
                CartManager.addProduct(product, 1)
                Toast.makeText(
                    requireContext(),
                    "已加入購物車：${product.name}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onRemoveFavorite = { product ->
                favoriteRepo.removeFavorite(
                    userId = userId,
                    productId = product.id
                ) { success ->
                    if (success) {
                        Toast.makeText(
                            requireContext(),
                            "已取消收藏：${product.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadFavorites()  // 重新載入列表
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "取消收藏失敗",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        rvFavorites.adapter = adapter

        loadFavorites()

        return v
    }

    private fun loadFavorites() {
        if (userId.isBlank()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "會員資訊有誤，請重新登入"
            return
        }

        favoriteRepo.getFavoritesByUser(
            userId = userId,
            onSuccess = { list ->
                if (list.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "目前沒有任何收藏"
                } else {
                    tvEmpty.visibility = View.GONE
                }
                adapter.updateList(list)
            },
            onFailure = { e ->
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "載入收藏失敗：${e.message}"
            }
        )
    }

    companion object {
        fun newInstance(userId: String): FavoriteFragment {
            val f = FavoriteFragment()
            f.arguments = bundleOf("userId" to userId)
            return f
        }
    }
}