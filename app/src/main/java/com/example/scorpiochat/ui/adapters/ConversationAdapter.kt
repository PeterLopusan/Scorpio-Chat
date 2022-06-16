package com.example.scorpiochat.ui.adapters

import android.content.res.ColorStateList
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.scorpiochat.*
import com.example.scorpiochat.data.Message
import com.example.scorpiochat.databinding.ConversationAdapterBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ConversationAdapter(val myId: String, val chatId: String, private val onItemLongClick: (Pair<Message, View>) -> Unit, val clickToScroll: (Long) -> Unit) :
    ListAdapter<Pair<Message, Message?>, ConversationAdapter.ConversationHolder>(DiffCallbackConversation) {

    private var previousItem: ConversationAdapterBinding? = null
    private var previousDate = ""
    private var previousPosition = -1

    inner class ConversationHolder(private val binding: ConversationAdapterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setMessageValues(data: Pair<Message, Message?>, itemPosition: Int) {
            val message = data.first
            val repliedToMessage = data.second

            binding.layoutLinkedMessage.setOnClickListener {
                if(repliedToMessage !=null) {
                    clickToScroll(repliedToMessage.time!!)
                }
            }

            binding.apply {
                val context = root.context
                var timeAndSeen: String
                val start: Int
                val end: Int
                val backgroundColor: Int

                txtMessage.text = message.text
                txtMessage.requestLayout()


                if (repliedToMessage != null) {
                    layoutLinkedMessage.visibility = View.VISIBLE
                    txtLinkedMessageText.text = repliedToMessage.text
                    txtLinkedMessageTitle.text = context.getString(R.string.replied)
                } else if(message.forwarded != null) {
                    layoutLinkedMessage.visibility = View.VISIBLE
                    txtLinkedMessageText.text = message.forwarded
                    txtLinkedMessageTitle.text = context.getString(R.string.forwarded)
                } else {
                    layoutLinkedMessage.visibility = View.GONE
                }

                if (message.recipientId == myId) {
                    layoutMessageRow.gravity = Gravity.START
                    start = 0
                    end = 300
                    backgroundColor = context.getColor(R.color.purple_500)
                    timeAndSeen = getTime(message.time!!, context)
                    if (message.seen == false) {
                        makeMessageSeen(message.time.toString())
                    }
                } else {
                    layoutMessageRow.gravity = Gravity.END
                    start = 300
                    end = 0
                    backgroundColor = context.getColor(R.color.teal_700)

                    timeAndSeen = if (message.seen == true) {
                        context.getString(R.string.time_seen, getTime(message.time!!, context))
                    } else {
                        getTime(message.time!!, context)
                    }
                }
                if(message.edited == true) {
                    timeAndSeen = context.getString(R.string.time_edit, timeAndSeen)
                }

                txtTime.text = timeAndSeen

                layoutMessage.backgroundTintList = ColorStateList.valueOf(backgroundColor)
                (layoutMessage.layoutParams as LinearLayout.LayoutParams).apply {
                    marginStart = start
                    marginEnd = end
                }


                if (itemPosition > previousPosition) {

                    if (getDate(message.time, context) != previousDate) {
                        txtDate.visibility = View.VISIBLE
                        txtDate.text = getDate(message.time, context)
                        previousDate = getDate(message.time, context)
                    } else {
                        txtDate.visibility = View.GONE
                    }
                } else {
                    if (getDate(message.time, context) != previousDate) {
                        previousItem?.txtDate?.visibility = View.VISIBLE
                        previousItem?.txtDate?.text = previousDate
                        previousDate = getDate(message.time, context)
                    } else {
                        previousItem?.txtDate?.visibility = View.GONE
                        txtDate.visibility = View.GONE
                    }
                }

                if (itemPosition == 0) {
                    txtDate.visibility = View.VISIBLE
                    txtDate.text = getDate(message.time, context)
                }

                previousItem = binding
                previousPosition = itemPosition
            }
        }

        private fun makeMessageSeen(messageKey: String) {
            val database = FirebaseDatabase.getInstance().reference
            val update = mapOf("seen" to true)
            database.child(chatId).child(conversations).child(myId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChild(messageKey)) {
                        database.child(chatId).child(conversations).child(myId).child(messageKey).updateChildren(update)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("TAG", error.toString())
                }

            })
            database.child(myId).child(conversations).child(chatId).child(messageKey).updateChildren(update)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationAdapter.ConversationHolder {
        return ConversationHolder(ConversationAdapterBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }


    override fun onBindViewHolder(holder: ConversationAdapter.ConversationHolder, position: Int) {
        holder.setMessageValues(getItem(position), holder.adapterPosition)
        holder.itemView.setOnLongClickListener {
            onItemLongClick(Pair(getItem(position).first, it))
            true
        }
    }

    companion object DiffCallbackConversation : DiffUtil.ItemCallback<Pair<Message, Message?>>() {
        override fun areItemsTheSame(oldItem: Pair<Message, Message?>, newItem: Pair<Message, Message?>): Boolean {
            return oldItem.first.time == newItem.first.time
        }

        override fun areContentsTheSame(oldItem: Pair<Message, Message?>, newItem: Pair<Message, Message?>): Boolean {
            return oldItem.first.time == newItem.first.time
        }
    }
}