package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * FilterAdapter - RecyclerView adapter for filter selection
 * Displays filter thumbnails in a horizontal list
 */
public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterViewHolder> {

    private Context context;
    private List<PreviewActivity.FilterType> filters;
    private Bitmap originalBitmap;
    private OnFilterSelectedListener listener;
    private PreviewActivity.FilterType selectedFilter = PreviewActivity.FilterType.ORIGINAL;

    /**
     * Interface for filter selection callback
     */
    public interface OnFilterSelectedListener {
        void onFilterSelected(PreviewActivity.FilterType filterType);
    }

    public FilterAdapter(Context context,
                        List<PreviewActivity.FilterType> filters,
                        Bitmap originalBitmap,
                        OnFilterSelectedListener listener) {
        this.context = context;
        this.filters = filters;
        this.originalBitmap = originalBitmap;
        this.listener = listener;
    }

    public void setOriginalBitmap(Bitmap bitmap) {
        this.originalBitmap = bitmap;
        notifyDataSetChanged();
    }

    public void setSelectedFilter(PreviewActivity.FilterType filterType) {
        this.selectedFilter = filterType;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_filter, parent, false);
        return new FilterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
        PreviewActivity.FilterType filter = filters.get(position);

        // Set filter name
        holder.tvFilterName.setText(filter.getDisplayName());

        // Highlight selected filter
        boolean isSelected = filter == selectedFilter;
        holder.cardView.setCardBackgroundColor(
            isSelected ? context.getColor(R.color.purple_500) : Color.WHITE
        );
        holder.tvFilterName.setTextColor(
            isSelected ? Color.WHITE : Color.BLACK
        );

        // Generate thumbnail with filter preview
        if (originalBitmap != null) {
            generateFilterThumbnail(holder.ivFilterPreview, filter);
        } else {
            // Show placeholder
            holder.ivFilterPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            selectedFilter = filter;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onFilterSelected(filter);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filters.size();
    }

    /**
     * Generate filter thumbnail preview
     */
    private void generateFilterThumbnail(ImageView imageView, PreviewActivity.FilterType filter) {
        // Create small thumbnail for preview
        int thumbSize = 100;
        Bitmap thumb = Bitmap.createScaledBitmap(
            originalBitmap,
            thumbSize,
            thumbSize,
            true
        );

        // Apply filter preview (simplified version)
        Bitmap filtered = applyQuickFilter(thumb, filter);
        imageView.setImageBitmap(filtered);
    }

    /**
     * Apply quick filter for thumbnail preview
     */
    private Bitmap applyQuickFilter(Bitmap bitmap, PreviewActivity.FilterType filter) {
        switch (filter) {
            case GRAYSCALE:
                return convertToGrayscale(bitmap);

            case BLACK_AND_WHITE:
                Bitmap gray = convertToGrayscale(bitmap);
                return adjustBrightnessContrast(gray, 0, 80);

            case AUTO_ENHANCE:
                return adjustBrightnessContrast(bitmap, 0, 30);

            case BRIGHTNESS:
                return adjustBrightnessContrast(bitmap, 30, 0);

            case SHARPEN:
                return adjustBrightnessContrast(bitmap, 10, 20);

            case ORIGINAL:
            default:
                return bitmap.copy(bitmap.getConfig(), true);
        }
    }

    /**
     * Simple grayscale conversion
     */
    private Bitmap convertToGrayscale(Bitmap bitmap) {
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        cm.setSaturation(0);

        Bitmap ret = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(ret);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return ret;
    }

    /**
     * Simple brightness/contrast adjustment
     */
    private Bitmap adjustBrightnessContrast(Bitmap bitmap, int brightness, int contrast) {
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();

        float scale = (contrast + 100f) / 100f;
        float translate = (-.5f * scale + .5f) * 255.f + brightness;

        cm.set(new float[] {
            scale, 0, 0, 0, translate,
            0, scale, 0, 0, translate,
            0, 0, scale, 0, translate,
            0, 0, 0, 1, 0
        });

        Bitmap ret = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(ret);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return ret;
    }

    /**
     * ViewHolder class
     */
    static class FilterViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivFilterPreview;
        TextView tvFilterName;

        FilterViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivFilterPreview = itemView.findViewById(R.id.ivFilterPreview);
            tvFilterName = itemView.findViewById(R.id.tvFilterName);
        }
    }
}

