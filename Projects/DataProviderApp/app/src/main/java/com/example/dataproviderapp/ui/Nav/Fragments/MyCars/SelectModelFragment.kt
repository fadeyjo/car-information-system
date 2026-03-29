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
import com.example.dataproviderapp.databinding.FragmentSelectModelBinding
import com.example.dataproviderapp.ui.Nav.ModelsState
import com.example.dataproviderapp.ui.Nav.NavViewModel
import kotlinx.coroutines.launch

class SelectModelFragment : Fragment() {

    private var _binding: FragmentSelectModelBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    private val adapter = SimpleStringListAdapter { model ->
        parentFragmentManager.setFragmentResult(
            CreateCarFragment.REQUEST_SELECT_MODEL,
            bundleOf(CreateCarFragment.KEY_MODEL to model)
        )
        parentFragmentManager.popBackStack()
    }

    private lateinit var brandName: String

    private val searchWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val text = s?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                viewModel.getAllModels(brandName)
            } else {
                viewModel.getModelsByText(brandName, text)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brandName = requireArguments().getString(ARG_BRAND_NAME)
            ?: throw IllegalArgumentException("brandName is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectModelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.etSearch.addTextChangedListener(searchWatcher)

        viewModel.getAllModels(brandName)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.modelsState.collect { state ->
                    when (state) {
                        ModelsState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is ModelsState.Data -> {
                            binding.progressBar.visibility = View.GONE
                            adapter.submitList(state.models)
                        }
                        ModelsState.SomeError, ModelsState.Idle -> {
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

    companion object {
        private const val ARG_BRAND_NAME = "brand_name"

        fun newInstance(brandName: String) = SelectModelFragment().apply {
            arguments = bundleOf(ARG_BRAND_NAME to brandName)
        }
    }
}
