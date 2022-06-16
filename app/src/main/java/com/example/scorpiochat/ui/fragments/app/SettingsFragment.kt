package com.example.scorpiochat.ui.fragments.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.scorpiochat.R
import com.example.scorpiochat.databinding.AlertDialogLayoutBinding
import com.example.scorpiochat.databinding.FragmentSettingsBinding
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
                        setProfilePictureFromUri(task.result, context)
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
            binding.componentSetProfilePicture.setProfilePictureFromUri(viewModel.userInfo.value?.customProfilePictureUri?.toUri(), requireContext())
        }
    }

    private fun resetPassword() {
        val context = requireContext()
        val function = { viewModel.changePassword(context) }
        val title = context.getString(R.string.send_reset_password_email)
        showAlertDialog(title, function = function)
    }


    private fun changeUsername() {
        val context = requireContext()
        val title = context.getString(R.string.change_username)
        val alertDialogBinding = activity?.layoutInflater?.let { AlertDialogLayoutBinding.inflate(it) }
        val function = {viewModel.changeUsername(alertDialogBinding?.editTxtAlertDialog?.text.toString())}

        alertDialogBinding?.apply {
            editTxtAlertDialog.setText(viewModel.userInfo.value?.username ?: return)
            editTxtPassword.visibility = View.GONE
        }

        showAlertDialog(title,alertDialogBinding, function)
    }

    private fun changeEmail() {
        val context = requireContext()
        val title = context.getString(R.string.change_email)
        val alertDialogBinding = activity?.layoutInflater?.let { AlertDialogLayoutBinding.inflate(it) }

        alertDialogBinding?.apply {
            editTxtAlertDialog.setText(viewModel.getUserEmail())

        }

        val newEmail = alertDialogBinding?.editTxtAlertDialog?.text.toString()
        val password = alertDialogBinding?.editTxtPassword?.text.toString()
        val function = { viewModel.changeEmail(newEmail, password, context) }

        showAlertDialog(title, alertDialogBinding, function)
    }

    private fun signOut() {
        val context = requireContext()
        val function = {viewModel.signOut(context)}
        val title = context.getString(R.string.are_you_sure)

        showAlertDialog(title, function = function)
    }

    private fun deleteAccount() {
        val context = requireContext()
        val title = context.getString(R.string.delete_account)
        val alertDialogBinding = activity?.layoutInflater?.let { AlertDialogLayoutBinding.inflate(it) }
        val function = {viewModel.deleteAccount(alertDialogBinding?.editTxtPassword?.text.toString(), context)}

        alertDialogBinding?.editTxtAlertDialog?.visibility = View.GONE
        showAlertDialog(title, alertDialogBinding, function)
    }


    private fun showAlertDialog(title: String, alertDialogBinding: AlertDialogLayoutBinding? = null, function: () -> Unit) {
        val context = requireContext()

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(alertDialogBinding?.root)
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                function()
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }
}