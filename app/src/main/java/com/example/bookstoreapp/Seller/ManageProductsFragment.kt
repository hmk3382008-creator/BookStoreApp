package com.example.bookstoreapp.Seller


import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstoreapp.Firebase.FirebaseProductRepository
import com.example.bookstoreapp.R





class ManageProductsFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: ProductManageAdapter

    private val productRepo = FirebaseProductRepository()

    private var sellerId: String = ""

    // 編輯商品時暫存「新選的圖片 Uri」（如果沒選，就維持 null）
    private var editingImageUri: Uri? = null
    // 編輯 dialog 裡用來顯示預覽的 ImageView
    private var editingImageView: ImageView? = null

    // 最大允許圖片大小（3MB）
    private val MAX_IMAGE_SIZE = 3 * 1024 * 1024L

    //  編輯商品 → 選圖片（檔案大小 + 縮圖載入）
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                try {
                    //  檔案大小檢查
                    val fileSize = getFileSize(uri)
                    if (fileSize > MAX_IMAGE_SIZE) {
                        Toast.makeText(requireContext(), "圖片不能超過 3MB", Toast.LENGTH_SHORT).show()
                        return@registerForActivityResult
                    }

                    //  通過檢查 → 記住 Uri
                    editingImageUri = uri

                    // 用「縮圖」方式載入到 ImageView
                    editingImageView?.let { iv ->
                        loadScaledImage(uri, iv)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "圖片載入失敗", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 從 arguments 拿到 sellerId（Firebase 的 user.id）
        sellerId = arguments?.getString("sellerId") ?: ""

        if (sellerId.isBlank()) {
            Toast.makeText(requireContext(), "商家資料有誤，請重新登入", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_manage_products, container, false)

        rv = v.findViewById(R.id.rvProducts)
        tvEmpty = v.findViewById(R.id.tvEmpty)
        val toolbar = v.findViewById<Toolbar>(R.id.toolbar)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        // 啟用返回鍵
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 返回鍵事件
        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = ProductManageAdapter(
            mutableListOf(),
            onEdit = { product -> showEditDialog(product) },   // 編輯商品
            onDelete = { product -> confirmDelete(product) }  // 刪除商品
        )
        rv.adapter = adapter

        loadData()
        return v
    }

    // 從 Firebase 抓「這個商家自己的商品」
    private fun loadData() {
        if (sellerId.isBlank()) {
            tvEmpty.visibility = View.VISIBLE
            return
        }

        productRepo.getProductsBySellerId(
            sellerId = sellerId,
            onSuccess = { list ->
                adapter.submitList(list)
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            },
            onFailure = { e ->
                tvEmpty.visibility = View.VISIBLE
                Toast.makeText(
                    requireContext(),
                    "載入商品失敗：${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // 刪除商品前先確認
    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("刪除商品")
            .setMessage("確認刪除「${product.name}」？此動作無法復原")
            .setPositiveButton("刪除") { _, _ ->
                productRepo.deleteProduct(
                    productId = product.id,
                    onSuccess = {
                        loadData()
                    },
                    onFailure = {
                        showInfo("刪除失敗，請稍後再試")
                    }
                )
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 編輯商品 Dialog
    private fun showEditDialog(product: Product) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_product, null)

        val ivProduct = dialogView.findViewById<ImageView>(R.id.ivProduct)
        val btnChangeImage = dialogView.findViewById<Button>(R.id.btnChangeImage)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDesc)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrice)
        val etStock = dialogView.findViewById<EditText>(R.id.etStock)

        // 暫存給 pickImageLauncher 使用
        editingImageView = ivProduct
        editingImageUri = null          // 預設這次沒有換圖

        // 顯示原本文字資料
        etName.setText(product.name)
        etDesc.setText(product.description)
        etPrice.setText(product.price.toString())
        etStock.setText(product.stock.toString())

        // 顯示原本圖片（如果有 imageUrl）→ 這裡用 Glide 載網路圖 OK
        if (!product.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(product.imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .into(ivProduct)
        } else {
            ivProduct.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        // 更換圖片 → 開啟選圖流程
        btnChangeImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(requireContext())
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

                productRepo.updateProduct(
                    product = updated,
                    newImageUri = editingImageUri, // null = 沒改圖；有 Uri = 要上傳新圖
                    onSuccess = {
                        loadData()
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
        AlertDialog.Builder(requireContext())
            .setMessage(msg)
            .setPositiveButton("確定", null)
            .show()
    }

    // 取得檔案大小（bytes）
    private fun getFileSize(uri: Uri): Long {
        val cursor = requireContext().contentResolver
            .query(uri, null, null, null, null)
        return cursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            it.moveToFirst()
            it.getLong(sizeIndex)
        } ?: 0L
    }

    // 把超大圖縮小後再丟給 ImageView，避免 Canvas 太大崩潰
    private fun loadScaledImage(uri: Uri, target: ImageView) {
        try {
            val resolver = requireContext().contentResolver

            // 第一次：只取得原始寬高，不載入整張圖
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) {
                // 取得不到尺寸，就退而求其次直接 setImageURI（很少發生）
                target.setImageURI(uri)
                return
            }

            // 設定允許的最大邊長（例如 2048px）
            val maxSize = 2048
            var sampleSize = 1

            // 一直除 2，直到縮到寬高都 <= maxSize
            while (srcWidth / sampleSize > maxSize || srcHeight / sampleSize > maxSize) {
                sampleSize *= 2
            }

            // 第二次：用 inSampleSize 正式 decode 成縮圖
            val finalOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, finalOptions)
            }

            if (bitmap != null) {
                target.setImageBitmap(bitmap)
            } else {
                target.setImageURI(uri) // fallback
            }
        } catch (e: Exception) {
            e.printStackTrace()
            target.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }

    companion object {
        fun newInstance(sellerId: String): ManageProductsFragment {
            val f = ManageProductsFragment()
            f.arguments = bundleOf("sellerId" to sellerId)
            return f
        }
    }
}
