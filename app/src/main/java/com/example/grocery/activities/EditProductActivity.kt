package com.example.grocery.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
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
import com.example.grocery.thumucquantrong.Constants
import com.example.grocery.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class EditProductActivity : AppCompatActivity() {
    //ui views
    private var backBtn: ImageButton? = null
    private var productIconIv: ImageView? = null
    private var titleEt: EditText? = null
    private var descriptionEt: EditText? = null
    private var categoryTv: TextView? = null
    private var quantityEt: TextView? = null
    private var priceEt: TextView? = null
    private var discountedPriceEt: TextView? = null
    private var discountedNoteEt: TextView? = null
    private var discountSwitch: SwitchCompat? = null
    private var updateProductBtn: Button? = null
    private var productId: String? = null

    //permission arrays
    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>

    //image picked uri
    private var image_uri: Uri? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var progressDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        //init ui views
        backBtn = findViewById(R.id.backBtn)
        productIconIv = findViewById<ImageView>(R.id.productIconIv)
        titleEt = findViewById(R.id.titleEt)
        descriptionEt = findViewById(R.id.descriptionEt)
        categoryTv = findViewById(R.id.categoryTv)
        quantityEt = findViewById(R.id.quantityEt)
        priceEt = findViewById(R.id.priceEt)
        discountSwitch = findViewById(R.id.discountSwitch)
        discountedPriceEt = findViewById(R.id.discountedPriceEt)
        discountedNoteEt = findViewById(R.id.discountedNoteEt)
        updateProductBtn = findViewById<Button>(R.id.updateProductBtn)

        //get id of the product from intent
        productId = intent.getStringExtra("productId")

        //on start is unchecked, so hide discountPriceEt, discountNoteEt
        discountedPriceEt!!.setVisibility(View.GONE)
        discountedNoteEt!!.setVisibility(View.GONE)
        firebaseAuth = FirebaseAuth.getInstance()
        loadProductDetails() //to set on views

        //setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Vui lòng đợi")
        progressDialog!!.setCanceledOnTouchOutside(false)

        //init permission arrays
        cameraPermissions =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        //if discountSwitch is checked: show discountPriceEt, discountNoteEt | if discountSwitch is not checked: hide discountPriceEt, discountNoteEt
        discountSwitch!!.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
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
        backBtn!!.setOnClickListener(View.OnClickListener { onBackPressed() })
        productIconIv!!.setOnClickListener(View.OnClickListener { //show dialog to pick image
            showImagePickDialog()
        })
        categoryTv?.setOnClickListener(View.OnClickListener { //pick category
            categoryDialog()
        })
        updateProductBtn!!.setOnClickListener(View.OnClickListener { //Flow:
            //1) Input data
            //2) Validate data
            //3) update data to db
            inputData()
        })
    }

    private fun loadProductDetails() {
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(firebaseAuth!!.uid!!).child("Products").child(productId!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //get data
                    val productId = "" + dataSnapshot.child("productId").value
                    val productTitle = "" + dataSnapshot.child("productTitle").value
                    val productDescription = "" + dataSnapshot.child("productDescription").value
                    val productCategory = "" + dataSnapshot.child("productCategory").value
                    val productQuantity = "" + dataSnapshot.child("productQuantity").value
                    val productIcon = "" + dataSnapshot.child("productIcon").value
                    val originalPrice = "" + dataSnapshot.child("originalPrice").value
                    val discountPrice = "" + dataSnapshot.child("discountPrice").value
                    val discountNote = "" + dataSnapshot.child("discountNote").value
                    val discountAvailable = "" + dataSnapshot.child("discountAvailable").value
                    val timestamp = "" + dataSnapshot.child("timestamp").value
                    val uid = "" + dataSnapshot.child("uid").value

                    //set data to views
                    if (discountAvailable == "true") {
                        discountSwitch!!.isChecked = true
                        discountedPriceEt!!.visibility = View.VISIBLE
                        discountedNoteEt!!.visibility = View.VISIBLE
                    } else {
                        discountSwitch!!.isChecked = false
                        discountedPriceEt!!.visibility = View.GONE
                        discountedNoteEt!!.visibility = View.GONE
                    }
                    titleEt!!.setText(productTitle)
                    descriptionEt!!.setText(productDescription)
                    categoryTv!!.setText(productCategory)
                    discountedNoteEt!!.setText(discountNote)
                    quantityEt!!.setText(productQuantity)
                    discountedPriceEt!!.setText(discountPrice)
                    priceEt!!.setText(originalPrice)

                    try {
                        Picasso.get().load(productIcon).placeholder(R.drawable.store)
                            .into(productIconIv)
                    } catch (e: Exception) {
                        productIconIv!!.setImageResource(R.drawable.store)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
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
        updateProduct()
    }

    private fun updateProduct() {
        //show progress
        progressDialog!!.setMessage("Đang cập nhật sản phẩm...")
        progressDialog!!.show()
        if (image_uri == null) {
            //update without image

            //setup data in hashmap to update
            val hashMap = HashMap<String, Any>()
            hashMap["productTitle"] = "" + productTitle
            hashMap["productDescription"] = "" + productDescription
            hashMap["productCategory"] = "" + productCategory
            hashMap["productQuantity"] = "" + productQuantity
            hashMap["originalPrice"] = "" + originalPrice
            hashMap["discountPrice"] = "" + discountPrice
            hashMap["discountNote"] = "" + discountNote
            hashMap["discountAvailable"] = "" + discountAvailable

            //update to db
            val reference = FirebaseDatabase.getInstance().getReference("Users")
            reference.child(firebaseAuth!!.uid!!).child("Products").child(productId!!)
                .updateChildren(hashMap)
                .addOnSuccessListener { //update success
                    progressDialog!!.dismiss()
                    Toast.makeText(
                        this@EditProductActivity,
                        "Cập nhật thành công...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { e -> //update failed
                    progressDialog!!.dismiss()
                    Toast.makeText(this@EditProductActivity, "" + e.message, Toast.LENGTH_SHORT)
                        .show()
                }
        } else {
            //update with image

            //first upload image
            //image name and path on firebase storage
            val filePathAndName = "product_images/$productId" //overide previous image using same id
            //updaload image
            val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
            storageReference.putFile(image_uri!!)
                .addOnSuccessListener { taskSnapshot ->
                    //image uploaded, get url of uploaded image
                    val uriTask = taskSnapshot.storage.downloadUrl
                    while (!uriTask.isSuccessful);
                    val downloadImageUri = uriTask.result
                    if (uriTask.isSuccessful) {
                        //setup data in hashmap to update
                        val hashMap = HashMap<String, Any>()
                        hashMap["productTitle"] = "" + productTitle
                        hashMap["productDescription"] = "" + productDescription
                        hashMap["productCategory"] = "" + productCategory
                        hashMap["productIcon"] = "" + downloadImageUri
                        hashMap["productQuantity"] = "" + productQuantity
                        hashMap["originalPrice"] = "" + originalPrice
                        hashMap["discountPrice"] = "" + discountPrice
                        hashMap["discountNote"] = "" + discountNote
                        hashMap["discountAvailable"] = "" + discountAvailable

                        //update to db
                        val reference = FirebaseDatabase.getInstance().getReference("Users")
                        reference.child(firebaseAuth!!.uid!!).child("Products").child(productId!!)
                            .updateChildren(hashMap)
                            .addOnSuccessListener { //update success
                                progressDialog!!.dismiss()
                                Toast.makeText(
                                    this@EditProductActivity,
                                    "Cập nhật thành công...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e -> //update failed
                                progressDialog!!.dismiss()
                                Toast.makeText(
                                    this@EditProductActivity,
                                    "" + e.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .addOnFailureListener { e -> //upload failed,
                    progressDialog!!.dismiss()
                    Toast.makeText(this@EditProductActivity, "" + e.message, Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    private fun categoryDialog() {
        //dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Product Category")
            .setItems(Constants.productCategories) { dialog, which -> //get picked category
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
        builder.setTitle("Pick Image")
            .setItems(options) { dialog, which ->
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
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                run {
                    if (grantResults.size > 0) {
                        val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                        val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                        if (cameraAccepted && storageAccepted) {
                            //both permissions granted
                            pickFromCamera()
                        } else {
                            //both or one of permissions denied
                            Toast.makeText(
                                this,
                                "Quyền đối với máy ảnh là cần thiết...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                run {
                    if (grantResults.size > 0) {
                        val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                        if (storageAccepted) {
                            //permission granted
                            pickFromGallery()
                        } else {
                            //permission denied
                            Toast.makeText(
                                this,
                                "Quyền lưu trữ là cần thiết...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            STORAGE_REQUEST_CODE -> {
                if (grantResults.size > 0) {
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (storageAccepted) {
                        pickFromGallery()
                    } else {
                        Toast.makeText(this, "Quyền lưu trữ là cần thiết...", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //handle image pick results
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //image picked from gallery

                //save picked image uri
                image_uri = data!!.data

                //set image
                productIconIv!!.setImageURI(image_uri)
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //image picked from camera
                productIconIv!!.setImageURI(image_uri)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        //permission constants
        private const val CAMERA_REQUEST_CODE = 200
        private const val STORAGE_REQUEST_CODE = 300

        //image pick constants
        private const val IMAGE_PICK_GALLERY_CODE = 400
        private const val IMAGE_PICK_CAMERA_CODE = 500
    }
}
