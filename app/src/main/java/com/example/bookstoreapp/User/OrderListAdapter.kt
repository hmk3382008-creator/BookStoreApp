package com.example.bookstoreapp.User

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.R


class OrderListAdapter(
    private var items: MutableList<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderListAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        val tvOrderTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_simple, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = items[position]

        holder.tvOrderId.text = "訂單編號：${order.id}"
        holder.tvOrderDate.text = "日期：${order.createdAtText}"

        // 如果有 sellerStatus（商家列表），優先顯示；沒有就用整張訂單的 status（會員列表）
        val statusToShow = if (order.sellerStatus.isNotBlank()) {
            order.sellerStatus
        } else {
            order.status
        }
        holder.tvOrderStatus.text = "狀態：$statusToShow"

        holder.tvOrderTotal.text = "總金額：$${order.totalAmount}"

        holder.itemView.setOnClickListener {
            onOrderClick(order)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Order>) {
        items = newList.toMutableList()
        notifyDataSetChanged()
    }
}
