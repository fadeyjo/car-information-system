package com.example.dataproviderapp.ui.Nav.Fragments.MyCars

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dataproviderapp.R

class SimpleStringListAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SimpleStringListAdapter.VH>() {

    private var items: List<String> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<String>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_string_row, parent, false) as TextView
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val text = items[position]
        holder.textView.text = text
        holder.textView.setOnClickListener { onItemClick(text) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
