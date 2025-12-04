package com.example.bookstoreapp.Seller

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bookstoreapp.R

class ManageProductsActivity : AppCompatActivity() {

    private var sellerId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_products)

        // 從 Intent 取得傳入的 sellerId（Firebase 的 user.id）
        sellerId = intent.getStringExtra("sellerId") ?: ""

        // 如果沒有拿到 sellerId，就直接關閉，避免之後全部爆掉
        if (sellerId.isBlank()) {
            Toast.makeText(this, "商家資料有誤，請重新登入", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 第一次進來 Activity 才建立 Fragment
        if (savedInstanceState == null) {
            val frag = ManageProductsFragment.newInstance(sellerId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, frag)
                .commit()
        }
    }
}