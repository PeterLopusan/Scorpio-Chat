package com.example.scorpiochat.ui.fragments.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.scorpiochat.databinding.FragmentAddContactBinding
import com.example.scorpiochat.ui.adapters.SearchingResultsAdapter
import com.example.scorpiochat.viewModels.AddContactViewModel

class AddContactFragment : Fragment() {
    private lateinit var binding: FragmentAddContactBinding
    private val viewModel: AddContactViewModel by viewModels()
    private val navigationArgs: AddContactFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAddContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerAdapter = SearchingResultsAdapter {
            view.findNavController().navigate(AddContactFragmentDirections.actionAddContactFragmentToConversationFragment(it.userId!!, navigationArgs.forwardMessage))
        }

        viewModel.userList.observe(viewLifecycleOwner) {
            binding.recyclerViewResults.apply {
                adapter = recyclerAdapter
                addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            }
            recyclerAdapter.submitList(it)
        }

        binding.btnSearch.setOnClickListener {
            findUsers()
        }
    }


    private fun findUsers() {
        val searchedText = binding.editTextSearch.text.toString()
        viewModel.findUsers(searchedText)
    }
}