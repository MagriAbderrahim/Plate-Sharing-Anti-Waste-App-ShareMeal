package com.example.shearemeal.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.shearemeal.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        // Reset Button Listener
        binding.btnReset.setOnClickListener {
            val strEmail = binding.edtForgotPasswordEmail.text.toString().trim()
            if (!TextUtils.isEmpty(strEmail)) {
                resetPassword(strEmail)
            } else {
                binding.edtForgotPasswordEmail.error = "Remplir le champ email svp"
            }
        }

        // Back Button Listener
        binding.btnForgotPasswordBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun resetPassword(strEmail: String) {
        binding.forgetPasswordProgressbar.visibility = View.VISIBLE
        binding.btnReset.visibility = View.INVISIBLE

        mAuth.sendPasswordResetEmail(strEmail)
            .addOnSuccessListener {
                Toast.makeText(this@ForgotPasswordActivity, "Un lien de réinitialisation de mot de passe a été envoyé à votre adresse e-mail enregistrée", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@ForgotPasswordActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@ForgotPasswordActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.forgetPasswordProgressbar.visibility = View.INVISIBLE
                binding.btnReset.visibility = View.VISIBLE
            }
    }
}