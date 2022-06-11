package com.example.scorpiochat.ui.fragments.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.scorpiochat.R
import com.example.scorpiochat.SharedPreferencesManager
import com.example.scorpiochat.databinding.ChangeEmailAlertDialogLayoutBinding
import com.example.scorpiochat.databinding.FragmentSettingsBinding
import com.example.scorpiochat.ui.activities.LoginActivity
import com.example.scorpiochat.ui.activities.MainActivity
import com.example.scorpiochat.viewModel.SettingsViewModel


class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadUserInfo()
        val context = requireContext()
        binding.apply {
            componentSetProfilePicture.setRegisterForActivityResult(this@SettingsFragment)

            txtUsername.setOnClickListener {
                changeUsername()
            }

            btnDelete.setOnClickListener {
                viewModel.getDefaultProfilePictureStorageRef().downloadUrl.addOnCompleteListener { task ->
                    binding.componentSetProfilePicture.apply {
                        setProfilePictureFromUri(task.result)
                        setProfilePictureUri(null)
                    }
                }
            }

            btnConfirm.setOnClickListener {
                viewModel.changeProfilePicture(binding.componentSetProfilePicture.profilePicture)
                Toast.makeText(context, context.getString(R.string.profile_picture_saved), Toast.LENGTH_SHORT).show()
            }

            txtResetPassword.setOnClickListener {
                resetPassword()
            }

            txtChangeEmail.setOnClickListener {
                changeEmail()
            }

            txtSignOut.setOnClickListener {
                signOut()
            }

            txtDeleteAccount.setOnClickListener {
                deleteAccount()
            }
        }

        viewModel.userInfo.observe(viewLifecycleOwner) {
            viewModel.getCurrentProfilePictureStorageRef().downloadUrl.addOnCompleteListener { task ->
                binding.componentSetProfilePicture.setProfilePictureFromUri(task.result)
            }
        }
    }

    private fun resetPassword() {
        val context = requireContext()
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.send_reset_password_email))
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                viewModel.changePassword(context)
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }


    private fun changeUsername() {
        val context = requireContext()
        val inputEditTextField = EditText(context)
        inputEditTextField.setText(viewModel.userInfo.value?.username ?: return)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.change_username))
            .setView(inputEditTextField)
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                val editTextInput = inputEditTextField.text.toString()
                viewModel.changeUsername(editTextInput)
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun changeEmail() {
        val context = requireContext()

        val inflater = activity?.layoutInflater
        val alertDialogBinding = inflater?.let { ChangeEmailAlertDialogLayoutBinding.inflate(it) }
        alertDialogBinding?.editTxtEmail?.setText(viewModel.getUserEmail())

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.change_email))
            .setView(alertDialogBinding?.root)
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                val newEmail = alertDialogBinding?.editTxtEmail?.text.toString()
                val password = alertDialogBinding?.editTxtPassword?.text.toString()
                viewModel.changeEmail(newEmail, password, context)
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()


    }

    private fun signOut() {
        val context = requireContext()

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.are_you_sure))
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                viewModel.signOut(context)
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun deleteAccount() {
        val context = requireContext()
        val inputEditTextField = EditText(context)
        inputEditTextField.hint = context.getString(R.string.confirm_with_password)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.delete_account))
            .setView(inputEditTextField)
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                val editTextInput = inputEditTextField.text.toString()
                viewModel.deleteAccount(context,editTextInput)
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }


    private fun createDialog(title: String, editText: EditText): AlertDialog.Builder {
        val context = requireContext()

        return AlertDialog.Builder(context)
            .setTitle(title)
            .setView(editText)
            .setNegativeButton(context.getString(R.string.cancel), null)
    }
}