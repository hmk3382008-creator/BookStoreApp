package com.example.bookstoreapp.View

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.bookstoreapp.Member.AdminHomeActivity
import com.example.bookstoreapp.Member.LoginActivity
import com.example.bookstoreapp.R
import com.example.bookstoreapp.Seller.VendorDashboardActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
////            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
////            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
////            insets
//        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        //  這裡全部都用 String
        val role = intent.getStringExtra("role") ?: ""
        val userId = intent.getStringExtra("userId") ?: ""
        val userName = intent.getStringExtra("userName") ?: "會員"

        if (userId.isBlank()) {
            Toast.makeText(this, "登入資料有誤，請重新登入", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 依照角色分流
        when (role) {
            "系統管理者" -> {
                val intent = Intent(this, AdminHomeActivity::class.java)
                startActivity(intent)
                finish()
                return
            }

            "一般商家" -> {
                val intent = Intent(this, VendorDashboardActivity::class.java)
                intent.putExtra("sellerId", userId)   //  把 Firebase user.id 當成商家 id 傳過去
                startActivity(intent)
                finish()
                return
            }
        }

        // 一般會員 → 留在 MainActivity，顯示 UserHomeFragment
        val homeFragment = UserHomeFragment.newInstance(userId, userName)
        val cartFragment = CartFragment.newInstance(userId, userName)
        val memberFragment = MemberFragment.newInstance(userId, userName)
        val favoriteFragment = FavoriteFragment.newInstance(userId)
        loadFragment(homeFragment)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(homeFragment)
                R.id.nav_cart -> loadFragment(cartFragment)
                R.id.nav_member -> loadFragment(memberFragment)
                R.id.nav_favorite -> loadFragment(favoriteFragment)
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, fragment)
            .commit()
    }
}
