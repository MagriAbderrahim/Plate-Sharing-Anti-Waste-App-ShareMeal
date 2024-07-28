package com.example.shearemeal.activities

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.shearemeal.R
import com.example.shearemeal.databinding.ActivityLocalisationDonneurBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.android.gms.maps.model.PolylineOptions


class LocalisationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityLocalisationDonneurBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var postsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private var postLang: Double? = null
    private var postLant: Double? = null
    private var userLang: Double? = null
    private var userLant: Double? = null
    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalisationDonneurBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        postsRef = database.getReference("posts")
        usersRef = database.getReference("Utilisateurs")
        auth = FirebaseAuth.getInstance()

        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map1) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Récupérer le postId à partir de l'intent
        val postId = intent.getStringExtra("postId")

        if (postId != null) {
            retrievePostLocation(postId)
            retrieveUserLocation(auth.currentUser?.uid)
        } else {
            Toast.makeText(this, "Post ID is missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun retrievePostLocation(postId: String) {
        postsRef.child(postId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    postLang = snapshot.child("longitude").getValue(Double::class.java)
                    postLant = snapshot.child("latitude").getValue(Double::class.java)

                    if (postLang != null && postLant != null) {
                        // If map is already ready, update it
                        if (::mMap.isInitialized) {
                            updateMap()
                        }
                    } else {
                        Toast.makeText(this@LocalisationActivity, "Post coordinates not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LocalisationActivity, "Post not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LocalisationActivity, "Failed to load post data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun retrieveUserLocation(userId: String?) {
        userId?.let {
            usersRef.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        userLang = snapshot.child("lang_utilisateur").getValue(Double::class.java)
                        userLant = snapshot.child("lant_utilisateur").getValue(Double::class.java)

                        if (userLang != null && userLant != null) {
                            // If map is already ready, update it
                            if (::mMap.isInitialized) {
                                updateMap()
                            }
                        } else {
                            Toast.makeText(this@LocalisationActivity, "User coordinates not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LocalisationActivity, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@LocalisationActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // If coordinates are already loaded, update the map
        if (postLang != null && postLant != null && userLang != null && userLant != null) {
            updateMap()
        }
    }

    private fun updateMap() {
        val postLocation = LatLng(postLant!!, postLang!!)
        val userLocation = LatLng(userLant!!, userLang!!)

        mMap.addMarker(MarkerOptions().position(postLocation).title("Post Location"))
        mMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))

        val pathOptions = PolylineOptions()
            .add(postLocation, userLocation)
            .width(10f)
            .color(Color.RED)

        mMap.addPolyline(pathOptions)

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(postLocation, 15f))
    }
}
