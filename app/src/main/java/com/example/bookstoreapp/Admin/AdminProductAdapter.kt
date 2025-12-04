package com.example.bookstoreapp.Admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstoreapp.R
import com.example.bookstoreapp.Seller.Product

class AdminProductAdapter(
    private var products: MutableList<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit,
    private val onStatusChange: (Product, Boolean) -> Unit
) : RecyclerView.Adapter<AdminProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        val switchStatus: Switch = itemView.findViewById(R.id.switchStatus)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        // 商品基本資訊
        holder.tvName.text = product.name
        holder.tvPrice.text = "$${product.price}"
        holder.tvStock.text = "庫存：${product.stock}"

        // ★★★ UI 綁定 Firestore 的 isActive 狀態 ★★★
        if (product.isActive) {
            holder.tvStatus.text = "狀態：上架中"
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32")) // 綠色
        } else {
            holder.tvStatus.text = "狀態：已下架"
            holder.tvStatus.setTextColor(Color.parseColor("#B71C1C")) // 紅色
        }

        // ★★★ 重要：避免 Switch 回收後狀態亂跳，先移除 listener ★★★
        holder.switchStatus.setOnCheckedChangeListener(null)

        // Switch 顯示狀態
        holder.switchStatus.isChecked = product.isActive
        holder.switchStatus.text = if (product.isActive) "上架" else "下架"

        // ★★★ 監聽切換上下架 ★★★
        holder.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            onStatusChange(product, isChecked)

            // 即時更新 UI（不用等待 Firebase 回來）
            holder.switchStatus.text = if (isChecked) "上架" else "下架"
            holder.tvStatus.text = if (isChecked) "狀態：上架中" else "狀態：已下架"
            holder.tvStatus.setTextColor(
                if (isChecked) Color.parseColor("#2E7D32")
                else Color.parseColor("#B71C1C")
            )
        }

        // 商品圖片處理
        if (!product.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(product.imageUrl)
                .placeholder(R.drawable.catcar)
                .into(holder.imgProduct)
        } else {
            holder.imgProduct.setImageResource(R.drawable.catcar)
        }

        // 編輯 / 刪除按鈕
        holder.btnEdit.setOnClickListener { onEditClick(product) }
        holder.btnDelete.setOnClickListener { onDeleteClick(product) }
    }

    // ★★★ 外部呼叫刷新資料用 ★★★
    fun updateData(newList: List<Product>) {
        products.clear()
        products.addAll(newList)
        notifyDataSetChanged()
    }
}