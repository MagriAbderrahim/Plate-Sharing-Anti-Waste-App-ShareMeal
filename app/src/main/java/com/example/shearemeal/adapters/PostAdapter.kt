package com.example.shearemeal.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shearemeal.models.Post
import com.example.shearemeal.R
import com.example.shearemeal.activities.DetailsPostActivity
import com.example.shearemeal.activities.EditPostActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PostAdapter(private val context: Context, private var postList: MutableList<Post>, private val showRedFavIcon: Boolean) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val currentUser = FirebaseAuth.getInstance().currentUser


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.titleTextView.text = post.title
        holder.descriptionTextView.text = post.description
        holder.locationTextView.text = post.datePublication
        holder.categoryTextView.text = post.category

        // Charger l'image à partir de l'URL
        Glide.with(context).load(post.imageUrl).into(holder.imageView)

        // Initialiser l'état du bouton favori
        val database = FirebaseDatabase.getInstance()
        val userFavRef = database.getReference("Utilisateurs").child(currentUser!!.uid).child("Favoris")

        userFavRef.child(post.postId).get().addOnSuccessListener {
            if (it.exists()) {
                holder.favButton.setImageResource(R.drawable.ic_favorite)
                holder.favButton.tag = true
            } else {
                holder.favButton.setImageResource(R.drawable.baseline_favorite_border_24)
                holder.favButton.tag = false
            }
        }

        if (showRedFavIcon) {
            holder.favButton.setImageResource(R.drawable.ic_favorite)
        } else {
            holder.favButton.setImageResource(R.drawable.baseline_favorite_border_24)
        }

        holder.favButton.setOnClickListener {
            val isFavorited = it.tag as? Boolean ?: false
            if (isFavorited) {
                holder.favButton.setImageResource(R.drawable.baseline_favorite_border_24)
                it.tag = false
                userFavRef.child(post.postId).removeValue()
            } else {
                holder.favButton.setImageResource(R.drawable.ic_favorite)
                it.tag = true
                userFavRef.child(post.postId).setValue(true)
            }
        }

        // Afficher le menu contextuel si le post appartient à l'utilisateur courant
        if (post.userId == currentUser?.uid) {
            holder.menuButton.visibility = View.VISIBLE
            holder.menuButton.setOnClickListener {
                showPopupMenu(it, position)
            }
        } else {
            holder.menuButton.visibility = View.GONE
        }




        holder.imageView.setOnClickListener {
            val intent = Intent(context, DetailsPostActivity::class.java)
            intent.putExtra("postId", post.postId)
            intent.putExtra("imagepost", post.imageUrl)
            intent.putExtra("title", post.title)
            intent.putExtra("description", post.description)
            intent.putExtra("datePublication", post.datePublication)
            intent.putExtra("category", post.category)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTv)
        val descriptionTextView: TextView = itemView.findViewById(R.id.titleTvdesc)
        val locationTextView: TextView = itemView.findViewById(R.id.addressTv)
        val imageView: ImageView = itemView.findViewById(R.id.imageIv)
        val categoryTextView: TextView = itemView.findViewById(R.id.categorytxt)
        val favButton: ImageButton = itemView.findViewById(R.id.favBtn)
        val menuButton: ImageButton = itemView.findViewById(R.id.menuButton)
    }

    private fun showPopupMenu(view: View, position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.post_menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit_post -> {
                    // Rediriger vers l'activité de modification
                    val intent = Intent(context, EditPostActivity::class.java)
                    intent.putExtra("postId", postList[position].postId)
                    context.startActivity(intent)
                    true
                }
                R.id.delete_post -> {
                    // Supprimer l'item
                    deleteItem(position)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    fun updateItem(position: Int, newPost: Post) {
        postList[position] = newPost
        notifyItemChanged(position)
    }

    fun deleteItem(position: Int) {
        val postId = postList[position].postId
        postList.removeAt(position)
        notifyItemRemoved(position)
        // Supprimer l'item de la base de données Firebase
        FirebaseDatabase.getInstance().getReference("posts").child(postId).removeValue()
    }

    fun submitList(newPostList: List<Post>) {
        postList.clear()
        postList.addAll(newPostList)
        notifyDataSetChanged()
    }


}