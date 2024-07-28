package com.example.shearemeal.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.shearemeal.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dialogue_progress: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        dialogue_progress = ProgressDialog(this)
        dialogue_progress.setTitle("Connexion")
        dialogue_progress.setCanceledOnTouchOutside(false)

        binding.textviewbacktoregister.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
        binding.btnbacklogin.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
        binding.textoublier.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
        }


        binding.loginButton.setOnClickListener {
            validateData()
        }
    }

    private var email = ""
    private var pass = ""

    private fun validateData() {
        email = binding.loginemailfieldTET.text.toString().trim()
        pass = binding.loginpasswordfieldTET.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.loginemailfieldTET.error = "email invalide"
            binding.loginemailfieldTET.requestFocus()

        } else if (pass.isEmpty()) {
            binding.loginpasswordfieldTET.error = "Entrer votre mot de passe"
            binding.loginpasswordfieldTET.requestFocus()
        } else {
            loginUser()
        }
    }

    private fun loginUser() {
        dialogue_progress.setMessage("En cours")
        dialogue_progress.show()
        val adminEmail = "admin@admin.com"
        val adminPass = "admin"

        if (email == adminEmail && pass == adminPass) {
            // Rediriger vers l'activité AdminActivity
            startActivity(Intent(this, AdminMainActivity::class.java))
            finishAffinity()
        } else {
            firebaseAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    val userId = firebaseAuth.currentUser!!.uid
                    val userRef = FirebaseDatabase.getInstance().getReference("Utilisateurs").child(userId)

                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            dialogue_progress.dismiss()
                            val isDeleted = snapshot.child("supprimer").getValue(Boolean::class.java) ?: false
                            val isBlocked = snapshot.child("blocked").getValue(Boolean::class.java) ?: false
                            val unblockDate = snapshot.child("unblockDate").getValue(Long::class.java) ?: 0

                            if (isDeleted) {
                                Toast.makeText(this@LoginActivity, "Votre compte a été supprimé.", Toast.LENGTH_SHORT).show()
                                firebaseAuth.signOut()
                            }else if (isBlocked && System.currentTimeMillis() < unblockDate) {
                                Toast.makeText(this@LoginActivity, "Votre compte est bloqué jusqu'à ${Date(unblockDate)}.", Toast.LENGTH_SHORT).show()
                                firebaseAuth.signOut()
                            } else {
                                userRef.child("en_ligne").setValue(true)
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finishAffinity()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            dialogue_progress.dismiss()
                            Toast.makeText(this@LoginActivity, "Erreur lors de la connexion", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                .addOnFailureListener {
                    dialogue_progress.dismiss()
                    Toast.makeText(this, "Erreur lors de la connexion", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
