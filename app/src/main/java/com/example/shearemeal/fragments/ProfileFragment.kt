package com.example.shearemeal.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.shearemeal.R
import com.example.shearemeal.activities.ModifierProfilActivity
import com.example.shearemeal.activities.WelcomeActivity
import com.example.shearemeal.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ProfileFragment : Fragment() {
    private lateinit var  binding : FragmentProfileBinding
    private lateinit var  mContext:Context
    private lateinit var  firebaseAuth: FirebaseAuth


    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(layoutInflater,container,false)
        return  binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        load_mes_infos()
        binding.logoutProfileCv.setOnClickListener {
            val userId = firebaseAuth.uid
            val ref = FirebaseDatabase.getInstance().getReference("Utilisateurs").child(userId!!)
            ref.child("en_ligne").setValue(false)

            firebaseAuth.signOut()
            startActivity(Intent(mContext, WelcomeActivity::class.java))
            activity?.finishAffinity()
        }
        binding.editProfileCv.setOnClickListener {
            startActivity(Intent(mContext, ModifierProfilActivity::class.java))
        }
    }

    private fun load_mes_infos() {
        val ref = FirebaseDatabase.getInstance().getReference("Utilisateurs")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nom_complet = "${snapshot.child("nom_utilisateur").value}"
                    val mail = "${snapshot.child("email_utilisateur").value}"
                    val phone = "${snapshot.child("numero_de_tele").value}"
                    val dateRegister = "${snapshot.child("registred_at").value}"
                    val profileimgurl = "${snapshot.child("profile_image_utilisateur").value}"

                    binding.nomcomplet.text = nom_complet
                    binding.emaill.text = mail
                    binding.numtele.text = phone
                    binding.inscripdate.text = dateRegister
                    try {
                        Glide.with(mContext)
                            .load(profileimgurl)
                            .placeholder(R.drawable.ic_person)
                            .into(binding.profileImg)

                    }catch (e : Exception){

                    }


                }

                override fun onCancelled(error: DatabaseError) {
                    // Votre logique en cas d'annulation de l'écouteur de données
                }
            })
    }



}