package com.example.shearemeal.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shearemeal.R
import com.example.shearemeal.models.User
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar


class UserAdapter(private val context: Context, private val userList: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_users_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.bind(user)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageIv)
        val emailTv: TextView = itemView.findViewById(R.id.emailTv)
        val nameTv: TextView = itemView.findViewById(R.id.nameTv)
        val registrationDateTv: TextView = itemView.findViewById(R.id.registrationDateTv)
        val menuButton: ImageButton = itemView.findViewById(R.id.menuButton)

        fun bind(user: User) {
            Glide.with(context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_person)
                .into(imageView)

            emailTv.text = user.email
            nameTv.text = user.name
            registrationDateTv.text = "Inscrit le ${user.registrationDate}"

            menuButton.setOnClickListener {
                val popupMenu = PopupMenu(context, menuButton)
                popupMenu.menuInflater.inflate(R.menu.user_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.action_delete_user -> {
                            confirmDeleteUser(user)
                        }
                        R.id.action_block_user -> {
                            confirmBlockUser(user)
                        }
                    }
                    true
                }
                popupMenu.show()
            }
        }

        private fun confirmDeleteUser(user: User) {
            AlertDialog.Builder(context)
                .setTitle("Supprimer utilisateur")
                .setMessage("Êtes-vous sûr de vouloir supprimer cet utilisateur?")
                .setPositiveButton("Oui") { dialog, which ->
                    deleteUser(user)
                }
                .setNegativeButton("Non", null)
                .show()
        }

        private fun deleteUser(user: User) {
            val userRef = FirebaseDatabase.getInstance().getReference("Utilisateurs").child(user.uid)
            val updates = mapOf<String, Any>(
                "supprimer" to true
            )

            userRef.updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(context, "Utilisateur ${user.name} supprimé", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Échec de la suppression: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        private fun confirmBlockUser(user: User) {
            // Afficher une alerte pour confirmer le blocage de l'utilisateur
            AlertDialog.Builder(context)
                .setTitle("Bloquer utilisateur")
                .setMessage("Êtes-vous sûr de vouloir bloquer cet utilisateur pendant 90 jours?")
                .setPositiveButton("Oui") { dialog, which ->
                    blockUser(user)
                }
                .setNegativeButton("Non", null)
                .show()
        }

        private fun blockUser(user: User) {
            // Calculer la date de déblocage (90 jours à partir d'aujourd'hui)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 90)
            val unblockDate = calendar.timeInMillis

            // Mettre à jour la base de données avec les informations de blocage
            val userRef = FirebaseDatabase.getInstance().getReference("Utilisateurs").child(user.uid)
            val updates = mapOf<String, Any>(
                "blocked" to true,
                "unblockDate" to unblockDate
            )

            userRef.updateChildren(updates)
                .addOnSuccessListener {
                    AlertDialog.Builder(context)
                        .setTitle("Utilisateur bloqué")
                        .setMessage("L'utilisateur ${user.name} est bloqué et ne pourra pas se connecter pendant 90 jours.")
                        .setPositiveButton("OK", null)
                        .show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Échec du blocage: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

}
