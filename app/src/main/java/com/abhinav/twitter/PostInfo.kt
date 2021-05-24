package com.abhinav.twitter

import android.util.Log

class PostInfo {
    var UserID:String?=null
    var Text:String?=null
    var PostImage:String?=null
    constructor(UserID:String,Text:String, PostImage:String){
        this.UserID=UserID
        this.Text=Text
        this.PostImage=PostImage
        Log.d("PostInfo","Executed")
    }

    override fun toString(): String {
      return "UserId : ${this.UserID}"

    }
}