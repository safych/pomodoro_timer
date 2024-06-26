package com.example.pomodorotimer.presentation

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.InputDeviceCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pomodorotimer.R
import com.example.pomodorotimer.databinding.FragmentTimerBinding
import com.example.pomodorotimer.presentation.viewmodel.HistoryViewModel
import com.example.pomodorotimer.presentation.viewmodel.TimerViewModel
import com.example.pomodorotimer.presentation.viewmodel.WorkIntervalViewModelFactory
import java.io.Serializable
import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class TimerFragment : Fragment(), Serializable {
    private val historyViewModel: HistoryViewModel by activityViewModels {
        WorkIntervalViewModelFactory(
            (activity?.application as WorkIntervalApplication).database.workIntervalDao()
        )
    }

    private val sharedViewModel: TimerViewModel by activityViewModels()

    private var binding: FragmentTimerBinding? = null

    private lateinit var countDownTimer: CountDownTimer

    private var remainingMilliseconds: Long = -1
    private var totalMillis: Long? = null

    private val workTime: Long = 25
    private val shortRelax: Long = 5
    private val longRelax: Long = 30

    private val pomodoroArray: Array<Long> = arrayOf(workTime, shortRelax,
        workTime, shortRelax, workTime, shortRelax, workTime, longRelax)

    private var isImage1Displayed = true

    private lateinit var context: Context

    private lateinit var progressBar: ProgressBar
    private lateinit var statusView: TextView
    private lateinit var timeView: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var historyButton: ImageButton
    private lateinit var resetButton: ImageButton

    private lateinit var vibrateService: Intent

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentBinding = FragmentTimerBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        context = requireContext()
        bindingTimer(fragmentBinding.root)
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.timerFragment = this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    fun moveToHistory() {
        findNavController().navigate(R.id.action_timerFragment_to_historyFragment)
    }

    private fun bindingTimer(view: View) {
        statusView = view.findViewById(R.id.status)
        timeView = view.findViewById(R.id.time)
        playPauseButton = view.findViewById(R.id.playPauseButton)
        historyButton = view.findViewById(R.id.historyButton)
        resetButton = view.findViewById(R.id.resetButton)
        progressBar = view.findViewById(R.id.progressBar)

        playPauseButton.setOnClickListener {
            if (!isImage1Displayed) context.stopService(vibrateService)
            changeButtons()
        }

        playPauseButton.setOnLongClickListener(OnLongClickListener {
            sharedViewModel.skipToNext()
            statusView.text = sharedViewModel.getStatus()
            remainingMilliseconds = -1
            changeButtons()
            true
        })

        resetButton.setOnClickListener {
            resetPomodoro()
        }

        view.setOnGenericMotionListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_SCROLL &&
                ev.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)
            ) {
                val axisValue = ev.getAxisValue(MotionEvent.AXIS_SCROLL)
                if (axisValue > 0.0f) {
                    sharedViewModel.moveToPreviousActivity()
                } else if (axisValue < 0.0f) {
                    sharedViewModel.moveToNextActivity()
                }

                statusView.text = sharedViewModel.getStatus()
                remainingMilliseconds = -1
                true
            } else {
                false
            }
        }
    }

    private fun resetPomodoro() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_dialog, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
        val alert = builder.create()

        dialogView.findViewById<TextView>(R.id.textQuestion).text = getString(R.string.resetPomodoro)

        val positiveButton = dialogView.findViewById<ImageButton>(R.id.positive_button)
        positiveButton.setOnClickListener {
            remainingMilliseconds = -1
            alert.dismiss()
        }

        val negativeButton = dialogView.findViewById<ImageButton>(R.id.negative_button)
        negativeButton.setOnClickListener {
            alert.dismiss()
        }

        alert.show()
    }

    private fun startTimer() {
        val currentTime = LocalTime.now()
        val targetTime = currentTime.plus(pomodoroArray[sharedViewModel.getPeriod()], ChronoUnit.MINUTES)

        val duration = Duration.between(currentTime, targetTime)

        val millisecondsRemaining : Long = if (remainingMilliseconds < 0) {
            totalMillis = duration.toMillis()
            duration.toMillis()
        } else {
            remainingMilliseconds
        }

        timeView.text = displayOfRemainingTime(millisecondsRemaining)

        vibrateService = Intent(context, VibrateService::class.java)

        if (isImage1Displayed) {
            countDownTimer = object : CountDownTimer(millisecondsRemaining, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val millisUntilFinished = millisUntilFinished
                    val progress = (millisUntilFinished.toFloat() / totalMillis!! * 100).toInt()
                    val secondsRemaining = millisUntilFinished / 1000
                    remainingMilliseconds = millisUntilFinished

                    updateTimeUI(secondsRemaining)
                    progressBar.progress = progress
                }

                override fun onFinish() {
                    insertWorkInterval()
                    remainingMilliseconds = -1
                    sharedViewModel.skipToNext()
                    statusView.text = sharedViewModel.getStatus()
                    changeButtons()
                }
            }
            vibrateService.putExtra("millisecondsRemaining", millisecondsRemaining)
            context.startService(vibrateService)
            countDownTimer.start()
        } else {
            countDownTimer.cancel()
        }
    }

    private fun displayOfRemainingTime(millisecondsRemaining: Long): String {
        val minutes = millisecondsRemaining / 60000
        val seconds = millisecondsRemaining % 60000
        val text = String.format("%02d:%02d", minutes, seconds)

        return if (remainingMilliseconds < 0) text else text.substring(0, text.length-3)
    }

    private fun updateTimeUI(secondsRemaining: Long) {
        val minutes = secondsRemaining / 60
        val seconds = secondsRemaining % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        timeView.text = timeString
    }

    private fun changeButtons() {
        if (isImage1Displayed) {
            historyButton.isVisible = false
            resetButton.isVisible = false
            progressBar.isVisible = true
            sharedViewModel.start()
            statusView.text = sharedViewModel.getStatus()
            startTimer()
        } else {
            progressBar.isVisible = false
            resetButton.isVisible = true
            startTimer()
        }
        isImage1Displayed = !isImage1Displayed
        setImageResource()
    }

    private fun setImageResource() {
        val imageResource = if (isImage1Displayed) {
            R.drawable.baseline_play_arrow_24
        } else {
            R.drawable.baseline_pause_24
        }
        playPauseButton.setImageResource(imageResource)
    }

    private fun insertWorkInterval() {
        historyViewModel.addNewWorkInterval(sharedViewModel.getTypeActivity(),
            pomodoroArray[sharedViewModel.getPeriod()].toInt())
    }
}