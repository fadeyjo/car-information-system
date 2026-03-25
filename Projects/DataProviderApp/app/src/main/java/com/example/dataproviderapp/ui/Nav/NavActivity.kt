package com.example.dataproviderapp.ui.Nav

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.dataproviderapp.BuildConfig
import com.example.dataproviderapp.R
import com.example.dataproviderapp.databinding.ActivityNavBinding
import com.example.dataproviderapp.jwtutils.TokenStorage
import com.example.dataproviderapp.ui.Nav.Fragments.MyCars.MyCarsFragment
import com.example.dataproviderapp.ui.Nav.Fragments.MyTrips.MyTripsFragment
import com.example.dataproviderapp.ui.Nav.Fragments.Profile.ProfileFragment
import com.example.dataproviderapp.ui.Nav.Fragments.StartTrip.StartTripFragment
import com.example.dataproviderapp.ui.SignIn.SignInActivity
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import kotlin.getValue

class NavActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNavBinding

    private val viewModel: NavViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarNav)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbarNav,
            R.string.open_drawer,
            R.string.close_drawer
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        fun openFragment(itemId: Int, closeDrawer: Boolean = true) {
            val (fragment, title) = when (itemId) {
                R.id.nav_profile -> ProfileFragment() to "Профиль"
                R.id.nav_my_cars -> MyCarsFragment() to "Мои авто"
                R.id.nav_my_trips -> MyTripsFragment() to "Мои поездки"
                R.id.nav_start_trip -> StartTripFragment() to "Начать поездку"
                else -> ProfileFragment() to "Профиль"
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()

            supportActionBar?.title = title

            if (closeDrawer) {
                binding.drawerLayout.closeDrawers()
            }
        }

        binding.navigationViewMain.setNavigationItemSelectedListener { item ->
            item.isChecked = true
            openFragment(item.itemId)
            true
        }

        binding.navigationViewBottom.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.nav_logout) {
                viewModel.logout()
                true
            } else {
                false
            }
        }

        if (savedInstanceState == null) {
            binding.navigationViewMain.setCheckedItem(R.id.nav_profile)
            openFragment(R.id.nav_profile, closeDrawer = false)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                    binding.drawerLayout.closeDrawers()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        observeViewModel()

        viewModel.getPersonData()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.logoutState.collect { state ->
                        when (state) {
                            LogOutState.LogOuted -> {
                                TokenStorage.clear()
                                moveToSignIn()
                            }

                            LogOutState.SomeError -> {
                                AlertDialog.Builder(this@NavActivity)
                                    .setTitle("Ошибка")
                                    .setMessage("Произошла непредвиденная ошибка.")
                                    .setPositiveButton("ОК") { dialog, _ ->
                                        TokenStorage.clear()
                                        moveToSignIn()
                                        dialog.dismiss()
                                    }
                                    .show()
                            }

                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.profileDataState.collect { state ->
                        when (state) {
                            ProfileDataState.NetworkError -> {
                                AlertDialog.Builder(this@NavActivity)
                                    .setTitle("Ошибка")
                                    .setMessage("Нет подключение к интернету.")
                                    .setPositiveButton("ОК") { dialog, _ ->
                                        moveToSignIn()
                                        dialog.dismiss()
                                    }
                                    .show()
                            }

                            is ProfileDataState.Person -> {
                                val headerView = binding.navigationViewMain.getHeaderView(0)
                                val tvFullName = headerView.findViewById<TextView>(R.id.tvFullName)
                                val ivAvatar = headerView.findViewById<ShapeableImageView>(R.id.ivAvatar)

                                val FIO = if (state.person.patronymic.isEmpty()) {
                                    "${state.person.lastName} ${state.person.firstName}"
                                } else {
                                    "${state.person.lastName} ${state.person.firstName} ${state.person.patronymic}"
                                }

                                tvFullName.text = FIO

                                loadAvatarIntoHeader(ivAvatar, state.person.avatarId)
                            }

                            ProfileDataState.PersonNotFound -> {
                                AlertDialog.Builder(this@NavActivity)
                                    .setTitle("Ошибка")
                                    .setMessage("Пользователь не найден.")
                                    .setPositiveButton("ОК") { dialog, _ ->
                                        TokenStorage.clear()
                                        moveToSignIn()
                                        dialog.dismiss()
                                    }
                                    .show()
                            }

                            ProfileDataState.UnknownError -> {
                                AlertDialog.Builder(this@NavActivity)
                                    .setTitle("Ошибка")
                                    .setMessage("Произошла неизвестная ошибка.")
                                    .setPositiveButton("ОК") { dialog, _ ->
                                        moveToSignIn()
                                        dialog.dismiss()
                                    }
                                    .show()
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun loadAvatarIntoHeader(target: ShapeableImageView, avatarId: UInt) {
        if (TokenStorage.getAccessToken().isNullOrBlank()) {
            target.setImageResource(R.drawable.ic_avatar_placeholder)
            return
        }

        val url = "${BuildConfig.BASE_URL}avatars/${avatarId}"

        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .error(R.drawable.ic_avatar_placeholder)
            .into(target)
    }

    private fun moveToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }
}