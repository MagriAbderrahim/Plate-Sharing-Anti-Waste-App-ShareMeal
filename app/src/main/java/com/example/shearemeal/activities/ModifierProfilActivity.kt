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
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.shearemeal.R
import com.example.shearemeal.databinding.ActivityModifierProfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class ModifierProfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModifierProfilBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dialogueProgress: ProgressDialog
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModifierProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialogueProgress = ProgressDialog(this)
        dialogueProgress.setTitle("Modification .....")
        dialogueProgress.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        loadInfos()

        binding.btnbackeditprofil.setOnClickListener {
            onBackPressed()
        }
        binding.profileImagePickFab.setOnClickListener {
            imagePickDialog()
        }
        binding.editButton.setOnClickListener {
            validateData()
        }
    }

    private var phoneNumber = ""
    private var fullName = ""

    private fun validateData() {
        phoneNumber = binding.profileditphonenumberfieldTET.text.toString().trim()
        fullName = binding.profileditnomcompletfieldTET.text.toString().trim()

        if (imageUri == null) {
            updateProfileDb(null)
        } else {
            uploadProfileImageStorage()
        }
    }

    private fun uploadProfileImageStorage() {
        dialogueProgress.setMessage("Chargement de l'image de profil")
        dialogueProgress.show()

        val filePathAndName = "UserProfile/profile_${firebaseAuth.uid}"
        val ref = FirebaseStorage.getInstance().reference.child(filePathAndName)

        ref.putFile(imageUri!!)
            .addOnProgressListener { snapshot ->
                val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                dialogueProgress.setMessage("Chargement de l'image de profil. Progression: $progress%")
            }
            .addOnSuccessListener { taskSnapshot ->
                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);

                val uploadedImageUrl = uriTask.result.toString()
                if (uriTask.isSuccessful) {
                    updateProfileDb(uploadedImageUrl)
                }
            }
            .addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "uploadProfileImageStorage: Échec du téléchargement de l'image: $e")
                dialogueProgress.dismiss()
                Toast.makeText(this, "Échec du téléchargement de l'image : ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfileDb(uploadedImageUrl: String?) {
        dialogueProgress.setMessage("Mise à jour des informations de l'utilisateur")
        dialogueProgress.show()

        val hashMap = HashMap<String, Any>()
        hashMap["nom_utilisateur"] = fullName
        hashMap["numero_de_tele"] = phoneNumber
        if (uploadedImageUrl != null) {
            hashMap["profile_image_utilisateur"] = uploadedImageUrl
        }

        val reference = FirebaseDatabase.getInstance().getReference("Utilisateurs")
        reference.child(firebaseAuth.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                Log.d(ContentValues.TAG, "updateProfileDb: Profil mis à jour avec succès.")
                dialogueProgress.dismiss()
                Toast.makeText(this, "Mis à jour avec succès.", Toast.LENGTH_SHORT).show()
                imageUri = null
            }
            .addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "updateProfileDb: Échec de la mise à jour du profil: $e")
                dialogueProgress.dismiss()
                Toast.makeText(this, "Échec de la mise à jour : ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadInfos() {
        val ref = FirebaseDatabase.getInstance().getReference("Utilisateurs")
        ref.child(firebaseAuth.uid!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nom_complet = "${snapshot.child("nom_utilisateur").value}"
                val mail = "${snapshot.child("email_utilisateur").value}"
                val phone = "${snapshot.child("numero_de_tele").value}"
                val profileimgurl = "${snapshot.child("profile_image_utilisateur").value}"
                binding.profileditemailfieldTET.setText(mail)
                binding.profileditnomcompletfieldTET.setText(nom_complet)
                binding.profileditphonenumberfieldTET.setText(phone)

                try {
                    Glide.with(this@ModifierProfilActivity)
                        .load(profileimgurl)
                        .placeholder(R.drawable.ic_person)
                        .into(binding.profileIv)
                } catch (e: Exception) {
                    Log.e(ContentValues.TAG, "Erreur lors du chargement de l'image: $e")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(ContentValues.TAG, "Erreur lors du chargement des informations utilisateur: $error")
            }
        })
    }

    private fun imagePickDialog() {
        val popmenu = PopupMenu(this, binding.profileImagePickFab)
        popmenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popmenu.menu.add(Menu.NONE, 2, 2, "Gallery")
        popmenu.show()
        popmenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    Log.d(ContentValues.TAG, "imagePickDialog: Camera Clicked")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pickImageCamera()
                    } else {
                        requestCameraPermissions.launch(
                            arrayOf(
                                android.Manifest.permission.CAMERA,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    }
                    true
                }
                2 -> {
                    Log.d(ContentValues.TAG, "imagePickDialog: Gallery Clicked")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pickImageGallery()
                    } else {
                        requestStoragePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun pickImageCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_image_title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_image_description")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

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
                        .placeholder(R.drawable.ic_person)
                        .into(binding.profileIv)
                } catch (e: Exception) {
                    Log.e(ContentValues.TAG, "Erreur lors du chargement de l'image: $e")
                }
            } else {
                Toast.makeText(this, "Annulé !", Toast.LENGTH_SHORT).show()
            }
        }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
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
                        .placeholder(R.drawable.ic_person)
                        .into(binding.profileIv)
                } catch (e: Exception) {
                    Log.e(ContentValues.TAG, "Erreur lors du chargement de l'image: $e")
                }
            }
        }
}
