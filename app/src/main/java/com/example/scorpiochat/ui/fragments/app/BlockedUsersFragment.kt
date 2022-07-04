package com.example.scorpiochat.ui.fragments.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.scorpiochat.R
import com.example.scorpiochat.data.User
import com.example.scorpiochat.databinding.FragmentBlockedUsersBinding
import com.example.scorpiochat.ui.adapters.BlockedUsersAdapter
import com.example.scorpiochat.viewModels.BlockedUsersViewModel

class BlockedUsersFragment : Fragment() {

    private lateinit var binding: FragmentBlockedUsersBinding
    private val viewModel: BlockedUsersViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBlockedUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadMyUserInfo()

        viewModel.myUserInfo.observe(viewLifecycleOwner) { user ->
            if (user.blockedUsers != null) {
                viewModel.loadBlockedUsers(user.blockedUsers!!)
            }
        }

        viewModel.listOfBlockedUsers.observe(viewLifecycleOwner) {
            val onRemoveButtonClick: ((User) -> Unit) = { userForUnblock ->
                unblockUser(userForUnblock)
            }

            val recyclerAdapter = BlockedUsersAdapter(viewModel.getMyId(), onRemoveButtonClick)
            binding.recyclerBlockedUsers.apply {
                adapter = recyclerAdapter
                addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            }

            recyclerAdapter.submitList(it)

            if (it.size == 0) {
                binding.txtNoBlockedUsers.visibility = View.VISIBLE
            } else {
                binding.txtNoBlockedUsers.visibility = View.GONE
            }
        }
    }


    private fun unblockUser(user: User) {
        val context = requireContext()

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.are_you_sure))
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                user.userId?.let { viewModel.unBlockUser(it) }
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }
}