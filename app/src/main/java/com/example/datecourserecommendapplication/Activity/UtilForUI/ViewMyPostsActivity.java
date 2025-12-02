package com.example.datecourserecommendapplication.Activity.UtilForUI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.Activity.ForPost.OpenPostActivity;
import com.example.datecourserecommendapplication.Activity.ForPost.RetweetActivity;
import com.example.datecourserecommendapplication.Activity.MainActivity;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.DB.UserRepo;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.PostAdapter;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.example.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ViewMyPostsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private PostAdapter adapter;
    private MainViewModel viewModel;
    private UserRepo userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_my_posts);

        mAuth = FirebaseAuth.getInstance();
        userRepo = ApplicationUtil.getUserRepo();
        Button btnBack = findViewById(R.id.back_btn);

        //viewModel setup
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        //adapter setup
        RecyclerView recyclerView = findViewById(R.id.recyclerViewPosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter((post, anchorView, actionType) -> {
            switch (actionType) {
                case LIKE:
                    viewModel.addLike(post.getId(), mAuth.getUid(), task ->{
                        if (task.isSuccessful()) {
                            Log.d("addLike", "좋아요 추가 성공");
                        }
                    });
                    break;
                case RETWEET:
                    if(post.getParentPostId() != null){
                        //자신이 자식 post일 때
                        Toast.makeText(ViewMyPostsActivity.this, "이미 리트윗 된 게시물입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(mAuth.getCurrentUser().toString().equals(post.getCreatedBy())){
                        //자기 게시물 리트윗 시도 시
                        Toast.makeText(ViewMyPostsActivity.this, "본인이 작성한 게시물은 리트윗 불가합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(ViewMyPostsActivity.this, RetweetActivity.class);
                    intent.putExtra("originalPostId", post.getId());
                    startActivity(intent);
                    finish();
                    break;
                case COMMENT:
                    openPost(post);
                case CLICK:
                    openPost(post);
            }
        });
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v->{
            finish();
        });

    }

    @Override
    protected void onStart(){
        super.onStart();
        //Post Ui setup
        userRepo.getMyPostsId(mAuth.getCurrentUser().getUid(), new UserRepo.OnGetMyPostsListener() {
            @Override
            public void onSuccess(List<String> postsId) {
                Log.d("getMyPostsId", "getMyPostsId success");
                viewModel.getPostsInUser(postsId, new MainViewModel.OnPostsListener() {
                    @Override
                    public void onSuccess(List<Post> posts) {
                        Log.d("getMyPostsId", "getPostsInUser success: "+ posts.size());
                        adapter.submitList(new ArrayList<>(posts));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.d("getMyPostsId", "getPostsInUser failed: " + errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.d("getMyPostsId", "getMyPostsId failed: " + errorMessage);
            }
        });
    }

    private void openPost(Post post) {
        Intent intent = new Intent(ViewMyPostsActivity.this, OpenPostActivity.class);
        intent.putExtra("postId", post.getId());
        startActivity(intent);
        finish();
    }
}