package com.example.bookstoreapp.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.R
import com.example.bookstoreapp.User.Order

class AdminOrdersAdapter(
    private val listener: OnOrderActionListener
) : RecyclerView.Adapter<AdminOrdersAdapter.VH>() {

    interface OnOrderActionListener {
        fun onViewDetail(order: Order)
        fun onChangeStatus(order: Order)
        fun onDeleteOrder(order: Order)
    }

    private val items = mutableListOf<Order>()

    fun submitList(list: List<Order>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)

        init {
            itemView.setOnClickListener {
                val order = items[bindingAdapterPosition]
                listener.onViewDetail(order)
            }

            btnMore.setOnClickListener {
                val order = items[bindingAdapterPosition]
                showPopupMenu(it, order)
            }
        }

        private fun showPopupMenu(anchor: View, order: Order) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menu.apply {
                add(0, 1, 0, "修改狀態")
                add(0, 2, 1, "刪除訂單")
                add(0, 3, 2, "查看明細")
            }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> listener.onChangeStatus(order)
                    2 -> listener.onDeleteOrder(order)
                    3 -> listener.onViewDetail(order)
                }
                true
            }
            popup.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_order, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = items[position]
        holder.tvOrderId.text = "訂單ID：${order.id}"
        holder.tvOrderStatus.text = order.status
        holder.tvUserName.text = "會員：${order.userName} (${order.userId})"
        holder.tvCreatedAt.text = order.createdAtText
        holder.tvTotalAmount.text = "NT$ ${order.totalAmount}"

        // 簡單依狀態變更文字顏色（需要 API >= 23 用 getColor）
        val ctx = holder.itemView.context
        val colorRes = when (order.status) {
            "處理中" -> android.R.color.holo_orange_dark
            "已出貨" -> android.R.color.holo_blue_dark
            "已完成" -> android.R.color.holo_green_dark
            "已取消" -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }
        holder.tvOrderStatus.setTextColor(ContextCompat.getColor(ctx, colorRes))
    }


    override fun getItemCount(): Int = items.size
}