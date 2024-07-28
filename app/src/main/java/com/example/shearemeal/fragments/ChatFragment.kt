package com.example.shearemeal.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shearemeal.models.Post
import com.example.shearemeal.R
import com.example.shearemeal.adapters.PostAdapter
import com.example.shearemeal.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.recyclerView1
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        postAdapter = PostAdapter(requireContext(), postList, showRedFavIcon = false)
        recyclerView.adapter = postAdapter

        // Fetch user posts from Firebase when the view is created
        loadPostsFromFirebase()
    }

    private fun loadPostsFromFirebase() {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserID != null) {
            val database = FirebaseDatabase.getInstance()
            val postsRef = database.getReference("posts")
            val query = postsRef.orderByChild("userId").equalTo(currentUserID)
            query.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    postList.clear()
                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        postList.add(post!!)
                    }
                    // Vérifier si la liste des publications est vide
                    if (postList.isEmpty()) {
                        // Si la liste est vide, remplacer le RecyclerView par une image et un TextView
                        replaceRecyclerViewWithImageAndText(binding.root)
                    } else {
                        // Si la liste n'est pas vide, mettre à jour le RecyclerView avec les publications
                        postAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load posts: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Log.e("ChatFragment", "Current user ID is null")
        }
    }

    @SuppressLint("ResourceType")
    private fun replaceRecyclerViewWithImageAndText(view: View) {
        // Supprimer le RecyclerView
        recyclerView.visibility = View.GONE

        // Afficher l'image et le TextView
        val emptyView = view.findViewById<LinearLayout>(R.id.empty_view)
        val image = view.findViewById<ImageView>(R.id.image_no_posts)
        val text = view.findViewById<TextView>(R.id.text_no_posts)

        emptyView.visibility = View.VISIBLE
        image.visibility = View.VISIBLE
        text.visibility = View.VISIBLE
    }

}
