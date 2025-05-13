package com.daeyoon.chargenote

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.daeyoon.chargenote.data.Car
import com.daeyoon.chargenote.databinding.ActivityAddCarBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCarActivity : AppCompatActivity() {
    lateinit var binding: ActivityAddCarBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isEditing = intent.getBooleanExtra("isEditing", false)
        val carId = intent.getIntExtra("carId", 0)
        var car: Car? = null
        if (isEditing) {
            binding.topAppBar.title = getString(R.string.app_bar_edit_car)
            lifecycleScope.launch(Dispatchers.IO) {
                val dao = MyApp.database.carDao()
                car = dao.getCarById(carId)

                car?.let {
                    withContext(Dispatchers.Main) {
                        binding.etNickname.setText(it.nickname)
                        binding.etNumberPlate.setText(it.numberPlate)
                        binding.etMileage.setText(it.mileage.toString())
                        binding.etCapacity.setText(it.capacity.toString())
                        binding.etInitialBattery.setText(it.initialBattery.toString())
                    }
                }
            }
        } else {
            binding.topAppBar.menu.clear()
        }

        binding.btnConfirm.setOnClickListener {
            val dao = MyApp.database.carDao()

            val nickname = binding.etNickname.text.toString()
            val numberPlate = binding.etNumberPlate.text.toString()
            val mileage = binding.etMileage.text.toString().toIntOrNull() ?: 0
            val capacity = binding.etCapacity.text.toString().toFloatOrNull() ?: 0.0f
            val initBattery = binding.etInitialBattery.text.toString().toIntOrNull() ?: 0

            if (nickname.isEmpty() || numberPlate.isEmpty()) {
                Snackbar.make(
                    binding.root,
                    resources.getString(R.string.add_car_error_confirm),
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            if (validateInputs(nickname, mileage, capacity, initBattery)) {
                if (isEditing) {
                    car?.let {
                        val updateCar = it.copy(
                            uid = it.uid,
                            nickname = nickname,
                            numberPlate = numberPlate,
                            mileage = mileage,
                            capacity = capacity,
                            initialBattery = initBattery
                        )
                        lifecycleScope.launch(Dispatchers.IO) {
                            dao.updateCars(updateCar)
                        }
                    }
                } else {
                    val newCar = Car(
                        nickname = nickname,
                        numberPlate = numberPlate,
                        mileage = mileage,
                        capacity = capacity,
                        initialBattery = initBattery
                    )

                    lifecycleScope.launch(Dispatchers.IO) {
                        dao.insertAll(newCar)
                    }
                }
                finish()
            }
        }


        binding.topAppBar.setNavigationOnClickListener {
            finishWithDialog()
        }

        onBackPressedDispatcher.addCallback {
            finishWithDialog()
        }


        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.remove -> {
                    car?.let {
                        deleteCarWithDialog(it)
                    }
                    true
                }

                else -> false
            }
        }

        binding.sliderBattery.addOnChangeListener { _, value, _ ->
            val currentText = binding.etInitialBattery.text.toString()
            val sliderValue = value.toInt().toString()
            if (currentText != sliderValue) {
                binding.etInitialBattery.setText(sliderValue)
                binding.etInitialBattery.setSelection(sliderValue.length) // 커서 끝으로
            }
        }
        binding.etInitialBattery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.toIntOrNull()
                if (text != null && text in 1..100) {
                    if (binding.sliderBattery.value.toInt() != text) {
                        binding.sliderBattery.value = text.toFloat()
                    }
                    binding.tfInitialBattery.error = null
                } else {
                    binding.tfInitialBattery.error = getString(R.string.alert_record_current_batter)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etNickname.addTextChangedListener {
            if (it.isNullOrEmpty().not()) {
                binding.tfNickname.error = null
            }
        }
        binding.etCapacity.addTextChangedListener {
            if (it.isNullOrEmpty().not()) {
                binding.tfCapacity.error = null
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

    private fun deleteCarWithDialog(car: Car) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.car_remove_alert))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val carDao = MyApp.database.carDao()
                    carDao.delete(car)
                    withContext(Dispatchers.Main) {
                        finish()
                    }
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun validateInputs(
        nickname: String,
        mileage: Int,
        capacity: Float,
        currentBattery: Int
    ): Boolean {

        var isSuccess = true
        if (nickname.isEmpty()) {
            binding.tfNickname.error = getString(R.string.error_text_field)
            isSuccess = false
        }
        if (mileage < 0) {
            binding.tfMileage.error = getString(R.string.error_text_field)
            isSuccess = false
        }
        if (capacity <= 0) {
            binding.tfCapacity.error = getString(R.string.error_text_field)
            isSuccess = false
        }

        if (currentBattery <= 0 || currentBattery > 100) {
            binding.tfInitialBattery.error = getString(R.string.alert_record_current_batter)
            isSuccess = false
        }

        return isSuccess
    }
}