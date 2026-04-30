package com.example.remind_ai.stage1

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.remind_ai.R
import org.json.JSONArray
import java.util.Locale

class VoiceAssistantActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnBack: ImageView
    private lateinit var micBtn: ImageView
    private lateinit var tvGoodMorning: TextView
    private lateinit var tvHelp: TextView
    private lateinit var tvSpeak: TextView

    private lateinit var btnReminder: Button
    private lateinit var btnMessages: Button
    private lateinit var btnSchedule: Button
    private lateinit var btnFamily: Button

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var prefs: SharedPreferences

    private var isTtsReady = false

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startListening()
            } else {
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voiceassistant_s1)

        btnBack = findViewById(R.id.btnBack)
        micBtn = findViewById(R.id.micBtn)
        tvGoodMorning = findViewById(R.id.tvGoodMorning)
        tvHelp = findViewById(R.id.tvHelp)
        tvSpeak = findViewById(R.id.tvSpeak)

        btnReminder = findViewById(R.id.btnReminder)
        btnMessages = findViewById(R.id.btnMessages)
        btnSchedule = findViewById(R.id.btnSchedule)
        btnFamily = findViewById(R.id.btnFamily)

        prefs = getSharedPreferences("remind_ai_prefs", MODE_PRIVATE)
        textToSpeech = TextToSpeech(this, this)

        setupGreeting()
        setupSpeechRecognizer()

        btnBack.setOnClickListener {
            finish()
        }

        micBtn.setOnClickListener {
            checkPermissionAndListen()
        }

        tvSpeak.setOnClickListener {
            checkPermissionAndListen()
        }

        btnReminder.setOnClickListener {
            startActivity(Intent(this, AddReminderS1Activity::class.java))
        }

        btnMessages.setOnClickListener {
            startActivity(Intent(this, QuickThoughtsActivity::class.java))
        }

        btnSchedule.setOnClickListener {
            speak("Opening reminders for your daily schedule")
            startActivity(Intent(this, AddReminderS1Activity::class.java))
        }

        btnFamily.setOnClickListener {
            startActivity(Intent(this, MyJournalActivity::class.java))
        }
    }

    private fun setupGreeting() {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        tvGoodMorning.text = when {
            hour < 12 -> "Good Morning,"
            hour < 17 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
        tvHelp.text = "How can I help you today?"
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvSpeak.text = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                tvSpeak.text = "Listening..."
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                tvSpeak.text = "Processing..."
            }

            override fun onError(error: Int) {
                tvSpeak.text = "Tap to Speak"
                Toast.makeText(this@VoiceAssistantActivity, "Could not understand. Try again.", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                tvSpeak.text = "Tap to Speak"
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.lowercase(Locale.getDefault()).orEmpty()

                if (spokenText.isNotEmpty()) {
                    handleVoiceCommand(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    private fun checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        speechRecognizer.startListening(speechIntent)
    }

    private fun handleVoiceCommand(command: String) {
        when {
            command.contains("open chatbot") || command.contains("personal chatbot") -> {
                speak("Opening chatbot")
                startActivity(Intent(this, PersonalChatbotS1Activity::class.java))
            }

            command.contains("open journal") || command.contains("my journal") -> {
                speak("Opening journal")
                startActivity(Intent(this, MyJournalActivity::class.java))
            }

            command.contains("open quick thought") || command.contains("quick thought pad") -> {
                speak("Opening quick thoughts")
                startActivity(Intent(this, QuickThoughtsActivity::class.java))
            }

            command.contains("open reminders") || command.contains("my reminders") -> {
                speak("Opening reminders")
                startActivity(Intent(this, AddReminderS1Activity::class.java))
            }

            command.contains("add reminder") || command.contains("set reminder") -> {
                val reminderText = extractReminderText(command)
                saveReminder(reminderText)
                Toast.makeText(this, "Reminder added", Toast.LENGTH_SHORT).show()
                speak("Reminder added successfully")
            }

            command.contains("add quick thought") || command.contains("save thought") -> {
                val thoughtText = extractQuickThoughtText(command)
                saveQuickThought(thoughtText)
                Toast.makeText(this, "Quick thought added", Toast.LENGTH_SHORT).show()
                speak("Quick thought added")
            }

            command.contains("checklist") && command.contains("unchecked") -> {
                val response = getChecklistStatusResponse()
                Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                speak(response)
            }

            command.contains("schedule") -> {
                val response = "Your daily schedule is available in reminders."
                Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
                speak(response)
                startActivity(Intent(this, AddReminderS1Activity::class.java))
            }

            else -> {
                Toast.makeText(this, "Command not recognized", Toast.LENGTH_SHORT).show()
                speak("Sorry, I did not understand that command.")
            }
        }
    }

    private fun extractReminderText(command: String): String {
        return when {
            command.contains("add reminder") ->
                command.substringAfter("add reminder").trim().ifEmpty { "New reminder" }

            command.contains("set reminder") ->
                command.substringAfter("set reminder").trim().ifEmpty { "New reminder" }

            else -> "New reminder"
        }
    }

    private fun extractQuickThoughtText(command: String): String {
        return when {
            command.contains("add quick thought") ->
                command.substringAfter("add quick thought").trim().ifEmpty { "New quick thought" }

            command.contains("save thought") ->
                command.substringAfter("save thought").trim().ifEmpty { "New quick thought" }

            else -> "New quick thought"
        }
    }

    private fun saveReminder(reminderText: String) {
        val reminders = getStringList("reminders")
        reminders.add(reminderText)
        saveStringList("reminders", reminders)
    }

    private fun saveQuickThought(thoughtText: String) {
        val thoughts = getStringList("quick_thoughts")
        thoughts.add(thoughtText)
        saveStringList("quick_thoughts", thoughts)
    }

    private fun getChecklistStatusResponse(): String {
        val checklist = getStringList("unchecked_checklist")
        return if (checklist.isEmpty()) {
            "No, all checklist activities are completed."
        } else {
            "Yes, you still have unchecked activities."
        }
    }

    private fun getStringList(key: String): MutableList<String> {
        val json = prefs.getString(key, null) ?: return mutableListOf()
        val jsonArray = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    private fun saveStringList(key: String, list: List<String>) {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        prefs.edit().putString(key, jsonArray.toString()).apply()
    }

    private fun speak(message: String) {
        if (isTtsReady) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "voice_assistant")
            } else {
                @Suppress("DEPRECATION")
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.UK
            isTtsReady = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
