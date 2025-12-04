package com.example.bookstoreapp.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.R
import com.example.bookstoreapp.User.OrderItem

class AdminOrderItemsAdapter(
    private val listener: OnOrderItemActionListener
) : RecyclerView.Adapter<AdminOrderItemsAdapter.VH>() {

    interface OnOrderItemActionListener {
        fun onDeleteItem(item: OrderItem)
    }

    private val items = mutableListOf<OrderItem>()

    fun submitList(list: List<OrderItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvItemInfo: TextView = itemView.findViewById(R.id.tvItemInfo)
        val btnDeleteItem: Button = itemView.findViewById(R.id.btnDeleteItem)

        init {
            btnDeleteItem.setOnClickListener {
                val item = items[bindingAdapterPosition]
                listener.onDeleteItem(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_order_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvProductName.text = item.productName
        val subtotal = item.price * item.quantity
        holder.tvItemInfo.text = "單價：${item.price} x 數量：${item.quantity} = 小計：$subtotal"
    }

    override fun getItemCount(): Int = items.size
}