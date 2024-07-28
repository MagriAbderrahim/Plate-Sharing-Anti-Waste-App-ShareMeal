package com.example.shearemeal.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.shearemeal.databinding.ActivitySignupBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Locale

class SignupActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySignupBinding
    private lateinit var dialogue_progress: ProgressDialog
    private lateinit var currentLocation: Location
    private lateinit var fusedClient: FusedLocationProviderClient

    companion object {
        private const val REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        firebaseAuth = FirebaseAuth.getInstance()
        dialogue_progress = ProgressDialog(this)
        dialogue_progress.setTitle("Inscription")
        dialogue_progress.setCanceledOnTouchOutside(false)

        binding.textviewlogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.btnbacksignup.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
        binding.textviewbacktologin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }


        binding.signupButton.setOnClickListener {
            getLocation()
        }
    }
    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE)
            return
        }

        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                validatedata()
            } else {
                currentLocation.longitude= 0.0
                currentLocation.latitude= 0.0
                validatedata()

            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            }
        }
    }

    private fun validatedata() {
        val email = binding.signupemailfieldTET.text.toString().trim()
        val pass = binding.signuppasswordfieldTET.text.toString().trim()
        val cpass = binding.signupconfirmpassfieldTET.text.toString().trim()
        val fname = binding.signupfullnamefieldTET.text.toString().trim()
        val phon = binding.signupphonefieldTET.text.toString().trim()

        if (pass != cpass) {
            binding.signuppasswordfieldTET.error = "les mots de passes ne sont pas identiques"
            binding.signupconfirmpassfieldTET.error = "les mots de passes ne sont pas identiques"
            binding.signuppasswordfieldTET.requestFocus()

        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.signupemailfieldTET.error = "email n'est pas valide"
            binding.signupemailfieldTET.requestFocus()

        } else if (fname.isEmpty()) {
            binding.signupfullnamefieldTET.error = "remplir le nom complet svp"
            binding.signupfullnamefieldTET.requestFocus()

        } else {
            registerUser(email, pass, fname,phon)
        }
    }

    private fun registerUser(email: String, pass: String, fname: String,phone:String) {
        dialogue_progress.setMessage("Création de votre compte")
        dialogue_progress.show()

        firebaseAuth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                modifierinfoutilisateur(authResult.user?.email, authResult.user?.uid, pass, fname,phone)
            }.addOnFailureListener { e ->
                dialogue_progress.dismiss()
                Toast.makeText(this, "Erreur lors de l'inscription: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun modifierinfoutilisateur(email: String?, uid: String?, pass: String, fname: String,phone:String) {
        dialogue_progress.setMessage("Sauvegarde de données")
        val hashmap_de_donnes = HashMap<String, Any>()
        hashmap_de_donnes["nom_utilisateur"] = fname
        hashmap_de_donnes["numero_de_tele"] = phone
        hashmap_de_donnes["uid"] = uid ?: ""
        hashmap_de_donnes["email_utilisateur"] = email ?: ""
        hashmap_de_donnes["profile_image_utilisateur"] = ""
        hashmap_de_donnes["password_utilisateur"] = pass
        hashmap_de_donnes["ville_utilisateur"] = ""
        hashmap_de_donnes["location_utilisateur"] = ""
        hashmap_de_donnes["lang_utilisateur"] = currentLocation.longitude
        hashmap_de_donnes["lant_utilisateur"] = currentLocation.latitude
        hashmap_de_donnes["nombres_pubs_utilisateur"] = ""
        val dateFormat = SimpleDateFormat("dd--MM--yyyy", Locale.getDefault())
        val dateAujourdhui = dateFormat.format(Date())

        hashmap_de_donnes["registred_at"] = dateAujourdhui.toString()
        hashmap_de_donnes["en_ligne"] = ""

        val reference = FirebaseDatabase.getInstance().getReference("Utilisateurs")
        uid?.let {
            reference.child(it)
                .setValue(hashmap_de_donnes)
                .addOnSuccessListener {
                    dialogue_progress.dismiss()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }.addOnFailureListener {
                    dialogue_progress.dismiss()
                    Toast.makeText(this, "Erreur lors de la sauvegarde des données", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
