package com.example.newsreader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsreader.exceptions.AuthenticationError;

import java.util.Properties;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "NewsReaderPrefs";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ImageButton btnBack = findViewById(R.id.btn_back);
        EditText editUsername = findViewById(R.id.edit_username);
        EditText editPassword = findViewById(R.id.edit_password);
        CheckBox checkRememberMe = findViewById(R.id.check_remember_me);
        Button btnLogin = findViewById(R.id.btn_login);

        btnBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnLogin.setOnClickListener(v -> {
            String username = editUsername.getText().toString();
            String password = editPassword.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                try {
                    Properties properties = new Properties();
                    properties.setProperty(ModelManager.ATTR_LOGIN_USER, username);
                    properties.setProperty(ModelManager.ATTR_LOGIN_PASS, password);
                    properties.setProperty(ModelManager.ATTR_SERVICE_URL, "https://sanger.dia.fi.upm.es/pmd-task/");

                    ModelManager modelManager = new ModelManager(properties);

                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show());

                    if (checkRememberMe.isChecked()) {
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putString(PREF_USERNAME, username);
                        editor.putString(PREF_PASSWORD, password);
                        editor.apply();
                    }

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("isLoggedIn", true);
                    resultIntent.putExtra("username", username);
                    resultIntent.putExtra("password", password);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } catch (AuthenticationError e) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
    }
}