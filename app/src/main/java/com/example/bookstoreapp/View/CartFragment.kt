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
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.Firbase.FirebaseOrderRepository
import com.example.bookstoreapp.R
import com.example.bookstoreapp.User.CheckoutActivity

class CartFragment : Fragment() {

    private lateinit var rvCart: RecyclerView
    private lateinit var tvTotal: TextView
    private lateinit var btnCheckout: Button
    private lateinit var adapter: CartAdapter

    //  目前登入會員資訊（由 Activity 傳入）
    private var memberId: String = ""
    private var memberName: String = "會員"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // 從 arguments 拿 userId / userName（MainActivity 建立 Fragment 時傳入）
        memberId = arguments?.getString("userId") ?: ""
        memberName = arguments?.getString("userName") ?: "會員"

        val view = inflater.inflate(R.layout.fragment_cart, container, false)
        rvCart = view.findViewById(R.id.rvCart)
        tvTotal = view.findViewById(R.id.tvTotal)
        btnCheckout = view.findViewById(R.id.btnCheckout)

        rvCart.layoutManager = LinearLayoutManager(requireContext())

        adapter = CartAdapter(
            items = CartManager.getAll().toMutableList(),
            onPlus = { item ->
                if (item.quantity >= item.product.stock) {
                    Toast.makeText(
                        requireContext(),
                        "庫存只有 ${item.product.stock} 本",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@CartAdapter
                }

                CartManager.changeQuantity(item.product.id, item.quantity + 1)
                refreshUI()
            },
            onMinus = { item ->
                CartManager.changeQuantity(item.product.id, item.quantity - 1)
                refreshUI()
            },
            onDelete = { item ->
                confirmDelete(item)
            },
            onEditQty = { item, newQty ->
                val max = item.product.stock
                val fixedQty = newQty.coerceIn(1, max)

                if (newQty > max) {
                    Toast.makeText(
                        requireContext(),
                        "庫存只有 $max 本，已自動改為 $max",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                CartManager.changeQuantity(item.product.id, fixedQty)
                tvTotal.text = "總金額：$${CartManager.totalPrice()}"
            }
        )

        rvCart.adapter = adapter

        refreshUI()

        btnCheckout.setOnClickListener {
            val items = CartManager.getAll()
            if (items.isEmpty()) {
                Toast.makeText(requireContext(), "購物車是空的", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (memberId.isBlank()) {
                Toast.makeText(requireContext(), "會員資訊有誤，請重新登入", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 跳轉到結帳頁（付款頁），帶真正登入的 userId / userName
            val intent = Intent(requireContext(), CheckoutActivity::class.java)
            intent.putExtra("userId", memberId)
            intent.putExtra("userName", memberName)
            startActivity(intent)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun refreshUI() {
        val list = CartManager.getAll()
        adapter.updateList(list)
        tvTotal.text = "總金額：$${CartManager.totalPrice()}"
    }

    private fun confirmDelete(item: CartManager.CartItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("刪除商品")
            .setMessage("確定要刪除「${item.product.name}」嗎？")
            .setPositiveButton("刪除") { dialog, _ ->
                CartManager.removeProduct(item.product.id)
                refreshUI()
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    companion object {
        fun newInstance(userId: String, userName: String): CartFragment {
            val f = CartFragment()
            f.arguments = bundleOf(
                "userId" to userId,
                "userName" to userName
            )
            return f
        }
    }
}