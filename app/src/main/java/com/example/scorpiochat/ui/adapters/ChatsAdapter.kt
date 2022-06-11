package com.example.scorpiochat.ui.adapters

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scorpiochat.*
import com.example.scorpiochat.data.Message
import com.example.scorpiochat.data.User
import com.example.scorpiochat.databinding.ChatsAdapterBinding
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ChatsAdapter(private val myId: String, private val applicationContext: Context?, private val onItemClicked: (Pair<User, Message>) -> Unit,private val onItemLongClick: (Pair<User, View>) -> Unit ) :
    ListAdapter<Triple<User, Message, Int>, ChatsAdapter.ChatsHolder>(DiffCallbackChats) {

    inner class ChatsHolder(private val binding: ChatsAdapterBinding) : RecyclerView.ViewHolder(binding.root) {
        private val storage = FirebaseStorage.getInstance().reference

        fun setChatValues(data: Triple<User, Message, Int>) {

            val user = data.first
            val message = data.second
            val newMessageCount = data.third
            val context = binding.root.context

            val username: String
            val visibility: Int
            val storageReference: StorageReference

            if (user.username == null) {
                username = context.getString(R.string.deleted_user)
                visibility = View.GONE
                storageReference = storage.child(defaultProfilePicture).child(default_icon)
            } else {
                username = user.username

                visibility = if (user.online == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                storageReference = if (user.customProfilePicture == true) {
                    storage.child(user.userId!!).child(customProfilePicture)
                } else {
                    storage.child(defaultProfilePicture).child(default_icon)
                }
            }

            binding.apply {
                txtUsername.text = username
                txtMessage.text = message.text
                if (message.recipientId == myId && message.seen == false) {
                    txtMessage.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                    imgNewMessage.visibility = View.VISIBLE
                    txtNewMessageCount.visibility = View.VISIBLE
                    txtNewMessageCount.text = newMessageCount.toString()
                } else {
                    txtMessage.setTypeface(Typeface.DEFAULT, Typeface.ITALIC)
                    imgNewMessage.visibility = View.GONE
                    txtNewMessageCount.visibility = View.GONE
                }

                if (getDate(message.time!!, context) == getDate(System.currentTimeMillis(), context)) {
                    txtTime.text = getTime(message.time, context)
                } else {
                    txtTime.text = getDate(message.time, context)
                }

                imgOnlineIcon.visibility = visibility

                if (applicationContext != null) {
                    storageReference.downloadUrl.addOnCompleteListener { task ->
                        Glide.with(applicationContext)
                            .load(task.result)
                            .into(binding.imgProfilePicture)
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ChatsAdapter.ChatsHolder, position: Int) {
        holder.setChatValues(getItem(position))
        holder.itemView.setOnClickListener {
            onItemClicked(Pair(getItem(position).first, getItem(position).second))
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(Pair(getItem(position).first, it))
            true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsAdapter.ChatsHolder {
        return ChatsHolder(ChatsAdapterBinding.inflate(LayoutInflater.from(parent.context)))
    }

    companion object DiffCallbackChats : DiffUtil.ItemCallback<Triple<User, Message, Int>>() {
        override fun areItemsTheSame(oldItem: Triple<User, Message, Int>, newItem: Triple<User, Message, Int>): Boolean {
            return oldItem.first.userId == newItem.first.userId
        }

        override fun areContentsTheSame(oldItem: Triple<User, Message, Int>, newItem: Triple<User, Message, Int>): Boolean {
            return oldItem.first.userId == newItem.first.userId
        }
    }
}