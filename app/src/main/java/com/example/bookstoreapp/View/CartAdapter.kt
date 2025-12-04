package com.example.bookstoreapp.View

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstoreapp.R


class CartAdapter(
    private var items: MutableList<CartManager.CartItem>,
    private val onPlus: (CartManager.CartItem) -> Unit,
    private val onMinus: (CartManager.CartItem) -> Unit,
    private val onDelete: (CartManager.CartItem) -> Unit,
    private val onEditQty: (CartManager.CartItem, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivThumb: ImageView = v.findViewById(R.id.ivThumb)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)
        val etQty: EditText = v.findViewById(R.id.etQty)
        val btnPlus: Button = v.findViewById(R.id.btnPlus)
        val btnMinus: Button = v.findViewById(R.id.btnMinus)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvName.text = item.product.name
        holder.tvPrice.text = "價格：$${item.product.price}"
        holder.etQty.setText(item.quantity.toString())
        holder.etQty.setSelection(holder.etQty.text.length)

        // 圖片 (如果有用 Firebase imageUrl)
        Glide.with(holder.itemView)
            .load(item.product.imageUrl)
            .placeholder(android.R.drawable.ic_menu_report_image)
            .error(android.R.drawable.ic_menu_report_image)
            .into(holder.ivThumb)

        // + 按鈕
        holder.btnPlus.setOnClickListener {
            onPlus(item)
        }

        // - 按鈕
        holder.btnMinus.setOnClickListener {
            onMinus(item)
        }

        // 刪除
        holder.btnDelete.setOnClickListener {
            onDelete(item)
        }

        //  直接輸入數量：當輸入框「失去焦點」時才更新
        holder.etQty.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newQty = holder.etQty.text.toString().toIntOrNull() ?: 1
                onEditQty(item, newQty)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<CartManager.CartItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}