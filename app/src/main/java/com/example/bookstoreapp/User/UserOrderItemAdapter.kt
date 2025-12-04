package com.example.bookstoreapp.User

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstoreapp.R

class UserOrderItemAdapter(
    private val items: MutableList<OrderItem>,
    private val orderStatus: String,                     // 整張訂單狀態（處理中/已取消...）
    private val onDelete: (OrderItem) -> Unit,
    private val onUpdateQty: (OrderItem, Int) -> Unit
) : RecyclerView.Adapter<UserOrderItemAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPriceQty: TextView = itemView.findViewById(R.id.tvPriceQty)
        val tvItemStatus: TextView = itemView.findViewById(R.id.tvItemStatus)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteItem)
        val btnEditQty: Button = itemView.findViewById(R.id.btnEditQty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_detail_user, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvName.text = "書名：${item.productName}"
        holder.tvPriceQty.text = "單價：$${item.price} x ${item.quantity}"

        // 🔥 顯示這一筆商品自己的狀態（就是那個賣家的 sellerStatus）
        val statusText = if (item.sellerStatus.isNullOrBlank()) {
            "處理中"
        } else {
            item.sellerStatus
        }
        holder.tvItemStatus.text = "商品狀態：$statusText"

        // 只有整張訂單還在「處理中」時，才允許修改/刪除
        if (orderStatus == "處理中") {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnEditQty.visibility = View.VISIBLE
        } else {
            holder.btnDelete.visibility = View.GONE
            holder.btnEditQty.visibility = View.GONE
        }

        holder.btnDelete.setOnClickListener {
            onDelete(item)
        }

        holder.btnEditQty.setOnClickListener {
            val ctx = holder.itemView.context
            val editText = EditText(ctx)
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.setText(item.quantity.toString())

            AlertDialog.Builder(ctx)
                .setTitle("修改數量")
                .setView(editText)
                .setPositiveButton("確定") { _, _ ->
                    val text = editText.text.toString()
                    val newQty = text.toIntOrNull() ?: 0
                    if (newQty <= 0) {
                        Toast.makeText(ctx, "數量必須大於 0", Toast.LENGTH_SHORT).show()
                    } else {
                        onUpdateQty(item, newQty)
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    override fun getItemCount(): Int = items.size
}
