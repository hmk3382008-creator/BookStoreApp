package com.example.bookstoreapp.User

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstoreapp.R
import com.example.bookstoreapp.Seller.Product
import java.util.Locale

class FavoriteAdapter(
    private var items: MutableList<Product>,
    private val onAddToCart: (Product) -> Unit,
    private val onRemoveFavorite: (Product) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val btnAddToCart: Button = itemView.findViewById(R.id.btnAddToCart)
        val btnRemoveFavorite: Button = itemView.findViewById(R.id.btnRemoveFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvName.text = item.name
        holder.tvPrice.text = String.format(Locale.TAIWAN, "價格：$%d", item.price)

        val url = item.imageUrl
        if (!url.isNullOrEmpty()) {
            Glide.with(holder.itemView)
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.ivThumb)
        } else {
            holder.ivThumb.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.btnAddToCart.setOnClickListener { onAddToCart(item) }
        holder.btnRemoveFavorite.setOnClickListener { onRemoveFavorite(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Product>) {
        items = newList.toMutableList()
        notifyDataSetChanged()
    }
}
