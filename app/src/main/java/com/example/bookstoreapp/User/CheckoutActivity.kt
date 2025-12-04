package com.example.bookstoreapp.User

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.bookstoreapp.Firbase.FirebaseOrderRepository
import com.example.bookstoreapp.R
import com.example.bookstoreapp.View.CartManager

class CheckoutActivity : AppCompatActivity() {

    private lateinit var tvTotalAmount: TextView
    private lateinit var etReceiverName: EditText
    private lateinit var etReceiverPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var spPaymentMethod: Spinner
    private lateinit var btnConfirmOrder: Button

    private val orderRepo = FirebaseOrderRepository()

    private var userId: String = ""
    private var userName: String = "會員"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // Toolbar + 返回箭頭
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "結帳付款"
        }
        toolbar.setNavigationOnClickListener { finish() }

        // 從 CartFragment 帶進來的真正會員資料
        userId = intent.getStringExtra("userId") ?: ""
        userName = intent.getStringExtra("userName") ?: "會員"

        if (userId.isBlank()) {
            Toast.makeText(this, "會員資料錯誤，請重新登入", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        etReceiverName = findViewById(R.id.etReceiverName)
        etReceiverPhone = findViewById(R.id.etReceiverPhone)
        etAddress = findViewById(R.id.etAddress)
        spPaymentMethod = findViewById(R.id.spPaymentMethod)
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder)

        // 顯示總金額
        val total = CartManager.totalPrice()
        tvTotalAmount.text = "總金額：$${total}"

        // 設定付款方式下拉選單
        ArrayAdapter.createFromResource(
            this,
            R.array.payment_methods,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spPaymentMethod.adapter = adapter
        }

        btnConfirmOrder.setOnClickListener {
            onClickConfirm()
        }
    }

    private fun onClickConfirm() {
        val receiverName = etReceiverName.text.toString().trim()
        val receiverPhone = etReceiverPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val paymentMethod = spPaymentMethod.selectedItem?.toString() ?: ""

        // 基礎驗證
        if (receiverName.isEmpty()) {
            etReceiverName.error = "請輸入收件人姓名"
            return
        }
        if (receiverPhone.isEmpty()) {
            etReceiverPhone.error = "請輸入電話"
            return
        }
        if (address.isEmpty()) {
            etAddress.error = "請輸入地址"
            return
        }

        val items = CartManager.getAll()
        if (items.isEmpty()) {
            Toast.makeText(this, "購物車是空的", Toast.LENGTH_SHORT).show()
            return
        }

        btnConfirmOrder.isEnabled = false
        btnConfirmOrder.text = "送出中..."

        orderRepo.createOrder(
            userId = userId,
            userName = userName,
            items = items,
            paymentMethod = paymentMethod,
            receiverName = receiverName,
            receiverPhone = receiverPhone,
            shippingAddress = address,
            onSuccess = {
                Toast.makeText(this, "訂單建立成功", Toast.LENGTH_SHORT).show()

                CartManager.clear()

                //  付款完成後，直接跳到「我的訂單」，那邊會在 onResume 再 loadOrders()
                val intent = Intent(this, MyOrdersActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userName", userName)
                startActivity(intent)
                finish()
            },
            onFailure = { e ->
                Toast.makeText(this, "建立訂單失敗：${e.message}", Toast.LENGTH_SHORT).show()
                btnConfirmOrder.isEnabled = true
                btnConfirmOrder.text = "確認結帳"
            }
        )
    }

    // Toolbar 返回鍵
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