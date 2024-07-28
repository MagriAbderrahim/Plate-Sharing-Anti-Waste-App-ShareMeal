package com.example.shearemeal.activities

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.shearemeal.R
import com.example.shearemeal.databinding.ActivityDetailsPostBinding // Importez la classe de liaison générée
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DetailsPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsPostBinding
    // Déclarez une variable pour la classe de liaison
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsPostBinding.inflate(layoutInflater) // Initialisez la classe de liaison
        setContentView(binding.root) // Utilisez la racine de la vue liée comme contenu de l'activité
        var phone=""
        val postId = intent.getStringExtra("postId")
        val imagePost = intent.getStringExtra("imagepost")
        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")
        val datePublication = intent.getStringExtra("datePublication")
        val category = intent.getStringExtra("category")

        binding.titleTv.text = title
        binding.descriptionTv.text = description
        binding.datepub.text = datePublication
        binding.CategorieTv.text = category

        Glide.with(this).load(imagePost).into(binding.imageSliderVp)
        binding.toolbarbackBtn.setOnClickListener {
            onBackPressed()
        }
        var idduser :String? = ""

        val database = FirebaseDatabase.getInstance()

        // Référence à la table des posts dans la base de données
        val postsRef = database.getReference("posts")

        // Récupérer les données du post à partir de Firebase en utilisant le postId
        postsRef.child(postId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Récupérer l'ID de l'utilisateur qui a publié ce post
                    val userId = snapshot.child("userId").getValue(String::class.java)
                    idduser = userId
                    // Référence à la table des utilisateurs dans la base de données
                    val usersRef = database.getReference("Utilisateurs")

                    // Récupérer les données de l'utilisateur à partir de Firebase en utilisant l'ID de l'utilisateur
                    usersRef.child(userId!!).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            if (userSnapshot.exists()) {
                                // Récupérer le nom d'utilisateur et la date d'inscription de l'utilisateur
                                val username = userSnapshot.child("nom_utilisateur").getValue(String::class.java)
                                val memberSince = userSnapshot.child("registred_at").getValue(String::class.java)
                                val userImage = userSnapshot.child("profile_image_utilisateur").getValue(String::class.java)
                                phone = userSnapshot.child("numero_de_tele").getValue(String::class.java)
                                    .toString()

                                binding.donneurNomTv.text = username
                                binding.memberSinceTv.text = memberSince
                                Glide.with(this@DetailsPostActivity)
                                    .load(userImage)
                                    .placeholder(R.drawable.profile_ic)
                                    .into(binding.profiledonneur)

                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Gestion des erreurs
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Gestion des erreurs
            }
        })

        binding.buttonCallDonneur.setOnClickListener {
            val phoneNumber = phone
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this@DetailsPostActivity, "Aucune application pour gérer les appels téléphoniques.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttongpsCallDonneur.setOnClickListener {
            val intent = Intent(this, LocalisationActivity::class.java)
            intent.putExtra("postId", postId)
            startActivity(intent)

        }

        binding.buttonSignalDonneur.setOnClickListener {
            if (postId != null && idduser != null) {
                progressDialog = ProgressDialog(this@DetailsPostActivity).apply {
                    setMessage("Signalement en cours...")
                    setCancelable(false)
                    show()
                }
                val warningsRef = FirebaseDatabase.getInstance().getReference("avertissements")
                val usersRef = FirebaseDatabase.getInstance().getReference("Utilisateurs")

                // Référence pour l'utilisateur actuel
                val userWarningsRef = usersRef.child(idduser!!).child("warningCount")

                // Vérifier si un avertissement existe déjà pour cet utilisateur et ce post
                warningsRef.orderByChild("postId").equalTo(postId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(warningSnapshot: DataSnapshot) {
                        var warningExists = false

                        for (snapshot in warningSnapshot.children) {
                            val warningUserId = snapshot.child("userId").getValue(String::class.java)
                            if (warningUserId == idduser) {
                                warningExists = true
                                // Mettre à jour le compteur d'avertissements
                                val currentWarningCount = snapshot.child("warningCount").getValue(Int::class.java) ?: 0
                                val newWarningCount = currentWarningCount + 1

                                snapshot.ref.child("warningCount").setValue(newWarningCount)
                                    .addOnSuccessListener {
                                        userWarningsRef.setValue(newWarningCount)
                                            .addOnSuccessListener {
                                                progressDialog.dismiss()
                                                Toast.makeText(this@DetailsPostActivity, "Avertissement signalé avec succès", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { e ->
                                                progressDialog.dismiss()
                                                Toast.makeText(this@DetailsPostActivity, "Erreur lors de la mise à jour du compteur d'avertissements: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        progressDialog.dismiss()
                                        Toast.makeText(this@DetailsPostActivity, "Erreur lors de la mise à jour de l'avertissement: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                break
                            }
                        }

                        if (!warningExists) {
                            // Ajouter un nouvel avertissement
                            usersRef.child(idduser!!).addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    val username = userSnapshot.child("nom_utilisateur").getValue(String::class.java) ?: "Nom inconnu"
                                    val profileImageUrl = userSnapshot.child("profile_image_utilisateur").getValue(String::class.java) ?: ""

                                    userWarningsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            var currentWarningCount = snapshot.getValue(Int::class.java) ?: 0
                                            currentWarningCount++

                                            val warningId = warningsRef.push().key
                                            val warningData = mapOf(
                                                "postId" to postId,
                                                "userId" to idduser,
                                                "username" to username,
                                                "profileImageUrl" to profileImageUrl,
                                                "warningCount" to currentWarningCount
                                            )

                                            if (warningId != null) {
                                                warningsRef.child(warningId).setValue(warningData)
                                                    .addOnSuccessListener {
                                                        userWarningsRef.setValue(currentWarningCount)
                                                            .addOnSuccessListener {
                                                                progressDialog.dismiss()
                                                                Toast.makeText(this@DetailsPostActivity, "Avertissement signalé avec succès", Toast.LENGTH_SHORT).show()
                                                            }
                                                            .addOnFailureListener { e ->
                                                                progressDialog.dismiss()
                                                                Toast.makeText(this@DetailsPostActivity, "Erreur lors de la mise à jour du compteur d'avertissements: ${e.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        progressDialog.dismiss()
                                                        Toast.makeText(this@DetailsPostActivity, "Erreur lors du signalement de l'avertissement: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            progressDialog.dismiss()
                                            Toast.makeText(this@DetailsPostActivity, "Erreur lors de la récupération du compteur d'avertissements: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    progressDialog.dismiss()
                                    Toast.makeText(this@DetailsPostActivity, "Erreur lors de la récupération des données utilisateur: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        progressDialog.dismiss()
                        Toast.makeText(this@DetailsPostActivity, "Erreur lors de la récupération des avertissements: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "ID du post ou de l'utilisateur manquant", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

