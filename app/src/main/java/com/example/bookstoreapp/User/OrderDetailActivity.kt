package com.example.bookstoreapp.User

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.Firbase.FirebaseOrderRepository
import com.example.bookstoreapp.R

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var tvOrderId: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvOrderTotal: TextView
    private lateinit var rvItems: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnCancelOrder: Button

    private val orderRepo = FirebaseOrderRepository()

    private var orderId: String = ""
    private var userId: String = ""
    private var currentOrder: Order? = null
    private val items: MutableList<OrderItem> = mutableListOf()
    private var adapter: UserOrderItemAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "訂單明細"
        }
        toolbar.setNavigationOnClickListener { finish() }

        orderId = intent.getStringExtra("orderId") ?: ""
        userId = intent.getStringExtra("userId") ?: ""

        if (orderId.isBlank()) {
            Toast.makeText(this, "訂單資料錯誤", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvOrderId = findViewById(R.id.tvOrderId)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        tvOrderStatus = findViewById(R.id.tvOrderStatus)
        tvOrderTotal = findViewById(R.id.tvOrderTotal)
        tvEmpty = findViewById(R.id.tvEmpty)
        rvItems = findViewById(R.id.rvItems)
        btnCancelOrder = findViewById(R.id.btnCancelOrder)

        rvItems.layoutManager = LinearLayoutManager(this)

        btnCancelOrder.setOnClickListener {
            confirmCancelOrder()
        }

        loadOrder()
    }

    private fun loadOrder() {
        orderRepo.getOrderDetail(
            orderId = orderId,
            onSuccess = { order, list ->
                currentOrder = order
                items.clear()
                items.addAll(list)

                tvOrderId.text = "訂單：${order.id}"
                tvOrderDate.text = "日期：${order.createdAtText}"
                tvOrderStatus.text = "狀態：${order.status}"

                val total = list.sumOf { it.price * it.quantity }
                tvOrderTotal.text = "總金額：$${total}"

                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

                if (order.status == "處理中") {
                    btnCancelOrder.visibility = View.VISIBLE
                } else {
                    btnCancelOrder.visibility = View.GONE
                }

                adapter = UserOrderItemAdapter(
                    items = items,
                    orderStatus = order.status,
                    onDelete = { item ->
                        confirmDeleteItem(item)
                    },
                    onUpdateQty = { item, newQty ->
                        updateItemQty(item, newQty)
                    }
                )
                rvItems.adapter = adapter
            },
            onFailure = { e ->
                Toast.makeText(this, "載入失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    //  取消訂單前跳出確認視窗
    private fun confirmCancelOrder() {
        val order = currentOrder ?: return
        if (order.status != "處理中") {
            Toast.makeText(this, "此訂單目前無法取消", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("取消訂單")
            .setMessage("確定要取消這筆訂單嗎？")
            .setPositiveButton("確認") { _, _ ->
                orderRepo.cancelOrder(
                    orderId = order.id,
                    onSuccess = {
                        Toast.makeText(this, "訂單已取消", Toast.LENGTH_SHORT).show()
                        loadOrder()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "取消失敗：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("取消", null)
            .show()
    }

    //  刪除明細前跳出確認視窗
    private fun confirmDeleteItem(item: OrderItem) {
        val order = currentOrder ?: return
        if (order.status != "處理中") {
            Toast.makeText(this, "此訂單目前無法修改", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("刪除商品")
            .setMessage("確定要刪除「${item.productName}」嗎？")
            .setPositiveButton("刪除") { _, _ ->
                orderRepo.deleteOrderItem(
                    orderId = order.id,
                    itemId = item.id,
                    onSuccess = {
                        Toast.makeText(this, "已刪除商品", Toast.LENGTH_SHORT).show()
                        loadOrder()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "刪除失敗：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateItemQty(item: OrderItem, newQty: Int) {
        val order = currentOrder ?: return
        if (order.status != "處理中") {
            Toast.makeText(this, "此訂單目前無法修改", Toast.LENGTH_SHORT).show()
            return
        }

        orderRepo.updateOrderItemQuantity(
            orderId = order.id,
            itemId = item.id,
            newQuantity = newQty,
            onSuccess = {
                Toast.makeText(this, "已更新數量", Toast.LENGTH_SHORT).show()
                loadOrder()
            },
            onFailure = { e ->
                Toast.makeText(this, "更新失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
}