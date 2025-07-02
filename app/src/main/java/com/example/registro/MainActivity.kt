package com.example.registro // Asegúrate de que este sea tu nombre de paquete

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Patterns // Importar para validación de email

class MainActivity : AppCompatActivity() {

    // Declaración de las instancias de Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Declaración de los elementos de la UI
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etFullName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: Button
    private lateinit var tvSectionTitle: TextView
    private lateinit var tvToggleMode: TextView
    private lateinit var tvForgotPassword: TextView

    private var isRegisterMode = true // Variable para controlar el modo actual (registro o login)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Firebase Authentication y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Asignar los elementos de la UI a sus IDs en el XML
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etFullName = findViewById(R.id.et_full_name)
        etPhoneNumber = findViewById(R.id.et_phone_number)
        btnRegister = findViewById(R.id.btn_register)
        btnLogin = findViewById(R.id.btn_login)
        tvSectionTitle = findViewById(R.id.tv_section_title)
        tvToggleMode = findViewById(R.id.tv_toggle_mode)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)

        // Configurar los listeners
        btnRegister.setOnClickListener {
            registerUser()
        }

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvToggleMode.setOnClickListener {
            toggleMode() // Llama a la función para cambiar entre modos
        }

        tvForgotPassword.setOnClickListener {
            sendPasswordReset() // Llama a la función para restablecer la contraseña
        }

        // Establecer el modo inicial (registro)
        updateUIMode()
    }

    /**
     * Este método se llama justo antes de que la actividad sea visible.
     * Aquí verificamos si el usuario ya está autenticado.
     */
    override fun onStart() {
        super.onStart()
        // Obtener el usuario actualmente logueado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Si hay un usuario logueado, redirigir a la pantalla de bienvenida
            goToWelcomeScreen()
        }
    }

    /**
     * Función para alternar entre el modo de registro y el modo de inicio de sesión.
     */
    private fun toggleMode() {
        isRegisterMode = !isRegisterMode // Invierte el valor de la variable
        updateUIMode() // Actualiza la UI según el nuevo modo
    }

    /**
     * Interfaz de usuario para reflejar el modo actual (registro o login).
     */
    private fun updateUIMode() {
        val paramsToggleMode = tvToggleMode.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

        if (isRegisterMode) {
            // Modo Registro
            tvSectionTitle.text = "Registro de Usuario"
            btnRegister.visibility = View.VISIBLE
            btnLogin.visibility = View.GONE
            etFullName.visibility = View.VISIBLE
            etPhoneNumber.visibility = View.VISIBLE
            tvForgotPassword.visibility = View.GONE // Ocultar en modo registro

            tvToggleMode.text = "¿Ya tienes una cuenta? Inicia Sesión"
            // Ajustar la restricción del botón de registro cuando los campos de perfil están visibles
            val paramsRegister = btnRegister.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            paramsRegister.topToBottom = R.id.et_phone_number
            btnRegister.layoutParams = paramsRegister

            // Posicion de tvToggleMode debajo de btnRegister en modo registro
            paramsToggleMode.topToBottom = R.id.btn_register
            tvToggleMode.layoutParams = paramsToggleMode

        } else {
            // Modo Inicio de Sesión
            tvSectionTitle.text = "Inicio de Sesión"
            btnRegister.visibility = View.GONE
            btnLogin.visibility = View.VISIBLE
            etFullName.visibility = View.GONE
            etPhoneNumber.visibility = View.GONE
            tvForgotPassword.visibility = View.VISIBLE // Mostrar en modo inicio de sesión

            tvToggleMode.text = "¿No tienes cuenta? Regístrate"

            // Ajustar la restricción del botón de login cuando está visible
            val paramsLogin = btnLogin.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            paramsLogin.topToBottom = R.id.et_password
            btnLogin.layoutParams = paramsLogin

            // Posicionar tvToggleMode debajo de tvForgotPassword en modo login
            paramsToggleMode.topToBottom = R.id.tv_forgot_password
            tvToggleMode.layoutParams = paramsToggleMode
        }
        // Limpiar los campos cada vez que se cambia de modo
        etEmail.text.clear()
        etPassword.text.clear()
        etFullName.text.clear()
        etPhoneNumber.text.clear()
    }

    /**
     * Función para manejar el proceso de registro del usuario.
     */
    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()

        // Validaciones mejoradas
        if (!isValidEmail(email)) {
            etEmail.error = "Ingresa un correo electrónico válido"
            etEmail.requestFocus()
            return
        }
        if (!isValidPassword(password)) {
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            etPassword.requestFocus()
            return
        }
        if (fullName.isEmpty()) {
            etFullName.error = "El nombre completo no puede estar vacío"
            etFullName.requestFocus()
            return
        }
        if (!isValidPhoneNumber(phoneNumber)) {
            etPhoneNumber.error = "Ingresa un número telefónico válido (ej. 10 dígitos)"
            etPhoneNumber.requestFocus()
            return
        }

        Toast.makeText(this, "Registrando usuario...", Toast.LENGTH_SHORT).show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                    user?.let {
                        saveUserToFirestore(it.uid, email, fullName, phoneNumber)
                    }
                    // La redirección a WelcomeScreen se hace después de guardar el perfil en Firestore
                } else {
                    Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Función para manejar el proceso de inicio de sesión del usuario.
     */
    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validaciones mejoradas
        if (!isValidEmail(email)) {
            etEmail.error = "Ingresa un correo electrónico válido"
            etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "La contraseña no puede estar vacía"
            etPassword.requestFocus()
            return
        }

        Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()
                    goToWelcomeScreen() // Redirigir a la pantalla de bienvenida después del login
                } else {
                    Toast.makeText(this, "Error al iniciar sesión: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Envía un correo electrónico de restablecimiento de contraseña al email ingresado.
     * Se activa al hacer clic en "¿Olvidaste tu contraseña?".
     */
    private fun sendPasswordReset() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tu correo electrónico para restablecer la contraseña.", Toast.LENGTH_LONG).show()
            etEmail.error = "Campo requerido"
            etEmail.requestFocus()
            return
        }
        if (!isValidEmail(email)) {
            etEmail.error = "Ingresa un correo electrónico válido"
            etEmail.requestFocus()
            return
        }

        Toast.makeText(this, "Enviando correo de restablecimiento...", Toast.LENGTH_SHORT).show()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Se ha enviado un correo electrónico para restablecer tu contraseña. Revisa tu bandeja de entrada y spam.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Error al enviar correo de restablecimiento: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Función para guardar los datos del usuario en Firestore, ahora incluye nombre completo y número telefónico.
     * Se llama después de un registro exitoso.
     * @param uid El ID único del usuario proporcionado por Firebase Authentication.
     * @param email El correo electrónico del usuario.
     * @param fullName El nombre completo del usuario.
     * @param phoneNumber El número telefónico del usuario.
     */
    private fun saveUserToFirestore(uid: String, email: String, fullName: String, phoneNumber: String) {
        val userMap = hashMapOf(
            "email" to email,
            "uid" to uid,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Perfil de usuario guardado.", Toast.LENGTH_SHORT).show()
                goToWelcomeScreen() // Redirigir después de guardar el perfil
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar perfil en Firestore: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Redirige al usuario a la WelcomeActivity y finaliza la MainActivity.
     */
    private fun goToWelcomeScreen() {
        val intent = Intent(this, WelcomeActivity::class.java)
        // Estas flags son importantes para limpiar la pila de actividades
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finaliza MainActivity para que no quede en la pila
    }

    // --- FUNCIONES DE VALIDACIÓN ---
    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Validación básica para números telefónicos en Venezuela:
        // No vacío, al menos 7 dígitos y solo números.
        // Puedes ajustar la longitud o el regex si necesitas una validación más estricta (ej. 10 dígitos)
        return phoneNumber.isNotEmpty() && phoneNumber.length >= 7 && phoneNumber.matches("[0-9]+".toRegex())
    }

}