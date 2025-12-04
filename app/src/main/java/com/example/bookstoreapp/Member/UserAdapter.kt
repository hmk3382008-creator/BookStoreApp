package com.example.bookstoreapp.Member

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.R

// UserAdapter 負責顯示會員列表，不負責存取資料庫 / Firebase
class UserAdapter(
    private var userList: MutableList<User>,          // 目前顯示在畫面上的會員列表
    private val onEditClick: (User) -> Unit,          // 點「編輯」按鈕要做什麼，由外面決定
    private val onDeleteClick: (User) -> Unit         // 點「刪除」按鈕要做什麼，由外面決定
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // ViewHolder：保存 item_member.xml 裡面的元件參考
    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvMemberName)    // 顯示會員姓名
        val tvEmail: TextView = view.findViewById(R.id.tvMemberEmail)  // 顯示會員 Email
        val tvRole: TextView = view.findViewById(R.id.tvMemberRole)    // 顯示會員角色
        val btnEdit: Button = view.findViewById(R.id.btnEdit)          // 編輯按鈕
        val btnDelete: Button = view.findViewById(R.id.btnDelete)      // 刪除按鈕
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // 把 item_member.xml 載入成 View
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        // 取得這一列對應的 User
        val user = userList[position]

        // 把資料顯示在畫面上
        holder.tvName.text = "姓名：${user.name}"
        holder.tvEmail.text = "Email：${user.email}"
        holder.tvRole.text = "角色：${user.role}"

        // 點擊「編輯」時，不在 Adapter 裡面直接開 Dialog，
        // 交給外面的 Activity 處理，這樣 Adapter 比較乾淨、好維護
        holder.btnEdit.setOnClickListener {
            onEditClick(user)
        }

        // 點擊「刪除」，同樣交給外面 Activity 決定要不要跳出確認視窗、怎麼刪
        holder.btnDelete.setOnClickListener {
            onDeleteClick(user)
        }
    }

    override fun getItemCount(): Int = userList.size

    // 提供外面更新列表使用（例如搜尋、重新載入 Firebase 後）
    fun updateList(newList: MutableList<User>) {
        userList.clear()            // 先清空舊資料
        userList.addAll(newList)    // 放入新的資料
        notifyDataSetChanged()      // 通知 RecyclerView「整個列表都改變了」
    }
}