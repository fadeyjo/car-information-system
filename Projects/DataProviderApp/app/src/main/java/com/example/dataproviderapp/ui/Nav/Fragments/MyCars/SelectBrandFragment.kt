package com.example.dataproviderapp.ui.Nav.Fragments.MyCars

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dataproviderapp.databinding.FragmentSelectBrandBinding
import com.example.dataproviderapp.ui.Nav.BrandsState
import com.example.dataproviderapp.ui.Nav.NavViewModel
import kotlinx.coroutines.launch

class SelectBrandFragment : Fragment() {

    private var _binding: FragmentSelectBrandBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    private val adapter = SimpleStringListAdapter { brand ->
        parentFragmentManager.setFragmentResult(
            CreateCarFragment.REQUEST_SELECT_BRAND,
            bundleOf(CreateCarFragment.KEY_BRAND to brand)
        )
        parentFragmentManager.popBackStack()
    }

    private val searchWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val text = s?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                viewModel.getAllBrands()
            } else {
                viewModel.getBrandsByText(text)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectBrandBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.etSearch.addTextChangedListener(searchWatcher)

        viewModel.getAllBrands()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.brandsState.collect { state ->
                    when (state) {
                        BrandsState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is BrandsState.Data -> {
                            binding.progressBar.visibility = View.GONE
                            adapter.submitList(state.brands)
                        }
                        BrandsState.SomeError, BrandsState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.etSearch.removeTextChangedListener(searchWatcher)
        _binding = null
        super.onDestroyView()
    }
}
