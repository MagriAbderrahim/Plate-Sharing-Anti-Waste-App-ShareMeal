package com.example.shearemeal.models

data class Post(
    var postId: String = "",
    var category: String = "",
    var datePublication: String = "",
    var description: String = "",
    var imageUrl: String = "",
    var latitude: Double = 0.0,
    var location: String = "",
    var longitude: Double = 0.0,
    var title: String = "",
    var userId: String = ""
)

