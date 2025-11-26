package com.example.datecourserecommendapplication.RecycerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.R;

public class PostAdapter extends ListAdapter<Post, PostAdapter.PostViewHolder> {

    //DiffUtil setup
    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new PostDiffCallback();
    public PostAdapter(OnItemActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    //OnItemActionListener setup -> 구현은 mainActivity
    public interface OnItemActionListener {
        void onItemClicked(Post post, View anchorView, PostActionType actionType);
    }
    //우선 item 클릭 시 intent, 좋아요,리트윗,댓글 기능은 클릭 이벤트 따로 진행
    private final OnItemActionListener listener;
    public enum PostActionType {
        CLICK, LIKE, COMMENT, RETWEET
    }


    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        Log.d("onCreateViewHolder", "success");
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = getItem(position);
        Log.d("onCreateViewHolder", "success");
        holder.bind(post, listener);
    }

    class PostViewHolder extends RecyclerView.ViewHolder {//ViewHolder setup.
        TextView title, contentPreview, meta, likes, retweet, comments;
        ImageView post_thumbnail;

        //viewHolder setup
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.post_title);
            contentPreview = itemView.findViewById(R.id.post_content_preview);
            meta = itemView.findViewById(R.id.post_meta);
            likes = itemView.findViewById(R.id.post_likes);
            retweet = itemView.findViewById(R.id.post_retweet);
            comments = itemView.findViewById(R.id.post_comments);
            post_thumbnail = itemView.findViewById(R.id.post_thumbnail);
        }
        void bind(final Post post, final OnItemActionListener listener) {
            title.setText(post.getTitle());
            contentPreview.setText(post.getPreviewText());
            meta.setText(post.getCreatedBy() + " · " + post.getCreatedAt());
            likes.setText(String.valueOf(post.getLikesCount()));
            retweet.setText(String.valueOf(post.getRetweetCount()));
            comments.setText(String.valueOf(post.getCommentsCount()));

            //img setup
            if (post.getThumbnail() != null) {
                Glide.with(itemView.getContext())
                        .load(post.getThumbnail())
                        .into(post_thumbnail);
            } else {
                post_thumbnail.setImageDrawable(null);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClicked(post, itemView, PostActionType.CLICK);
            });

            itemView.findViewById(R.id.post_likes_btn).setOnClickListener(v->{
                if (listener != null) listener.onItemClicked(post, itemView, PostActionType.LIKE);
            });

            itemView.findViewById(R.id.post_retweet_btn).setOnClickListener(v->{
                if (listener != null) listener.onItemClicked(post, itemView, PostActionType.RETWEET);
            });

            itemView.findViewById(R.id.post_comments_btn).setOnClickListener(v->{
                if (listener != null) listener.onItemClicked(post, itemView, PostActionType.COMMENT);
            });
        }
    }
}
