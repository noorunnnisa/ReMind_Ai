package com.example.remind_ai.stage1

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.remind_ai.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Stage1Activity : AppCompatActivity() {

    private lateinit var tvTodayLabel: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var cardDailyReminders: CardView
    private lateinit var cardVoiceAssistant: CardView
    private lateinit var cardRoutineChecklist: CardView
    private lateinit var cardQuickThoughtsPad: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stage_01)

        tvTodayLabel = findViewById(R.id.tvTodayLabel)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)
        cardDailyReminders = findViewById(R.id.cardDailyReminders)
        cardVoiceAssistant = findViewById(R.id.cardVoiceAssistant)
        cardRoutineChecklist = findViewById(R.id.cardRoutineChecklist)
        cardQuickThoughtsPad = findViewById(R.id.cardQuickThoughtsPad)

        updateCurrentDate()

        cardDailyReminders.setOnClickListener {
            startActivity(Intent(this, AddReminderS1Activity::class.java))
        }

        cardVoiceAssistant.setOnClickListener {
            startActivity(Intent(this, VoiceAssistantActivity::class.java))
        }

        cardRoutineChecklist.setOnClickListener {
            startActivity(Intent(this, RoutineChecklistActivity::class.java))
        }

        cardQuickThoughtsPad.setOnClickListener {
            startActivity(Intent(this, QuickThoughtsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateCurrentDate()
    }

    private fun updateCurrentDate() {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        tvTodayLabel.text = "Today"
        tvCurrentDate.text = dateFormat.format(currentDate)
    }
}
