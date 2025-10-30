package com.example.newsreader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        ImageView articleImage = convertView.findViewById(R.id.article_image);

        if (article != null) {
            articleTitle.setText(article.getTitleText());
            try {
                Image image = article.getImage();
                if (image != null) {
                    byte[] decodedString = Base64.decode(image.getImage(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    articleImage.setImageBitmap(decodedByte);
                }
            } catch (ServerCommunicationError e) {
                // Handle error, maybe set a default image
            }
        }

        return convertView;
    }
}