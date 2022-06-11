package com.example.scorpiochat.ui.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.scorpiochat.R
import com.example.scorpiochat.SharedPreferencesManager
import com.example.scorpiochat.data.AuthenticationState
import com.example.scorpiochat.data.User
import com.example.scorpiochat.databinding.ActivityMainBinding
import com.example.scorpiochat.viewModel.MainActivityViewModel
import com.google.android.material.navigation.NavigationView
import de.hdodenhof.circleimageview.CircleImageView


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    private var isUpButton = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        setMode()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_settings, R.id.nav_about_app), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        viewModel.loadUserInfo()

        viewModel.userInfo.observe(this) {
            setNavHeader(it)
        }

        viewModel.authenticationState.observe(this) { authenticationState ->
            if (authenticationState == AuthenticationState.UNAUTHENTICATED) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (isUpButton && item.itemId == android.R.id.home) {
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            navController.navigateUp()
            showUpButtonInsteadDrawerButton(false)
            true
        } else super.onOptionsItemSelected(item)
    }

    fun showUpButtonInsteadDrawerButton(showUpButton: Boolean) {
        isUpButton = if(showUpButton) {
            supportActionBar?.setHomeAsUpIndicator(
                androidx.appcompat.R.drawable.abc_ic_ab_back_material
            )
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            true
        } else {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            false
        }
    }


    private fun setNavHeader(user: User) {
        val headerView = binding.navView.getHeaderView(0)
        val profilePicture = headerView.findViewById<CircleImageView>(R.id.img_profile_picture)
        val email = headerView.findViewById<TextView>(R.id.txt_mail)
        val username = headerView.findViewById<TextView>(R.id.txt_username)
        val modeSwitch = headerView.findViewById<ImageButton>(R.id.btn_mode_switch)

        email.text = viewModel.getUserEmail()
        username.text = user.username

        viewModel.getStorage().downloadUrl.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Glide.with(applicationContext)
                    .load(task.result)
                    .into(profilePicture)
            }
        }

        modeSwitch.apply {
            val darkMode = this@MainActivity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

            if (darkMode) {
                setBackgroundResource(R.drawable.ic_baseline_light_mode_24)
            } else {
                setBackgroundResource(R.drawable.ic_baseline_mode_night_24)
            }
            setOnClickListener {
                if (darkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                SharedPreferencesManager.setDarkTheme(this@MainActivity, !darkMode)
            }
        }
    }

    private fun setMode() {
        if(SharedPreferencesManager.getDarkTheme(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    override fun onResume() {
        super.onResume()
        viewModel.changeUserStatus(true)
    }

    override fun onPause() {
        super.onPause()
        viewModel.changeUserStatus(false)
    }

}