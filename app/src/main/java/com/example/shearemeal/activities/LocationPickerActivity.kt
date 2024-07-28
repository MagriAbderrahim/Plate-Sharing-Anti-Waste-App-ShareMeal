package com.example.shearemeal.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.shearemeal.models.Post
import com.example.shearemeal.R
import com.example.shearemeal.databinding.ActivityLocationPickerBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityLocationPickerBinding
    private lateinit var map: GoogleMap
    private lateinit var currentLocation: Location
    private lateinit var marker: Marker
    private lateinit var fusedClient: FusedLocationProviderClient

    companion object {
        private const val REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        binding.toolbarGpsBtn.setOnClickListener {
            getLocation()
        }
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }
        binding.doneBtn.setOnClickListener {
            if (::currentLocation.isInitialized) {
                val address = getAddress(currentLocation.latitude, currentLocation.longitude)
                binding.selectedPlaceTv.text = address

                // Transférer les données vers l'activité CreatepostActivity
                val intent = Intent(this, CreatepostActivity::class.java)
                intent.putExtra("address", address)
                intent.putExtra("latitude", currentLocation.latitude)
                intent.putExtra("longitude", currentLocation.longitude)
                startActivity(intent)

            } else {
                Toast.makeText(this, "Cliquez sur GPS pour trouver votre localisation", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun getAddress(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.FRENCH)
        val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
        return if (addresses!!.isNotEmpty()) {
            val address: Address = addresses[0]
            val country = address.countryName
            val city = address.locality
            "$city, $country"
        } else {
            "Adresse introuvable"
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
                val supportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                supportMapFragment.getMapAsync(this)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        val markerOptions = MarkerOptions().position(latLng).title("My Current Location")
        map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
        marker = map.addMarker(markerOptions)!!
        loadPostsFromFirebase()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            }
        }
    }

    private fun loadPostsFromFirebase() {
        val database = FirebaseDatabase.getInstance().reference.child("posts")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null) {
                        addMarkerForPost(post)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                Log.e("Firebase", "Error loading posts: ${error.message}")
            }
        })
    }

    private fun addMarkerForPost(post: Post) {
        val latLng = LatLng(post.latitude, post.longitude)
        val markerOptions = MarkerOptions().position(latLng).title(post.title)
        val marker = map.addMarker(markerOptions)

        // Load image and set it as marker icon
        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.getReferenceFromUrl(post.imageUrl)
        storageReference.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this)
                .asBitmap()
                .load(uri)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val resizedBitmap = resizeBitmap(resource, 100, 100)  // Adjust the size as needed
                        marker?.setIcon(BitmapDescriptorFactory.fromBitmap(resizedBitmap))
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }.addOnFailureListener {
            // Handle failure
            Log.e("Firebase", "Error loading image: ${it.message}")
        }
    }

    private fun resizeBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(source, width, height, false)
    }
}
