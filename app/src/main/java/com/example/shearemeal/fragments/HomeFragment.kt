package com.example.shearemeal.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shearemeal.models.Post
import com.example.shearemeal.adapters.PostAdapter
import com.example.shearemeal.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class HomeFragment : Fragment(), LocationListener {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var mContext: Context
    private lateinit var locationManager: LocationManager
    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var progressDialog: ProgressDialog
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private var searchJob: Job? = null

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Determination de votre localisation....")

        binding.recyclerpostes.layoutManager = LinearLayoutManager(requireContext())
        postAdapter = PostAdapter(requireContext(), postList, showRedFavIcon = false)
        binding.recyclerpostes.adapter = postAdapter

        loadPostsFromFirebase()

        binding.allCard.setOnClickListener {
            loadPostsFromFirebase()

        }

        binding.drinkscard.setOnClickListener {
            filterPostsByCategory("Boissons")
        }
        binding.platsscard.setOnClickListener {
            filterPostsByCategory("Plats")
        }
        binding.nouriturecard.setOnClickListener {
            filterPostsByCategory("Nourriture")
        }

        val searchEditText = binding.searchEt

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Ne rien faire avant la modification du texte
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Annuler le job précédent si en cours
                searchJob?.cancel()
            }

            override fun afterTextChanged(s: Editable?) {
                // Attendre un délai avant de déclencher la recherche
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(40) // Attendre 500 ms avant de déclencher la recherche
                    filterPostList(s.toString())
                }
            }
        })

        // Vérifier les autorisations de localisation
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun filterPostsByCategory(category: String) {
        val database = FirebaseDatabase.getInstance()
        val postsRef = database.getReference("posts")

        // Écoutez les modifications des données uniquement pour la catégorie spécifiée
        postsRef.orderByChild("category").equalTo(category).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let { postList.add(it) }
                }
                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterPostList(query: String) {
        val database = FirebaseDatabase.getInstance()
        val postsRef = database.getReference("posts")

        val lowercaseQuery = query.lowercase(Locale.getDefault())

        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        if (it.title.lowercase(Locale.getDefault()).contains(lowercaseQuery) ||
                            it.category.lowercase(Locale.getDefault()).contains(lowercaseQuery)
                        ) {
                            postList.add(it)
                        }
                    }
                }
                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadPostsFromFirebase() {
        val database = FirebaseDatabase.getInstance()
        val postsRef = database.getReference("posts")
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let { postList.add(it) }
                }
                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun showProgressDialog() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setMessage("Determination de votre localisation....")
        progressDialog?.setCancelable(false)
        progressDialog?.show()
    }
    private fun locationEnabled() {

        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gpsEnabled = false
        var networkEnabled = false
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (!gpsEnabled && !networkEnabled) {
            AlertDialog.Builder(requireContext())
                .setTitle("Enable GPS Service")
                .setMessage("We need your GPS location to show Near Places around you.")
                .setCancelable(false)
                .setPositiveButton("Enable") { paramDialogInterface, paramInt ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        try {
            locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 5f, this)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }


    }

    override fun onLocationChanged(location: Location) {
        try {
            progressDialog?.dismiss()
            val geocoder = Geocoder(requireContext(),Locale.ENGLISH)
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            // Concaténez le nom de la localité et du pays et affichez-le dans le TextView
            val locality = addresses?.get(0)?.locality
            val countryName = addresses?.get(0)?.countryName
            saveLocationToFirebase(locality, countryName)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    private fun saveLocationToFirebase(locality: String?, countryName: String?) {
        // Vérifier si l'utilisateur est connecté à Firebase
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Référence à l'utilisateur dans la base de données Firebase
            val userRef = FirebaseDatabase.getInstance().reference.child("Utilisateurs").child(currentUser.uid)

            // Créer un objet pour stocker les informations de localisation
            val locationData = HashMap<String, Any>()
            locationData["location_utilisateur"] = "$locality, $countryName"

            // Mettre à jour le champ location_utilisateur pour l'utilisateur actuel dans Firebase
            userRef.updateChildren(locationData)
                .addOnSuccessListener {
                    // Mise à jour réussie
                    Log.d("Firebase", "Localisation mise à jour avec succès dans Firebase.")
                }
                .addOnFailureListener { e ->
                    // Échec de la mise à jour
                    Log.e("Firebase", "Erreur lors de la mise à jour de la localisation dans Firebase : ${e.message}")
                }
        }
    }


    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}



    private fun getLocationFromFirebase(callback: (String) -> Unit) {
        // Vérifier si l'utilisateur est connecté à Firebase
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Référence à l'utilisateur dans la base de données Firebase
            val userRef = FirebaseDatabase.getInstance().reference.child("Utilisateurs").child(currentUser.uid)

            // Écouter les modifications de la localisation de l'utilisateur dans Firebase
            userRef.child("location_utilisateur").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Récupérer la localisation de l'utilisateur
                    val location = dataSnapshot.value.toString()
                    // Appeler le callback avec la localisation récupérée depuis Firebase
                    callback(location)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // En cas d'annulation
                    Log.e("Firebase", "Erreur lors de la récupération de la localisation depuis Firebase : ${databaseError.message}")
                }
            })
        }
    }



}


