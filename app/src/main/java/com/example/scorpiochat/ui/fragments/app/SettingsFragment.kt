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
import androidx.navigation.findNavController
import com.example.scorpiochat.R
import com.example.scorpiochat.databinding.AlertDialogLayoutBinding
import com.example.scorpiochat.databinding.FragmentSettingsBinding
import com.example.scorpiochat.ui.activities.MainActivity
import com.example.scorpiochat.viewModels.SettingsViewModel

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

            txtBlockedUsers.setOnClickListener {
                view.findNavController().navigate(SettingsFragmentDirections.actionNavSettingsToBlockedUsersFragment())
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
            binding.componentSetProfilePicture.setProfilePictureFromUri(viewModel.userInfo.value?.customProfilePictureUri?.toUri())
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
        val alertDialogBinding = activity?.layoutInflater?.let { AlertDialogLayoutBinding.inflate(it) }
        val function = { viewModel.changeUsername(alertDialogBinding?.editTxtAlertDialog?.text.toString()) }

        alertDialogBinding?.apply {
            editTxtAlertDialog.setText(viewModel.userInfo.value?.username ?: return)
            editTxtPassword.visibility = View.GONE
            txtAlertDialogTitle.text = context.getString(R.string.change_username)
        }

        showAlertDialog(alertDialogBinding = alertDialogBinding, function = function)
    }

    private fun changeEmail() {
        val context = requireContext()
        val alertDialogBinding = activity?.layoutInflater?.let { AlertDialogLayoutBinding.inflate(it) }

        alertDialogBinding?.apply {
            editTxtAlertDialog.setText(viewModel.getUserEmail())
            txtAlertDialogTitle.text = context.getString(R.string.change_email)
        }
        val activity = (activity as MainActivity?)
        val function = {
            viewModel.changeEmail(alertDialogBinding?.editTxtAlertDialog?.text.toString(), alertDialogBinding?.editTxtPassword?.text.toString(), context).observe(viewLifecycleOwner) {
                if (it == true) {
                    (activity as MainActivity).refreshEmailInNavHeader()
                }
            }
        }

        showAlertDialog(alertDialogBinding = alertDialogBinding, function = function)
    }

    private fun signOut() {
        val context = requireContext()
        val function = { viewModel.signOut(context) }
        val title = context.getString(R.string.are_you_sure)

        showAlertDialog(title, function = function)
    }

    private fun deleteAccount() {
        val context = requireContext()

        val alertDialogBinding = activity?.layoutInflater?.let { AlertDialogLayoutBinding.inflate(it) }
        val function = { viewModel.deleteAccount(alertDialogBinding?.editTxtPassword?.text.toString(), context) }

        alertDialogBinding?.editTxtAlertDialog?.visibility = View.GONE
        alertDialogBinding?.txtAlertDialogTitle?.text = context.getString(R.string.delete_account)
        showAlertDialog(alertDialogBinding = alertDialogBinding, function = function)
    }


    private fun showAlertDialog(title: String? = null, alertDialogBinding: AlertDialogLayoutBinding? = null, function: () -> Unit) {
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