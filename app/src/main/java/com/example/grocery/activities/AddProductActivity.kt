package com.example.grocery.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.grocery.R
import com.example.grocery.thumucquantrong.Constants

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.util.Objects

class AddProductActivity : AppCompatActivity() {
    private var productIconIv: ImageView? = null
    private var titleEt: EditText? = null
    private var descriptionEt: EditText? = null
    private var categoryTv: TextView? = null
    private var quantityEt: TextView? = null
    private var priceEt: TextView? = null
    private var discountedPriceEt: TextView? = null
    private var discountedNoteEt: TextView? = null
    private var discountSwitch: SwitchCompat? = null

    //permission arrays
    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>

    //image picked uri
    private var image_uri: Uri? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var progressDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        //init ui views
        //ui views
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        productIconIv = findViewById(R.id.productIconIv)
        titleEt = findViewById(R.id.titleEt)
        descriptionEt = findViewById(R.id.descriptionEt)
        categoryTv = findViewById(R.id.categoryTv)
        quantityEt = findViewById(R.id.quantityEt)
        priceEt = findViewById(R.id.priceEt)
        discountSwitch = findViewById(R.id.discountSwitch)
        discountedPriceEt = findViewById(R.id.discountedPriceEt)
        discountedNoteEt = findViewById(R.id.discountedNoteEt)
        val addProductBtn = findViewById<Button>(R.id.addProductBtn)

        //on start is unchecked, so hide discountPriceEt, discountNoteEt
        discountedPriceEt!!.setVisibility(View.GONE)
        discountedNoteEt!!.setVisibility(View.GONE)
        firebaseAuth = FirebaseAuth.getInstance()

        //setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Vui lòng đợi")
        progressDialog!!.setCanceledOnTouchOutside(false)

        //init permission arrays
        cameraPermissions =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        //if discountSwitch is checked: show discountPriceEt, discountNoteEt | if discountSwitch is not checked: hide discountPriceEt, discountNoteEt
        discountSwitch?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                //checked, show discountPriceEt, discountNoteEt
                discountedPriceEt!!.setVisibility(View.VISIBLE)
                discountedNoteEt!!.setVisibility(View.VISIBLE)
            } else {
                //unchecked, hide discountPriceEt, discountNoteEt
                discountedPriceEt!!.setVisibility(View.GONE)
                discountedNoteEt!!.setVisibility(View.GONE)
            }
        })
        backBtn.setOnClickListener { v: View? -> onBackPressed() }
        productIconIv?.setOnClickListener(View.OnClickListener { v: View? ->
            //show dialog to pick image
            showImagePickDialog()
        })
        categoryTv?.setOnClickListener(View.OnClickListener { v: View? ->
            //pick category
            categoryDialog()
        })
        addProductBtn.setOnClickListener { v: View? ->
            //Flow:
            //1) Input data
            //2) Validate data
            //3) Add data to db
            inputData()
        }
    }

    private var productTitle: String? = null
    private var productDescription: String? = null
    private var productCategory: String? = null
    private var productQuantity: String? = null
    private var originalPrice: String? = null
    private var discountPrice: String? = null
    private var discountNote: String? = null

    private var discountAvailable = false

    private fun inputData() {
        //1) Input data
        productTitle = titleEt!!.text.toString().trim { it <= ' ' }
        productDescription = descriptionEt!!.text.toString().trim { it <= ' ' }
        productCategory = categoryTv!!.text.toString().trim { it <= ' ' }
        productQuantity = quantityEt!!.text.toString().trim { it <= ' ' }
        originalPrice = priceEt!!.text.toString().trim { it <= ' ' }
        discountAvailable = discountSwitch!!.isChecked //true/false


        //2) Validate data
        if (TextUtils.isEmpty(productTitle)) {
            Toast.makeText(this, "Tiêu đề là bắt buộc...", Toast.LENGTH_SHORT).show()
            return  // don't proceed further
        }
        if (TextUtils.isEmpty(productQuantity)) {
            Toast.makeText(this, "Số lượng là bắt buộc...", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(productCategory)) {
            Toast.makeText(this, "Thể loại là bắt buộc...", Toast.LENGTH_SHORT).show()
            return  // don't proceed further
        }
        if (TextUtils.isEmpty(originalPrice)) {
            Toast.makeText(this, "Giá là bắt buộc...", Toast.LENGTH_SHORT).show()
            return  // don't proceed further
        }
        if (discountAvailable) {
            //product is with discount
            discountPrice = discountedPriceEt!!.text.toString().trim { it <= ' ' }
            discountNote = discountedNoteEt!!.text.toString().trim { it <= ' ' }

            if (TextUtils.isEmpty(discountPrice)) {
                Toast.makeText(this, "Giá chiết khấu là bắt buộc...", Toast.LENGTH_SHORT).show()
                return  // don't proceed further
            }
        } else {
            //product is without discount
            discountPrice = "0"
            discountNote = ""
        }
        addProduct()
    }

    private fun addProduct() {
        //3) Add data to db
        progressDialog!!.setMessage("Thêm sản phẩm...")
        progressDialog!!.show()
        val timestamp = "" + System.currentTimeMillis()
        if (image_uri == null) {
            //upload without image

            //setup data to upload
            val hashMap = HashMap<String, Any>()
            hashMap["productId"] = "" + timestamp
            hashMap["productTitle"] = "" + productTitle
            hashMap["productDescription"] = "" + productDescription
            hashMap["productCategory"] = "" + productCategory
            hashMap["productQuantity"] = "" + productQuantity
            hashMap["productIcon"] = "" // no image, set empty
            hashMap["originalPrice"] = "" + originalPrice
            hashMap["discountPrice"] = "" + discountPrice
            hashMap["discountNote"] = "" + discountNote
            hashMap["discountAvailable"] = "" + discountAvailable
            hashMap["timestamp"] = "" + timestamp
            hashMap["uid"] = "" + firebaseAuth!!.uid
            //add to db
            val reference = FirebaseDatabase.getInstance().getReference("Users")
            reference.child(Objects.requireNonNull(firebaseAuth!!.uid).toString()).child("Products")
                .child(timestamp).setValue(hashMap)
                .addOnSuccessListener { aVoid: Void? ->
                    //added to db
                    progressDialog!!.dismiss()
                    Toast.makeText(
                        this@AddProductActivity,
                        "Sản phẩm được thêm vào...",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearData()
                }
                .addOnFailureListener { e: Exception ->
                    //failed adding to db
                    progressDialog!!.dismiss()
                    Toast.makeText(this@AddProductActivity, "" + e.message, Toast.LENGTH_SHORT)
                        .show()
                }
        } else {
            // upload with image

            //first upload image to storage

            //name and path of image to be uploaded
            val filePathAndName = "product_images/$timestamp"
            val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
            storageReference.putFile(image_uri!!)
                .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
                    //image uploaded
                    //get url of uploaded image
                    val uriTask = taskSnapshot.storage.downloadUrl
                    uriTask.addOnSuccessListener { uri ->
                        val downloadImageUri = uri.toString()

                        // Sau khi có link ảnh, upload dữ liệu
                        val hashMap = HashMap<String, Any>()
                        hashMap["productId"] = "$timestamp"
                        hashMap["productTitle"] = "$productTitle"
                        hashMap["productDescription"] = "$productDescription"
                        hashMap["productCategory"] = "$productCategory"
                        hashMap["productQuantity"] = "$productQuantity"
                        hashMap["productIcon"] = downloadImageUri
                        hashMap["originalPrice"] = "$originalPrice"
                        hashMap["discountPrice"] = "$discountPrice"
                        hashMap["discountNote"] = "$discountNote"
                        hashMap["discountAvailable"] = "$discountAvailable"
                        hashMap["timestamp"] = "$timestamp"
                        hashMap["uid"] = firebaseAuth!!.uid!!

                        // Thêm vào Realtime Database
                        val reference = FirebaseDatabase.getInstance().getReference("Users")
                        reference.child(firebaseAuth!!.uid!!)
                            .child("Products").child(timestamp)
                            .setValue(hashMap)
                            .addOnSuccessListener {
                                progressDialog!!.dismiss()
                                Toast.makeText(this@AddProductActivity, "Sản phẩm được thêm thành công", Toast.LENGTH_SHORT).show()
                                clearData()
                            }
                            .addOnFailureListener { e ->
                                progressDialog!!.dismiss()
                                Toast.makeText(this@AddProductActivity, "Lỗi thêm sản phẩm: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                    }.addOnFailureListener { e ->
                        progressDialog!!.dismiss()
                        Toast.makeText(this, "Không lấy được link ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e: Exception ->
                    //failed uploading image
                    progressDialog!!.dismiss()
                    Toast.makeText(this@AddProductActivity, "" + e.message, Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    private fun clearData() {
        //clear data after uploading product
        titleEt!!.setText("")
        descriptionEt!!.setText("")
        categoryTv!!.text = ""
        quantityEt!!.text = ""
        priceEt!!.text = ""
        discountedPriceEt!!.text = ""
        discountedNoteEt!!.text = ""
        productIconIv!!.setImageResource(R.drawable.ic_add_shopping_primary)
        image_uri = null
    }

    private fun categoryDialog() {
        //dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Danh mục sản phẩm")
            .setItems(Constants.productCategories) { dialog: DialogInterface?, which: Int ->
                //get picked category
                val category = Constants.productCategories[which]

                //set picked category
                categoryTv!!.text = category
            }
            .show()
    }

    private fun showImagePickDialog() {
        //options to display in dialog
        val options = arrayOf("Camera", "Gallery")
        //dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chọn hình ảnh")
            .setItems(options) { dialog: DialogInterface?, which: Int ->
                //handle item clicks
                if (which == 0) {
                    //camera clicked
                    if (checkCameraPermission()) {
                        //permission granted
                        pickFromCamera()
                    } else {
                        //permission not granted, request
                        requestCameraPermission()
                    }
                } else {
                    //gallery clicked
                    if (checkStoragePermission()) {
                        //permission granted
                        pickFromGallery()
                    } else {
                        //permission not granted, request
                        requestStoragePermission()
                    }
                }
            }
            .show()
    }

    private fun pickFromGallery() {
        //intent to pick image from gallery
        val intent = Intent(Intent.ACTION_PICK)
        intent.setType("image/*")
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE)
    }

    private fun pickFromCamera() {
        //intent to pick image from camera

        //using media store to pick high/original quality image
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image_Title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image_Description")
        image_uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE)
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) ==
                PackageManager.PERMISSION_GRANTED //returns true/false
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE)
    }

    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        val result1 =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        return result && result1
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    //handle permission results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && storageAccepted) {
                        pickFromCamera()
                    } else {
                        Toast.makeText(this, "Quyền sử dụng máy ảnh & bộ nhớ là bắt buộc", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (storageAccepted) {
                        pickFromGallery()
                    } else {
                        Toast.makeText(this, "Quyền sử dụng bộ nhớ là bắt buộc", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                image_uri = data?.data
                productIconIv?.setImageURI(image_uri)
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                productIconIv?.setImageURI(image_uri)
            }
        }
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
        private const val IMAGE_PICK_GALLERY_CODE = 102
        private const val IMAGE_PICK_CAMERA_CODE = 103
    }
}
