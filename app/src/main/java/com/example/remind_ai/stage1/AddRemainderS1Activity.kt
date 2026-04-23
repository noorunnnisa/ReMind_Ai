package com.example.remind_ai.stage1

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.remind_ai.R
import com.example.remind_ai.model.ReminderModel

class AddReminderS1Activity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etReminderTitle: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var spinnerRepeat: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSave: Button

    // This instance will store the user's selected date and time
    private val calendar = Calendar.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addremainder_s1)

        btnBack = findViewById(R.id.btnBack)
        etReminderTitle = findViewById(R.id.etReminderTitle)
        etDate = findViewById(R.id.etDate)
        etTime = findViewById(R.id.etTime)
        spinnerRepeat = findViewById(R.id.spinnerRepeat)
        etNotes = findViewById(R.id.etNotes)
        btnSave = findViewById(R.id.btnSave)

        createNotificationChannel()
        setupRepeatSpinner()
        setupListeners()
    }

    private fun setupRepeatSpinner() {
        val repeatOptions = arrayOf("No Repeat", "Daily", "Weekly", "Monthly")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, repeatOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRepeat.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        etDate.setOnClickListener { showDatePicker() }

        etTime.setOnClickListener { showTimePicker() }

        btnSave.setOnClickListener { saveReminder() }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                etDate.setText(dateFormat.format(calendar.time))
                etDate.error = null // Clear error once date is picked
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                etTime.setText(timeFormat.format(calendar.time))
                etTime.error = null // Clear error once time is picked
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePickerDialog.show()
    }

    private fun saveReminder() {
        val title = etReminderTitle.text.toString().trim()
        val dateStr = etDate.text.toString().trim()
        val timeStr = etTime.text.toString().trim()
        val repeat = spinnerRepeat.selectedItem.toString()
        val notes = etNotes.text.toString().trim()

        // Validation logic
        if (title.isEmpty()) {
            etReminderTitle.error = "Enter reminder title"
            return
        }
        if (dateStr.isEmpty()) {
            etDate.error = "Select date"
            return
        }
        if (timeStr.isEmpty()) {
            etTime.error = "Select time"
            return
        }

        // Use a 1-minute buffer (60000ms) to prevent "past time" errors
        // if the user spends time typing notes after picking the time.
        val currentTime = System.currentTimeMillis()
        if (calendar.timeInMillis <= currentTime) {
            Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show()
            return
        }

        val reminderId = database.child("reminders").push().key ?: return

        val reminder = ReminderModel(
            id = reminderId,
            title = title,
            date = dateStr,
            time = timeStr,
            repeat = repeat,
            notes = notes,
            timestamp = calendar.timeInMillis
        )

        btnSave.isEnabled = false // Prevent double clicks
        database.child("reminders").child(reminderId).setValue(reminder)
            .addOnSuccessListener {
                scheduleNotification(reminder)
                Toast.makeText(this, "Reminder saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                btnSave.isEnabled = true
                Toast.makeText(this, "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun scheduleNotification(reminder: ReminderModel) {
        // Explicitly use the full path to the Receiver in the stage1 package
        val intent = Intent(this, com.example.remind_ai.stage1.ReminderReceiver::class.java).apply {
            putExtra("title", reminder.title)
            putExtra("notes", reminder.notes)
            putExtra("id", reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Use setExactAndAllowWhileIdle for reliable timing on modern Android
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.timestamp,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback for cases where exact alarm permission isn't granted
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.timestamp, pendingIntent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Reminder Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Used for patient daily reminders"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}