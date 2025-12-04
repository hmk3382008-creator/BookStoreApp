package com.example.bookstoreapp.Seller

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.example.bookstoreapp.Member.LoginActivity
import com.example.bookstoreapp.Member.MyDBHelper
import com.example.bookstoreapp.Member.User
import com.example.bookstoreapp.R

class VendorDashboardActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar          // 上方工具列
    private lateinit var sellerId: String          // 商家 ID（Firebase user.id，字串）

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor_dashboard)

        // 設定 Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //  從 Intent 拿商家 ID（這個是登入時從 Firebase 拿到的 user.id）
        //
        sellerId = intent.getStringExtra("sellerId") ?: ""

        if (sellerId.isBlank()) {
            // 如果拿不到 sellerId，代表前面傳遞資料出問題，先提示一下然後結束這個畫面
            Toast.makeText(this, "找不到商家資料（sellerId 不存在）", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 【上架商品】功能（用 Firebase 版 AddProductDialog）
        findViewById<CardView>(R.id.cardAddProduct).setOnClickListener {
            // 直接把 sellerId 傳給 Dialog，讓它知道這個商品是誰上架的
            val dialog = AddProductDialog(sellerId) { newProduct ->
                // 上架成功後的回呼（你現在先簡單 Toast 提示即可）
                Toast.makeText(this, "${newProduct.name} 已上架", Toast.LENGTH_SHORT).show()
            }
            dialog.show(supportFragmentManager, "AddProductDialog")
        }

        //  【管理商品】功能（打開 ManageProductsActivity，裡面用 Firebase 抓 sellerId 自己的商品）
        findViewById<CardView>(R.id.cardManageProduct).setOnClickListener {
            val intent = Intent(this, ManageProductsActivity::class.java)
            intent.putExtra("sellerId", sellerId)      // 把商家 Firebase id 一起帶過去
            startActivity(intent)
        }


        findViewById<CardView>(R.id.cardViewOrders).setOnClickListener {
            val intent = Intent(this, MerchantOrdersActivity::class.java)
            intent.putExtra("sellerId", sellerId)      // 一樣帶商家 id，方便查自己的訂單
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // 載入右上角 menu（包含登出）
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // 點擊右上角「登出」
                AlertDialog.Builder(this)
                    .setTitle("登出確認")
                    .setMessage("您確定要登出嗎？")
                    .setPositiveButton("確認") { dialog, _ ->
                        dialog.dismiss()
                        // 回登入畫面，清掉所有舊頁面
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}