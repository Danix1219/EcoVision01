package com.example.ecovision;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity; // <-- IMPORTANTE

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegisterActivity extends AppCompatActivity { // <-- AÑADIDO AppCompatActivity
    private TextInputEditText etNombres, etApellidos, etEmail, etTelefono, etUsername, etPassword;
    private MaterialButton btnRegister;

    private static final String API_URL = "https://ecovision.bsite.net/api/Cliente/CrearCliente/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cuenta); // <-- corrige el nombre si el XML se llama así

        etNombres = findViewById(R.id.etNombres);
        etApellidos = findViewById(R.id.etApellidos);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registrarUsuario());
    }

    private void registrarUsuario() {
        String nombres = etNombres.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo = etEmail.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nombres.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || telefono.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("nombres", nombres);
                jsonBody.put("apellidos", apellidos);
                jsonBody.put("correo", correo);
                jsonBody.put("telefono", telefono);
                jsonBody.put("username", username);
                jsonBody.put("password", password);

                OutputStream os = conn.getOutputStream();
                byte[] input = jsonBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
                os.close();

                int code = conn.getResponseCode();
                InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }

                runOnUiThread(() -> {
                    if (code == 200) {
                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error al registrar: " + response, Toast.LENGTH_LONG).show();
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
