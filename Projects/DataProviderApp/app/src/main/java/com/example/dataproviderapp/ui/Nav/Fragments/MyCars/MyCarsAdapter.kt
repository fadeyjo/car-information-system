package com.example.dataproviderapp.ui.Nav.Fragments.MyCars

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dataproviderapp.BuildConfig
import com.example.dataproviderapp.R
import com.example.dataproviderapp.databinding.ItemCarBinding
import com.example.dataproviderapp.dto.responses.CarDto

class MyCarsAdapter(
    private val onCarClick: (CarDto) -> Unit
) : ListAdapter<CarDto, MyCarsAdapter.CarViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CarViewHolder(
        private val binding: ItemCarBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(car: CarDto) {
            binding.tvBrandModel.text = "${car.brandName} ${car.modelName}"
            binding.tvVin.text = "VIN: ${car.vinNumber}"

            if (car.stateNumber.isNullOrBlank()) {
                binding.tvStateNumber.visibility = View.GONE
            } else {
                binding.tvStateNumber.visibility = View.VISIBLE
                binding.tvStateNumber.text = "Гос. номер: ${car.stateNumber}"
            }

            Glide.with(binding.root)
                .load("${BuildConfig.BASE_URL}car-photos/${car.photoId}")
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(binding.ivCarPhoto)

            binding.root.setOnClickListener {
                onCarClick(car)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CarDto>() {
        override fun areItemsTheSame(oldItem: CarDto, newItem: CarDto): Boolean {
            return oldItem.carId == newItem.carId
        }

        override fun areContentsTheSame(oldItem: CarDto, newItem: CarDto): Boolean {
            return oldItem == newItem
        }
    }
}
