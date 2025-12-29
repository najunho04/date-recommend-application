package com.najunho.datecourserecommendapplication.Util;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.najunho.datecourserecommendapplication.Activity.ForPost.OpenPostActivity;
import com.najunho.datecourserecommendapplication.CloudFunction.CloudFunctionManager;
import com.najunho.datecourserecommendapplication.DB.Post;
import com.najunho.datecourserecommendapplication.DB.User;
import com.najunho.datecourserecommendapplication.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PostPreviewBottomSheet extends BottomSheetDialogFragment {
    private Post post; // 너가 사용하는 Post 모델
    private User user; // 너가 사용하는 User 모델
    private String postId;
    private CloudFunctionManager cloudFunctionManager = new CloudFunctionManager();

    public PostPreviewBottomSheet(Post post, User user) {
        this.post = post;
        this.user = user;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        postId = post.getId();


        View view = inflater.inflate(R.layout.bottomsheet_post_preview, container, false);

        ImageView thumb = view.findViewById(R.id.preview_thumb);
        TextView title = view.findViewById(R.id.preview_title);
        TextView likes = view.findViewById(R.id.preview_likes);
        TextView comments = view.findViewById(R.id.preview_comments);
        TextView retweets = view.findViewById(R.id.preview_retweets);
        TextView tip = view.findViewById(R.id.preview_tip);
        TextView myPoint = view.findViewById(R.id.my_point);

        Button btnYes = view.findViewById(R.id.btn_yes);
        Button btnNo = view.findViewById(R.id.btn_no);

        // UI 바인딩
        title.setText(post.getTitle());
        likes.setText("♥ " + post.getLikesCount());
        comments.setText("💬 " + post.getCommentsCount());
        retweets.setText("🔁 " + post.getRetweetCount());

        tip.setText("게시물 확인 시 50p 차감됩니다");
        myPoint.setText("현재 남은 포인트: " + user.getMyPoint() + "p");

        Glide.with(this)
                .load(post.getThumbnail())
                .into(thumb);

        btnYes.setOnClickListener(v -> {
            // 포인트 차감 로직 → 상세 화면 이동
            purchaseAndOpenPost(postId, post);
        });

        btnNo.setOnClickListener(v -> dismiss());

        return view;
    }

    private void openPostDetail(String postId) {
        // 상세 액티비티 이동
        Intent intent = new Intent(getContext(), OpenPostActivity.class);
        intent.putExtra("postId", postId);
        startActivity(intent);
    }

    public void purchaseAndOpenPost(String postId, Post post){
        cloudFunctionManager.callPurchasePost(postId, new CloudFunctionManager.PointCallback() {

            @Override
            public void onSuccess(boolean success, String result) {
                openPostDetail(postId);
            }

            @Override
            public void onError(Exception e) {
                Log.d("purchaseAndOpenPost", e.getMessage());
            }
        });
    }
}
