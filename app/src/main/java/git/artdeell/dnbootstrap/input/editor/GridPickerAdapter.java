// /app/src/main/java/git/artdeell/dnbootstrap/input/editor/GridPickerAdapter.java
package git.artdeell.dnbootstrap.input.editor;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import git.artdeell.dnbootstrap.R;

public class GridPickerAdapter extends RecyclerView.Adapter<GridPickerAdapter.ViewHolder> {
    private final List<GridItem> items;
    private final Context context;
    private Object currentSelection;
    private final OnItemSelectedListener listener;
    private AlertDialog dialog;

    public interface OnItemSelectedListener {
        void onSelected(Object value);
    }

    public GridPickerAdapter(Context context, List<GridItem> items, Object currentSelection, OnItemSelectedListener listener) {
        this.context = context;
        this.items = items;
        this.currentSelection = currentSelection;
        this.listener = listener;
    }

    public void setDialog(AlertDialog dialog) {
        this.dialog = dialog;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout layout = new FrameLayout(context);
        int size = (int) (80 * context.getResources().getDisplayMetrics().density);
        int padding = (int) (4 * context.getResources().getDisplayMetrics().density);

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(size, size);
        layout.setLayoutParams(lp);
        layout.setPadding(padding, padding, padding, padding);

        ImageView content = new ImageView(context);
        content.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        content.setScaleType(ImageView.ScaleType.FIT_CENTER);

        View overlay = new View(context);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundResource(R.drawable.grid_select_material);
        overlay.setVisibility(View.GONE);

        layout.addView(content);
        layout.addView(overlay);

        return new ViewHolder(layout, content, overlay);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GridItem item = items.get(position);
        item.bind(holder.content, context);

        if (item.getValue().equals(currentSelection)) {
            holder.selectionOverlay.setVisibility(View.VISIBLE);
        } else {
            holder.selectionOverlay.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelected(item.getValue());
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView content;
        View selectionOverlay;

        ViewHolder(View itemView, ImageView content, View selectionOverlay) {
            super(itemView);
            this.content = content;
            this.selectionOverlay = selectionOverlay;
        }
    }
}