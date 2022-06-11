package com.example.scorpiochat.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.scorpiochat.R
import com.example.scorpiochat.SharedPreferencesManager
import com.example.scorpiochat.data.AuthenticationState
import com.example.scorpiochat.viewModel.LoginViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        NotificationManagerCompat.from(this).cancelAll()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        setupActionBarWithNavController(navController)


        viewModel.authenticationState.observe(this) { authenticationState ->
            if (authenticationState == AuthenticationState.AUTHENTICATED) {
                val intent = Intent(this, MainActivity::class.java)
                viewModel.subscribeTopic(this)
                startActivity(intent)
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}