package com.example.newsreader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.newsreader.exceptions.ServerCommunicationError;

public class ArticleDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        Button btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        ImageView articleImage = findViewById(R.id.detail_article_image);
        TextView articleTitle = findViewById(R.id.detail_article_title);
        TextView articleCategory = findViewById(R.id.detail_article_category);
        TextView articleAbstract = findViewById(R.id.detail_article_abstract);
        TextView articleBody = findViewById(R.id.detail_article_body);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            articleTitle.setText(extras.getString("title"));
            articleCategory.setText(extras.getString("category"));
            articleAbstract.setText(Html.fromHtml(extras.getString("abstract"), Html.FROM_HTML_MODE_COMPACT));
            articleBody.setText(Html.fromHtml(extras.getString("body"), Html.FROM_HTML_MODE_COMPACT));

            String imageString = extras.getString("image");
            if (imageString != null && !imageString.isEmpty()) {
                byte[] decodedString = Base64.decode(imageString, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                articleImage.setImageBitmap(decodedByte);
            } else {
                articleImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.baseline_document_scanner_24));
            }
        }
    }
}