package com.example.shearemeal.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shearemeal.models.Post
import com.example.shearemeal.adapters.PostAdapter
import com.example.shearemeal.databinding.FragmentFavorisBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FavorisFragment : Fragment() {

    private lateinit var binding: FragmentFavorisBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val favorisList = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFavorisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.recyclerView2
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        postAdapter = PostAdapter(requireContext(), favorisList, showRedFavIcon = true)
        recyclerView.adapter = postAdapter

        // Fetch favorite posts from Firebase when the view is created
        loadFavoritePostsFromFirebase()
    }

    private fun loadFavoritePostsFromFirebase() {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserID != null) {
            val database = FirebaseDatabase.getInstance()
            val userFavRef = database.getReference("Utilisateurs").child(currentUserID).child("Favoris")
            userFavRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val postIdList = mutableListOf<String>()
                    for (favSnapshot in snapshot.children) {
                        val postId = favSnapshot.key
                        postId?.let { postIdList.add(it) }
                    }
                    fetchFavoritePostsDetails(postIdList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FavorisFragment", "Failed to load favorite posts: ${error.message}")
                }
            })
        } else {
            Log.e("FavorisFragment", "Current user ID is null")
        }
    }

    private fun fetchFavoritePostsDetails(postIdList: List<String>) {
        if (postIdList.isEmpty()) {
            // Afficher l'image et le texte si la liste des favoris est vide
            recyclerView.visibility = View.GONE
            binding.emptyView1.visibility = View.VISIBLE
            binding.imageNoPosts1.visibility = View.VISIBLE
            binding.textNoPosts1.visibility = View.VISIBLE
        } else {
            val database = FirebaseDatabase.getInstance()
            val postsRef = database.getReference("posts")

            // Liste temporaire pour stocker les favoris actuels
            val currentFavorisList = mutableListOf<Post>()

            // Fetch details of each favorite post individually
            for (postId in postIdList) {
                val postQuery = postsRef.orderByKey().equalTo(postId)
                postQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (postSnapshot in snapshot.children) {
                            val post = postSnapshot.getValue(Post::class.java)
                            post?.let { currentFavorisList.add(it) }
                        }
                        // Mettre à jour la liste des favoris affichée dans l'adaptateur
                        postAdapter.submitList(currentFavorisList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FavorisFragment", "Failed to fetch favorite post details: ${error.message}")
                    }
                })
            }
        }
    }


}
