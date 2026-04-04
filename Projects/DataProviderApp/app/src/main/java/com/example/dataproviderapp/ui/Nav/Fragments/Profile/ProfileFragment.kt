package com.example.dataproviderapp.ui.Nav.Fragments.Profile

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.util.Util
import com.example.dataproviderapp.BuildConfig
import com.example.dataproviderapp.R
import com.example.dataproviderapp.databinding.FragmentProfileBinding
import com.example.dataproviderapp.dto.responses.PersonDto
import com.example.dataproviderapp.jwtutils.TokenStorage
import com.example.dataproviderapp.ui.Nav.NavViewModel
import com.example.dataproviderapp.ui.Nav.ProfileDataState
import com.example.dataproviderapp.ui.SignIn.SignInActivity
import com.example.dataproviderapp.utils.Utils
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEdit.setOnClickListener {
            redactPerson()
        }

        observeViewModel()
        viewModel.getPersonData()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profileDataState.collect { state ->
                        when (state) {
                            is ProfileDataState.Person -> {
                                renderPerson(state.person)
                            }
                            ProfileDataState.NetworkError -> Utils.showNetworkErrorDialog(requireContext())
                            ProfileDataState.PersonNotFound -> {
                                TokenStorage.clear()
                                Utils.showErrorDialogWithAction("Пользователь не найден", requireContext()) {
                                    startSignInActivity()
                                }
                            }
                            ProfileDataState.UnknownError -> Utils.showUnknownErrorDialog(requireContext())

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun renderPerson(person: PersonDto) {
        val fio = if (person.patronymic.isNullOrEmpty()) {
            "${person.lastName} ${person.firstName}"
        } else {
            "${person.lastName} ${person.firstName} ${person.patronymic}"
        }

        val birthFormatted = person.birth.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val driveLicenseFormatted = formatDriveLicense(person.driveLicense)

        binding.tvProfileFullName.text = fio
        binding.tvProfileBirth.text = birthFormatted
        binding.tvProfileEmail.text = person.email
        binding.tvProfilePhone.text = person.phone
        binding.tvProfileDriveLicense.text = driveLicenseFormatted

        loadAvatarIntoProfile(binding.ivProfileAvatar, person.avatarId)
    }

    private fun formatDriveLicense(raw: String?): String {
        val normalized = raw?.trim().orEmpty()
        if (normalized.isBlank()) return "-"
        return if (normalized.length >= 10) {
            val first = normalized.substring(0, 4)
            val last = normalized.substring(4, 10)
            "$first $last"
        } else {
            normalized
        }
    }

    private fun loadAvatarIntoProfile(target: ShapeableImageView, avatarId: UInt) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun redactPerson() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, UpdatePersonFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun startSignInActivity() {
        val intent = Intent(requireContext(), SignInActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}