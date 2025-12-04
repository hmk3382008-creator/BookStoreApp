package com.example.bookstoreapp.Seller

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstoreapp.R
import java.io.File
import java.util.Locale


class ProductManageAdapter(
    private var items: MutableList<Product>,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<ProductManageAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_manage, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]


        holder.tvName.text = item.name



        holder.tvPrice.text =
            String.format(Locale.TAIWAN, "價格：$%,d", item.price)



        holder.tvStock.text =
            String.format(Locale.TAIWAN, "庫存：%d", item.stock)

        // 這裡改成從「網路圖片網址」載入，而不是本機檔案
        val url = item.imageUrl
        if (!url.isNullOrEmpty()) {
            Glide.with(holder.itemView)          // 以目前這個 itemView 的 Context 為基準
                .load(url)                       // 載入 Firebase Storage 的圖片網址
                .placeholder(android.R.drawable.ic_menu_report_image) // 載入中先顯示的圖
                .error(android.R.drawable.ic_menu_report_image)       // 載入失敗顯示的圖
                .into(holder.ivThumb)           // 放進縮圖 ImageView
        } else {
            holder.ivThumb.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        // 編輯按鈕：交給外面傳進來的 callback 處理
        holder.btnEdit.setOnClickListener { onEdit(item) }

        // 刪除按鈕：交給外面傳進來的 callback 處理
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size

    // 讓外面可以一次換整個列表（例如重新抓 Firebase 資料）
    fun submitList(newList: List<Product>) {
        items = newList.toMutableList()
        notifyDataSetChanged()
    }
}
