package com.example.petcare;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ModificarPerfil extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 100;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;
    private Button btnYaEsta;

    private ImageView imageView22; // Imagen de perfil
    private Uri selectedImageUri; // URI de la imagen seleccionada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editar_perfil);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("profile_pictures");
        btnYaEsta = findViewById(R.id.YaEsta);
        EditText editNombre = findViewById(R.id.Nombre);
        EditText editCorreo = findViewById(R.id.Correo);
        EditText editApellidos = findViewById(R.id.Apellidos);

        imageView22 = findViewById(R.id.imageView22);

        // Configurar el listener para abrir la galería
        imageView22.setOnClickListener(v -> checkPermissionsAndOpenGallery());

        // Cargar imagen de perfil actual (si existe)
        loadProfileImage();

        // Acción al presionar "¡Ya Está!"
        btnYaEsta.setOnClickListener(v -> {
            String nombre = editNombre.getText().toString().trim();
            String correo = editCorreo.getText().toString().trim();
            String apellidos = editApellidos.getText().toString().trim();

            if (nombre.isEmpty() || correo.isEmpty() || apellidos.isEmpty()) {
                Toast.makeText(ModificarPerfil.this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                String userId = user.getUid();

                // Crear un mapa con los datos actualizados
                Map<String, Object> updates = new HashMap<>();
                updates.put("nombre", nombre);
                updates.put("correo", correo);
                updates.put("apellidos", apellidos);

                // Actualizar en Firestore
                firestore.collection("users").document(userId)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ModificarPerfil.this, "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(ModificarPerfil.this, MiperfilActivity.class);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(ModificarPerfil.this, "Error al actualizar el perfil. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(ModificarPerfil.this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para verificar permisos y abrir la galería
    private void checkPermissionsAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_CODE);
            }
        }
        else {
            openGallery();
        }
    }


    // Método para manejar la respuesta de la solicitud de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permiso denegado. No se puede acceder a la galería.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Método para abrir la galería
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // ActivityResultLauncher para manejar el resultado de la galería
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    uploadImageToFirebase(selectedImageUri);
                }
            });

    // Método para subir la imagen a Firebase Storage
    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        StorageReference userImageRef = storageReference.child(userId + ".jpg");

        userImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> userImageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();

                            // Guardar la URI en Firestore
                            saveImageUriToFirestore(downloadUrl);

                            // Guardar la URI en SharedPreferences
                            SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("profileImage", downloadUrl);
                            editor.apply();

                            Toast.makeText(this, "Imagen subida correctamente.", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> Toast.makeText(this, "Error al subir la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Método para guardar el enlace de la imagen en Firestore
    private void saveImageUriToFirestore(String imageUrl) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("profilePicture", imageUrl);

            firestore.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> loadProfileImage())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar la URL de la imagen.", Toast.LENGTH_SHORT).show());
        }
    }

    // Método para cargar la imagen de perfil en el ImageView
    private void loadProfileImage() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            firestore.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String profilePictureUrl = documentSnapshot.getString("profilePicture");
                            if (profilePictureUrl != null) {
                                Glide.with(this).load(profilePictureUrl).into(imageView22);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al cargar la imagen de perfil.", Toast.LENGTH_SHORT).show());
        }
    }
}
