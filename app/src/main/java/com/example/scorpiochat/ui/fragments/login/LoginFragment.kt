package com.example.scorpiochat.ui.fragments.login

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.example.scorpiochat.R
import com.example.scorpiochat.databinding.FragmentLoginBinding
import com.example.scorpiochat.viewModel.LoginViewModel

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val loginViewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            if (requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                imgLogo.setBackgroundResource(R.drawable.logo_text_black)
            } else {
                imgLogo.setBackgroundResource(R.drawable.logo_text)
            }

            btnCreateAccount.setOnClickListener {
                view.findNavController().navigate(LoginFragmentDirections.actionSignInFragmentToCreateAccountFragment())
            }

            btnConfirm.setOnClickListener {
                login()
            }
        }
    }

    private fun login() {
        val context = requireContext()
        val email = binding.editTxtEmail.text.toString()
        val password = binding.editTxtPassword.text.toString()

        if (email.isNotBlank() && password.isNotBlank()
        ) {
            loginViewModel.login(email, password).observe(viewLifecycleOwner) {
                if (!it) {
                    Toast.makeText(context, context.getString(R.string.login_failed), Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, context.getString(R.string.missing_input_value), Toast.LENGTH_LONG).show()
        }
    }
}