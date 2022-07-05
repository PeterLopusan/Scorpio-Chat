package com.example.scorpiochat.ui.fragments.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.scorpiochat.R
import com.example.scorpiochat.databinding.FragmentCreateAccountBinding
import com.example.scorpiochat.viewModels.LoginViewModel


class CreateAccountFragment : Fragment() {
    private lateinit var binding: FragmentCreateAccountBinding
    private val loginViewModel: LoginViewModel by activityViewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCreateAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginViewModel.getStorageWithDefaultPictureImage().downloadUrl.addOnCompleteListener { task ->
            binding.componentSetProfilePicture.setProfilePictureFromUri(task.result)
        }

        binding.apply {
            componentSetProfilePicture.setRegisterForActivityResult(this@CreateAccountFragment)

            btnConfirm.setOnClickListener {
                createAccount()
            }

            btnDelete.setOnClickListener {
                deleteCustomPicture()
            }
        }
    }

    private fun createAccount() {
        val context = requireContext()
        if (binding.editTxtUsername.text.toString().isNotBlank() && binding.editTxtEmail.text.toString().isNotBlank() && binding.editTxtPassword.text.toString().isNotBlank()) {
            if (binding.editTxtPassword.text.toString() != binding.editTxtConfirmPassword.text.toString()) {
                Toast.makeText(context, context.getString(R.string.passwords_do_not_match), Toast.LENGTH_SHORT).show()
            } else {
                val email = binding.editTxtEmail.text.toString()
                val password = binding.editTxtPassword.text.toString()
                val username = binding.editTxtUsername.text.toString()
                loginViewModel.createAccount(email, password, username, binding.componentSetProfilePicture.profilePicture, context).observe(viewLifecycleOwner) {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, context.getString(R.string.missing_input_value), Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteCustomPicture() {
        loginViewModel.getStorageWithDefaultPictureImage().downloadUrl.addOnCompleteListener { task ->
            binding.componentSetProfilePicture.apply {
                setProfilePictureFromUri(task.result)
                setProfilePictureUri(null)
            }
        }
    }
}