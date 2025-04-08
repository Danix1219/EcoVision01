package com.example.ecovision;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity{

        private TextInputEditText etUsername, etPassword;
        private MaterialButton btnLogin;

        private static final String API_URL = "https://ecovision.bsite.net/api/Usuarios/ValidarLogin";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.login); // Asegúrate de que login.xml sea el nombre del layout

            etUsername = findViewById(R.id.etUsername);
            etPassword = findViewById(R.id.etPassword);
            btnLogin = findViewById(R.id.btnLogin);

            btnLogin.setOnClickListener(v -> validarLogin());
        }

        private void validarLogin() {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show();
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
                        if (code == 200 && !response.toString().equals("null")) {
                            Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show();
                            // Aquí puedes redirigir a tu actividad principal, por ejemplo:
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                        }
                    });

                    conn.disconnect();
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        }
}
