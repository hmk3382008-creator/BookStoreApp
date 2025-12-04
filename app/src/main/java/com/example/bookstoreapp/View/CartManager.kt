package com.example.bookstoreapp.View

import com.example.bookstoreapp.Seller.Product

object CartManager {

    data class CartItem(
        val product: Product,
        var quantity: Int
    )

    private val cartItems = mutableListOf<CartItem>()

    fun addProduct(product: Product, qty: Int = 1) {
        val existing = cartItems.find { it.product.id == product.id }
        if (existing != null) {
            existing.quantity += qty
        } else {
            cartItems.add(CartItem(product, qty))
        }
    }

    fun removeProduct(productId: String) {
        cartItems.removeAll { it.product.id == productId }
    }

    fun changeQuantity(productId: String, qty: Int) {
        val item = cartItems.find { it.product.id == productId } ?: return

        val maxStock = item.product.stock           // 這個商品的庫存數
        val newQty = qty.coerceIn(1, maxStock)      // 介於 1 和庫存之間

        item.quantity = newQty
    }

    fun getAll(): List<CartItem> = cartItems.toList()

    fun clear() {
        cartItems.clear()
    }

    fun totalPrice(): Int {
        return cartItems.sumOf { it.product.price * it.quantity }
    }
}