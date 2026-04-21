package com.example.remind_ai.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.remind_ai.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.remind_ai.stage1.Stage1DashboardActivity
import com.example.remind_ai.stage2.Stage2DashboardActivity
import com.example.remind_ai.stage3.Stage3PatientDashboardActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkUserDataAndRedirect()
                } else {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Log In"
                    Toast.makeText(
                        this,
                        "Login Failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun checkUserDataAndRedirect() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            btnLogin.isEnabled = true
            btnLogin.text = "Log In"
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        database.reference.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Log In"
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val role = snapshot.child("role").getValue(String::class.java).orEmpty()
                val stage = snapshot.child("stage").getValue(String::class.java).orEmpty()

                val intent = when (role) {
                    "caregiver" -> Intent(this, CaregiverHomeActivity::class.java)

                    "patient" -> {
                        when (stage) {
                            "Stage 1" -> Intent(this, Stage1DashboardActivity::class.java)
                            "Stage 2" -> Intent(this, Stage2DashboardActivity::class.java)
                            "Stage 3" -> Intent(this, Stage3PatientDashboardActivity::class.java)
                            else -> Intent(this, StageDetectionActivity::class.java)
                        }
                    }

                    else -> Intent(this, RoleSelectionActivity::class.java)
                }

                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                btnLogin.isEnabled = true
                btnLogin.text = "Log In"
                Toast.makeText(
                    this,
                    "Failed to fetch user data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
