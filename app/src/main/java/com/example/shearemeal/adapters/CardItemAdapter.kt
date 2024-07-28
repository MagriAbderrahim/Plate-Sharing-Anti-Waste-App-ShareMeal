package com.example.shearemeal.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shearemeal.models.Post
import com.example.shearemeal.R // Assurez-vous d'importer le package correct
import com.google.firebase.database.FirebaseDatabase

class CardItemAdapter(private val context: Context, private val itemList: MutableList<Post>) :
    RecyclerView.Adapter<CardItemAdapter.CardItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post_admin, parent, false)
        return CardItemViewHolder(view)
    }



    override fun onBindViewHolder(holder: CardItemViewHolder, position: Int) {
        val item = itemList[position]

        holder.titleTextView.text = item.title
        holder.descriptionTextView.text = item.description
        holder.addressTextView.text = item.datePublication
        holder.categoryTextView.text = item.category

        Glide.with(context).load(item.imageUrl).into(holder.imageView)

        // Gérer les clics sur les boutons ou les éléments ici si nécessaire
        holder.favButton.setOnClickListener {
            showPopupMenu(holder.favButton, position)


        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
    fun submitList(newItemList: List<Post>) {
        itemList.clear()
        itemList.addAll(newItemList)
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int) {
        val postId = itemList[position].postId
        itemList.removeAt(position)
        notifyItemRemoved(position)
        // Supprimer l'item de la base de données Firebase
        FirebaseDatabase.getInstance().getReference("posts").child(postId).removeValue()
    }
    private fun showPopupMenu(view: View, position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.menu_admin_post) // Référence au fichier XML du menu contextuel

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.delete_post -> {
                    deleteItem(position)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    inner class CardItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageIv)
        val titleTextView: TextView = itemView.findViewById(R.id.titleTv)
        val descriptionTextView: TextView = itemView.findViewById(R.id.titleTvdesc)
        val addressTextView: TextView = itemView.findViewById(R.id.addressTv)
        val categoryTextView: TextView = itemView.findViewById(R.id.categorytxt)
        val favButton: ImageButton = itemView.findViewById(R.id.favBtn)
    }
}
