package com.example.scorpiochat.ui.fragments.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.scorpiochat.R
import com.example.scorpiochat.SharedPreferencesManager
import com.example.scorpiochat.data.Message
import com.example.scorpiochat.data.User
import com.example.scorpiochat.databinding.AlertDialogMuteConversationLayoutBinding
import com.example.scorpiochat.databinding.FragmentChatsBinding
import com.example.scorpiochat.ui.activities.MainActivity
import com.example.scorpiochat.ui.adapters.ChatsAdapter
import com.example.scorpiochat.viewModels.ChatsViewModel

class ChatsFragment : Fragment() {
    private lateinit var binding: FragmentChatsBinding
    private val viewModel: ChatsViewModel by viewModels()
    private val navigationArgs: ChatsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadMyUserInfo()
        if (navigationArgs.forwardMessage != null) {
            val toolbar = requireActivity().findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

            (activity as MainActivity).showUpButtonInsteadDrawerButton(true)
            toolbar.setTitle(R.string.forward)
            toolbar.subtitle = ""
        }

        val onItemClick: ((Pair<User, Message>) -> Unit) = {
            view.findNavController().navigate(ChatsFragmentDirections.actionNavHomeToConversationFragment(it.first.userId!!, navigationArgs.forwardMessage))
        }
        val onItemLongClick: ((Pair<User, View>) -> Unit) = {
            showPopupMenu(it)
        }

        viewModel.myUserInfo.observe(viewLifecycleOwner) { myUserInfo ->
            if (myUserInfo != null) {

                val recyclerAdapter = ChatsAdapter(myUserInfo, activity?.applicationContext, onItemClick, onItemLongClick)
                val previousUserMessageList: MutableList<Triple<User?, Message, Int>> = mutableListOf()

                binding.recyclerChats.apply {
                    adapter = recyclerAdapter
                    addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                }

                viewModel.loadKeysAndMessages()

                viewModel.listOfUsersAndMessages.observe(viewLifecycleOwner) { listOfUsersAndMessages ->
                    val filteredList = listOfUsersAndMessages.filterNot { previousUserMessageList.contains(it) }
                    listOfUsersAndMessages.sortWith(compareByDescending { it.second.time })

                    if (filteredList.isEmpty()) {
                        recyclerAdapter.submitList(listOfUsersAndMessages)
                    } else {
                        recyclerAdapter.notifyDataSetChanged()
                    }

                    previousUserMessageList.clear()
                    for (item in listOfUsersAndMessages) {
                        previousUserMessageList.add(item)
                    }
                }
            }
        }
        binding.fab.setOnClickListener {
            view.findNavController().navigate(ChatsFragmentDirections.actionNavHomeToAddContactFragment(navigationArgs.forwardMessage))
        }
    }

    private fun showPopupMenu(data: Pair<User, View>) {
        val context = requireContext()
        val user = data.first
        val view = data.second
        val popupMenu = PopupMenu(context, view, Gravity.END)
        val username: String = user.username ?: context.getString(R.string.deleted_user)

        popupMenu.apply {
            menu.add(Menu.NONE, -1, 0, username).apply { isEnabled = false }
            inflate(R.menu.menu_chats)

            if (user.userId?.let { SharedPreferencesManager.getIfUserIsMuted(context, it) } == true) {
                menu.findItem(R.id.header_mute).isVisible = false
            } else {
                menu.findItem(R.id.header_unmute).isVisible = false
            }

            if (user.username == null) {
                menu.apply {
                    findItem(R.id.header_mute).isVisible = false
                    findItem(R.id.header_unmute).isVisible = false
                    findItem(R.id.header_block).isVisible = false
                }
            }

            if (viewModel.myUserInfo.value?.blockedUsers?.contains(user.userId) == true) {
                menu.apply {
                    findItem(R.id.header_mute).isVisible = false
                    findItem(R.id.header_unmute).isVisible = false
                    findItem(R.id.header_block).isVisible = false
                }
            } else {
                menu.apply {
                    findItem(R.id.header_unblock).isVisible = false
                }
            }

            setForceShowIcon(true)
            setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.header_delete -> {
                        showAlertDialog({ viewModel.deleteConversation(user.userId!!) })
                    }
                    R.id.header_mute -> {
                        muteConversation(user.userId!!)
                    }
                    R.id.header_unmute -> {
                        user.userId?.let { viewModel.unmuteConversation(context, it) }
                    }
                    R.id.header_block -> {
                        val checkbox = CheckBox(context)
                        checkbox.setText(R.string.also_delete_conversation)
                        showAlertDialog({ user.userId?.let { viewModel.blockUser(it, checkbox.isChecked) } }, checkbox)
                    }
                    R.id.header_unblock -> {
                        showAlertDialog({ viewModel.unblockUser(user.userId!!) })
                    }
                }
                true
            }
            show()
        }
    }


    private fun showAlertDialog(function: () -> Unit, checkBox: CheckBox? = null) {
        val context = requireContext()
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.are_you_sure))
            .setView(checkBox)
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                function()
                view?.findNavController()?.navigate(ChatsFragmentDirections.actionNavHomeSelf())
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun muteConversation(userId: String) {
        val context = requireContext()
        val alertDialogBinding = activity?.layoutInflater?.let { AlertDialogMuteConversationLayoutBinding.inflate(it) }


        AlertDialog.Builder(context)
            .setView(alertDialogBinding?.root)
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                when (alertDialogBinding?.radioGroupMute?.checkedRadioButtonId) {
                    alertDialogBinding?.radioBtn15Minutes?.id -> {
                        viewModel.muteConversation(context, userId, 15)
                    }
                    alertDialogBinding?.radioBtn1Hour?.id -> {
                        viewModel.muteConversation(context, userId, 60)
                    }
                    alertDialogBinding?.radioBtn8Hours?.id -> {
                        viewModel.muteConversation(context, userId, 480)
                    }
                    alertDialogBinding?.radioBtnPermanently?.id -> {
                        viewModel.muteConversation(context, userId)
                    }
                }

            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }
}