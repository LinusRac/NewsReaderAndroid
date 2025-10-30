package com.example.newsreader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.newsreader.exceptions.ServerCommunicationError;

import java.util.List;

public class ArticleAdapter extends ArrayAdapter<Article> {

    public ArticleAdapter(@NonNull Context context, @NonNull List<Article> articles) {
        super(context, 0, articles);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_article, parent, false);
        }

        Article article = getItem(position);

        TextView articleTitle = convertView.findViewById(R.id.article_title);
        TextView articleCategory = convertView.findViewById(R.id.article_category);
        TextView articleAbstract = convertView.findViewById(R.id.article_abstract);
        ImageView articleImage = convertView.findViewById(R.id.article_image);

        if (article != null) {
            articleTitle.setText(article.getTitleText());
            articleCategory.setText(article.getCategory());
            articleAbstract.setText(Html.fromHtml(article.getAbstractText(), Html.FROM_HTML_MODE_COMPACT));

            try {
                Image image = article.getImage();
                if (image != null && image.getImage() != null && !image.getImage().isEmpty()) {
                    byte[] decodedString = Base64.decode(image.getImage(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    articleImage.setImageBitmap(decodedByte);
                } else {
                    articleImage.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_launcher_background));
                }
            } catch (ServerCommunicationError e) {
                articleImage.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_launcher_background));
            }
        }

        return convertView;
    }
}