package com.example.scorpiochat.ui.fragments.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.scorpiochat.R
import com.example.scorpiochat.data.Message
import com.example.scorpiochat.data.User
import com.example.scorpiochat.databinding.FragmentConversationBinding
import com.example.scorpiochat.getDate
import com.example.scorpiochat.getTime
import com.example.scorpiochat.ui.adapters.ConversationAdapter
import com.example.scorpiochat.viewModel.ConversationViewModel

class ConversationFragment : Fragment() {
    private lateinit var binding: FragmentConversationBinding
    private val navigationArgs: ConversationFragmentArgs by navArgs()
    private val viewModel: ConversationViewModel by activityViewModels()
    private var repliedTo: Long? = null
    private var forwardMessageText: String? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        forwardMessageText = navigationArgs.forwardMessage
        viewModel.loadUserInfo(navigationArgs.userId)

        if (forwardMessageText != null) {
            showActionWindow(Message(text = forwardMessageText), Action.FORWARD)
        }


        val onItemLongClick: ((Pair<Message, View>) -> Unit) = {
            showPopupMenu(it)
        }

        val clickToScroll: (Long) -> Unit = {
            for ((index, item) in viewModel.messagesList.value!!.withIndex()) {
                if (item.first.time == it) {
                    binding.recyclerMessages.scrollToPosition(index)
                }
            }
        }
        val recyclerAdapter = ConversationAdapter(viewModel.getMyId(), navigationArgs.userId, onItemLongClick, clickToScroll)
        val previousUserMessageList: MutableList<Pair<Message, Message?>> = mutableListOf()

        binding.recyclerMessages.adapter = recyclerAdapter
        viewModel.messagesList.observe(viewLifecycleOwner) { it ->
            val filteredList = it.filterNot { previousUserMessageList.contains(it) }

            if (filteredList.isEmpty()) {
                recyclerAdapter.submitList(it)
            } else {
                recyclerAdapter.notifyDataSetChanged()
            }

            previousUserMessageList.clear()
            for (item in it) {
                previousUserMessageList.add(item)
            }
        }

        viewModel.userInfo.observe(viewLifecycleOwner) {
            if(it!= null) {
                setUserInformation(it)
            }
            viewModel.loadMessage(navigationArgs.userId)
        }

        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }
    }

    private fun setUserInformation(user: User) {
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        val context = requireContext()
        val title = requireActivity().findViewById<TextView>(R.id.txt_toolbar_title)
        val subtitle = requireActivity().findViewById<TextView>(R.id.txt_toolbar_subtitle)
        val profileIcon = requireActivity().findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.img_toolbar_profile_icon)

        requireActivity().findViewById<LinearLayout>(R.id.layout_toolbar_user_info).visibility = View.VISIBLE
        actionBar?.setDisplayShowTitleEnabled(false)


        Glide.with(context)
            .load(user.customProfilePictureUri)
            .placeholder(R.drawable.loading_animation)
            .error(R.drawable.loading_animation)
            .into(profileIcon)


        if (user.username != null) {
            title.text = user.username
            binding.layoutSendMessage.visibility = View.VISIBLE

            if (user.online == true) {
                subtitle.text = context.getString(R.string.online)
            } else {
                if (getDate(user.lastSeen!!, context) == getDate(System.currentTimeMillis(), context)) {
                    subtitle.text = context.getString(R.string.last_seen_time, getTime(user.lastSeen!!, context))
                } else {
                    subtitle.text = context.getString(R.string.last_seen_date_time, getDate(user.lastSeen!!, context), getTime(user.lastSeen!!, context))
                }
            }
        } else {
            title.text = context.getString(R.string.deleted_user)
            subtitle.text = ""
            binding.layoutSendMessage.visibility = View.GONE
        }
    }


    private fun sendMessage() {
        val text = binding.editTextSendMessage.text.toString()
        if (text.isNotBlank()) {
            val time = System.currentTimeMillis()
            val recipientId = navigationArgs.userId
            val message = Message(text = text, time = time, recipientId = recipientId, seen = false, repliedTo = repliedTo, forwarded = forwardMessageText)
            viewModel.sendMessage(message)
            binding.editTextSendMessage.setText("")
            if (binding.layoutActionWindow.isVisible) {
                hideActionWindow(Action.REPLY)
            }
        }
    }


    private fun showPopupMenu(data: Pair<Message, View>) {
        val context = requireContext()
        val message = data.first
        val view = data.second
        val isNotMyMessage = message.recipientId == viewModel.getMyId()
        val popupMenu = PopupMenu(context, view, Gravity.END)

        popupMenu.apply {
            inflate(R.menu.menu_conversation)
            setForceShowIcon(true)
            if (isNotMyMessage) {
                menu.removeItem(R.id.header_edit)
            }

            if (viewModel.userInfo.value == null) {
                menu.apply {
                    removeItem(R.id.header_edit)
                    removeItem(R.id.header_reply)
                }
            }

            setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.header_reply -> {
                        showActionWindow(message, Action.REPLY)
                    }
                    R.id.header_copy -> {
                        copyText(message.text!!)
                    }
                    R.id.header_forward -> {
                        forwardMessage(message)
                    }
                    R.id.header_edit -> {
                        editMessage(message)
                    }
                    R.id.header_delete -> {
                        deleteMessage(isNotMyMessage, message)
                    }
                }
                true
            }
            show()
        }
    }

    private fun copyText(text: String) {
        val clipboard: ClipboardManager = activity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("", text)
        clipboard.setPrimaryClip(clipData)
        Toast.makeText(requireContext(), requireContext().getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    private fun deleteMessage(isNotMyMessage: Boolean, message: Message) {
        val context = requireContext()
        val checkBox: CheckBox? = if (isNotMyMessage || viewModel.userInfo.value?.username == null) {
            null
        } else {
            CheckBox(context)
        }
        checkBox?.text = context.getString(R.string.also_delete_for, viewModel.userInfo.value?.username)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.are_you_sure))
            .setView(checkBox)
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                viewModel.deleteMessage(checkBox?.isChecked, message)
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }


    private fun showActionWindow(message: Message, action: Action) {
        if (binding.btnEditMessage.isVisible) {
            hideActionWindow(Action.EDIT)
        }
        val context = requireContext()
        val title: String?
        val logo: Int

        when (action) {
            Action.EDIT -> {
                title = context.getString(R.string.edited_text)
                logo = R.drawable.ic_baseline_edit_24
                binding.apply {
                    editTextSendMessage.setText(message.text)
                    btnSendMessage.visibility = View.GONE
                    btnEditMessage.visibility = View.VISIBLE
                }
            }
            Action.REPLY -> {
                title = viewModel.userInfo.value?.username
                logo = R.drawable.ic_baseline_turn_left_24
                repliedTo = message.time
            }
            else -> {
                title = context.getString(R.string.forwarded_text)
                logo = R.drawable.ic_baseline_turn_right_24
            }
        }

        binding.apply {
            layoutActionWindow.visibility = View.VISIBLE
            txtActionTitle.text = title
            txtActionMessageText.text = message.text
            imgActionLogo.setBackgroundResource(logo)

            btnCloseActionMessage.setOnClickListener {
                hideActionWindow(action)
            }
        }
    }

    private fun hideActionWindow(action: Action) {
        binding.apply {
            layoutActionWindow.visibility = View.GONE
            btnSendMessage.visibility = View.VISIBLE
            repliedTo = null
            forwardMessageText = null
            if (action == Action.EDIT) {
                editTextSendMessage.setText("")
                btnEditMessage.visibility = View.GONE
            }
        }
    }

    private fun editMessage(message: Message) {
        showActionWindow(message, Action.EDIT)
        binding.btnEditMessage.setOnClickListener {
            val editedText = binding.editTextSendMessage.text.toString()

            if (editedText.isNotBlank() && editedText != message.text) {
                viewModel.editMessage(message.recipientId!!, editedText, message)
                hideActionWindow(Action.EDIT)
            } else if (editedText == message.text) {
                hideActionWindow(Action.EDIT)
            }
        }
    }

    private fun forwardMessage(message: Message) {
        view?.findNavController()?.navigate(ConversationFragmentDirections.actionConversationFragmentToNavHome(message.text))
    }

    override fun onStop() {
        super.onStop()
        viewModel.clearUserInfo()
        requireActivity().apply {
            findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.img_toolbar_profile_icon).setBackgroundResource(R.drawable.loading_animation)
            findViewById<LinearLayout>(R.id.layout_toolbar_user_info).visibility = View.GONE
            (this as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(true)
        }
    }
}

enum class Action {
    REPLY, EDIT, FORWARD
}