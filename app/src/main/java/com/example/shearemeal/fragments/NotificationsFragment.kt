package com.example.shearemeal.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shearemeal.R
import com.example.shearemeal.models.Warning
import com.example.shearemeal.adapters.WarningAdapter

import com.google.firebase.database.*

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var warningAdapter: WarningAdapter
    private val warningList = mutableListOf<Warning>()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewAvert)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        warningAdapter = WarningAdapter(requireContext(), warningList)
        recyclerView.adapter = warningAdapter

        loadWarnings()

        return view
    }

    private fun loadWarnings() {
        val warningsRef = FirebaseDatabase.getInstance().getReference("avertissements")
        warningsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                warningList.clear()
                for (warningSnapshot in snapshot.children) {
                    val warning = warningSnapshot.getValue(Warning::class.java)
                    if (warning != null) {
                        warningList.add(warning)
                    }
                }
                    warningAdapter.notifyDataSetChanged()


            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

}
