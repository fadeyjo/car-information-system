package com.example.dataproviderapp.ui.Nav.Fragments.MyCars

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.dataproviderapp.databinding.FragmentCreateCarBinding
import com.example.dataproviderapp.ui.Nav.NavViewModel
import kotlin.getValue

class CreateCarFragment : Fragment() {

    private var _binding: FragmentCreateCarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NavViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateCarBinding.inflate(inflater, container, false)
        return binding.root
    }
}