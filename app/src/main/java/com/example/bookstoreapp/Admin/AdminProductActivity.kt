package com.example.bookstoreapp.Admin

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstoreapp.Firebase.FirebaseProductRepository
import com.example.bookstoreapp.R
import com.example.bookstoreapp.Seller.Product
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

 class AdminProductActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseProductRepository
    private lateinit var adapter: AdminProductAdapter

    private lateinit var toolbar: MaterialToolbar
    private lateinit var spStatusFilter: Spinner        // ⬅ 新增：狀態篩選
    private lateinit var spSearchType: Spinner
    private lateinit var etKeyword: EditText
    private lateinit var btnSearch: Button
    private lateinit var rvProducts: RecyclerView
    private lateinit var fabAddProduct: FloatingActionButton

    private var allProducts: List<Product> = emptyList()

    // 編輯圖片暫存
    private var editingImageView: ImageView? = null
    private var editingImageUri: Uri? = null

    // 選圖
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_product)

        repository = FirebaseProductRepository()

        toolbar = findViewById(R.id.toolbarAdminProduct)
        spStatusFilter = findViewById(R.id.spStatusFilter)   // ⬅ 新增
        spSearchType = findViewById(R.id.spSearchType)
        etKeyword = findViewById(R.id.etKeyword)
        btnSearch = findViewById(R.id.btnSearch)
        rvProducts = findViewById(R.id.rvProducts)
        fabAddProduct = findViewById(R.id.fabAddProduct)

        initImagePicker()
        setupToolbar()
        setupSpinners()          // ⬅ 改名
        setupRecyclerView()
        setupClickListeners()
        loadAllProducts()
    }

    private fun initImagePicker() {
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null && editingImageView != null) {
                editingImageUri = uri
                loadScaledImage(uri, editingImageView!!)
            }
        }
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // ⬅ 取代原本的 setupSpinner()
    private fun setupSpinners() {
        // 狀態篩選：全部 / 上架中 / 已下架
        val statusItems = listOf("全部狀態", "上架中", "已下架")
        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statusItems
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatusFilter.adapter = statusAdapter

        // 搜尋類型：商品名稱 / 商家ID
        val searchItems = listOf("商品名稱", "商家ID")
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            searchItems
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSearchType.adapter = spinnerAdapter
    }

    private fun setupRecyclerView() {
        adapter = AdminProductAdapter(
            products = mutableListOf(),
            onEditClick = { product ->
                showEditDialog(product)
            },
            onDeleteClick = { product ->
                showDeleteConfirm(product)
            },
            onStatusChange = { product, isActive ->
                repository.updateProductActive(
                    productId = product.id,
                    isActive = isActive,
                    onSuccess = {
                        product.isActive = isActive
                        val msg = if (isActive) {
                            "「${product.name}」已上架"
                        } else {
                            "「${product.name}」已下架"
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        adapter.notifyDataSetChanged()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "更新狀態失敗：${e.message}", Toast.LENGTH_SHORT).show()
                        loadAllProducts()
                    }
                )
            }
        )

        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = adapter
    }

    private fun setupClickListeners() {
        btnSearch.setOnClickListener { applySearch() }

        fabAddProduct.setOnClickListener {
            Toast.makeText(this, "之後可接到新增商品頁面", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAllProducts() {
        repository.getAllProducts(
            onSuccess = { list ->
                allProducts = list
                adapter.updateData(list)
            },
            onFailure = { e ->
                Toast.makeText(this, "讀取商品失敗：${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ⬅ 改成：先照狀態篩選，再照關鍵字 + 搜尋類型
    private fun applySearch() {
        var filtered = allProducts

        // 1) 狀態篩選
        when (spStatusFilter.selectedItem.toString()) {
            "上架中" -> {
                filtered = filtered.filter { it.isActive }
            }
            "已下架" -> {
                filtered = filtered.filter { !it.isActive }
            }
            // "全部狀態" -> 不動
        }

        // 2) 關鍵字 + 搜尋類型
        val keyword = etKeyword.text.toString().trim()
        if (keyword.isNotEmpty()) {
            val type = spSearchType.selectedItem.toString()
            filtered = when (type) {
                "商品名稱" -> filtered.filter { it.name.contains(keyword, ignoreCase = true) }
                "商家ID" -> filtered.filter { it.sellerId.contains(keyword, ignoreCase = true) }
                else -> filtered
            }
        }

        adapter.updateData(filtered)
    }

    private fun showDeleteConfirm(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("刪除商品")
            .setMessage("確定要刪除「${product.name}」嗎？")
            .setPositiveButton("刪除") { _, _ ->
                repository.deleteProduct(
                    productId = product.id,
                    onSuccess = {
                        Toast.makeText(this, "刪除成功", Toast.LENGTH_SHORT).show()
                        loadAllProducts()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "刪除失敗：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditDialog(product: Product) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_product, null)

        val ivProduct = dialogView.findViewById<ImageView>(R.id.ivProduct)
        val btnChangeImage = dialogView.findViewById<Button>(R.id.btnChangeImage)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDesc)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrice)
        val etStock = dialogView.findViewById<EditText>(R.id.etStock)

        editingImageView = ivProduct
        editingImageUri = null

        etName.setText(product.name)
        etDesc.setText(product.description)
        etPrice.setText(product.price.toString())
        etStock.setText(product.stock.toString())

        if (!product.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(product.imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .into(ivProduct)
        } else {
            ivProduct.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        btnChangeImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(this)
            .setTitle("編輯商品")
            .setView(dialogView)
            .setPositiveButton("儲存") { _, _ ->
                val newName = etName.text.toString().trim()
                val newDesc = etDesc.text.toString().trim()
                val newPrice = etPrice.text.toString().toIntOrNull() ?: 0
                val newStock = etStock.text.toString().toIntOrNull() ?: 0

                if (newName.isEmpty() || newDesc.isEmpty()) {
                    showInfo("商品名稱與描述不可空白")
                    editingImageView = null
                    editingImageUri = null
                    return@setPositiveButton
                }

                val updated = product.copy(
                    name = newName,
                    description = newDesc,
                    price = newPrice,
                    stock = newStock
                )

                repository.updateProduct(
                    product = updated,
                    newImageUri = editingImageUri,
                    onSuccess = {
                        Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show()
                        loadAllProducts()
                    },
                    onFailure = {
                        showInfo("更新失敗，請稍後再試")
                    }
                )

                editingImageView = null
                editingImageUri = null
            }
            .setNegativeButton("取消") { _, _ ->
                editingImageView = null
                editingImageUri = null
            }
            .show()
    }

    private fun showInfo(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("確定", null)
            .show()
    }

    private fun loadScaledImage(uri: Uri, target: ImageView) {
        try {
            val resolver = contentResolver

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) {
                target.setImageURI(uri)
                return
            }

            val maxSize = 2048
            var sampleSize = 1

            while (srcWidth / sampleSize > maxSize || srcHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }

            val finalOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, finalOptions)
            }

            if (bitmap != null) {
                target.setImageBitmap(bitmap)
            } else {
                target.setImageURI(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            target.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }
}