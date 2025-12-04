package com.example.bookstoreapp.Seller

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.Firbase.FirebaseOrderRepository
import com.example.bookstoreapp.R
import com.example.bookstoreapp.User.OrderListAdapter

class MerchantOrdersActivity : AppCompatActivity() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: OrderListAdapter

    private val orderRepo = FirebaseOrderRepository()
    private var sellerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merchant_orders)

        sellerId = intent.getStringExtra("sellerId") ?: ""

        if (sellerId.isBlank()) {
            Toast.makeText(this, "商家資料有誤", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "訂單管理"
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { finish() }

        rvOrders = findViewById(R.id.rvOrders)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvOrders.layoutManager = LinearLayoutManager(this)

        adapter = OrderListAdapter(mutableListOf()) { order ->
            val intent = Intent(this, MerchantOrderDetailActivity::class.java)
            intent.putExtra("orderId", order.id)
            intent.putExtra("sellerId", sellerId)   //  把 sellerId 傳過去
            startActivity(intent)
        }

        rvOrders.adapter = adapter
        loadOrders()
    }

    private fun loadOrders() {
        orderRepo.getOrdersBySeller(
            sellerId = sellerId,
            onSuccess = { list ->
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                adapter.updateList(list)
            },
            onFailure = {
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "載入失敗：${it.message}"
            }
        )
    }

    override fun onResume() {
        super.onResume()
        loadOrders()   // 回到這個畫面就重新載入訂單
    }
}