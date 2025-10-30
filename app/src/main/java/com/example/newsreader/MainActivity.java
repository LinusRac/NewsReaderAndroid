package com.example.newsreader;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.newsreader.exceptions.AuthenticationError;
import com.example.newsreader.exceptions.ServerCommunicationError;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private ArticleAdapter adapter;
    private List<Article> allArticles = new ArrayList<>();
    private ImageButton btnNational, btnEconomy, btnSports, btnTechnology, btnInternational, btnAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ListView articleList = findViewById(R.id.article_list);
        adapter = new ArticleAdapter(this, new ArrayList<>());
        articleList.setAdapter(adapter);

        articleList.setOnItemClickListener((parent, view, position, id) -> {
            Article article = adapter.getItem(position);
            if (article != null) {
                Intent intent = new Intent(this, ArticleDetailActivity.class);
                intent.putExtra("title", article.getTitleText());
                intent.putExtra("category", article.getCategory());
                intent.putExtra("abstract", article.getAbstractText());
                intent.putExtra("body", article.getBodyText());
                try {
                    Image image = article.getImage();
                    if (image != null) {
                        intent.putExtra("image", image.getImage());
                    }
                } catch (ServerCommunicationError e) {
                    // Don't send image if there's an error
                }
                startActivity(intent);
            }
        });

        // Navbar button listeners
        btnNational = findViewById(R.id.btn_national);
        btnEconomy = findViewById(R.id.btn_economy);
        btnSports = findViewById(R.id.btn_sports);
        btnTechnology = findViewById(R.id.btn_technology);
        btnInternational = findViewById(R.id.btn_international);
        btnAll = findViewById(R.id.btn_all);

        btnNational.setOnClickListener(v -> filterArticles("National"));
        btnEconomy.setOnClickListener(v -> filterArticles("Economy"));
        btnSports.setOnClickListener(v -> filterArticles("Sports"));
        btnTechnology.setOnClickListener(v -> filterArticles("Technology"));
        btnInternational.setOnClickListener(v -> filterArticles("International"));
        btnAll.setOnClickListener(v -> filterArticles(null)); // null category shows all articles

        downloadArticlesAsync();
    }

    private void filterArticles(String category) {
        List<Article> filteredArticles;
        if (category == null) {
            filteredArticles = new ArrayList<>(allArticles);
        } else {
            filteredArticles = allArticles.stream()
                    .filter(article -> category.equalsIgnoreCase(article.getCategory()))
                    .collect(Collectors.toList());
        }

        adapter.clear();
        adapter.addAll(filteredArticles);
        adapter.notifyDataSetChanged();
        Toast.makeText(this, (category == null ? "Showing all" : "Showing " + category) + " news", Toast.LENGTH_SHORT).show();
        updateButtonHighlights(category);
    }

    private void updateButtonHighlights(String activeCategory) {
        btnNational.setBackgroundColor(Color.TRANSPARENT);
        btnEconomy.setBackgroundColor(Color.TRANSPARENT);
        btnSports.setBackgroundColor(Color.TRANSPARENT);
        btnTechnology.setBackgroundColor(Color.TRANSPARENT);
        btnInternational.setBackgroundColor(Color.TRANSPARENT);
        btnAll.setBackgroundColor(Color.TRANSPARENT);

        if (activeCategory == null) {
            btnAll.setBackgroundColor(Color.LTGRAY);
        } else {
            switch (activeCategory) {
                case "National":
                    btnNational.setBackgroundColor(Color.LTGRAY);
                    break;
                case "Economy":
                    btnEconomy.setBackgroundColor(Color.LTGRAY);
                    break;
                case "Sports":
                    btnSports.setBackgroundColor(Color.LTGRAY);
                    break;
                case "Technology":
                    btnTechnology.setBackgroundColor(Color.LTGRAY);
                    break;
                case "International":
                    btnInternational.setBackgroundColor(Color.LTGRAY);
                    break;
            }
        }
    }

    private void downloadArticlesAsync() {
        new Thread(() -> {
            try {
                final Properties properties = new Properties();
                properties.setProperty("service_url", "https://sanger.dia.fi.upm.es/pmd-task/");
                final ModelManager modelManager = new ModelManager(properties);
                allArticles = modelManager.getArticles(1024, 0);

                runOnUiThread(() -> {
                    if (allArticles != null) {
                        filterArticles(null); // Initially show all articles
                    }
                });
            } catch (AuthenticationError | ServerCommunicationError e) {
                // Handle error, for example by showing a Toast
                 runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error downloading articles: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
