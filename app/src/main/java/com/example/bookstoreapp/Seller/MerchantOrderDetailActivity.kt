package com.example.bookstoreapp.Seller

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.Firbase.FirebaseOrderRepository
import com.example.bookstoreapp.R
import com.example.bookstoreapp.User.OrderItemAdapter

class MerchantOrderDetailActivity : AppCompatActivity() {

    private lateinit var tvOrderId: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvOrderTotal: TextView
    private lateinit var tvReceiverName: TextView
    private lateinit var tvReceiverPhone: TextView
    private lateinit var tvShippingAddress: TextView
    private lateinit var tvPaymentMethod: TextView

    private lateinit var rvItems: RecyclerView
    private lateinit var spStatus: Spinner
    private lateinit var btnUpdateStatus: Button

    private val orderRepo = FirebaseOrderRepository()

    private var orderId = ""
    private var sellerId = ""
    private var currentStatus: String = "處理中"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merchant_order_detail)

        orderId = intent.getStringExtra("orderId") ?: ""
        sellerId = intent.getStringExtra("sellerId") ?: ""

        if (orderId.isBlank() || sellerId.isBlank()) {
            Toast.makeText(this, "訂單或商家資料錯誤", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "訂單明細"
        toolbar.setNavigationOnClickListener { finish() }

        tvOrderId = findViewById(R.id.tvOrderId)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        tvOrderStatus = findViewById(R.id.tvOrderStatus)
        tvOrderTotal = findViewById(R.id.tvOrderTotal)
        tvReceiverName = findViewById(R.id.tvReceiverName)
        tvReceiverPhone = findViewById(R.id.tvReceiverPhone)
        tvShippingAddress = findViewById(R.id.tvShippingAddress)
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod)

        rvItems = findViewById(R.id.rvItems)
        spStatus = findViewById(R.id.spStatus)
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus)

        rvItems.layoutManager = LinearLayoutManager(this)

        setupStatusSpinner()
        loadOrder()

        btnUpdateStatus.setOnClickListener {
            val newStatus = spStatus.selectedItem?.toString() ?: ""
            if (newStatus.isEmpty()) {
                Toast.makeText(this, "請選擇狀態", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newStatus == currentStatus) {
                Toast.makeText(this, "狀態未變更", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            updateStatus(newStatus)
        }
    }

    private fun setupStatusSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.order_status_options,          // 在 strings.xml 裡要有這個陣列
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spStatus.adapter = adapter
        }
    }

    private fun loadOrder() {
        orderRepo.getOrderDetailForSeller(
            orderId = orderId,
            sellerId = sellerId,
            onSuccess = { order, items ->
                currentStatus = if (order.sellerStatus.isNotBlank()) {
                    order.sellerStatus
                } else {
                    "處理中"
                }

                tvOrderId.text = "訂單：${order.id}"
                tvOrderDate.text = "日期：${order.createdAtText}"
                tvOrderStatus.text = "狀態：$currentStatus"
                tvOrderTotal.text = "總金額：$${order.totalAmount}"

                tvReceiverName.text = "收件人：${order.receiverName}"
                tvReceiverPhone.text = "電話：${order.receiverPhone}"
                tvShippingAddress.text = "地址：${order.shippingAddress}"
                tvPaymentMethod.text = "付款方式：${order.paymentMethod}"

                val statusOptions = resources.getStringArray(R.array.order_status_options)
                val index = statusOptions.indexOf(currentStatus).let {
                    if (it >= 0) it else 0
                }
                spStatus.setSelection(index, false)

                rvItems.adapter = OrderItemAdapter(items.toMutableList())
            },
            onFailure = {
                Toast.makeText(this, "載入錯誤", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateStatus(newStatus: String) {
        //  只更新這個商家在這張訂單的 sellerStatus
        orderRepo.updateSellerStatus(
            orderId = orderId,
            sellerId = sellerId,
            newStatus = newStatus,
            onSuccess = {
                Toast.makeText(this, "狀態更新為：$newStatus", Toast.LENGTH_SHORT).show()
                currentStatus = newStatus
                tvOrderStatus.text = "狀態：$newStatus"
            },
            onFailure = {
                Toast.makeText(this, "更新失敗", Toast.LENGTH_SHORT).show()
            }
        )
    }
}