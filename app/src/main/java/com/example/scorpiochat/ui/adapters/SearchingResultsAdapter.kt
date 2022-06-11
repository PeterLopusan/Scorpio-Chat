package com.example.scorpiochat.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scorpiochat.customProfilePicture
import com.example.scorpiochat.data.User
import com.example.scorpiochat.databinding.SearchingResultsAdapterBinding
import com.example.scorpiochat.defaultProfilePicture
import com.example.scorpiochat.default_icon
import com.google.firebase.storage.FirebaseStorage

class SearchingResultsAdapter(private val onItemClicked: (User) -> Unit): ListAdapter<User, SearchingResultsAdapter.SearchingResultsHolder>(DiffCallbackSearchingResults) {

    inner class SearchingResultsHolder(private val binding: SearchingResultsAdapterBinding): RecyclerView.ViewHolder(binding.root) {
        fun setSearchingResult (user: User) {
            binding.txtUsername.text = user.username

            val storageReference = if(user.customProfilePicture == true) {
                FirebaseStorage.getInstance().reference.child(user.userId!!).child(customProfilePicture)
            } else {
                FirebaseStorage.getInstance().reference.child(defaultProfilePicture).child(default_icon)
            }

            storageReference.downloadUrl.addOnCompleteListener { task ->
                Glide.with(binding.root.context)
                    .load(task.result)
                    .into(binding.imgProfilePicture)
            }
        }
    }

    override fun onBindViewHolder(holder: SearchingResultsHolder, position: Int) {
        holder.setSearchingResult(getItem(position))
        holder.itemView.setOnClickListener {
            onItemClicked(getItem(position))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchingResultsHolder {
        return SearchingResultsHolder(SearchingResultsAdapterBinding.inflate(LayoutInflater.from(parent.context)))
    }

    companion object DiffCallbackSearchingResults : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }
    }
}