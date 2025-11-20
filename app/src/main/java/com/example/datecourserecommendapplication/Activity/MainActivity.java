package com.example.datecourserecommendapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.DB.PostRepo;
import com.example.datecourserecommendapplication.DB.User;
import com.example.datecourserecommendapplication.DB.UserRepo;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.PostAdapter;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.example.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private Button btnUserInfo, btnMainFeed;
    private FloatingActionButton btnAddPost;
    private UserRepo userRepo;
    private PostRepo postRepo;
    private MainViewModel viewModel;
    private PostAdapter adapter;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();

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
                        Toast.makeText(MainActivity.this, "이미 리트윗 된 게시물입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(MainActivity.this, RetweetActivity.class);
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

        //viewModel setup
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getPosts().observe(this,posts -> {
            Log.d("test", "test");
            adapter.submitList(new ArrayList<>(posts)); //새 객체로 감싸야 Diff가 감지 가능.
        });

        //Repo setup
        postRepo = ApplicationUtil.getPostRepo();
        postRepo.getPost(); //창 열릴때 item 생성용
        userRepo = ApplicationUtil.getUserRepo();
        userRepo.getUser(new UserRepo.OnUserGetListener() {
            @Override
            public void onSuccess(User user) {
                String nickName = user.getNickname();
                Toast.makeText(MainActivity.this, nickName + "님 환영합니다", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this,  "error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });


        //MainFeedBtn setup
        btnMainFeed = findViewById(R.id.btnMainFeed);
        btnMainFeed.setOnClickListener(v-> {
            Toast.makeText(MainActivity.this, "현재 페이지입니다", Toast.LENGTH_SHORT).show();
        });

        //UserInfoBtn setup
        btnUserInfo = findViewById(R.id.btnUserInfo);
        btnUserInfo.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserSetUpActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        //AddPostBtn setup
        btnAddPost = findViewById(R.id.btnAddPost);
        btnAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(this, WritePostActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void openPost(Post post) {
        Intent intent = new Intent(MainActivity.this, OpenPostActivity.class);
        intent.putExtra("postId", post.getId());
        startActivity(intent);
        finish();
    }
}
