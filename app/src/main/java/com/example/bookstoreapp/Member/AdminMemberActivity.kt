package com.example.bookstoreapp.Member

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.R
import com.example.bookstoreapp.Firbase.FirebaseUserRepository

class AdminMemberActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnAdd: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserAdapter

    // 使用 FirebaseUserRepository，專門負責存取 "users" collection
    private val userRepo = FirebaseUserRepository()

    // 保存完整會員列表（未被搜尋過濾的原始資料）
    private var fullUserList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_member)

        // 連結畫面元件
        etSearch = findViewById(R.id.etSearchMember)
        btnSearch = findViewById(R.id.btnSearch)
        btnAdd = findViewById(R.id.btnAddMember)
        recyclerView = findViewById(R.id.rvMembers)
        val toolbar: Toolbar = findViewById(R.id.toolbar)

        // 設定 Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)   // 顯示返回箭頭
            title = "會員管理"                // 標題
        }
        toolbar.setNavigationOnClickListener { finish() } // 點擊返回箭頭就關閉 Activity

        // 建立 Adapter，傳入「按下編輯 & 刪除」要做的事情
        adapter = UserAdapter(
            userList = mutableListOf(),                    // 一開始先給空列表
            onEditClick = { user -> showEditDialog(user) },       // 按編輯 → 開啟編輯 Dialog
            onDeleteClick = { user -> confirmDeleteUser(user) }   // 按刪除 → 跳出確認視窗
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 一進來就先從 Firebase 載入全部會員
        loadUsers()

        // 搜尋按鈕：根據 fullUserList 做過濾
        btnSearch.setOnClickListener {
            val keyword = etSearch.text.toString().trim()
            val filteredList = if (keyword.isEmpty()) {
                // 沒輸入關鍵字 → 顯示全部
                fullUserList.toMutableList()
            } else {
                // 有關鍵字 → 用姓名或 Email 模糊比對
                fullUserList.filter {
                    it.name.contains(keyword, true) || it.email.contains(keyword, true)
                }.toMutableList()
            }
            adapter.updateList(filteredList)   // 更新畫面列表
        }

        // 新增會員：打開 AddUserDialog（Firebase 版）
        btnAdd.setOnClickListener {
            val dialog = AddUserDialog { newUser ->
                // 新增成功後，為了保險，重新從 Firebase 抓一次全部會員
                loadUsers()
            }
            dialog.show(supportFragmentManager, "AddUserDialog")
        }
    }

    // 從 Firebase 載入全部會員
    private fun loadUsers() {
        userRepo.getAllUsers(
            onSuccess = { users ->
                fullUserList = users.toMutableList()   // 更新本地完整列表
                adapter.updateList(fullUserList)       // 更新 RecyclerView 顯示
            },
            onFailure = { e ->
                Toast.makeText(this, "載入會員失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 顯示編輯 Dialog
    private fun showEditDialog(user: User) {
        val dialog = EditUserDialog(user) { updatedUser ->
            Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show()
            //  方式一（你現在用的）：每次修改完都整批重抓 Firebase
            loadUsers()

        }
        dialog.show(supportFragmentManager, "EditUserDialog")
    }

    // 刪除會員前先跳出確認視窗
    private fun confirmDeleteUser(user: User) {
        AlertDialog.Builder(this)
            .setTitle("確認刪除")
            .setMessage("確定要刪除 ${user.name} 嗎？")
            .setPositiveButton("刪除") { dialog, _ ->
                // 按「刪除」後，呼叫 Firebase 刪掉該會員
                userRepo.deleteUser(
                    userId = user.id,
                    onSuccess = {
                        Toast.makeText(this, "${user.name} 已刪除", Toast.LENGTH_SHORT).show()
                        loadUsers()   // 刪除成功後重新載入列表
                    },
                    onFailure = {
                        Toast.makeText(this, "刪除失敗，請稍後再試", Toast.LENGTH_SHORT).show()
                    }
                )
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}