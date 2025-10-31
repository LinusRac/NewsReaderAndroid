package com.example.newsreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.newsreader.exceptions.ServerCommunicationError;

import java.io.IOException;
import java.io.InputStream;

public class ArticleDetailActivity extends AppCompatActivity {

    private ImageView articleImage;
    private Article article;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            articleImage.setImageBitmap(bitmap);

                            Image image = article.getImage();
                            String b64Image = Utils.imgToBase64String(bitmap);
                            if (image == null) {
                                article.addImage(b64Image, "");
                            } else {
                                image.setData(b64Image);
                                article.setImage(image);
                            }

                            uploadImageToServer();
                        } catch (IOException | ServerCommunicationError e) {
                            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        articleImage = findViewById(R.id.detail_article_image);
        TextView articleTitle = findViewById(R.id.detail_article_title);
        TextView articleCategory = findViewById(R.id.detail_article_category);
        TextView articleIdUser = findViewById(R.id.detail_article_id_user);
        TextView articleLastModified = findViewById(R.id.detail_article_last_modified);
        TextView articleAbstract = findViewById(R.id.detail_article_abstract);
        TextView articleBody = findViewById(R.id.detail_article_body);
        TextView articleFooter = findViewById(R.id.detail_article_footer);
        Button btnChangeImage = findViewById(R.id.btn_change_image);

        btnChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int articleIndex = extras.getInt("articleIndex", -1);

            if (articleIndex != -1) {
                article = MainActivity.allArticles.get(articleIndex);

                articleTitle.setText(article.getTitleText());
                articleCategory.setText(article.getCategory());
                articleIdUser.setText("User ID: " + article.getIdUser());
                articleLastModified.setText("Last Modified: " + article.getLastModified());
                articleAbstract.setText(Html.fromHtml(article.getAbstractText(), Html.FROM_HTML_MODE_COMPACT));
                articleBody.setText(Html.fromHtml(article.getBodyText(), Html.FROM_HTML_MODE_COMPACT));
                articleFooter.setText(article.getFooterText());

                try {
                    Image image = article.getImage();
                    if (image != null && image.getImage() != null && !image.getImage().isEmpty()) {
                        byte[] decodedString = Base64.decode(image.getImage(), Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        articleImage.setImageBitmap(decodedByte);
                    } else {
                        articleImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_document_placeholder));
                    }
                } catch (ServerCommunicationError e) {
                    articleImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_document_placeholder));
                }
            }
        }
    }

    private void uploadImageToServer() {
        new Thread(() -> {
            try {
                this.article.save();
                runOnUiThread(() -> Toast.makeText(ArticleDetailActivity.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(ArticleDetailActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
