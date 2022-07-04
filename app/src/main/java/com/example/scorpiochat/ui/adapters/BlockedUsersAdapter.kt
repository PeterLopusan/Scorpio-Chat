package com.example.scorpiochat.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scorpiochat.R
import com.example.scorpiochat.data.User
import com.example.scorpiochat.databinding.BlockedUsersAdapterBinding

class BlockedUsersAdapter(private val myId: String ,private val onRemoveButtonClicked: (User) -> Unit) : ListAdapter<User, BlockedUsersAdapter.BlockedUsersHolder>(DiffCallbackBlockedUsers) {

    inner class BlockedUsersHolder(private val binding: BlockedUsersAdapterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setBlockedUsers(user: User) {
            binding.apply {
                val context = root.context
                var showProfilePicture = true
                txtUsername.text = user.username ?: context.getString(R.string.deleted_user)

                if(user.blockedUsers?.contains(myId) == true) {
                    showProfilePicture = false
                }

                if(showProfilePicture) {
                    Glide.with(context)
                        .load(user.customProfilePictureUri)
                        .placeholder(R.drawable.loading_animation)
                        .error(R.drawable.loading_animation)
                        .into(binding.imgProfilePicture)
                } else {
                    Glide.with(context)
                        .load(context.getDrawable(R.drawable.ic_baseline_block_24))
                        .placeholder(R.drawable.loading_animation)
                        .error(R.drawable.loading_animation)
                        .into(binding.imgProfilePicture)
                }



                imgRemoveFromBlockedUsers.setOnClickListener {
                    onRemoveButtonClicked(user)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedUsersHolder {
        return BlockedUsersHolder(BlockedUsersAdapterBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BlockedUsersHolder, position: Int) {
        holder.setBlockedUsers(getItem(position))
    }

    companion object DiffCallbackBlockedUsers : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }
    }
}