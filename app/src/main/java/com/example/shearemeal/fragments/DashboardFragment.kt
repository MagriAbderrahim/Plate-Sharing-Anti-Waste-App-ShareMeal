package com.example.shearemeal.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import com.example.shearemeal.R
import com.example.shearemeal.activities.WelcomeActivity
import com.example.shearemeal.databinding.FragmentDashboardBinding
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class DashboardFragment : Fragment() {

    private lateinit var binding: FragmentDashboardBinding

    private val usersRef = FirebaseDatabase.getInstance().getReference("Utilisateurs")
    private val postsRef = FirebaseDatabase.getInstance().getReference("posts")
    private val avertissement = FirebaseDatabase.getInstance().getReference("avertissements")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDashboardBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val currentDate = Calendar.getInstance().time
        val formattedDate = dateFormat.format(currentDate)
        binding.dateText.text = formattedDate


        binding.profileImage.setOnClickListener { view ->
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.menu_deconex)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_logout -> {
                        // Code pour déconnecter l'utilisateur et le rediriger vers WelcomeActivity
                        logoutAndRedirectToWelcomeActivity()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }



        // Récupérez les nombres de publications et d'utilisateurs
        getNbUsers()
        getNbPosts()
        getAvertisements()
    }

    private fun getAvertisements() {
        avertissement.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                val nbUsers = snapshot.childrenCount.toInt()
                binding.valueNbAvertissements.text = "$nbUsers"

            } else {
                // Gérez les erreurs ici
            }
        }
    }

    private fun logoutAndRedirectToWelcomeActivity() {

        val intent = Intent(context, WelcomeActivity::class.java)
        startActivity(intent)
        activity?.finish() // Terminez l'activité actuelle si nécessaire
    }



    private fun getNbUsers() {
        usersRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                val nbUsers = snapshot.childrenCount.toInt()
                binding.valueNbUsers.text = "$nbUsers"

                // Comptez le nombre d'utilisateurs en ligne
                var nbUsersEnLigne = 0
                snapshot.children.forEach {
                    if (it.child("en_ligne").value as Boolean) {
                        nbUsersEnLigne++
                    }
                }
                binding.valueNbMetric4.text = "$nbUsersEnLigne"
            } else {
                // Gérez les erreurs ici
            }
        }
    }

    private fun getNbPosts() {
        postsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                val nbPosts = snapshot.childrenCount.toInt()
                binding.valueNbPublications.text = "$nbPosts"
            } else {
                // Gérez les erreurs ici
            }
        }
    }
}
