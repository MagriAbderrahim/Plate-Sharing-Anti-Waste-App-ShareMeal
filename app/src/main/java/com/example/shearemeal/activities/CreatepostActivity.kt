package com.example.shearemeal.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.example.shearemeal.databinding.ActivityCreatepostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreatepostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatepostBinding
    private var imageUri: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var progressDialog: ProgressDialog
    private lateinit var address: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatepostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Enregistrement de la publication...")
        progressDialog.setCanceledOnTouchOutside(false)

        sharedPreferences = getSharedPreferences("CreatePostPrefs", Context.MODE_PRIVATE)

        val categories = arrayOf("Boissons", "Plats", "Nourriture")
        val categorieEditText: AutoCompleteTextView = findViewById(R.id.CategorieEditText)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categorieEditText.setAdapter(adapter)
        categorieEditText.setText(categories[0], false)

        // Charger les données stockées dans SharedPreferences
        loadSharedPreferencesData()

        binding.btnbackhome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.locationInputLayout.setOnClickListener {
            storeDataInSharedPreferences()
            startActivity(Intent(this, LocationPickerActivity::class.java))
        }

        binding.btnaddphoto.setOnClickListener {
            storeDataInSharedPreferences()
            imagePickDialog()
        }
        address = intent.getStringExtra("address").toString()
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        // Afficher l'adresse dans le TextView locationTv
        if (address == null){
            binding.locationTv.text = "Choisir votre Localisation"
        }else{
            binding.locationTv.text = "$address"
        }


        binding.btnsubmit.setOnClickListener {
            val title = binding.titleEditText.text.toString().trim()
            val description = binding.descriptionEditText.text.toString().trim()
            val location = binding.locationTv.text.toString().trim()
            val category = categorieEditText.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || location.isEmpty() || category.isEmpty() || imageUri == null) {
                Toast.makeText(this, "Veuillez remplir tous les champs et ajouter une image.", Toast.LENGTH_SHORT).show()
            } else {
                progressDialog.show()
                uploadPost(title, description, location, category)
            }
        }
    }

    private fun storeDataInSharedPreferences() {
        val editor = sharedPreferences.edit()
        editor.putString("title", binding.titleEditText.text.toString())
        editor.putString("description", binding.descriptionEditText.text.toString())
        editor.putString("category", binding.CategorieEditText.text.toString())
        editor.putString("imageUri", imageUri?.toString())
        editor.apply()
    }

    private fun loadSharedPreferencesData() {
        val title = sharedPreferences.getString("title", "")
        val description = sharedPreferences.getString("description", "")
        val category = sharedPreferences.getString("category", "")
        val imageUriString = sharedPreferences.getString("imageUri", null)

        binding.titleEditText.setText(title)
        binding.descriptionEditText.setText(description)
        binding.CategorieEditText.setText(category, false)

        if (imageUriString != null) {
            imageUri = Uri.parse(imageUriString)
            Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.baseline_add_a_photo_24)
                .into(binding.uploadedImageView)
        }
    }
    // Image Selection Dialog
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

    // Upload Post to Firebase Realtime Database
    private fun uploadPost(title: String, description: String, location: String, category: String) {
        if (title.isEmpty() || description.isEmpty() || location.isEmpty() || category.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Veuillez remplir tous les champs et ajouter une image.", Toast.LENGTH_SHORT).show()
            return
        }
        progressDialog.show()

        // Obtenir le fichier image à partir de l'URI
        val imageRef = storageReference.child("post_images/${System.currentTimeMillis()}")
        val uploadTask = imageRef.putFile(imageUri!!)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Obtenir l'URL de téléchargement de l'image
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                val currentDate = sdf.format(Date())
                // Créer un objet post
                val postId = database.reference.child("posts").push().key // Générer une clé unique pour la publication
                val post = hashMapOf(
                    "postId" to postId,
                    "title" to title,
                    "description" to description,
                    "location" to address,
                    "longitude" to longitude,
                    "latitude" to latitude,
                    "category" to category,
                    "imageUrl" to uri.toString(),
                    "userId" to auth.uid, // Remplacer par l'ID utilisateur réel de Firebase Auth
                    "datePublication" to currentDate
                )

                // Ajouter la publication à la base de données Realtime
                database.reference.child("posts").child(postId!!).setValue(post) // Utiliser la clé générée
                    .addOnSuccessListener {
                        Log.d(ContentValues.TAG, "Post uploaded successfully with ID: $postId")
                        Toast.makeText(this, "Publication ajoutée avec succès!", Toast.LENGTH_SHORT).show()
                        // Rediriger vers l'activité principale ou actualiser la liste des publications
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                        // Fermer le ProgressDialog
                        progressDialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Log.w(ContentValues.TAG, "Error uploading post", e)
                        Toast.makeText(this, "Erreur lors de l'ajout de la publication.", Toast.LENGTH_SHORT).show()
                        // Fermer le ProgressDialog
                        progressDialog.dismiss()
                    }
            }
        }
            .addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error uploading image", e)
                Toast.makeText(this, "Erreur lors du téléchargement de l'image.", Toast.LENGTH_SHORT).show()
                // Fermer le ProgressDialog
                progressDialog.dismiss()
            }
    }
    override fun onDestroy() {
        super.onDestroy()
        clearSharedPreferences()
    }

    private fun clearSharedPreferences() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}