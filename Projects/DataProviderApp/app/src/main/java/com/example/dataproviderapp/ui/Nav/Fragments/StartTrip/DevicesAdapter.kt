import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dataproviderapp.databinding.ItemBtDeviceBinding
import com.example.dataproviderapp.ui.Nav.Fragments.StartTrip.BtDevice

class DevicesAdapter(
    private val onClick: (BtDevice) -> Unit
) : RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<BtDevice>()

    fun addDevice(device: BtDevice) {
        if (devices.none { it.macAddress == device.macAddress }) {
            devices.add(device)
            notifyItemInserted(devices.size - 1)
        }
    }

    inner class DeviceViewHolder(private val binding: ItemBtDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(device: BtDevice) {
            binding.tvBtDeviceName.text = device.deviceName
            binding.tvBtMac.text = device.macAddress

            binding.root.setOnClickListener {
                binding.state.text = "Подключение..."
                binding.state.visibility = View.VISIBLE
                onClick(device)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemBtDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearDevices() {
        devices.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size
}