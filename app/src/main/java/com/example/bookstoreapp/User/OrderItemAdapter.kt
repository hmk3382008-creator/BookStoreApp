package com.example.bookstoreapp.User

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.R

class OrderItemAdapter(
    private var items: MutableList<OrderItem>,
) : RecyclerView.Adapter<OrderItemAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPriceQty: TextView = itemView.findViewById(R.id.tvPriceQty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_detail, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = "書名:${item.productName}"
        holder.tvPriceQty.text = "單價：$${item.price} x ${item.quantity}"
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<OrderItem>) {
        items = newList.toMutableList()
        notifyDataSetChanged()
    }
}