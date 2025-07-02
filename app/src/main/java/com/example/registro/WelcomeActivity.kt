package com.example.registro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        auth = FirebaseAuth.getInstance() // Inicializa Firebase Auth

        val logoutButton: Button = findViewById(R.id.btn_logout)
        logoutButton.setOnClickListener {
            signOutUser()
        }
    }

    /**
     * Cierra la sesión del usuario y lo redirige a la MainActivity.
     */
    private fun signOutUser() {
        auth.signOut() // Cierra la sesión del usuario actual
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpia la pila de actividades
        startActivity(intent)
        finish() // Finaliza esta actividad para que el usuario no pueda volver con el botón de atrás
    }
}