package com.example.scorpiochat.ui.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scorpiochat.R
import com.example.scorpiochat.SharedPreferencesManager
import com.example.scorpiochat.data.Message
import com.example.scorpiochat.data.User
import com.example.scorpiochat.databinding.ChatsAdapterBinding
import com.example.scorpiochat.utils.getDate
import com.example.scorpiochat.utils.getTime

class ChatsAdapter(
    private val myUserInfo: User,
    private val applicationContext: Context?,
    private val onItemClicked: (Pair<User, Message>) -> Unit,
    private val onItemLongClick: (Pair<User, View>) -> Unit
) : ListAdapter<Triple<User, Message, Int>, ChatsAdapter.ChatsHolder>(DiffCallbackChats) {

    inner class ChatsHolder(private val binding: ChatsAdapterBinding) : RecyclerView.ViewHolder(binding.root) {

        fun setChatValues(data: Triple<User, Message, Int>) {

            val user = data.first
            val message = data.second
            val newMessageCount = data.third
            val context = binding.root.context
            val username: String
            val visibility: Int
            val showProfilePicture: Boolean

            if (user.username == null) {
                username = context.getString(R.string.deleted_user)
                visibility = View.GONE
                showProfilePicture = true
            } else {
                username = user.username

                if (user.blockedUsers?.contains(myUserInfo.userId) == true) {
                    visibility = View.GONE
                    showProfilePicture = false
                } else {
                    visibility = if (user.online == true) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    showProfilePicture = true
                }
            }

            binding.apply {
                txtUsername.text = username
                txtMessage.text = message.text
                if (message.recipientId == myUserInfo.userId && message.seen == false) {
                    txtMessage.setTypeface(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                    imgNewMessage.visibility = View.VISIBLE
                    txtNewMessageCount.visibility = View.VISIBLE
                    txtNewMessageCount.text = newMessageCount.toString()
                } else {
                    txtMessage.setTypeface(Typeface.DEFAULT, Typeface.ITALIC)
                    imgNewMessage.visibility = View.GONE
                    txtNewMessageCount.visibility = View.GONE
                }


                if (myUserInfo.blockedUsers?.contains(user.userId) == true) {
                    imgBlock.visibility = View.VISIBLE
                } else {
                    imgBlock.visibility = View.GONE
                    if (user.userId?.let { SharedPreferencesManager.getIfUserIsMuted(context, it) } == true) {
                        imgMute.visibility = View.VISIBLE
                    } else {
                        imgMute.visibility = View.GONE
                    }
                }

                if (getDate(message.time!!, context) == getDate(System.currentTimeMillis(), context)) {
                    txtTime.text = getTime(message.time, context)
                } else {
                    txtTime.text = getDate(message.time, context)
                }

                imgOnlineIcon.visibility = visibility

                if (applicationContext != null) {
                    if (showProfilePicture) {
                        Glide.with(applicationContext)
                            .load(user.customProfilePictureUri)
                            .placeholder(R.drawable.loading_animation)
                            .error(R.drawable.loading_animation)
                            .into(binding.imgProfilePicture)
                    } else {
                        Glide.with(applicationContext)
                            .load(context.getDrawable(R.drawable.ic_baseline_block_24))
                            .placeholder(R.drawable.loading_animation)
                            .error(R.drawable.loading_animation)
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