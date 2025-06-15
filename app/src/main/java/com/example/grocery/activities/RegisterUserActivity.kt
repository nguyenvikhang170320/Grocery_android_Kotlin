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
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.grocery.R
import com.example.grocery.databinding.ActivityRegisterUserBinding // Import View Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class RegisterUserActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityRegisterUserBinding // Khai báo View Binding

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
        binding = ActivityRegisterUserBinding.inflate(layoutInflater) // Khởi tạo View Binding
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

        binding.gpsBtn.setOnClickListener {
            // Detect current location
            if (checkLocationPermission()) {
                detectLocation()
            } else {
                requestLocationPermission()
            }
        }

        binding.profileIv.setOnClickListener {
            // Pick image
            showImagePickDialog()
        }

        binding.registerBtn.setOnClickListener {
            // Register user
            inputData()
        }

        binding.registerSellerTv.setOnClickListener {
            // Open register seller activity
            startActivity(Intent(this@RegisterUserActivity, RegisterSellerActivity::class.java))
        }
    }

    private var fullName: String = ""
    private var phoneNumber: String = ""
    private var country: String = ""
    private var state: String = ""
    private var city: String = ""
    private var address: String = ""
    private var email: String = ""
    private var password: String = ""

    private fun inputData() {
        // Input data
        fullName = binding.nameEt.text.toString().trim()
        phoneNumber = binding.phoneEt.text.toString().trim()
        country = binding.countryEt.text.toString().trim()
        state = binding.stateEt.text.toString().trim()
        city = binding.cityEt.text.toString().trim()
        address = binding.addressEt.text.toString().trim()
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        val confirmPassword = binding.cPasswordEt.text.toString().trim()

        // Validate data
        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Nhập tên...", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Nhập số điện thoại...", Toast.LENGTH_SHORT).show()
            return
        }
        if (latitude == 0.0 || longitude == 0.0) {
            Toast.makeText(
                this,
                "Vui lòng nhấp vào nút GPS để phát hiện vị trí...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Mẫu email không hợp lệ...", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Mật khẩu phải có tối thiểu 6 ký tự...", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Mật khẩu không khớp...", Toast.LENGTH_SHORT).show()
            return
        }
        createAccount()
    }

    private fun createAccount() {
        progressDialog.setMessage("Tạo tài khoản...")
        progressDialog.show()

        // Create account
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // Account created, now save Firebase data
                saveFirebaseData()
            }
            .addOnFailureListener { e: Exception ->
                // Failed creating account
                progressDialog.dismiss()
                Toast.makeText(this, "Tạo tài khoản thất bại: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun saveFirebaseData() {
        progressDialog.setMessage("Lưu thông tin tài khoản...")
        val timestamp = System.currentTimeMillis() // Lưu timestamp dưới dạng Long

        val uid = firebaseAuth.uid ?: run {
            progressDialog.dismiss()
            Toast.makeText(this, "Lỗi: UID người dùng không tồn tại.", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup data to save
        val hashMap = HashMap<String, Any>()
        hashMap["uid"] = uid
        hashMap["email"] = email
        hashMap["name"] = fullName
        hashMap["phone"] = phoneNumber
        hashMap["country"] = country
        hashMap["state"] = state
        hashMap["city"] = city
        hashMap["address"] = address
        hashMap["latitude"] = latitude // Đã là Double
        hashMap["longitude"] = longitude // Đã là Double
        hashMap["timestamp"] = timestamp // Đã là Long
        hashMap["accountType"] = "User"
        hashMap["online"] = true // Lưu dưới dạng Boolean

        if (imageUri == null) {
            // Save info without image
            hashMap["profileImage"] = ""

            // Save to db
            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(uid).setValue(hashMap)
                .addOnSuccessListener {
                    // DB updated
                    Log.d(TAG, "saverFirebaseData: Thành công khi đăng ký không up ảnh")
                    progressDialog.dismiss()
                    startActivity(Intent(this@RegisterUserActivity, MainUserActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e: Exception ->
                    // Failed updating db
                    progressDialog.dismiss()
                    Toast.makeText(this, "Lưu thông tin thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Vẫn chuyển hướng để người dùng có thể thử lại hoặc đăng nhập
                    startActivity(Intent(this@RegisterUserActivity, MainUserActivity::class.java))
                    finish()
                }
        } else {
            // Save info with image

            // Name and path of image
            val filePathAndName = "profile_images/$uid"
            // Upload image
            val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
            storageReference.putFile(imageUri!!) // imageUri đã được đảm bảo không null
                .addOnSuccessListener { taskSnapshot ->
                    // Get url of uploaded image
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        Log.d(TAG, "saverFirebaseData: Upload cơ sở dữ liệu")
                        hashMap["profileImage"] = downloadUri.toString() // URL of uploaded image

                        // Save to db
                        val ref = FirebaseDatabase.getInstance().getReference("Users")
                        ref.child(uid).setValue(hashMap)
                            .addOnSuccessListener {
                                // DB updated
                                progressDialog.dismiss()
                                Log.d(TAG, "saverFirebaseData: Tạo tài khoản người dùng thành công")
                                Toast.makeText(
                                    this,
                                    "Tạo tài khoản người dùng thành công",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startActivity(Intent(this@RegisterUserActivity, MainUserActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e: Exception ->
                                // Failed updating db
                                progressDialog.dismiss()
                                Toast.makeText(this, "Thất bại: ${e.message}", Toast.LENGTH_SHORT)
                                    .show()
                                startActivity(Intent(this@RegisterUserActivity, MainUserActivity::class.java))
                                finish()
                            }
                    }.addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Toast.makeText(this, "Không thể lấy URL ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterUserActivity, MainUserActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener { e: Exception ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Tải ảnh lên thất bại: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    private fun showImagePickDialog() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(this)
            .setTitle("Pick Image")
            .setItems(options) { _, which ->
                // Handle clicks
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

    // --- Image Pick ---
    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private fun pickFromCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image Title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Image Description")
        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        cameraLauncher.launch(imageUri)
    }

    // --- Location Detection ---
    private fun detectLocation() {
        Toast.makeText(this, "Đang phát hiện vị trí...", Toast.LENGTH_LONG).show()
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
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission() // Yêu cầu quyền nếu chưa có
            return
        }
        // Nếu quyền đã có và GPS đã bật, yêu cầu cập nhật vị trí
        locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
    }

    private fun findAddress() {
        // Find address, country, state, city
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0].getAddressLine(0) ?: "" // Complete address
                val city = addresses[0].locality ?: ""
                val state = addresses[0].adminArea ?: ""
                val country = addresses[0].countryName ?: ""

                // Set addresses
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

    // --- Permission Checks ---
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
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

    // --- Permission Requests ---
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_REQUEST_CODE)
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)
    }

    // --- Location Listener Callbacks ---
    override fun onLocationChanged(location: Location) {
        // Location detected
        latitude = location.latitude
        longitude = location.longitude
        findAddress()
        // Ngừng cập nhật vị trí sau khi đã tìm thấy để tiết kiệm pin
        locationManager?.removeUpdates(this)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        // Deprecated in API 29, but still need to override for older APIs
    }
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {
        // GPS/location disabled
        Toast.makeText(this, "Vui lòng bật vị trí trên điện thoại...", Toast.LENGTH_SHORT).show()
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
                    showPermissionDeniedDialog("Vị trí", Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            CAMERA_REQUEST_CODE -> {
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
                    showPermissionDeniedDialog("Máy ảnh và Lưu trữ", cameraPermissions[0])
                }
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery()
                } else {
                    showPermissionDeniedDialog("Lưu trữ", storagePermissions[0])
                }
            }
        }
    }

    // Hàm chung để hiển thị dialog khi quyền bị từ chối
    private fun showPermissionDeniedDialog(permissionName: String, permissionManifest: String) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissionManifest)) {
            AlertDialog.Builder(this)
                .setTitle("Quyền $permissionName Cần Thiết")
                .setMessage("Để sử dụng tính năng này, bạn cần cấp quyền $permissionName. Vui lòng cấp quyền.")
                .setPositiveButton("Cấp quyền") { _, _ ->
                    when (permissionManifest) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> requestLocationPermission()
                        Manifest.permission.CAMERA -> requestCameraPermission()
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_IMAGES -> requestStoragePermission()
                    }
                }
                .setNegativeButton("Hủy") { _, _ ->
                    Toast.makeText(this, "Quyền $permissionName bị từ chối.", Toast.LENGTH_SHORT).show()
                }
                .show()
        } else {
            Toast.makeText(this, "Quyền $permissionName bị từ chối vĩnh viễn. Vui lòng cấp quyền trong Cài đặt ứng dụng.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    // --- Constants ---
    companion object {
        private const val LOCATION_REQUEST_CODE = 100
        private const val CAMERA_REQUEST_CODE = 200
        private const val STORAGE_REQUEST_CODE = 300
        private const val TAG = "DANGKY"
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(this)
    }
}