package com.example.bookstoreapp.View

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstoreapp.R
import com.example.bookstoreapp.Seller.Product
import java.util.Locale

class VendorProductAdapter(
    private var productList: MutableList<Product>
) : RecyclerView.Adapter<VendorProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvName: TextView = view.findViewById(R.id.tvProductName)
        val tvPrice: TextView = view.findViewById(R.id.tvProductPrice)
        val tvStock: TextView = view.findViewById(R.id.tvProductStock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vendor_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        // 商品名稱
        holder.tvName.text = product.name

        // 價格 / 庫存
        holder.tvPrice.text = String.format(Locale.TAIWAN, "價格：$%d", product.price)
        holder.tvStock.text = "庫存：${product.stock}"

        // 圖片：改用 Firebase Storage 的 imageUrl + Glide 顯示
        val url = product.imageUrl
        if (!url.isNullOrEmpty()) {
            Glide.with(holder.itemView)
                .load(url)
                .placeholder(android.R.drawable.ic_menu_report_image) // 載入中
                .error(android.R.drawable.ic_menu_report_image)       // 失敗
                .into(holder.ivImage)
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }

    override fun getItemCount(): Int = productList.size

    // 讓外面可以更新這個 adapter 的資料（例如重新抓 Firebase 後）
    fun submitList(newList: List<Product>) {
        productList = newList.toMutableList()
        notifyDataSetChanged()
    }
}