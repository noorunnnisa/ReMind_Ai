package com.example.remind_ai.stage1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar
import java.util.Locale

class VoiceAssistantActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnBack: ImageView
    private lateinit var micBtn: ImageView
    private lateinit var tvSpeak: TextView
    private lateinit var tvGoodMorning: TextView
    private lateinit var tvHelp: TextView

    private lateinit var btnReminder: Button
    private lateinit var btnMessages: Button
    private lateinit var btnSchedule: Button
    private lateinit var btnFamily: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var speechIntent: Intent
    private var textToSpeech: TextToSpeech? = null

    private var isListening = false
    private var isTtsReady = false

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startVoiceListening()
            } else {
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show()
                tvHelp.text = "Microphone permission is required"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voiceassistant_s1)

        initViews()
        setGreeting()

        tvSpeak.text = "Tap to Speak"
        tvHelp.text = "Tap mic and speak slowly"

        textToSpeech = TextToSpeech(this, this)
        setupSpeechRecognizer()
        setupClickListeners()

        micBtn.setOnLongClickListener {
            speak("Testing voice response")
            true
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        micBtn = findViewById(R.id.micBtn)
        tvSpeak = findViewById(R.id.tvSpeak)
        tvGoodMorning = findViewById(R.id.tvGoodMorning)
        tvHelp = findViewById(R.id.tvHelp)

        btnReminder = findViewById(R.id.btnReminder)
        btnMessages = findViewById(R.id.btnMessages)
        btnSchedule = findViewById(R.id.btnSchedule)
        btnFamily = findViewById(R.id.btnFamily)
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning,"
            hour < 17 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
        tvGoodMorning.text = greeting
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        micBtn.setOnClickListener {
            checkAudioPermissionAndStart()
        }

        btnReminder.setOnClickListener {
            speak("Opening reminder screen")
            startActivity(Intent(this, AddReminderS1Activity::class.java))
        }

        btnMessages.setOnClickListener {
            speak("Quick thought pad screen will open here")
        }

        btnSchedule.setOnClickListener {
            speak("Daily schedule screen will open here")
        }

        btnFamily.setOnClickListener {
            speak("My journal screen will open here")
        }
    }

    private fun checkAudioPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_LONG).show()
            tvHelp.text = "Speech recognition not available on this device"
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                tvSpeak.text = "Listening..."
                tvHelp.text = "Speak clearly now"
            }

            override fun onBeginningOfSpeech() {
                tvSpeak.text = "Hearing you..."
            }

            override fun onRmsChanged(rmsdB: Float) {
            }

            override fun onBufferReceived(buffer: ByteArray?) {
            }

            override fun onEndOfSpeech() {
                isListening = false
                tvSpeak.text = "Processing..."
            }

            override fun onError(error: Int) {
                isListening = false
                tvSpeak.text = "Error: $error"
                tvHelp.text = getSpeechErrorMessage(error)

                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    speak("I could not understand. Please speak slowly and clearly.")
                } else {
                    Toast.makeText(
                        this@VoiceAssistantActivity,
                        "Speech error: ${getSpeechErrorMessage(error)}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0].trim()
                    tvSpeak.text = "Heard: $spokenText"
                    tvHelp.text = matches.joinToString(prefix = "Matches: ")
                    Toast.makeText(this@VoiceAssistantActivity, spokenText, Toast.LENGTH_SHORT).show()
                    handleVoiceInput(spokenText)
                } else {
                    tvSpeak.text = "No Result"
                    tvHelp.text = "I could not hear anything"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    tvHelp.text = "Listening: ${matches[0]}"
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
            }
        })
    }

    private fun startVoiceListening() {
        if (speechRecognizer == null || isListening) return

        try {
            tvSpeak.text = "Starting listener..."
            tvHelp.text = "Please speak now"
            speechRecognizer?.startListening(speechIntent)
        } catch (e: Exception) {
            isListening = false
            tvSpeak.text = "Start failed"
            tvHelp.text = e.message ?: "Could not start listening"
            Toast.makeText(this, "Could not start listening: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleVoiceInput(spokenText: String) {
        val command = spokenText.lowercase(Locale.getDefault())
        tvSpeak.text = "Heard: $spokenText"
        Toast.makeText(this, spokenText, Toast.LENGTH_SHORT).show()
        processCommand(command)
    }

    private fun processCommand(command: String) {
        when {
            command.contains("reminder") || command.contains("open reminder") || command.contains("set reminder") -> {
                speak("Opening reminder screen")
                startActivity(Intent(this, AddReminderS1Activity::class.java))
            }

            command.contains("checklist") || command.contains("what is left") || command.contains("left to do") -> {
                readRemainingChecklistItems()
            }

            command.contains("journal") -> {
                speak("Opening journal")
            }

            command.contains("schedule") -> {
                speak("Opening daily schedule")
            }

            command.contains("quick thought") || command.contains("thought pad") || command.contains("notes") -> {
                speak("Opening quick thought pad")
            }

            else -> {
                speak("Sorry, I did not understand that command")
            }
        }
    }

    private fun readRemainingChecklistItems() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            speak("User is not logged in")
            return
        }

        FirebaseDatabase.getInstance().reference
            .child("checklists")
            .child(uid)
            .child("items")
            .get()
            .addOnSuccessListener { snapshot ->
                val pendingItems = mutableListOf<String>()

                for (child in snapshot.children) {
                    val title = child.child("title").getValue(String::class.java).orEmpty()
                    val completed = child.child("completed").getValue(Boolean::class.java) ?: false

                    if (!completed && title.isNotEmpty()) {
                        pendingItems.add(title)
                    }
                }

                if (pendingItems.isEmpty()) {
                    speak("Nothing left to do")
                } else {
                    speak("You still need to do ${pendingItems.joinToString()}")
                }
            }
            .addOnFailureListener {
                speak("I could not read your checklist")
            }
    }

    private fun speak(text: String) {
        tvHelp.text = text
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

        if (!isTtsReady) {
            Toast.makeText(this, "TTS not ready", Toast.LENGTH_SHORT).show()
            return
        }

        textToSpeech?.setSpeechRate(0.9f)
        textToSpeech?.setPitch(1.0f)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_assistant")
    }

    private fun getSpeechErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission missing"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

            if (isTtsReady) {
                textToSpeech?.setSpeechRate(0.9f)
                textToSpeech?.setPitch(1.0f)
                textToSpeech?.speak(
                    "Voice assistant is ready",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "startup_test"
                )
            } else {
                tvHelp.text = "Text to speech language not supported"
            }
        } else {
            isTtsReady = false
            tvHelp.text = "Text to speech initialization failed"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
