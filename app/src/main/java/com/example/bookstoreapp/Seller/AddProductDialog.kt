package com.example.bookstoreapp.Seller

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.bookstoreapp.R
import com.example.bookstoreapp.Firebase.FirebaseProductRepository

class AddProductDialog(
    private val sellerId: String,
    private val onProductAdded: (Product) -> Unit
) : DialogFragment() {

    private lateinit var ivImage: ImageView
    private lateinit var etName: EditText
    private lateinit var etDesc: EditText
    private lateinit var etPrice: EditText
    private lateinit var etStock: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnCancel: Button
    private lateinit var btnSelect: Button

    private var selectedImageUri: Uri? = null

    // 最大允許檔案大小（3MB）
    private val MAX_IMAGE_SIZE = 3 * 1024 * 1024L

    private val productRepo = FirebaseProductRepository()

    // 選圖片（檔案大小 + 縮圖載入）
    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try {
                    val fileSize = getFileSize(uri)
                    if (fileSize > MAX_IMAGE_SIZE) {
                        Toast.makeText(context, "圖片不能超過 3MB", Toast.LENGTH_SHORT).show()
                        return@registerForActivityResult
                    }

                    selectedImageUri = uri

                    // 用縮圖方式載入預覽
                    loadScaledImage(uri, ivImage)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "圖片載入失敗", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_add_product, container, false)

        // 綁定畫面元件
        ivImage = view.findViewById(R.id.ivProductImage)
        etName = view.findViewById(R.id.etProductName)
        etDesc = view.findViewById(R.id.etProductDesc)
        etPrice = view.findViewById(R.id.etProductPrice)
        etStock = view.findViewById(R.id.etProductStock)
        btnAdd = view.findViewById(R.id.btnAddProduct)
        btnCancel = view.findViewById(R.id.btnCancelProduct)
        btnSelect = view.findViewById(R.id.btnSelectImage)

        btnSelect.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val price = etPrice.text.toString().toIntOrNull()
            val stock = etStock.text.toString().toIntOrNull()
            val imageUri = selectedImageUri

            if (name.isEmpty() || desc.isEmpty() || price == null || stock == null || imageUri == null) {
                Toast.makeText(context, "請填寫完整資料並選擇圖片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sellerId.isBlank()) {
                Toast.makeText(context, "商家資料有誤，請重新登入", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val product = Product(
                id = "",
                name = name,
                description = desc,
                price = price,
                stock = stock,
                sellerId = sellerId,
                imageUrl = ""
            )

            btnAdd.isEnabled = false
            btnAdd.text = "上架中..."

            productRepo.addProductWithImage(
                product = product,
                imageUri = imageUri,
                onSuccess = { savedProduct ->
                    val ctx = context ?: return@addProductWithImage
                    Toast.makeText(ctx, "商品上架成功", Toast.LENGTH_SHORT).show()
                    onProductAdded(savedProduct)
                    dismiss()
                },
                onFailure = { e ->
                    val ctx = context ?: return@addProductWithImage
                    Toast.makeText(
                        ctx,
                        "上架失敗：${e.localizedMessage ?: e.toString()}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("AddProductDialog", "上架商品失敗", e)

                    btnAdd.isEnabled = true
                    btnAdd.text = "新增"
                }
            )
        }

        btnCancel.setOnClickListener { dismiss() }

        return view
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun getFileSize(uri: Uri): Long {
        val cursor = requireContext().contentResolver
            .query(uri, null, null, null, null)
        return cursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            it.moveToFirst()
            it.getLong(sizeIndex)
        } ?: 0L
    }

    // ⭐ 跟 Fragment 一樣的縮圖邏輯
    private fun loadScaledImage(uri: Uri, target: ImageView) {
        try {
            val resolver = requireContext().contentResolver

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