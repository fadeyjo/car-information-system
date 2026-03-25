package com.example.dataproviderapp.ui.Main

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dataproviderapp.R
import com.example.dataproviderapp.jwtutils.TokenStorage
import com.example.dataproviderapp.ui.Nav.NavActivity
import com.example.dataproviderapp.ui.SignIn.SignInActivity
import com.example.dataproviderapp.ui.SignUp.SignUpActivity
import kotlinx.coroutines.launch
import kotlin.getValue

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenStorage.init(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val accessToken = TokenStorage.getAccessToken()
        val refreshToken = TokenStorage.getRefreshToken()

        if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
            startSignInActivity()
            finish()
            return
        }

        observeViewModel()

        viewModel.checkPersonAuthorized()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startSignInActivity() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
    }

    private fun startNavActivity() {
        val intent = Intent(this, NavActivity::class.java)
        startActivity(intent)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.personState.collect { state ->
                    when (state) {
                        is PersonState.Authorized -> {
                            startNavActivity()
                            finish()
                        }

                        is PersonState.Unauthorized -> {
                            startSignInActivity()
                            finish()
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}