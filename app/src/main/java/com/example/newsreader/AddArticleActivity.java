package com.example.newsreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.newsreader.exceptions.AuthenticationError;
import com.example.newsreader.exceptions.ServerCommunicationError;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AddArticleActivity extends AppCompatActivity {

    private Spinner spinnerArticleCategory;
    private EditText editArticleTitle, editArticleAbstract, editArticleBody;
    private ImageView imagePreview;
    private ProgressBar loadingSpinner;
    private String imageBase64;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            imagePreview.setImageBitmap(bitmap);
                            imageBase64 = Utils.imgToBase64String(bitmap);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_article);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        spinnerArticleCategory = findViewById(R.id.spinner_article_category);
        editArticleTitle = findViewById(R.id.edit_article_title);
        editArticleAbstract = findViewById(R.id.edit_article_abstract);
        editArticleBody = findViewById(R.id.edit_article_body);
        imagePreview = findViewById(R.id.image_preview);
        loadingSpinner = findViewById(R.id.loading_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.article_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerArticleCategory.setAdapter(adapter);

        Button btnAddImage = findViewById(R.id.btn_add_image);
        btnAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        Button btnSaveArticle = findViewById(R.id.btn_save_article);
        btnSaveArticle.setOnClickListener(v -> saveArticle());
    }

    private void saveArticle() {
        String category = spinnerArticleCategory.getSelectedItem().toString();
        String title = editArticleTitle.getText().toString().trim();
        String abstractText = editArticleAbstract.getText().toString().trim();
        String body = editArticleBody.getText().toString().trim();

        List<String> missingFields = new ArrayList<>();
        if (title.isEmpty()) {
            missingFields.add("Title");
        }
        if (abstractText.isEmpty()) {
            missingFields.add("Abstract");
        }
        if (body.isEmpty()) {
            missingFields.add("Body");
        }
        if (imageBase64 == null || imageBase64.isEmpty()) {
            missingFields.add("Image");
        }

        if (!missingFields.isEmpty()) {
            String missingFieldsMsg = "Please fill in all mandatory fields: " + String.join(", ", missingFields);
            Toast.makeText(this, missingFieldsMsg, Toast.LENGTH_LONG).show();
            return;
        }

        runOnUiThread(() -> loadingSpinner.setVisibility(View.VISIBLE));

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
                if (imageBase64 != null) {
                    article.addImage(imageBase64, "");
                }
                article.save();

                runOnUiThread(() -> {
                    loadingSpinner.setVisibility(View.GONE);
                    Toast.makeText(AddArticleActivity.this, "Article saved successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (ServerCommunicationError | AuthenticationError e) {
                runOnUiThread(() -> {
                    loadingSpinner.setVisibility(View.GONE);
                    Toast.makeText(AddArticleActivity.this, "Failed to save article: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
