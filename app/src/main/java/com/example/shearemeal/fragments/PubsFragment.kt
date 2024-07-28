package com.example.shearemeal.fragments
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shearemeal.models.Post
import com.example.shearemeal.adapters.CardItemAdapter
import com.example.shearemeal.databinding.FragmentPubsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job

class PubsFragment : Fragment() {
    private lateinit var binding: FragmentPubsBinding

    private lateinit var mContext: Context
    private lateinit var postAdapter: CardItemAdapter
    private val postList = mutableListOf<Post>()
    private var searchJob: Job? = null
    private lateinit var auth: FirebaseAuth

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPubsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        // Setup RecyclerView
        postAdapter = CardItemAdapter(mContext, postList)
        binding.recyclerviewpostesadmin.apply {
            layoutManager = LinearLayoutManager(mContext)
            adapter = postAdapter
        }

        // Retrieve posts from Firebase
        val database = FirebaseDatabase.getInstance()
        val postsRef = database.getReference("posts")

        // Listen for changes in the database reference
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        postList.add(it)
                    }
                }
                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Filter the list based on the search query
                filterPostList(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })




    }
    private fun filterPostList(query: String) {
        val filteredList = mutableListOf<Post>()
        for (post in postList) {
            if (post.title.contains(query, ignoreCase = true) ||
                post.description.contains(query, ignoreCase = true) ||
                post.category.contains(query, ignoreCase = true)
            ) {
                filteredList.add(post)
            }
        }
        postAdapter.submitList(filteredList)
    }


}
