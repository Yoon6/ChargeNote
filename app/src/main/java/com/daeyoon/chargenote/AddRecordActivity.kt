package com.daeyoon.chargenote

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.transition.Visibility
import com.daeyoon.chargenote.data.Car
import com.daeyoon.chargenote.data.DrivingRecord
import com.daeyoon.chargenote.databinding.ActivityAddRecordBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddRecordActivity : AppCompatActivity() {
    lateinit var binding: ActivityAddRecordBinding
    lateinit var selectedCar: Car
    private var selectedDate: Date = Date()  // 기본값: 현재 날짜

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = intent.getIntExtra("uid", 0)
        val record = intent.getParcelableExtra<DrivingRecord>("record")
        val isEditing = record != null

        record?.let {
            binding.etCurrentMileage.setText(it.currentMileage.toString())
            binding.etCost.setText(it.totalAmount.toString())
            binding.etChargeAmount.setText(it.chargeAmount.toString())
            val date = it.date ?: Date()
            binding.etDate.setText(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date))
            binding.etCurrentBattery.setText(it.currentBattery.toString())
            binding.sliderBattery.value = it.currentBattery?.toFloat() ?: 1f
            binding.btnConfirm.visibility = View.GONE

            binding.tfCost.isEnabled = false
            binding.tfChargeAmount.isEnabled = false
            binding.tfCurrentMileage.isEnabled = false
            binding.tfDate.isEnabled = false
            binding.tfCurrentBattery.isEnabled = false
            binding.sliderBattery.isEnabled = false
        } ?: run {
            binding.topAppBar.menu.clear()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.etDate.setText(dateFormat.format(selectedDate))
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val carDao = MyApp.database.carDao()
            val car = carDao.getCarById(uid)
            val recordDao = MyApp.database.drivingRecordDao()
            var prev = recordDao.getPreviousRecord(uid, selectedDate)?.currentMileage?.toString()
            val next = recordDao.getNextRecord(uid, selectedDate)?.currentMileage?.toString() ?: ""

            withContext(Dispatchers.Main)
            {
                car?.let {
                    selectedCar = it
                    if (prev == null) prev = selectedCar.mileage?.toString()
                    binding.topAppBar.subtitle = it.nickname + " " + it.numberPlate
                    binding.etCurrentMileage.hint = "$prev ~ $next"
                }
            }
        }

        binding.btnConfirm.setOnClickListener {

            val currentMileage = binding.etCurrentMileage.text.toString().toIntOrNull() ?: -1
            val cost = binding.etCost.text.toString().toIntOrNull() ?: 0
            val chargeAmount = binding.etChargeAmount.text.toString().toFloatOrNull() ?: 0.0f
            val currentBattery = binding.etCurrentBattery.text.toString().toIntOrNull() ?: 0
            val dateString = binding.etDate.text.toString()
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date: Date = format.parse(dateString) ?: Date()

            lifecycleScope.launch(Dispatchers.IO) {
                val recordDao = MyApp.database.drivingRecordDao()
                val prevRecord = recordDao.getPreviousRecord(selectedCar.uid, date)
                val nextRecord = recordDao.getNextRecord(selectedCar.uid, date)

                withContext(Dispatchers.Main) {
                    if (validateInputs(
                            currentMileage,
                            cost,
                            chargeAmount,
                            currentBattery,
                            prevRecord,
                            nextRecord
                        )
                    ) {
                        val newRecord = DrivingRecord(
                            carId = selectedCar.uid,
                            totalAmount = cost,
                            chargeAmount = chargeAmount,
                            currentMileage = currentMileage,
                            currentBattery = currentBattery,
                            date = date,
                        )
                        withContext(Dispatchers.IO) {
                            recordDao.insertAll(newRecord)
                        }

                        finish()
                    }
                }
            }
        }

        binding.etDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.topAppBar.setNavigationOnClickListener {
            if (isEditing.not()) {
                finishWithDialog()
            } else {
                finish()
            }
        }

        if (isEditing.not()) {
            onBackPressedDispatcher.addCallback {
                finishWithDialog()
            }
        }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.remove -> {
                    record?.let {
                        deleteCarWithDialog(it)
                    }
                    true
                }

                else -> false
            }
        }

        binding.sliderBattery.addOnChangeListener { _, value, _ ->
            val currentText = binding.etCurrentBattery.text.toString()
            val sliderValue = value.toInt().toString()
            if (currentText != sliderValue) {
                binding.etCurrentBattery.setText(sliderValue)
                binding.etCurrentBattery.setSelection(sliderValue.length) // 커서 끝으로
            }
        }
        binding.etCurrentBattery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.toIntOrNull()
                if (text != null && text in 1..100) {
                    if (binding.sliderBattery.value.toInt() != text) {
                        binding.sliderBattery.value = text.toFloat()
                    }
                    binding.tfCurrentBattery.error = null
                } else {
                    binding.tfCurrentBattery.error = getString(R.string.alert_record_current_batter)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.etCurrentMileage.addTextChangedListener {
            if (it.isNullOrEmpty().not()) {
                binding.tfCurrentMileage.error = null
            }
        }
        binding.etCost.addTextChangedListener {
            if (it.isNullOrEmpty().not()) {
                binding.tfCost.error = null
            }
        }
        binding.etChargeAmount.addTextChangedListener {
            if (it.isNullOrEmpty().not()) {
                binding.tfChargeAmount.error = null
            }
        }
    }

    private fun showDatePickerDialog() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(selectedDate.time)  // 기존 선택된 날짜
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            // selection은 Long (UTC timestamp)
            selectedDate = Date(selection)

            // EditText에 날짜 표시
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.etDate.setText(format.format(selectedDate))
            setMileageHint()
        }

        picker.show(supportFragmentManager, picker.toString())
    }

    private fun setMileageHint() {
        lifecycleScope.launch(Dispatchers.IO) {
            val uid = selectedCar.uid
            val recordDao = MyApp.database.drivingRecordDao()
            val prev =
                recordDao.getPreviousRecord(uid, selectedDate)?.currentMileage?.toString()
                    ?: selectedCar.mileage.toString()
            val next = recordDao.getNextRecord(uid, selectedDate)?.currentMileage?.toString() ?: ""

            withContext(Dispatchers.Main)
            {
                binding.etCurrentMileage.hint = "$prev ~ $next"
            }
        }

    }

    private fun finishWithDialog() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.back_alert_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                finish()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun deleteCarWithDialog(record: DrivingRecord) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.record_remove_alert))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val recordDao = MyApp.database.drivingRecordDao()
                    recordDao.delete(record)

                    withContext(Dispatchers.Main) {
                        finish()
                    }
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private suspend fun validateInputs(
        mileage: Int,
        cost: Int,
        chargeAmount: Float,
        currentBattery: Int,
        prevRecord: DrivingRecord?,
        nextRecord: DrivingRecord?
    ): Boolean {

        var isSuccess = true

        if (mileage < 0) {
            binding.tfCurrentMileage.error = getString(R.string.error_text_field)
            isSuccess = false
        }

        var prev = prevRecord?.currentMileage?.toString() ?: ""
        val next = nextRecord?.currentMileage?.toString() ?: ""

        prevRecord?.currentMileage?.let {
            if (it >= mileage) {
                withContext(Dispatchers.Main) {
                    binding.tfCurrentMileage.error =
                        getString(R.string.error_mileage_field, "$prev ~ $next")
                    isSuccess = false
                }
            }
        } ?: run {
            selectedCar.mileage?.let {
                if (it >= mileage) {
                    withContext(Dispatchers.Main) {
                        prev = it.toString()
                        binding.tfCurrentMileage.error =
                            getString(R.string.error_mileage_field, "$prev ~ $next")
                        isSuccess = false
                    }
                }
            }
        }

        nextRecord?.currentMileage?.let {
            if (it <= mileage) {
                withContext(Dispatchers.Main) {
                    binding.tfCurrentMileage.error =
                        getString(R.string.error_mileage_field, "$prev ~ $next")
                    isSuccess = false
                }
            }
        }

        if (cost <= 0) {
            binding.tfCost.error = getString(R.string.error_text_field)
            isSuccess = false
        }

        if (chargeAmount <= 0 || chargeAmount > selectedCar.capacity!!) {
            binding.tfChargeAmount.error = getString(R.string.error_text_field)
            isSuccess = false
        }

        if (currentBattery <= 0 || currentBattery > 100) {
            binding.tfCurrentBattery.error = getString(R.string.alert_record_current_batter)
            isSuccess = false
        }

        return isSuccess
    }

}