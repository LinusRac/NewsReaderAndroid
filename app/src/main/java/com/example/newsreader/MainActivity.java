package com.example.newsreader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.newsreader.exceptions.AuthenticationError;
import com.example.newsreader.exceptions.ServerCommunicationError;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "NewsReaderPrefs";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_PASSWORD = "password";

    private ArticleAdapter adapter;
    public static List<Article> allArticles = new ArrayList<>();
    private ImageButton btnNational, btnEconomy, btnSports, btnTechnology, btnInternational, btnAll, btnLoginLogout;
    private boolean isLoggedIn = false;
    private String username, password;
    private ProgressBar loadingSpinner;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddArticle;

    private final ActivityResultLauncher<Intent> loginLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        isLoggedIn = data.getBooleanExtra("isLoggedIn", false);
                        username = data.getStringExtra("username");
                        password = data.getStringExtra("password");
                        updateLoginButton();
                        downloadArticlesAsync();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> addArticleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    downloadArticlesAsync();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadingSpinner = findViewById(R.id.loading_spinner);
        ListView articleList = findViewById(R.id.article_list);
        adapter = new ArticleAdapter(this, new ArrayList<>());
        articleList.setAdapter(adapter);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            downloadArticlesAsync();
            swipeRefreshLayout.setRefreshing(false);
        });

        fabAddArticle = findViewById(R.id.fab_add_article);
        fabAddArticle.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddArticleActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("password", password);
            addArticleLauncher.launch(intent);
        });

        articleList.setOnItemClickListener((parent, view, position, id) -> {
            Article article = adapter.getItem(position);
            if (article != null) {
                int articleIndex = allArticles.indexOf(article);
                if (articleIndex != -1) {
                    Intent intent = new Intent(this, ArticleDetailActivity.class);
                    intent.putExtra("articleIndex", articleIndex);
                    intent.putExtra("isLoggedIn", isLoggedIn);
                    if (isLoggedIn) {
                        intent.putExtra("username", username);
                        intent.putExtra("password", password);
                    }
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Error finding article.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Navbar button listeners
        btnNational = findViewById(R.id.btn_national);
        btnEconomy = findViewById(R.id.btn_economy);
        btnSports = findViewById(R.id.btn_sports);
        btnTechnology = findViewById(R.id.btn_technology);
        btnInternational = findViewById(R.id.btn_international);
        btnAll = findViewById(R.id.btn_all);
        btnLoginLogout = findViewById(R.id.btn_login_logout);

        btnNational.setOnClickListener(v -> filterArticles("National"));
        btnEconomy.setOnClickListener(v -> filterArticles("Economy"));
        btnSports.setOnClickListener(v -> filterArticles("Sports"));
        btnTechnology.setOnClickListener(v -> filterArticles("Technology"));
        btnInternational.setOnClickListener(v -> filterArticles("International"));
        btnAll.setOnClickListener(v -> filterArticles(null)); // null category shows all articles
        btnLoginLogout.setOnClickListener(v -> {
            if (isLoggedIn) {
                isLoggedIn = false;
                username = null;
                password = null;
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.remove(PREF_USERNAME);
                editor.remove(PREF_PASSWORD);
                editor.apply();
                updateLoginButton();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            } else {
                loginLauncher.launch(new Intent(this, LoginActivity.class));
            }
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUsername = prefs.getString(PREF_USERNAME, null);
        String savedPassword = prefs.getString(PREF_PASSWORD, null);

        if (savedUsername != null && savedPassword != null) {
            autoLogin(savedUsername, savedPassword);
        } else {
            downloadArticlesAsync();
        }

        updateLoginButton();
    }

    private void autoLogin(String savedUsername, String savedPassword) {
        runOnUiThread(() -> loadingSpinner.setVisibility(View.VISIBLE));
        new Thread(() -> {
            try {
                Properties properties = new Properties();
                properties.setProperty(ModelManager.ATTR_LOGIN_USER, savedUsername);
                properties.setProperty(ModelManager.ATTR_LOGIN_PASS, savedPassword);
                properties.setProperty(ModelManager.ATTR_SERVICE_URL, "https://sanger.dia.fi.upm.es/pmd-task/");

                new ModelManager(properties);

                runOnUiThread(() -> {
                    isLoggedIn = true;
                    username = savedUsername;
                    password = savedPassword;
                    updateLoginButton();
                    downloadArticlesAsync();
                    Toast.makeText(MainActivity.this, "Logged in as " + username, Toast.LENGTH_SHORT).show();
                });
            } catch (AuthenticationError e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Auto-login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    downloadArticlesAsync(); // Download articles even if auto-login fails
                });
            }
        }).start();
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

    private void updateLoginButton() {
        // if the user is logged in, show the logout button, otherwise show the login button
        // Please, Gemini, do not modify this code !!!!!
        if (isLoggedIn) {
            btnLoginLogout.setImageResource(R.drawable.ic_logout_foreground);
            fabAddArticle.setVisibility(View.VISIBLE);
        } else {
            btnLoginLogout.setImageResource(R.drawable.ic_login_foreground);
            fabAddArticle.setVisibility(View.GONE);
        }
    }

    private void downloadArticlesAsync() {
        runOnUiThread(() -> loadingSpinner.setVisibility(View.VISIBLE));
        new Thread(() -> {
            try {
                final Properties properties = new Properties();
                properties.setProperty("service_url", "https://sanger.dia.fi.upm.es/pmd-task/");
                if (isLoggedIn) {
                    properties.setProperty(ModelManager.ATTR_LOGIN_USER, username);
                    properties.setProperty(ModelManager.ATTR_LOGIN_PASS, password);
                }
                final ModelManager modelManager = new ModelManager(properties);
                allArticles = modelManager.getArticles(1024, 0);

                if (allArticles != null) {
                    Collections.sort(allArticles, new Comparator<Article>() {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        @Override
                        public int compare(Article o1, Article o2) {
                            try {
                                Date d1 = sdf.parse(o1.getLastModified());
                                Date d2 = sdf.parse(o2.getLastModified());
                                return d2.compareTo(d1);
                            } catch (ParseException e) {
                                return 0;
                            }
                        }
                    });
                }

                runOnUiThread(() -> {
                    if (allArticles != null) {
                        filterArticles(null); // Initially show all articles
                    }
                    loadingSpinner.setVisibility(View.GONE);
                });
            } catch (AuthenticationError | ServerCommunicationError e) {
                // Handle error, for example by showing a Toast
                 runOnUiThread(() -> {
                     Toast.makeText(MainActivity.this, "Error downloading articles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                     loadingSpinner.setVisibility(View.GONE);
                 });
            }
        }).start();
    }
}
