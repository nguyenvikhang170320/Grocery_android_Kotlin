package com.example.grocery.activities

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.grocery.R
import com.example.grocery.databinding.ActivityProfileEditUserBinding // Import View Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.Locale

class ProfileEditUserActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityProfileEditUserBinding // Khai báo View Binding

    // Permission arrays
    private lateinit var locationPermissions: Array<String>
    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>

    // Image URI
    private var imageUri: Uri? = null

    // Location data
    private var latitude = 0.0
    private var longitude = 0.0
    private var locationManager: LocationManager? = null

    // Progress dialog
    private lateinit var progressDialog: ProgressDialog

    // Firebase auth
    private lateinit var firebaseAuth: FirebaseAuth

    // ActivityResultLauncher for image pick (new way to handle onActivityResult)
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditUserBinding.inflate(layoutInflater) // Khởi tạo View Binding
        setContentView(binding.root) // Set root view từ binding

        // Init permission arrays based on Android version
        locationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
            storagePermissions = arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else { // Android 12-
            cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }


        // Setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Vui lòng đợi")
        progressDialog.setCanceledOnTouchOutside(false)

        // Firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        // Init ActivityResultLaunchers
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imageUri = result.data?.data
                binding.profileIv.setImageURI(imageUri)
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                binding.profileIv.setImageURI(imageUri)
            }
        }


        // Handle UI clicks
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Cách mới để quay lại
        }

        binding.profileIv.setOnClickListener {
            showImagePickDialog()
        }

        binding.gpsBtn.setOnClickListener {
            if (checkLocationPermission()) {
                detectLocation()
            } else {
                requestLocationPermission()
            }
        }

        binding.updateBtn.setOnClickListener {
            inputData()
        }
    }

    private fun inputData() {
        val name = binding.nameEt.text.toString().trim()
        val phone = binding.phoneEt.text.toString().trim()
        val country = binding.countryEt.text.toString().trim()
        val state = binding.stateEt.text.toString().trim()
        val city = binding.cityEt.text.toString().trim()
        val address = binding.addressEt.text.toString().trim()

        updateProfile(name, phone, country, state, city, address)
    }

    private fun updateProfile(name: String, phone: String, country: String, state: String, city: String, address: String) {
        progressDialog.setMessage("Cập nhật hồ sơ...")
        progressDialog.show()

        val uid = firebaseAuth.uid ?: run {
            progressDialog.dismiss()
            Toast.makeText(this, "Người dùng chưa đăng nhập.", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            // Update without image
            val hashMap = HashMap<String, Any>()
            hashMap["name"] = name
            hashMap["phone"] = phone
            hashMap["country"] = country
            hashMap["state"] = state
            hashMap["city"] = city
            hashMap["address"] = address
            hashMap["latitude"] = latitude
            hashMap["longitude"] = longitude

            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(uid).updateChildren(hashMap)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Hồ sơ cá nhân đã cập nhật...", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e: Exception ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Cập nhật hồ sơ thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Update with image
            val filePathAndName = "profile_images/$uid"
            val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
            storageReference.putFile(imageUri!!) // imageUri đã được đảm bảo không null
                .addOnSuccessListener { taskSnapshot ->
                    val uriTask = taskSnapshot.storage.downloadUrl
                    uriTask.addOnSuccessListener { downloadUri ->
                        val hashMap = HashMap<String, Any>()
                        hashMap["name"] = name
                        hashMap["phone"] = phone
                        hashMap["country"] = country
                        hashMap["state"] = state
                        hashMap["city"] = city
                        hashMap["address"] = address
                        hashMap["latitude"] = latitude
                        hashMap["longitude"] = longitude
                        hashMap["profileImage"] = downloadUri.toString() // Lưu URL dưới dạng String

                        val ref = FirebaseDatabase.getInstance().getReference("Users")
                        ref.child(uid).updateChildren(hashMap)
                            .addOnSuccessListener {
                                progressDialog.dismiss()
                                Toast.makeText(this, "Hồ sơ cá nhân đã cập nhật...", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e: Exception ->
                                progressDialog.dismiss()
                                Toast.makeText(this, "Cập nhật hồ sơ thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    uriTask.addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Toast.makeText(this, "Không thể lấy URL ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e: Exception ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Tải ảnh lên thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkUser() {
        val user = firebaseAuth.currentUser
        if (user == null) {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        } else {
            loadMyInfo()
        }
    }

    private fun loadMyInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!) // Giả định uid không null vì đã kiểm tra ở checkUser
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Lấy dữ liệu an toàn
                    val accountType = dataSnapshot.child("accountType").value?.toString() ?: ""
                    val address = dataSnapshot.child("address").value?.toString() ?: ""
                    val city = dataSnapshot.child("city").value?.toString() ?: ""
                    val state = dataSnapshot.child("state").value?.toString() ?: ""
                    val country = dataSnapshot.child("country").value?.toString() ?: ""
                    val email = dataSnapshot.child("email").value?.toString() ?: ""
                    latitude = dataSnapshot.child("latitude").value?.toString()?.toDoubleOrNull() ?: 0.0
                    longitude = dataSnapshot.child("longitude").value?.toString()?.toDoubleOrNull() ?: 0.0
                    val name = dataSnapshot.child("name").value?.toString() ?: ""
                    val phone = dataSnapshot.child("phone").value?.toString() ?: ""
                    val profileImage = dataSnapshot.child("profileImage").value?.toString() ?: ""

                    // Set data to views
                    binding.nameEt.setText(name)
                    binding.phoneEt.setText(phone)
                    binding.countryEt.setText(country)
                    binding.stateEt.setText(state)
                    binding.cityEt.setText(city)
                    binding.addressEt.setText(address)

                    try {
                        Picasso.get().load(profileImage)
                            .placeholder(R.drawable.user) // Placeholder nếu ảnh đang tải
                            .error(R.drawable.user)       // Ảnh lỗi nếu không tải được
                            .into(binding.profileIv)
                    } catch (e: Exception) {
                        binding.profileIv.setImageResource(R.drawable.user) // Fallback nếu có lỗi Picasso
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@ProfileEditUserActivity, "Lỗi tải thông tin: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showImagePickDialog() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(this)
            .setTitle("Chọn ảnh từ:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Camera clicked
                        if (checkCameraPermission()) {
                            pickFromCamera()
                        } else {
                            requestCameraPermission()
                        }
                    }
                    1 -> { // Gallery clicked
                        if (checkStoragePermission()) {
                            pickFromGallery()
                        } else {
                            requestStoragePermission()
                        }
                    }
                }
            }
            .show()
    }

    // --- Permission Checks ---
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission(): Boolean {
        val cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        return cameraResult && storageResult
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    // --- Permission Requests ---
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_REQUEST_CODE)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE)
    }

    // --- Image Pick ---
    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent) // Use launcher
    }

    private fun pickFromCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Image Title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Image Description")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        cameraLauncher.launch(imageUri) // Use launcher
    }

    // --- Location Detection ---
    private fun detectLocation() {
        Toast.makeText(this, "Đang phát hiện vị trí...", Toast.LENGTH_SHORT).show()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Kiểm tra xem GPS có được bật không
        if (!locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Vui lòng bật GPS trên thiết bị của bạn.", Toast.LENGTH_LONG).show()
            // Tùy chọn: Mở cài đặt vị trí
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            return
        }

        // Kiểm tra quyền lại một lần nữa trước khi yêu cầu cập nhật
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Trường hợp này không nên xảy ra nếu checkLocationPermission đã được gọi trước đó,
            // nhưng là một biện pháp an toàn.
            requestLocationPermission()
            return
        }
        locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
    }

    private fun findAddress() {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0].getAddressLine(0) ?: ""
                val city = addresses[0].locality ?: ""
                val state = addresses[0].adminArea ?: ""
                val country = addresses[0].countryName ?: ""

                binding.countryEt.setText(country)
                binding.stateEt.setText(state)
                binding.cityEt.setText(city)
                binding.addressEt.setText(address)
            } else {
                Toast.makeText(this, "Không tìm thấy địa chỉ cho vị trí này.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi tìm kiếm địa chỉ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Location Listener Callbacks ---
    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        findAddress()
        // Ngừng cập nhật vị trí sau khi đã tìm thấy để tiết kiệm pin
        locationManager?.removeUpdates(this)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {
        Toast.makeText(this, "Vui lòng bật vị trí...", Toast.LENGTH_SHORT).show()
    }

    // --- Permission Request Results ---
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    detectLocation()
                } else {
                    showPermissionDeniedDialog("Quyền Vị trí", Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            CAMERA_REQUEST_CODE -> {
                // Kiểm tra tất cả các quyền trong mảng cameraPermissions
                var allGranted = true
                for (i in grantResults.indices) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                        break
                    }
                }
                if (allGranted) {
                    pickFromCamera()
                } else {
                    showPermissionDeniedDialog("Quyền Máy ảnh và Lưu trữ", cameraPermissions[0]) // Chỉ cần một quyền trong mảng để kiểm tra rationale
                }
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery()
                } else {
                    showPermissionDeniedDialog("Quyền Lưu trữ", storagePermissions[0])
                }
            }
        }
    }

    // Hàm chung để hiển thị dialog khi quyền bị từ chối
    private fun showPermissionDeniedDialog(permissionName: String, permissionManifest: String) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionManifest)) {
            // Người dùng từ chối nhưng chưa chọn "Don't ask again"
            AlertDialog.Builder(this)
                .setTitle("Quyền $permissionName Cần Thiết")
                .setMessage("Để sử dụng tính năng này, bạn cần cấp quyền $permissionName. Vui lòng cấp quyền.")
                .setPositiveButton("Cấp quyền") { dialog, which ->
                    // Yêu cầu lại quyền tương ứng
                    when (permissionManifest) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> requestLocationPermission()
                        Manifest.permission.CAMERA -> requestCameraPermission()
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_IMAGES -> requestStoragePermission()
                    }
                }
                .setNegativeButton("Hủy") { dialog, which ->
                    Toast.makeText(this, "Quyền $permissionName bị từ chối.", Toast.LENGTH_SHORT).show()
                }
                .show()
        } else {
            // Người dùng từ chối và chọn "Don't ask again"
            Toast.makeText(this, "Quyền $permissionName bị từ chối vĩnh viễn. Vui lòng cấp quyền trong Cài đặt ứng dụng.", Toast.LENGTH_LONG).show()
            // Tùy chọn: Mở cài đặt ứng dụng
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }


    // --- Constants ---
    companion object {
        //permission constants
        private const val LOCATION_REQUEST_CODE = 100
        private const val CAMERA_REQUEST_CODE = 200
        private const val STORAGE_REQUEST_CODE = 300
    }

    override fun onDestroy() {
        super.onDestroy()
        // Quan trọng: Ngừng cập nhật vị trí khi Activity bị hủy để tránh rò rỉ bộ nhớ
        locationManager?.removeUpdates(this)
    }
}