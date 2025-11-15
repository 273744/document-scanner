package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.database.Document;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DocumentGalleryAdapter - RecyclerView adapter for document gallery
 * Displays document thumbnails in a grid layout
 */
public class DocumentGalleryAdapter extends RecyclerView.Adapter<DocumentGalleryAdapter.DocumentViewHolder> {

    private Context context;
    private List<Document> documents;
    private OnDocumentActionListener listener;

    /**
     * Interface for document action callbacks
     */
    public interface OnDocumentActionListener {
        void onDocumentClick(Document document);
        void onDocumentLongClick(Document document);
    }

    public DocumentGalleryAdapter(Context context, List<Document> documents, OnDocumentActionListener listener) {
        this.context = context;
        this.documents = documents;
        this.listener = listener;
    }

    /**
     * Update documents list
     */
    public void setDocuments(List<Document> documents) {
        this.documents = documents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_document_gallery, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document document = documents.get(position);

        // Set document name
        holder.tvDocumentName.setText(document.getName());

        // Set date
        String dateStr = formatDate(document.getCreatedDate());
        holder.tvDate.setText(dateStr);

        // Set file size
        String sizeStr = formatFileSize(document.getFileSize());
        holder.tvFileSize.setText(sizeStr);

        // Set page count
        if (document.getPageCount() > 1) {
            holder.tvPageCount.setVisibility(View.VISIBLE);
            holder.tvPageCount.setText(document.getPageCount() + " pages");
        } else {
            holder.tvPageCount.setVisibility(View.GONE);
        }

        // Show favorite indicator
        holder.ivFavorite.setVisibility(document.isFavorite() ? View.VISIBLE : View.GONE);

        // Load thumbnail
        loadThumbnail(holder.ivThumbnail, document);

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDocumentClick(document);
            }
        });

        // Long click listener
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDocumentLongClick(document);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return documents != null ? documents.size() : 0;
    }

    /**
     * Load thumbnail image
     */
    private void loadThumbnail(ImageView imageView, Document document) {
        new Thread(() -> {
            try {
                String imagePath = document.getThumbnailPath() != null ?
                    document.getThumbnailPath() : document.getFilePath();

                if (imagePath != null && new File(imagePath).exists()) {
                    // Decode with reduced size for thumbnail
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(imagePath, options);

                    // Calculate sample size
                    int maxSize = 300;
                    options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
                    options.inJustDecodeBounds = false;

                    // Decode thumbnail
                    Bitmap thumbnail = BitmapFactory.decodeFile(imagePath, options);

                    if (thumbnail != null) {
                        imageView.post(() -> imageView.setImageBitmap(thumbnail));
                    } else {
                        imageView.post(() -> imageView.setImageResource(android.R.drawable.ic_menu_gallery));
                    }
                } else {
                    imageView.post(() -> imageView.setImageResource(android.R.drawable.ic_menu_gallery));
                }
            } catch (Exception e) {
                e.printStackTrace();
                imageView.post(() -> imageView.setImageResource(android.R.drawable.ic_menu_gallery));
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
     * Format date for display
     */
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        return sdf.format(new Date(timestamp));
    }

    /**
     * Format file size for display
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        } else {
            return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * ViewHolder class
     */
    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivThumbnail;
        ImageView ivFavorite;
        TextView tvDocumentName;
        TextView tvDate;
        TextView tvFileSize;
        TextView tvPageCount;

        DocumentViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvDocumentName = itemView.findViewById(R.id.tvDocumentName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvPageCount = itemView.findViewById(R.id.tvPageCount);
        }
    }
}

