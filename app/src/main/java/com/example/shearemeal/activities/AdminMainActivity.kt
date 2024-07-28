package com.example.shearemeal.activities


import com.example.shearemeal.fragments.UsersFragment
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shearemeal.fragments.DashboardFragment
import com.example.shearemeal.fragments.NotificationsFragment
import com.example.shearemeal.fragments.PubsFragment
import com.example.shearemeal.R
import com.example.shearemeal.databinding.ActivityAdminMainBinding

class AdminMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashbord -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameContainerr, DashboardFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                R.id.users -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameContainerr, UsersFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                R.id.pubs -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameContainerr, PubsFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                R.id.notifications -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.frameContainerr, NotificationsFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                else -> false
            }

        }
        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.dashbord
        }

    }
}