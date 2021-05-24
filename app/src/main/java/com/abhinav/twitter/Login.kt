package com.abhinav.twitter

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_login.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class Login : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private  var database = FirebaseDatabase.getInstance()
    private var myRef = database.reference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Image.setOnClickListener(View.OnClickListener {
            //TODO:select image from phone
            checkPermission()
        })

        FirebaseMessaging.getInstance().subscribeToTopic("news")
    }


    fun LogintoFirebase(email: String, password: String){

        mAuth!!.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this){ task ->
            if(task.isSuccessful){
                Toast.makeText(applicationContext, "Successful Login", Toast.LENGTH_LONG).show()
                var currentUser = mAuth!!.currentUser
                saveImageInFirebase(currentUser!!)
            }
            else {
                Toast.makeText(applicationContext, "Login Failed", Toast.LENGTH_LONG).show()
            }
        }
    }
    var DownloadURL: String? = ""
    fun saveImageInFirebase(currentUser: FirebaseUser) {
        val storage = FirebaseStorage.getInstance()
        val email = currentUser.email.toString()
        val storageRef = storage.reference
        val df = SimpleDateFormat("ddMMyyHHmmss")
        val dataobj = Date()
        val imagePath = splitString(email) + "." + df.format(dataobj) + ".jpg"
        val ImageRef = storageRef.child("Images/" + imagePath)
        Image.isDrawingCacheEnabled = true
        Image.buildDrawingCache()
        val drawable = Image.drawable as BitmapDrawable
        val bitmap = drawable.bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val uploadTask = ImageRef.putBytes(data)
        uploadTask.addOnFailureListener {
            Toast.makeText(applicationContext, "Failed to Upload", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener() { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { url ->
                run {
                    DownloadURL = url.toString()
                    myRef.child("Users").child(currentUser.uid).child("ProfileImage").setValue(DownloadURL)
                    Log.d("Test", DownloadURL!!)
                }

            }
            myRef.child("Users").child(currentUser.uid).child("email").setValue(currentUser.email)

            loadTweets()
        }
    }

    fun splitString(email: String):String{
        val split=email.split("@")
        return split[0]

    }

    override fun onStart() {
        super.onStart()
        loadTweets()
    }

    fun loadTweets(){
        var currentUser = mAuth!!.currentUser
        if(currentUser!=null){
            var intent = Intent(this, MainActivity::class.java)
            intent.putExtra("email", currentUser.email)
            intent.putExtra("uid", currentUser.uid)
            startActivity(intent)


        }
    }

val ReadImage:Int = 23
    fun checkPermission(){
        if(Build.VERSION.SDK_INT>=23){
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)!=
                    PackageManager.PERMISSION_GRANTED){
                        requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), ReadImage)
                return
            }
        }
        loadImage()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            ReadImage -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadImage()
                } else {
                    Toast.makeText(this, "Cannot access storage", Toast.LENGTH_LONG).show()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    val PICK_IMAGE_CODE= 123
    fun loadImage(){
        //TODO:load image from phone
        var intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==PICK_IMAGE_CODE  && data!=null && resultCode == RESULT_OK){
            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(selectedImage!!, filePathColumn, null, null, null)
            cursor!!.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor.close()
            Image.setImageBitmap(BitmapFactory.decodeFile(picturePath))
        }
    }

    fun Login(view: View) {
        LogintoFirebase(Email.text.toString(), Password.text.toString())
    }
}