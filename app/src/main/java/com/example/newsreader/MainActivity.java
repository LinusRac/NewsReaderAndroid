package com.example.newsreader;

import android.os.Bundle;
import android.widget.ListView;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ArticleAdapter adapter;

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
        adapter = new ArticleAdapter(this, new ArrayList<Article>());
        articleList.setAdapter(adapter);

        downloadArticlesAsync();
    }

    private void downloadArticlesAsync() {
        executorService.execute(() -> {
            try {
                Properties properties = new Properties();
                properties.setProperty("service_url", "https://sanger.dia.fi.upm.es/pmd-task/");
                ModelManager modelManager = new ModelManager(properties);
                List<Article> articles = modelManager.getArticles(1024, 0);
                runOnUiThread(() -> {
                    adapter.clear();
                    adapter.addAll(articles);
                    adapter.notifyDataSetChanged();
                });
            } catch (AuthenticationError | ServerCommunicationError e) {
                // Handle error
            }
        });
    }
}