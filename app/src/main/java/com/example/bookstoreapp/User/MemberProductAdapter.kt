package com.example.bookstoreapp.User

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstoreapp.Firbase.FirebaseFavoriteRepository
import com.example.bookstoreapp.R
import com.example.bookstoreapp.Seller.Product
import java.util.Locale

class MemberProductAdapter(
    private val userId: String,
    private val favoriteRepo: FirebaseFavoriteRepository,
    private var items: MutableList<Product>,
    private val onAddToCart: (Product) -> Unit,
    private val onDetail: (Product) -> Unit
) : RecyclerView.Adapter<MemberProductAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivThumb: ImageView = v.findViewById(R.id.ivThumb)          // 商品圖片
        val tvName: TextView = v.findViewById(R.id.tvName)             // 商品名稱
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)           // 價格
        val tvStock: TextView = v.findViewById(R.id.tvStock)           // 庫存
        val btnAddToCart: Button = v.findViewById(R.id.btnAddToCart)   // 加入購物車
        val btnFavorite: Button = v.findViewById(R.id.btnFavorite)     // 收藏 / 已收藏
        val btnDetail: ImageButton = v.findViewById(R.id.btnDetail)    // 查看詳情（右上角的小按鈕）
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member_product, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // 商品名稱
        holder.tvName.text = item.name

        // 價格（用台灣格式）
        holder.tvPrice.text = String.format(Locale.TAIWAN, "價格：$%d", item.price)

        // 庫存
        holder.tvStock.text = "庫存：${item.stock}"

        // 圖片載入：Firebase Storage 的 imageUrl + Glide
        val url = item.imageUrl
        if (!url.isNullOrEmpty()) {
            Glide.with(holder.itemView)
                .load(url)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.ivThumb)
        } else {
            holder.ivThumb.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        // 依照 isFavorite 設定按鈕文字 / 顏色
        applyFavoriteUi(holder, item.isFavorite)

        // 加入購物車
        holder.btnAddToCart.setOnClickListener { onAddToCart(item) }

        // 收藏 / 取消收藏
        holder.btnFavorite.setOnClickListener {
            // 先鎖一下按鈕，避免連點
            holder.btnFavorite.isEnabled = false

            favoriteRepo.toggleFavorite(userId, item) { isSuccess, isFavoriteNow ->
                holder.btnFavorite.isEnabled = true

                if (isSuccess) {
                    item.isFavorite = isFavoriteNow
                    applyFavoriteUi(holder, isFavoriteNow)

                    val msg = if (isFavoriteNow) {
                        "已加入收藏：${item.name}"
                    } else {
                        "已取消收藏：${item.name}"
                    }
                    Toast.makeText(holder.itemView.context, msg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        holder.itemView.context,
                        "變更收藏狀態失敗",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // 查看詳情（按圖示）
        holder.btnDetail.setOnClickListener { onDetail(item) }

        // 整張 item 點下去也看詳情
        holder.itemView.setOnClickListener { onDetail(item) }
    }

    override fun getItemCount(): Int = items.size

    // 外面（Fragment）用這個方法來更新列表
    fun submitList(newList: List<Product>) {
        items = newList.toMutableList()
        notifyDataSetChanged()
    }

    /**
     * 根據是否收藏改變按鈕文字 / 顏色
     * - 已收藏：顯示「已收藏」，文字紅色
     * - 未收藏：顯示「收藏」，文字灰色
     */
    private fun applyFavoriteUi(holder: VH, isFavorite: Boolean) {
        if (isFavorite) {
            holder.btnFavorite.text = "已收藏"
            holder.btnFavorite.setTextColor(Color.BLUE)
        } else {
            holder.btnFavorite.text = "收藏"
            holder.btnFavorite.setTextColor(Color.WHITE)
        }
    }
}