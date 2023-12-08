package com.example.pomodorotimer.presentation

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.view.View.OnLongClickListener
import androidx.core.view.InputDeviceCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.pomodorotimer.R
import com.example.pomodorotimer.databinding.HistoryFragmentBinding
import com.example.pomodorotimer.presentation.viewmodel.HistoryViewModel
import com.example.pomodorotimer.presentation.viewmodel.WorkIntervalViewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class HistoryFragment : Fragment() {
    private val historyViewModel: HistoryViewModel by activityViewModels {
        WorkIntervalViewModelFactory(
            (activity?.application as WorkIntervalApplication).database.workIntervalDao()
        )
    }

    private var _binding: HistoryFragmentBinding? = null
    private val binding get() = _binding!!

    private val dateList = mutableListOf<LocalDate>()

    private var number: Int  = 0
    private var workString: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = HistoryFragmentBinding.inflate(inflater, container, false)
        bindingHistory(binding.root)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun bindingHistory(view: View) {
        val previousButton: ImageButton = view.findViewById(R.id.moveToTimer)
        val textDate: TextView = view.findViewById(R.id.textDateHistory)
        val textWork: TextView = view.findViewById(R.id.timeWork)
        val textHobby: TextView = view.findViewById(R.id.timeHobby)
        val textEducation: TextView = view.findViewById(R.id.timeEducation)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteData)

        previousButton.setOnClickListener {
            findNavController().navigate(R.id.action_historyFragment_to_timerFragment)
        }

        deleteButton.setOnClickListener {
            if(dateList.isNotEmpty() && textDate.text != getString(R.string.spinRotary)) deleteWorkIntervals(getDate(textDate))
        }

        lifecycleScope.launch {
            historyViewModel.workIntervalDates.collect { workIntervals ->
                dateList.addAll(workIntervals)
            }
        }

        view.setOnGenericMotionListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_SCROLL &&
                ev.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)
            ) {
                changePosition()
                textDate.text = getFormattedDateForPosition()
                historyViewModel.setTime(getDate(textDate))

                lifecycleScope.launch {
                    textWork.text = getCategoryTime(getString(R.string.work)).first()
                    textHobby.text = getCategoryTime(getString(R.string.hobby)).first()
                    textEducation.text = getCategoryTime(getString(R.string.education)).first()
                }
                true
            } else {
                false
            }
        }
    }

    private fun getDate(textDate: TextView): LocalDate {
        val dateText = textDate.text.toString()
        return LocalDate.of(dateText.substring(6,10).toInt(),
            dateText.substring(3,5).toInt(), dateText.substring(0,2).toInt())
    }

    private fun deleteWorkIntervals(date: LocalDate) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_dialog, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
        val alert = builder.create()

        dialogView.findViewById<TextView>(R.id.textQuestion).text = getString(R.string.deleteData)

        val positiveButton = dialogView.findViewById<ImageButton>(R.id.positive_button)
        positiveButton.setOnClickListener {
            historyViewModel.deleteByDate(date)
            findNavController().navigate(R.id.action_historyFragment_to_timerFragment)
            alert.dismiss()
        }

        val negativeButton = dialogView.findViewById<ImageButton>(R.id.negative_button)
        negativeButton.setOnClickListener {
            alert.dismiss()
        }

        alert.show()
    }

    private fun changePosition() {
        if (number == dateList.size-1) number = 0 else number += 1
    }

    private fun getFormattedDateForPosition(): String {
        return if(dateList.isNotEmpty()) {
            val date = dateList[number]
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            date.format(formatter).toString()
        } else {
            getString(R.string.noData)
        }
    }

    private fun getCategoryTime(categoryName: String): Flow<String> = flow {
        val timeFlow = when (categoryName) {
            "Work" -> historyViewModel.getTimeForWork
            "Hobby" -> historyViewModel.getTimeForHobby
            "Education" -> historyViewModel.getTimeForEducation
            else -> null
        }

        if (timeFlow != null) {
            timeFlow.collect { localTimes ->
                var totalMinutes = 0
                val intList: MutableList<Int> = mutableListOf()

                for (localtime in localTimes) {
                    intList.add(localtime.minute)
                }

                intList.forEach {
                    totalMinutes += it
                }

                val totalSeconds = totalMinutes * 60
                val workString = LocalTime.of((totalSeconds / 3600) % 24, (totalSeconds / 60) % 60, totalSeconds % 60).toString()

                if (totalSeconds == 0) {
                    emit("$categoryName: 00:00")
                } else {
                    emit("$categoryName: $workString")
                }
            }
        } else {
            emit("Unknown category: $categoryName")
        }
    }
}