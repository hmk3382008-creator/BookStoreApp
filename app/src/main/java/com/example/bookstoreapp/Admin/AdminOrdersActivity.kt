package com.example.bookstoreapp.Admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bookstoreapp.Firbase.FirebaseOrderRepository
import com.example.bookstoreapp.R
import com.example.bookstoreapp.User.Order
import com.google.android.material.tabs.TabLayout
import java.util.Locale

class AdminOrdersActivity : AppCompatActivity(), AdminOrdersAdapter.OnOrderActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminOrdersAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var searchView: SearchView
    private lateinit var tabLayoutStatus: TabLayout

    private val orderRepo = FirebaseOrderRepository()

    // 所有訂單原始資料
    private val fullOrderList = mutableListOf<Order>()

    // 篩選狀態
    private var currentKeyword: String = ""
    private var currentStatusFilter: String? = null   // null = 全部

    // 統一使用這幾個狀態文字（要跟你存到 Firestore 的 status 對得起來）
    private val orderStatusList = arrayOf("處理中", "已出貨", "已完成", "已取消")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_orders)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAdminOrders)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerAdminOrders)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        searchView = findViewById(R.id.searchViewOrders)
        tabLayoutStatus = findViewById(R.id.tabLayoutStatus)

        // RecyclerView + Adapter
        adapter = AdminOrdersAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // SwipeRefresh
        swipeRefresh.setOnRefreshListener {
            loadOrders()
        }

        // 設定 Tab（全部 / 各狀態）
        setupStatusTabs()

        // 設定 SearchView 事件
        setupSearchView()

        // 第一次載入
        loadOrders()
    }

    private fun setupStatusTabs() {
        // 加入「全部」Tab
        tabLayoutStatus.addTab(tabLayoutStatus.newTab().setText("全部"))

        // 加入各狀態 Tab
        orderStatusList.forEach { status ->
            tabLayoutStatus.addTab(tabLayoutStatus.newTab().setText(status))
        }

        tabLayoutStatus.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val text = tab.text?.toString() ?: "全部"
                currentStatusFilter = if (text == "全部") null else text
                applyFilter()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 預設選「全部」
        tabLayoutStatus.getTabAt(0)?.select()
    }

    private fun setupSearchView() {
        // 讓搜尋一開始就展開
        searchView.isIconified = false

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentKeyword = query?.trim() ?: ""
                applyFilter()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentKeyword = newText?.trim() ?: ""
                applyFilter()
                return true
            }
        })
    }

    private fun loadOrders() {
        progressBar.visibility = View.VISIBLE

        orderRepo.getAllOrders(
            onSuccess = { list ->
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                fullOrderList.clear()
                fullOrderList.addAll(list)

                applyFilter()
            },
            onFailure = { e ->
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                Toast.makeText(this, "載入訂單失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * 根據 currentKeyword + currentStatusFilter 在記憶體裡做篩選
     */
    private fun applyFilter() {
        var filtered = fullOrderList.asSequence()

        // 狀態篩選
        currentStatusFilter?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        // 關鍵字搜尋（訂單ID / userName / userId）
        if (currentKeyword.isNotEmpty()) {
            val keyword = currentKeyword.lowercase(Locale.getDefault())
            filtered = filtered.filter { order ->
                order.id.lowercase(Locale.getDefault()).contains(keyword) ||
                        order.userName.lowercase(Locale.getDefault()).contains(keyword) ||
                        order.userId.lowercase(Locale.getDefault()).contains(keyword)
            }
        }

        adapter.submitList(filtered.toList())
    }

    // 點 item or popup menu「查看明細」
    override fun onViewDetail(order: Order) {
        val intent = Intent(this, AdminOrderDetailActivity::class.java)
        intent.putExtra("orderId", order.id)
        startActivity(intent)
    }

    // popup menu「修改狀態」
    override fun onChangeStatus(order: Order) {
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
                        Toast.makeText(this, "狀態已更新", Toast.LENGTH_SHORT).show()
                        // 更新本地資料
                        val target = fullOrderList.find { it.id == order.id }
                        target?.status = newStatus
                        applyFilter()
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

    // popup menu「刪除訂單」
    override fun onDeleteOrder(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("刪除訂單")
            .setMessage("確定要刪除訂單 ${order.id} 嗎？此動作無法復原。")
            .setPositiveButton("刪除") { _, _ ->
                progressBar.visibility = View.VISIBLE

                orderRepo.deleteOrderWithItems(
                    orderId = order.id,
                    onSuccess = {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "訂單已刪除", Toast.LENGTH_SHORT).show()
                        fullOrderList.removeAll { it.id == order.id }
                        applyFilter()
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
