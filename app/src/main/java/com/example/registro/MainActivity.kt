// MainActivity.kt

package com.example.registro

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    // Declaración de las instancias de Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Declaración de los elementos de la UI
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firebase Authentication y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Asignar los elementos de la UI a sus IDs en el XML
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnRegister = findViewById(R.id.btn_register)

        // Configurar el listener para el botón de registro
        btnRegister.setOnClickListener {
            registerUser()
        }
    }

    /**
     * Función para manejar el proceso de registro del usuario.
     */
    private fun registerUser() {
        val email = etEmail.text.toString().trim() // Obtener el texto del campo de email
        val password = etPassword.text.toString().trim() // Obtener el texto del campo de contraseña

        // Validar que los campos no estén vacíos
        if (email.isEmpty()) {
            etEmail.error = "El correo electrónico es requerido"
            etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "La contraseña es requerida"
            etPassword.requestFocus()
            return
        }
        if (password.length < 6) { // Firebase requiere al menos 6 caracteres para la contraseña
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            etPassword.requestFocus()
            return
        }

        // Mostrar un mensaje de "Registrando..."
        Toast.makeText(this, "Registrando usuario...", Toast.LENGTH_SHORT).show()

        // Llamar a Firebase Authentication para crear un nuevo usuario
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro exitoso
                    val user = auth.currentUser
                    Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()

                    // Opcional: Guardar datos adicionales del usuario en Firestore
                    user?.let {
                        saveUserToFirestore(it.uid, email) // Guardar UID y email en Firestore
                    }

                    // Aquí podrías navegar a otra actividad (por ejemplo, una pantalla de bienvenida)
                    // val intent = Intent(this, WelcomeActivity::class.java)
                    // startActivity(intent)
                    // finish() // Para que el usuario no pueda volver a la pantalla de registro con el botón de atrás
                } else {
                    // Si el registro falla, mostrar un mensaje de error
                    Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Función opcional para guardar los datos del usuario en Firestore.
     * @param uid El ID único del usuario proporcionado por Firebase Authentication.
     * @param email El correo electrónico del usuario.
     */
    private fun saveUserToFirestore(uid: String, email: String) {
        val userMap = hashMapOf(
            "email" to email,
            "uid" to uid,
            "createdAt" to System.currentTimeMillis() // Opcional: añadir un timestamp
            // Puedes añadir más campos aquí, por ejemplo: "nombre" to "Nombre del Usuario"
        )

        db.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Datos de usuario guardados en Firestore.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar datos en Firestore: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}