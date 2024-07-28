// WarningAdapter.kt
package com.example.shearemeal.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shearemeal.R
import com.example.shearemeal.models.Warning
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class WarningAdapter(private val context: Context, private val warningList: MutableList<Warning>) :
    RecyclerView.Adapter<WarningAdapter.WarningViewHolder>() {


    class WarningViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userImageIv: ImageView = view.findViewById(R.id.profileImageIv)
        val userNameTv: TextView = view.findViewById(R.id.usernameTv)
        val warningCountTv: TextView = view.findViewById(R.id.warningCountTv)
        val blockButton: Button = view.findViewById(R.id.blockButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarningViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_avertissement, parent, false)
        return WarningViewHolder(view)
    }

    override fun onBindViewHolder(holder: WarningViewHolder, position: Int) {
        val warning = warningList[position]

        holder.userNameTv.text = warning.username
        holder.warningCountTv.text = "Avertissements: ${warning.warningCount}"
        Glide.with(context).load(warning.profileImageUrl).placeholder(R.drawable.profile_ic).into(holder.userImageIv)

        holder.deleteButton.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Confirmer l'action")
            builder.setMessage("Voulez-vous vraiment supprimer cet utilisateur ?")
            builder.setPositiveButton("Oui") { dialog, _ ->
                // Supprimer l'élément de la liste RecyclerView
                val warning = warningList[position]
                warningList.removeAt(position)
                notifyItemRemoved(position)

                // Références à la base de données Firebase
                val database = FirebaseDatabase.getInstance()
                val warningsRef = database.getReference("avertissements")
                val userRef = database.getReference("Utilisateurs").child(warning.userId)

                // Rechercher et supprimer l'avertissement par postId
                warningsRef.orderByChild("postId").equalTo(warning.postId).addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (snapshot in dataSnapshot.children) {
                            snapshot.ref.removeValue().addOnSuccessListener {
                                // Mettre à jour l'utilisateur pour indiquer qu'il doit être supprimé
                                val updates = mapOf<String, Any>(
                                    "supprimer" to true,
                                    "warningCount" to 0
                                )
                                userRef.updateChildren(updates).addOnSuccessListener {
                                    Toast.makeText(context, " utilisateur ${warning.username} supprimé avec succès", Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener { e ->
                                    Toast.makeText(context, "Erreur lors de la mise à jour de l'utilisateur: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener { e ->
                                Toast.makeText(context, "Erreur lors de la suppression de l'avertissement: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(context, "Erreur lors de la recherche de l'avertissement: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                    }
                })

                dialog.dismiss()
            }
            builder.setNegativeButton("Non") { dialog, _ ->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }


        holder.blockButton.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Confirmer l'action")
            builder.setMessage("Voulez-vous vraiment bloquer cet utilisateur pendant 90 jours ?")
            builder.setPositiveButton("Oui") { dialog, _ ->
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 90)
                val unblockDate = calendar.timeInMillis

                // Mettre à jour la base de données avec les informations de blocage
                val userRef = FirebaseDatabase.getInstance().getReference("Utilisateurs").child(warning.userId)
                val updates = mapOf<String, Any>(
                    "blocked" to true,
                    "unblockDate" to unblockDate
                )

                userRef.updateChildren(updates)
                    .addOnSuccessListener {
                        AlertDialog.Builder(context)
                            .setTitle("Utilisateur bloqué")
                            .setMessage("L'utilisateur ${warning.username} est bloqué et ne pourra pas se connecter pendant 90 jours.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Échec du blocage: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            builder.setNegativeButton("Non") { dialog, _ ->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }


    override fun getItemCount(): Int {
        return warningList.size
    }
}