package com.example.shearemeal.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu


import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.PopupMenu

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.shearemeal.R
import com.example.shearemeal.databinding.ActivityEditPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class EditPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPostBinding
    private var imageUri: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var progressDialog: ProgressDialog
    private lateinit var address: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var postId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Mise à jour de la publication...")
        progressDialog.setCanceledOnTouchOutside(false)


        val categories = arrayOf("Boissons", "Plats", "Nourriture")
        val categorieEditText: AutoCompleteTextView = findViewById(R.id.CategorieEditText)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categorieEditText.setAdapter(adapter)

        val imageUriString = intent.getStringExtra("imageUri")
        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")
        val category = intent.getStringExtra("category")

        // Mettre à jour les champs correspondants avec les données récupérées
        binding.titleEditText.setText(title)
        binding.descriptionEditText.setText(description)
        categorieEditText.setText(category,false)
        // Mettre à jour l'URI de l'image si elle est disponible
        imageUriString?.let {
            imageUri = Uri.parse(it)
            Glide.with(this).load(imageUri).into(binding.uploadedImageView)
        }
        // Get the postId from the intent
        postId = intent.getStringExtra("postId")
        loadPostData()

        address = intent.getStringExtra("addressedit").toString()
        latitude = intent.getDoubleExtra("latitudeedit", 0.0)
        longitude = intent.getDoubleExtra("longitudeedit", 0.0)
        binding.locationTv.text = address

        binding.btnbackhome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.locationInputLayout.setOnClickListener {
            val intent = Intent(this, LocationmodifyActivity::class.java)
            // Ajouter les données actuelles à l'intention
            intent.putExtra("imageUri", imageUri?.toString())
            intent.putExtra("title", binding.titleEditText.text.toString())
            intent.putExtra("description", binding.descriptionEditText.text.toString())
            intent.putExtra("category", categorieEditText.text.toString())
            startActivity(intent)
        }

        binding.btnaddphoto.setOnClickListener {
            imagePickDialog()
        }

        binding.btnsubmit.setOnClickListener {
            val title = binding.titleEditText.text.toString().trim()
            val description = binding.descriptionEditText.text.toString().trim()
            val location = binding.locationTv.text.toString().trim()
            val category = categorieEditText.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || location.isEmpty() || category.isEmpty() || imageUri == null) {
                Toast.makeText(
                    this,
                    "Veuillez remplir tous les champs et ajouter une image.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                progressDialog.show()
                modifierpost(title, description, location, category)
            }
        }
    }
    private fun imagePickDialog() {
        val popmenu = PopupMenu(this, binding.btnaddphoto)
        popmenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popmenu.menu.add(Menu.NONE, 2, 2, "Gallery")
        popmenu.show()
        popmenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> pickImageCamera()
                2 -> pickImageGallery()
            }
            true
        }
    }

    // Pick Image from Camera
    private fun pickImageCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imageUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.Images.Media.TITLE, "Temp_image_title")
            contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_image_description")
            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    // Pick Image from Gallery
    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    // Result Launchers for Camera and Gallery
    private val requestCameraPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            var areAllGranted = true
            for (isGranted in result.values) {
                areAllGranted = areAllGranted && isGranted
            }
            if (areAllGranted) {
                pickImageCamera()
            } else {
                Log.d(ContentValues.TAG, "requestCameraPermissions: Au moins une des permissions est refusée")
                Toast.makeText(this, "Les permissions de la caméra ou du stockage ont été refusées", Toast.LENGTH_LONG).show()
            }
        }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pickImageGallery()
            } else {
                Log.d(ContentValues.TAG, "requestStoragePermission: Permission de stockage refusée")
                Toast.makeText(this, "La permission de stockage a été refusée", Toast.LENGTH_LONG).show()
            }
        }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.baseline_add_a_photo_24)
                        .into(binding.uploadedImageView)
                } catch (e: Exception) {
                    Log.e(ContentValues.TAG, "Erreur lors du chargement de l'image: $e")
                }
            } else {
                Toast.makeText(this, "Annulé !", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                imageUri = data!!.data
                Log.d(ContentValues.TAG, "URI de l'image sélectionnée: $imageUri")
                try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.baseline_add_a_photo_24)
                        .into(binding.uploadedImageView)
                } catch (e: Exception) {
                    Log.e(ContentValues.TAG, "Erreur lors du chargement de l'image: $e")
                }
            }
        }
    private fun modifierpost(title: String, description: String, location: String, category: String) {
        if (imageUri == null) {
            // Mettre à jour le post sans changer l'image

            val postUpdates = hashMapOf<String, Any>(
                "title" to title,
                "description" to description,
                "location" to location,
                "latitude" to latitude,
                "longitude" to longitude,
                "category" to category
            )

            // Mettre à jour les données du post dans la base de données Firebase
            postId?.let { postId ->
                database.reference.child("posts").child(postId).updateChildren(postUpdates)
                    .addOnSuccessListener {
                        Log.d("EditPostActivity", "Post updated successfully with ID: $postId")
                        Toast.makeText(this, "Publication mise à jour avec succès!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                        progressDialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditPostActivity", "Error updating post", e)
                        Toast.makeText(this, "Erreur lors de la mise à jour de la publication.", Toast.LENGTH_SHORT).show()
                        progressDialog.dismiss()
                    }
            }
        } else {
            // Mettre à jour le post avec un changement d'image

            val imageRef = storageReference.child("post_images/${System.currentTimeMillis()}")
            val uploadTask = imageRef.putFile(imageUri!!)

            uploadTask.addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { imageUrl ->
                    val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                    val currentDate = sdf.format(Date())

                    val postUpdates = hashMapOf<String, Any>(
                        "title" to title,
                        "description" to description,
                        "location" to location,
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "category" to category,
                        "imageUrl" to imageUrl.toString() // Convertir l'URL de l'image en String
                    )

                    // Mettre à jour les données du post dans la base de données Firebase
                    postId?.let { postId ->
                        database.reference.child("posts").child(postId).updateChildren(postUpdates)
                            .addOnSuccessListener {
                                Log.d("EditPostActivity", "Post updated successfully with ID: $postId")
                                Toast.makeText(this, "Publication mise à jour avec succès!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                                progressDialog.dismiss()
                            }
                            .addOnFailureListener { e ->
                                Log.e("EditPostActivity", "Error updating post", e)
                                Toast.makeText(this, "Erreur lors de la mise à jour de la publication.", Toast.LENGTH_SHORT).show()
                                progressDialog.dismiss()
                            }
                    }
                }
            }
                .addOnFailureListener { e ->
                    Log.e("EditPostActivity", "Error uploading image", e)
                    Toast.makeText(this, "Erreur lors du téléchargement de l'image.", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
        }
    }


    private fun loadPostData() {
            postId?.let { postId ->
                val postRef = database.reference.child("posts").child(postId)
                postRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val title = snapshot.child("title").getValue(String::class.java) ?: ""
                            val description = snapshot.child("description").getValue(String::class.java) ?: ""
                            address = snapshot.child("location").getValue(String::class.java) ?: ""
                            latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                            longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                            val category = snapshot.child("category").getValue(String::class.java) ?: ""
                            val imageUrl = snapshot.child("imageUrl").getValue(String::class.java) ?: ""

                            // Remplissage des champs avec les données du post
                            binding.titleEditText.setText(title)
                            binding.descriptionEditText.setText(description)
                            binding.locationTv.text = address
                            binding.CategorieEditText.setText(category, false)

                            // Chargement et affichage de l'image à partir de l'URL
                            Glide.with(this@EditPostActivity)
                                .load(imageUrl)
                                .placeholder(R.drawable.baseline_add_a_photo_24) // Placeholder en attendant le chargement de l'image
                                .into(binding.uploadedImageView)

                            imageUri = Uri.parse(imageUrl) // Conversion de l'URL en Uri
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("EditPostActivity", "Error loading post data: ${error.message}")
                        // Gérer l'erreur de chargement des données du post
                    }
                })
            }
        }

    }




