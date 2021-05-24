package com.abhinav.twitter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewConfiguration.get
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_ticket.*
import kotlinx.android.synthetic.main.add_ticket.view.*
import kotlinx.android.synthetic.main.tweets.view.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.reflect.Array.get
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {
    private var database = FirebaseDatabase.getInstance()
    private var myRef = database.reference
    var ListTweets = ArrayList<Ticket>()
    var adapter: MyTweetAdpater? = null
    var myEmail: String? = null
    var userUID: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var b: Bundle = intent.extras!!
        myEmail = b.getString("email")
        userUID = b.getString("uid")
        //Dummy data
        ListTweets.add(Ticket("0", "Him", "url", "add"))

        adapter = MyTweetAdpater(this, ListTweets)
        listViewTweets.adapter = adapter
        LoadPost()

    }

    inner class MyTweetAdpater : BaseAdapter {
        var listNotesAdpater = ArrayList<Ticket>()
        var context: Context? = null

        constructor(context: Context, listNotesAdpater: ArrayList<Ticket>) : super() {
            this.listNotesAdpater = listNotesAdpater
            this.context = context
        }
        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            var mytweet = listNotesAdpater[p0]
            if (mytweet.tweetPersonUID.equals("add")) {
                var myView = layoutInflater.inflate(R.layout.add_ticket, null)
                //load add ticket
                myView.iv_attach.setOnClickListener(View.OnClickListener {
                    loadImage()

                })
                myView.iv_post.setOnClickListener(View.OnClickListener {
                    //upload to the server
                    Log.d("Post", "Successful")
                    Toast.makeText(applicationContext, "Uploaded", Toast.LENGTH_LONG).show()
                    myRef.child("posts").push().setValue(
                            PostInfo(userUID!!, myView.etPost!!.text.toString(),
                                    downloadURL!!))
                    myView.etPost.setText("")
                    Log.d("Post", "Successful2")
                })
                return myView
            }
            else if(mytweet.tweetPersonUID.equals("loading")){
                var myView = layoutInflater.inflate(R.layout.loading_ticket, null)
                return myView
            }else {
                var myView = layoutInflater.inflate(R.layout.tweets, null)
                //load tweet ticket
                myView.txt_tweet.setText(mytweet.tweetText)
                myView.txtUserName.setText(mytweet.tweetPersonUID)
                //myView.tweet_picture.setImageURI(mytweet.tweetImageURL)
                Picasso.with(context).load(mytweet.tweetImageURL).into( myView.tweet_picture)
                myRef.child("Users").child(mytweet.tweetPersonUID!!)
                        .addValueEventListener(object:ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                try{

                                    var td = snapshot!!.value as HashMap<String,Any>
                                    for(key in td.keys){
                                        var userInfo = td[key] as String
                                        if(key.equals("email")) {
                                            myView.txtUserName.setText(userInfo)
                                        }
                                    }
                                }catch(ex:Exception){

                                }

                            }

                            override fun onCancelled(error: DatabaseError) {

                            }

                        })
                return myView
            }
        }

        override fun getItem(p0: Int): Any {
            return listNotesAdpater[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {
            return listNotesAdpater.size
        }
    }

    val PICK_IMAGE_CODE = 123
    fun loadImage() {
        //TODO:load image from phone
        var intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_CODE && data != null && resultCode == RESULT_OK) {
            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(selectedImage!!, filePathColumn, null, null, null)
            cursor!!.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor.close()
            UploadImage(BitmapFactory.decodeFile(picturePath))
        }
    }

    var downloadURL: String? = ""
    fun UploadImage(bitmap: Bitmap) {
        ListTweets.add(0,Ticket("0", "Him", "url","loading"))
        adapter!!.notifyDataSetChanged()
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val df = SimpleDateFormat("ddMMyyHHmmss")
        val dataobj = Date()
        val imagePath = splitString(myEmail!!) + "." + df.format(dataobj) + ".jpg"
        val ImageRef = storageRef.child("ImagesPost/" + imagePath)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val uploadTask = ImageRef.putBytes(data)
        uploadTask.addOnFailureListener {
            Toast.makeText(applicationContext, "Failed to Upload", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener() { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { url ->
                downloadURL = url.toString()
            }
            ListTweets.removeAt(0)
            adapter!!.notifyDataSetChanged()

        }
    }

    fun splitString(email: String): String {
        val split = email.split("@")
        return split[0]

    }

    fun LoadPost(){
        myRef.child("posts")
                .addValueEventListener(object:ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try{
                            ListTweets.clear()
                            ListTweets.add(Ticket("0", "Him", "url", "add"))


                            var td = snapshot!!.value as HashMap<String,Any>
                            for(i in snapshot.children){
                                Log.d("Snapshot" ,i.toString())
                                Log.d("TD" ,td.toString())
                            }
                            for(key in td.keys){
                                var post = td[key] as HashMap<String,Any>
                                ListTweets.add(Ticket(key, post["text"] as String, post["postImage"] as String, post["userID"] as String))
                            }
                            adapter!!.notifyDataSetChanged()
                        }catch(ex:Exception){

                        }

                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
    }
}