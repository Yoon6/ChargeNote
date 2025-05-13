package com.daeyoon.chargenote

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isEmpty
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.daeyoon.chargenote.data.Car
import com.daeyoon.chargenote.data.DrivingRecord
import com.daeyoon.chargenote.data.RecordUiData
import com.daeyoon.chargenote.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecordListAdapter
    private lateinit var selectedCar: Car

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RecordListAdapter(object : OnItemClickListener {
            override fun onItemClick(item: RecordUiData) {
                val intent = Intent(this@MainActivity, AddRecordActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                intent.putExtra("uid", item.originData.carId)
                intent.putExtra("record", item.originData)
                startActivity(intent)
            }
        })
        binding.rvRecordList.layoutManager = LinearLayoutManager(this@MainActivity)
        binding.rvRecordList.adapter = adapter
        binding.rvRecordList.addItemDecoration(LeafItemDecoration())

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        binding.fab.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val dao = MyApp.database.carDao()
                val list = dao.getAll()

                withContext(Dispatchers.Main) {
                    if (list.isEmpty()) {
                        val intent = Intent(this@MainActivity, AddCarActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                    } else {
                        val intent = Intent(this@MainActivity, AddRecordActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        intent.putExtra("uid", list[0].uid)
                        startActivity(intent)
                    }
                }
            }
        }

        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.edit -> {
                    val intent = Intent(this@MainActivity, AddCarActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    intent.putExtra("carId", selectedCar.uid)
                    intent.putExtra("isEditing", true)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }

    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch(Dispatchers.IO) {
            val carDao = MyApp.database.carDao()
            val recordDao = MyApp.database.drivingRecordDao()
            val list = carDao.getAll()

            if (list.isEmpty().not()) {
                withContext(Dispatchers.Main) {

                    selectedCar = list[0]
                    binding.topAppBar.title = list[0].nickname
                    binding.topAppBar.subtitle = list[0].numberPlate

                    if (binding.topAppBar.menu.isEmpty()) {
                        binding.topAppBar.menu.add(
                            Menu.NONE,
                            R.id.edit,
                            Menu.NONE,
                            R.string.app_bar_edit_car
                        )
                            .setIcon(R.drawable.ic_add_record)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    }
                }
                val recordList = recordDao.getDrivingRecordsByIdOrderByDateAndId(list[0].uid)
                withContext(Dispatchers.Main) {
                    if (recordList.isEmpty().not()) {
                        binding.layoutInfo.visibility = View.VISIBLE
                        setCarInfos(recordList)
                        binding.imgEmptyList.visibility = View.INVISIBLE
                        binding.tvEmptyList.visibility = View.INVISIBLE
                    } else {
                        binding.layoutInfo.visibility = View.GONE
                        binding.imgEmptyList.visibility = View.VISIBLE
                        binding.tvEmptyList.visibility = View.VISIBLE
                    }
                    adapter.submitList(convertRecordToUiData(recordList)) {
                        binding.rvRecordList.scrollToPosition(0)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.topAppBar.menu.clear()
                    binding.topAppBar.title = getString(R.string.app_name)
                    binding.topAppBar.subtitle = null
                    adapter.submitList(emptyList())
                    binding.imgEmptyList.visibility = View.VISIBLE
                    binding.tvEmptyList.visibility = View.VISIBLE
                    binding.layoutInfo.visibility = View.GONE
                }
            }
        }
    }

    private fun setCarInfos(recordList: List<DrivingRecord>) {
        if (recordList.isEmpty()) {
            return
        }

        val mileage = String.format("%,d", recordList[recordList.lastIndex].currentMileage) + "Km"
        binding.tvInfoMileage.text = mileage

        val battery = recordList[0].currentBattery.toString() + "%"
        binding.tvInfoBattery.text = battery

        var totalAmount = 0
        for (record in recordList) {
            totalAmount += record.totalAmount!!
        }
        val amount = String.format("%,d", totalAmount) + getString(R.string.unit_krw)
        binding.tvInfoAmount.text = amount

        var distance = (recordList[recordList.lastIndex].currentMileage!! - selectedCar.mileage!!)
        var totalConsumedWattage = 0f

        for (i in recordList.lastIndex downTo 0) {
            val prevBattery =
                if (i == 0) selectedCar.initialBattery!! else recordList[i - 1].currentBattery!!

            val record = recordList[i]
            val batteryBeforeCharging =
                record.currentBattery!! - (record.chargeAmount!! / selectedCar.capacity!! * 100)
            val consumedWattage =
                ((prevBattery - batteryBeforeCharging) / 100f) * selectedCar.capacity!!

            totalConsumedWattage += consumedWattage
        }
        val efficiency = String.format("%.1f", distance / totalConsumedWattage) + "Km/kWh"
        binding.tvInfoEfficiency.text = efficiency
    }

    private fun convertRecordToUiData(recordList: List<DrivingRecord>): List<RecordUiData> {
        val list = mutableListOf<RecordUiData>()

        for (i in recordList.lastIndex downTo 0) {
            if (i == 0) {
                val record = recordList[i]
                val distance = record.currentMileage!! - selectedCar.mileage!!
                val tripMileage = String.format("%,d", distance) + "Km"
                val totalAmount =
                    String.format("%,d", record.totalAmount!!) + getString(R.string.unit_krw)
                val date = record.date
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formatted = if (date != null) format.format(date) else ""

                val batteryBeforeCharging =
                    record.currentBattery!! - (record.chargeAmount!! / selectedCar.capacity!! * 100)
                val battery = selectedCar.initialBattery!!
                val consumedWattage =
                    ((battery - batteryBeforeCharging) / 100f) * selectedCar.capacity!!

                val efficiency = String.format("%.2f", distance / consumedWattage) + "Km/kWh"
                val uiData = RecordUiData(
                    uid = record.uid,
                    efficiency = efficiency,
                    totalAmount = totalAmount,
                    tripMileage = tripMileage,
                    date = formatted,
                    originData = record
                )
                list.add(uiData)
            } else {
                val curRecord = recordList[i]
                val prevRecord = recordList[i - 1]

                val distance = curRecord.currentMileage!! - prevRecord.currentMileage!!
                val tripMileage = String.format("%,d", distance) + "Km"
                val totalAmount =
                    String.format("%,d", curRecord.totalAmount!!) + getString(R.string.unit_krw)
                val date = curRecord.date
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formatted = if (date != null) format.format(date) else ""

                val batteryBeforeCharging =
                    curRecord.currentBattery!! - (curRecord.chargeAmount!! / selectedCar.capacity!! * 100)
                val battery = prevRecord.currentBattery!!
                val consumedWattage =
                    ((battery - batteryBeforeCharging) / 100f) * selectedCar.capacity!!

                val efficiency = String.format("%.2f", distance / consumedWattage) + "Km/kWh"
                val uiData = RecordUiData(
                    uid = curRecord.uid,
                    efficiency = efficiency,
                    totalAmount = totalAmount,
                    tripMileage = tripMileage,
                    date = formatted,
                    originData = curRecord
                )
                list.add(uiData)
            }
        }

        return list
    }
}