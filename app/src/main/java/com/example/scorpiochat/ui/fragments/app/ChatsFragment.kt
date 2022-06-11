package com.example.scorpiochat.ui.fragments.app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.scorpiochat.R
import com.example.scorpiochat.data.Message
import com.example.scorpiochat.data.User
import com.example.scorpiochat.databinding.FragmentChatsBinding
import com.example.scorpiochat.ui.activities.MainActivity
import com.example.scorpiochat.ui.adapters.ChatsAdapter
import com.example.scorpiochat.viewModel.ChatsViewModel

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
        viewModel.loadKeysAndMessages()

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

        val recyclerAdapter = ChatsAdapter(viewModel.getMyId(), activity?.applicationContext, onItemClick, onItemLongClick)
        val previousUserMessageList: MutableList<Triple<User?, Message, Int>> = mutableListOf()

        binding.recyclerChats.apply {
            adapter = recyclerAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        viewModel.listOfUsersAndMessages.observe(viewLifecycleOwner) { it ->
            val filteredList = it.filterNot { previousUserMessageList.contains(it) }
            it.sortWith(compareByDescending { it.second.time })

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
            setForceShowIcon(true)
            setOnMenuItemClickListener { item: MenuItem? ->
                when (item!!.itemId) {
                    R.id.header_delete -> {
                        showAlertDialog { viewModel.deleteConversation(user.userId!!) }
                    }
                    R.id.header_mute -> {
                        showAlertDialog { Toast.makeText(context, "header2", Toast.LENGTH_SHORT).show() }
                    }
                    R.id.header_block -> {
                        showAlertDialog { Toast.makeText(context, "header3", Toast.LENGTH_SHORT).show() }
                    }
                }
                true
            }
            show()
        }
    }


    private fun showAlertDialog(function: () -> Unit) {
        val context = requireContext()
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.are_you_sure))
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                function()
                view?.findNavController()?.navigate(ChatsFragmentDirections.actionNavHomeSelf())
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }
}