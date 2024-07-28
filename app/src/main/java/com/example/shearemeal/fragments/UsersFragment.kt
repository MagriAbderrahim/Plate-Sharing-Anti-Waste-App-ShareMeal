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
import com.example.shearemeal.models.User
import com.example.shearemeal.adapters.UserAdapter
import com.example.shearemeal.databinding.FragmentUsersBinding
import com.google.firebase.database.*
import java.util.*

class UsersFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var userAdapter: UserAdapter
    private lateinit var mContext: Context
    private lateinit var binding: FragmentUsersBinding
    private val userList = mutableListOf<User>()
    private val filteredUserList = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mContext = requireContext()
        database = FirebaseDatabase.getInstance().reference.child("Utilisateurs")
        setupRecyclerView()

        // Ajoutez un TextWatcher pour écouter les changements dans le champ de recherche
        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterUsers(s.toString())
            }
        })

        loadUsers()
    }

    private fun setupRecyclerView() {
        binding.recycleradminusers.layoutManager = LinearLayoutManager(mContext)
        userAdapter = UserAdapter(mContext, filteredUserList)
        binding.recycleradminusers.adapter = userAdapter
    }

    private fun loadUsers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key ?: ""
                    val email = userSnapshot.child("email_utilisateur").getValue(String::class.java) ?: ""
                    val name = userSnapshot.child("nom_utilisateur").getValue(String::class.java) ?: ""
                    val registrationDate = userSnapshot.child("registred_at").getValue(String::class.java) ?: ""
                    val profileImageUrl = userSnapshot.child("profile_image_utilisateur").getValue(String::class.java) ?: ""
                    val supprime = userSnapshot.child("supprimer").getValue(Boolean::class.java) ?: false
                    if (!supprime) {
                        val user = User(uid, email, name, registrationDate, profileImageUrl)
                        userList.add(user)
                    }
                }
                // Initialiser la liste filtrée avec toutes les données d'utilisateur
                filteredUserList.clear()
                filteredUserList.addAll(userList)
                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Gérer l'annulation de la lecture depuis la base de données Firebase
            }
        })
    }

    private fun filterUsers(query: String) {
        val lowerCaseQuery = query.toLowerCase(Locale.getDefault())
        filteredUserList.clear()
        if (query.isNotEmpty()) {
            userList.forEach { user ->
                if (user.name.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredUserList.add(user)
                }
            }
        } else {
            filteredUserList.addAll(userList)
        }
        userAdapter.notifyDataSetChanged()
    }
}
