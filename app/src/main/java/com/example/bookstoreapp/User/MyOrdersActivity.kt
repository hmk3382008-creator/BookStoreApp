package com.example.bookstoreapp.User

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.Firbase.FirebaseOrderRepository
import com.example.bookstoreapp.R

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: OrderListAdapter
    private val orderRepo = FirebaseOrderRepository()
    private var userId: String = ""
    private var userName: String = "會員"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "我的訂單"
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { finish() }

        rvOrders = findViewById(R.id.rvOrders)
        tvEmpty = findViewById(R.id.tvEmpty)

        userId = intent.getStringExtra("userId") ?: ""
        userName = intent.getStringExtra("userName") ?: "會員"

        if (userId.isBlank()) {
            Toast.makeText(this, "會員資訊有誤，請重新登入", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rvOrders.layoutManager = LinearLayoutManager(this)
        adapter = OrderListAdapter(
            mutableListOf(),
            onOrderClick = { order ->
                val intent = Intent(this, OrderDetailActivity::class.java)
                intent.putExtra("orderId", order.id)
                intent.putExtra("userId", userId)
                intent.putExtra("userName", userName)
                startActivity(intent)
            }
        )
        rvOrders.adapter = adapter

        loadOrders()
    }

    private fun loadOrders() {
        orderRepo.getOrdersByUser(
            userId = userId,
            onSuccess = { list ->
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                adapter.updateList(list)
            },
            onFailure = { e ->
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "載入訂單失敗：${e.message}"
            }
        )
    }

    override fun onResume() {
        super.onResume()
        //  每次回到這個頁面，就重新抓一次最新訂單（付款後、取消後都會更新）
        loadOrders()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}