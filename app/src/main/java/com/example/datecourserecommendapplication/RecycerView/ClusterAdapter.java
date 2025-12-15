package com.example.datecourserecommendapplication.RecycerView;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.R;

import java.util.List;

public class ClusterAdapter extends RecyclerView.Adapter<ClusterAdapter.ClusterViewHolder>{

    public interface OnClickListener{
        void onClick(Post post);
    }
    private final OnClickListener listener;

    private List<Post> itemList;
    private Context context;

    public ClusterAdapter(Context context, List<Post> itemList, OnClickListener listener) {
        this.context = context;
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClusterAdapter.ClusterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_cluster, parent, false);
        return new ClusterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClusterAdapter.ClusterViewHolder holder, int position) {
        Post item = itemList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public class ClusterViewHolder extends RecyclerView.ViewHolder {

        private ImageView preview_thumb;
        private TextView title, preview_likes, preview_comments, preview_retweets;

        public ClusterViewHolder(@NonNull View itemView) {
            super(itemView);

            preview_thumb = itemView.findViewById(R.id.preview_thumb);
            title = itemView.findViewById(R.id.title);
            preview_likes = itemView.findViewById(R.id.preview_likes);
            preview_comments = itemView.findViewById(R.id.preview_comments);
            preview_retweets = itemView.findViewById(R.id.preview_retweets);
        }

        public void bind(Post item){
            title.setText(item.getTitle());
            preview_likes.setText("♥" + item.getLikesCount());
            preview_comments.setText("💬 " + item.getCommentsCount());
            preview_retweets.setText("🔁 " + item.getRetweetCount());

            Glide.with(context)
                    .load(item.getThumbnail())
                    .into(preview_thumb);

            itemView.setOnClickListener(v->{
                listener.onClick(item);
            });
        }
    }
}
