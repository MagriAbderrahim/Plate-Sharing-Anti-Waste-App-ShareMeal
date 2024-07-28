package com.example.shearemeal.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shearemeal.fragments.ChatFragment
import com.example.shearemeal.fragments.FavorisFragment
import com.example.shearemeal.fragments.HomeFragment
import com.example.shearemeal.fragments.ProfileFragment
import com.example.shearemeal.R
import com.example.shearemeal.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        // Vérifier si l'utilisateur est connecté, sinon rediriger vers l'activité de bienvenue
        if (auth.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }
        binding.buttonsellfragement.setOnClickListener{
            startActivity(Intent(this, CreatepostActivity::class.java))
            finish()
        }

        // Gérer les sélections d'éléments du BottomNavigationView
        binding.bottomnavicons.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    binding.textviewtitle.text = "Home"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, HomeFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.menu_favoris -> {
                    binding.textviewtitle.text = "Mes Favoris"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, FavorisFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.menu_chat -> {
                    binding.textviewtitle.text = "Mes Publications"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, ChatFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.menu_profile -> {
                    binding.textviewtitle.text = "Mon profil"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, ProfileFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Sélectionner l'élément de menu "Home" par défaut si aucun état n'est sauvegardé
        if (savedInstanceState == null) {
            binding.bottomnavicons.selectedItemId = R.id.menu_home
        }
    }
}

/*
1) Create & Setup the Project
2) Bottom Navigation Menu
3) Bottom Navigation Item Click | Handle Fragment Switch
4) Login Options
5) Connect with Firebase
6) Check the User Login Status

 */