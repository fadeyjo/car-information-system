package com.example.dataproviderapp.ui.Nav.Fragments.StartTrip

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dataproviderapp.R
import com.example.dataproviderapp.databinding.FragmentMyCarsBinding
import com.example.dataproviderapp.databinding.FragmentProfileBinding
import com.example.dataproviderapp.databinding.FragmentStartTripBinding
import com.example.dataproviderapp.ui.Nav.CarsState
import com.example.dataproviderapp.ui.Nav.Fragments.MyCars.CarDetailsFragment
import com.example.dataproviderapp.ui.Nav.Fragments.MyCars.CreateCarFragment
import com.example.dataproviderapp.ui.Nav.Fragments.MyCars.MyCarsAdapter
import com.example.dataproviderapp.ui.Nav.NavViewModel
import kotlinx.coroutines.launch
import kotlin.getValue

class StartTripFragment : Fragment() {

    private var _binding: FragmentStartTripBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val carsAdapter = MyCarsAdapter { car ->
            viewModel.selectedCarToTrip = car
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SelectDeviceFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerViewCars.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = carsAdapter
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_my_cars, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_car -> {
                        onAddCarClicked()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        observeViewModel(carsAdapter)
        viewModel.getPersonCars()
    }

    private fun onAddCarClicked() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, CreateCarFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun observeViewModel(carsAdapter: MyCarsAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.carsState.collect { state ->
                        when (state) {
                            is CarsState.Cars -> {
                                val hasCars = state.cars.isNotEmpty()
                                binding.recyclerViewCars.visibility = if (hasCars) View.VISIBLE else View.GONE
                                binding.tvEmptyCars.visibility = if (hasCars) View.GONE else View.VISIBLE
                                carsAdapter.submitList(state.cars)
                            }

                            CarsState.PersonNotFound -> showErrorDialog("Пользователь не найден.")
                            CarsState.NetworkError -> showErrorDialog("Нет подключения к интернету.")
                            CarsState.UnknownError -> showErrorDialog("Произошла неизвестная ошибка.")
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}