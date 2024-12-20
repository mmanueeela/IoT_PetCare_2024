package com.example.petcare;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {
    private EditText nombreField, apellidosField, correoField, contraseñaField;
    private Button btnRegistrar;

    private FirebaseAuth auth; // Instancia de Firebase Authentication
    private FirebaseFirestore db; // Instancia de Firebase Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro);

        // Inicializar Firebase Authentication y Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Referencias a las vistas
        nombreField = findViewById(R.id.nombre);
        apellidosField = findViewById(R.id.apellidos);
        correoField = findViewById(R.id.correo);
        contraseñaField = findViewById(R.id.contraseña);
        btnRegistrar = findViewById(R.id.register);

        // Acción al presionar el botón de registrar
        btnRegistrar.setOnClickListener(v -> {
            // Obtener los valores de los campos
            String nombre = nombreField.getText().toString().trim();
            String apellidos = apellidosField.getText().toString().trim();
            String correo = correoField.getText().toString().trim();
            String contraseña = contraseñaField.getText().toString().trim();

            // Validar que los campos no estén vacíos
            if (nombre.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || contraseña.isEmpty()) {
                Toast.makeText(RegistroActivity.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Usar Firebase Authentication para registrar al usuario
            auth.createUserWithEmailAndPassword(correo, contraseña)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Obtener el usuario actual después de registrarlo
                            FirebaseUser user = auth.getCurrentUser();

                            if (user != null) {
                                String userId = user.getUid(); // UID generado por Firebase Authentication

                                // Crear un mapa con los datos del usuario
                                Map<String, Object> usuario = new HashMap<>();
                                usuario.put("nombre", nombre);
                                usuario.put("apellidos", apellidos);
                                usuario.put("correo", correo);
                                usuario.put("userId", userId);

                                // Guardar los datos adicionales en Firestore
                                db.collection("users")
                                        .document(userId) // Usa el UID del usuario como el ID del documento
                                        .set(usuario)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(RegistroActivity.this, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show();
                                            finish(); // Finalizar la actividad actual
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(RegistroActivity.this, "Error al guardar los datos en Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            }
                        } else {
                            Toast.makeText(RegistroActivity.this, "Error al registrar el usuario: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}