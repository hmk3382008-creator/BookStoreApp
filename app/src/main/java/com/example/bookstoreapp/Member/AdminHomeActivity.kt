package com.example.bookstoreapp.Member

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bookstoreapp.Admin.AdminOrdersActivity
import com.example.bookstoreapp.Admin.AdminProductActivity
import com.example.bookstoreapp.R
import com.google.firebase.firestore.FirebaseFirestore

class AdminHomeActivity : AppCompatActivity() {

    private lateinit var btnMember: Button
    private lateinit var btnProduct: Button
    private lateinit var btnOrder: Button
    private lateinit var btnLogout: Button
    private lateinit var tvMemberCount: TextView
    private lateinit var tvProductCount: TextView
    private lateinit var tvOrderCount: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)

        btnMember = findViewById(R.id.btnMemberManagement)
        btnProduct = findViewById(R.id.btnProductManagement)
        btnOrder = findViewById(R.id.btnOrderManagement)
        btnLogout = findViewById<Button>(R.id.btnLogout)


        tvMemberCount = findViewById(R.id.tvMemberCount)
        tvProductCount = findViewById(R.id.tvProductCount)
        tvOrderCount = findViewById(R.id.tvOrderCount)

        btnMember.setOnClickListener {
            startActivity(Intent(this, AdminMemberActivity::class.java))
        }

        btnProduct.setOnClickListener {
            startActivity(Intent(this, AdminProductActivity::class.java))

        }

        btnOrder.setOnClickListener {
            startActivity(Intent(this, AdminOrdersActivity::class.java))
        }

        btnLogout.setOnClickListener {
            // 彈出確認對話框
            AlertDialog.Builder(this)
                .setTitle("確認登出")
                .setMessage("確定要登出嗎？")
                .setPositiveButton("登出") { dialog, _ ->
                    // 登出：跳回登入頁，清空歷史頁面
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    dialog.dismiss()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    // 取消，對話框消失
                    dialog.dismiss()
                }
                .show()
        }
        //  進入畫面時載入統計資料
        loadDashboardStats()
    }

    //  從 Firestore 抓統計數字
    private fun loadDashboardStats() {
        loadMemberCount()
        loadProductCount()
        loadOrderCount()
    }

    // 會員數
    private fun loadMemberCount() {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.size()
                tvMemberCount.text = count.toString()
            }
            .addOnFailureListener {
                tvMemberCount.text = "-"
            }
    }

    // 商品數
    private fun loadProductCount() {
        db.collection("products")
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.size()
                tvProductCount.text = count.toString()
            }
            .addOnFailureListener {
                tvProductCount.text = "-"
            }
    }

    // 訂單數
    private fun loadOrderCount() {
        db.collection("orders")
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.size()
                tvOrderCount.text = count.toString()
            }
            .addOnFailureListener {
                tvOrderCount.text = "-"
            }
    }
    override fun onResume() {
        super.onResume()
        //  每次畫面回到前景，都重新抓統計數字
        loadDashboardStats()
    }
}