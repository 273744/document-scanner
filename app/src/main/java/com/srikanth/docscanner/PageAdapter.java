package com.srikanth.docscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.List;

/**
 * PageAdapter - RecyclerView adapter for multi-page document view
 * Displays thumbnail images with page numbers in a grid layout
 */
public class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageViewHolder> {

    private Context context;
    private List<String> imagePaths;
    private OnPageActionListener listener;

    /**
     * Interface for page action callbacks
     */
    public interface OnPageActionListener {
        void onPageClicked(int position);
        void onPageLongClicked(RecyclerView.ViewHolder viewHolder);
    }

    public PageAdapter(Context context, List<String> imagePaths, OnPageActionListener listener) {
        this.context = context;
        this.imagePaths = imagePaths;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        String imagePath = imagePaths.get(position);

        // Set page number
        holder.tvPageNumber.setText("Page " + (position + 1));

        // Load thumbnail asynchronously
        loadThumbnail(holder.ivThumbnail, imagePath);

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPageClicked(position);
            }
        });

        // Long click listener for drag
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onPageLongClicked(holder);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    /**
     * Load thumbnail image asynchronously
     */
    private void loadThumbnail(ImageView imageView, String imagePath) {
        new Thread(() -> {
            try {
                // Decode with reduced size for thumbnail
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imagePath, options);

                // Calculate sample size for thumbnail
                int maxSize = 300;
                options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
                options.inJustDecodeBounds = false;

                // Decode thumbnail
                Bitmap thumbnail = BitmapFactory.decodeFile(imagePath, options);

                if (thumbnail != null) {
                    // Update UI on main thread
                    imageView.post(() -> imageView.setImageBitmap(thumbnail));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Calculate sample size for efficient bitmap loading
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * ViewHolder class
     */
    static class PageViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivThumbnail;
        TextView tvPageNumber;

        PageViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvPageNumber = itemView.findViewById(R.id.tvPageNumber);
        }
    }
}


