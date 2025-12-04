package com.example.bookstoreapp.Admin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.Firbase.FirebaseOrderRepository
import com.example.bookstoreapp.R
import com.example.bookstoreapp.User.Order
import com.example.bookstoreapp.User.OrderItem

class AdminOrderDetailActivity : AppCompatActivity(), AdminOrderItemsAdapter.OnOrderItemActionListener {

    private val orderRepo = FirebaseOrderRepository()

    private lateinit var tvOrderInfo: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnChangeStatus: Button
    private lateinit var recyclerItems: RecyclerView
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: AdminOrderItemsAdapter

    private var currentOrder: Order? = null
    private var currentItems: MutableList<OrderItem> = mutableListOf()
    private lateinit var orderId: String

    private val orderStatusList = arrayOf("處理中", "已出貨", "已完成", "已取消")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_order_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAdminOrderDetail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        orderId = intent.getStringExtra("orderId") ?: ""

        tvOrderInfo = findViewById(R.id.tvOrderInfo)
        tvStatus = findViewById(R.id.tvStatus)
        tvTotalAmount = findViewById(R.id.tvTotalAmountDetail)
        btnChangeStatus = findViewById(R.id.btnChangeStatus)
        recyclerItems = findViewById(R.id.recyclerOrderItems)
        progressBar = findViewById(R.id.progressBarDetail)

        adapter = AdminOrderItemsAdapter(this)
        recyclerItems.layoutManager = LinearLayoutManager(this)
        recyclerItems.adapter = adapter

        btnChangeStatus.setOnClickListener {
            currentOrder?.let { showStatusDialog(it) }
        }

        loadOrderDetail()
    }

    private fun loadOrderDetail() {
        progressBar.visibility = View.VISIBLE

        orderRepo.getOrderDetail(
            orderId = orderId,
            onSuccess = { order, items ->
                progressBar.visibility = View.GONE
                currentOrder = order
                currentItems = items.toMutableList()
                bindOrder(order)
                adapter.submitList(currentItems)
            },
            onFailure = { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "載入失敗：${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun bindOrder(order: Order) {
        tvOrderInfo.text = buildString {
            append("訂單ID：${order.id}\n")
            append("會員：${order.userName} (${order.userId})\n")
            append("建立時間：${order.createdAtText}")
        }
        tvStatus.text = "狀態：${order.status}"
        tvTotalAmount.text = "總金額：NT$ ${order.totalAmount}"
    }

    private fun showStatusDialog(order: Order) {
        val index = orderStatusList.indexOf(order.status).let { if (it == -1) 0 else it }

        AlertDialog.Builder(this)
            .setTitle("修改訂單狀態")
            .setSingleChoiceItems(orderStatusList, index, null)
            .setPositiveButton("確定") { dialog, _ ->
                val listView = (dialog as AlertDialog).listView
                val selected = listView.checkedItemPosition
                val newStatus = orderStatusList[selected]

                progressBar.visibility = View.VISIBLE
                orderRepo.updateOrderStatus(
                    orderId = order.id,
                    newStatus = newStatus,
                    onSuccess = {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show()
                        currentOrder?.status = newStatus
                        bindOrder(currentOrder!!)
                    },
                    onFailure = { e ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "更新失敗：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 刪除單一品項
    override fun onDeleteItem(item: OrderItem) {
        AlertDialog.Builder(this)
            .setTitle("刪除品項")
            .setMessage("確定要刪除 ${item.productName} 嗎？")
            .setPositiveButton("刪除") { _, _ ->
                progressBar.visibility = View.VISIBLE
                orderRepo.deleteOrderItem(
                    orderId = orderId,
                    itemId = item.id,
                    onSuccess = {
                        // 更新本地列表 & 總金額
                        currentItems.remove(item)
                        adapter.submitList(currentItems.toList())

                        val newTotal = currentItems.sumOf { it.price * it.quantity }
                        currentOrder?.totalAmount = newTotal
                        tvTotalAmount.text = "總金額：NT$ $newTotal"

                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "品項已刪除", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "刪除失敗：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("取消", null)
            .show()
    }
}