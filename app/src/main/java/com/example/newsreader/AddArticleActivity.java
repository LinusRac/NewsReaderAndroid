package com.example.newsreader;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.newsreader.exceptions.AuthenticationError;
import com.example.newsreader.exceptions.ServerCommunicationError;

import java.util.Properties;

public class AddArticleActivity extends AppCompatActivity {

    private EditText editArticleCategory, editArticleTitle, editArticleAbstract, editArticleBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_article);

        editArticleCategory = findViewById(R.id.edit_article_category);
        editArticleTitle = findViewById(R.id.edit_article_title);
        editArticleAbstract = findViewById(R.id.edit_article_abstract);
        editArticleBody = findViewById(R.id.edit_article_body);

        Button btnSaveArticle = findViewById(R.id.btn_save_article);
        btnSaveArticle.setOnClickListener(v -> saveArticle());
    }

    private void saveArticle() {
        String category = editArticleCategory.getText().toString();
        String title = editArticleTitle.getText().toString();
        String abstractText = editArticleAbstract.getText().toString();
        String body = editArticleBody.getText().toString();

        if (category.isEmpty() || title.isEmpty() || abstractText.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Properties properties = new Properties();
                String username = getIntent().getStringExtra("username");
                String password = getIntent().getStringExtra("password");
                properties.setProperty(ModelManager.ATTR_LOGIN_USER, username);
                properties.setProperty(ModelManager.ATTR_LOGIN_PASS, password);
                properties.setProperty(ModelManager.ATTR_SERVICE_URL, "https://sanger.dia.fi.upm.es/pmd-task/");

                ModelManager modelManager = new ModelManager(properties);

                Article article = new Article(modelManager, category, title, abstractText, body, "");
                article.save();

                runOnUiThread(() -> {
                    Toast.makeText(AddArticleActivity.this, "Article saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (ServerCommunicationError | AuthenticationError e) {
                runOnUiThread(() -> Toast.makeText(AddArticleActivity.this, "Failed to save article: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
