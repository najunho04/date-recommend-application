package com.example.datecourserecommendapplication.RecycerView;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.R;

public class ReadContentAdapter extends ListAdapter<Content, ReadContentAdapter.ReadContentViewHolder> {
    //DiffUtil logic
    private static final DiffUtil.ItemCallback<Content> DIFF_CALLBACK = new DiffUtil.ItemCallback<Content>() {
        @Override
        public boolean areItemsTheSame(@NonNull Content oldItem, @NonNull Content newItem) {
            String oldId = oldItem.getContentId();
            String newId = newItem.getContentId();
            return oldId != null && oldId.equals(newId);
        }
        @Override
        public boolean areContentsTheSame(@NonNull Content oldItem, @NonNull Content newItem) {
            String oldTitle = oldItem.getTitle() != null ? oldItem.getTitle() : "";
            String newTitle = newItem.getTitle() != null ? newItem.getTitle() : "";
            String oldDesc = oldItem.getDescription() != null ? oldItem.getDescription() : "";
            String newDesc = newItem.getDescription() != null ? newItem.getDescription() : "";
            return oldTitle.equals(newTitle) && oldDesc.equals(newDesc);
        }
    };
    public ReadContentAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ReadContentAdapter.ReadContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_read_content, parent, false);
        Log.d("ReadContentAdapter", "onCreateViewHolder success");
        return new ReadContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReadContentAdapter.ReadContentViewHolder holder, int position) {
        Content item = getItem(position);
        Log.d("ReadContentAdapter", "onCreateViewHolder success");
        holder.bind(item);
    }

    public static class ReadContentViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle, tvStartTime, tvEndTime, tvPlace, tvDescription;
        ImageView imgPreview;

        public ReadContentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvStartTime = itemView.findViewById(R.id.tvStartTime);
            tvEndTime = itemView.findViewById(R.id.tvEndTime);
            tvPlace = itemView.findViewById(R.id.tvPlace);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            imgPreview = itemView.findViewById(R.id.imgPreview);
        }

        public void bind(Content item) {
            tvTitle.setText(item.getTitle());
            tvStartTime.setText(item.getStartTimeString());
            tvEndTime.setText(item.getEndTimeString());
            tvPlace.setText(item.getLocation().getName());
            tvDescription.setText(item.getDescription());

            //img setup
            if (item.getImageUrl() != null) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                Log.e("GLIDE", "Load failed: " + e.getMessage());
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target,
                                                           DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(imgPreview);
            } else {
                imgPreview.setImageDrawable(null);
            }
        }
    }
}
