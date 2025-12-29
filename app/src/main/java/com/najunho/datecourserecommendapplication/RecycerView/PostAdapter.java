package com.najunho.datecourserecommendapplication.RecycerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.najunho.datecourserecommendapplication.DB.Post;
import com.najunho.datecourserecommendapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

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
        ImageButton post_likes_btn, post_comments_btn, post_retweet_btn;
        TextView title, likes, retweet, comments;
        ImageView post_thumbnail;
        boolean isLiked = false;
        private final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


        //viewHolder setup
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.post_title);
            likes = itemView.findViewById(R.id.post_likes);
            retweet = itemView.findViewById(R.id.post_retweet);
            comments = itemView.findViewById(R.id.post_comments);
            post_thumbnail = itemView.findViewById(R.id.post_image);

            post_likes_btn = itemView.findViewById(R.id.post_likes_btn);
            post_retweet_btn = itemView.findViewById(R.id.post_retweet_btn);
            post_comments_btn = itemView.findViewById(R.id.post_comments_btn);
        }
        void bind(final Post post, final OnItemActionListener listener) {
            title.setText(post.getTitle());
            retweet.setText(String.valueOf(post.getRetweetCount()));
            comments.setText(String.valueOf(post.getCommentsCount()));
            likes.setText(String.valueOf(post.getLikesCount()));

            List<String> likesBy = post.getLikesBy();
            if(likesBy == null){likesBy = new ArrayList<>();}
            if(likesBy.contains(user.getUid())){
                post_likes_btn.setImageResource(R.drawable.like_full_32px_v2);
                isLiked = true;
            }else {
                post_likes_btn.setImageResource(R.drawable.like_32px);
                isLiked = false;
            }

            //img setup
            if (post.getThumbnail() != null) {
                Glide.with(itemView.getContext())
                        .load(post.getThumbnail())
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .into(post_thumbnail);
            } else {
                post_thumbnail.setImageDrawable(null);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClicked(post, itemView, PostActionType.CLICK);
            });

            post_likes_btn.setOnClickListener(v->{
                if (listener != null) listener.onItemClicked(post, itemView, PostActionType.LIKE);
            });

            post_retweet_btn.setOnClickListener(v->{
                if (listener != null) listener.onItemClicked(post, itemView, PostActionType.RETWEET);
            });

            post_comments_btn.setOnClickListener(v->{
                if (listener != null) listener.onItemClicked(post, itemView, PostActionType.COMMENT);
            });
        }
    }
}
