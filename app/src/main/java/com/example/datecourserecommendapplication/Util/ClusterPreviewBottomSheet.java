package com.example.datecourserecommendapplication.Util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.Activity.ForPost.OpenPostActivity;
import com.example.datecourserecommendapplication.Activity.ViewPostInMapActivity;
import com.example.datecourserecommendapplication.CloudFunction.CloudFunctionManager;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.DB.User;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.ClusterAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.Objects;

public class ClusterPreviewBottomSheet extends BottomSheetDialogFragment {
    private User user;
    private List<Post> clusterList;
    private ClusterAdapter clusterAdapter;
    private CloudFunctionManager cloudFunctionManager;

    public ClusterPreviewBottomSheet(User user, List<Post> clusterList) {
        this.user = user;
        this.clusterList = clusterList;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottomsheet_cluster_preview, container, false);
        ImageButton closeBtn = view.findViewById(R.id.closeBtn);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);

        cloudFunctionManager = new CloudFunctionManager();

        clusterAdapter = new ClusterAdapter(getContext(), clusterList, new ClusterAdapter.OnClickListener() {
            @Override
            public void onClick(Post post) {
                //클릭 로직
                checkPurchase(post);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(clusterAdapter);

        clusterAdapter.notifyDataSetChanged();

        closeBtn.setOnClickListener(v-> {
            dismiss();
        });

        return view;
    }

    public void checkPurchase(Post post){
        Log.d("checkPurchase", "postId : " + post.getId());
        //구매 여부 확인 로직
        cloudFunctionManager.callCheckPurchase(post.getId(), new CloudFunctionManager.PointCallback() {
            @Override
            public void onSuccess(boolean success, String result) {
                if(Objects.equals(result, "noPurchasedPost")){
                    //구매해야함 -> PostPreviewBottomSheet로 이동.

                    Log.d("checkPurchase", "open PostPreviewBottomSheet");

                    PostPreviewBottomSheet sheet = new PostPreviewBottomSheet(post, user);
                    sheet.show(getParentFragmentManager(), "post_preview");

                }else if (Objects.equals(result, "purchasedPost") || Objects.equals(result, "myPost")) {
                    //구매했거나 내 게시물 -> openPost
                    Activity activity = getActivity();
                    if (activity == null || activity.isFinishing()) return;

                    Intent intent = new Intent(activity, OpenPostActivity.class);
                    intent.putExtra("postId", post.getId());
                    activity.startActivity(intent);
                    dismiss();
                }else {
                    Log.d("checkPurchase", "error. result is not expected");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.d("checkPurchase", "error: " + e.getMessage());
            }
        });
    }
}
