package com.example.datecourserecommendapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.DB.Comment;
import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.ContentRepo;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.DB.PostRepo;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.CommentAdapter;
import com.example.datecourserecommendapplication.RecycerView.ReadContentAdapter;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.example.datecourserecommendapplication.ViewModel.CommentViewModel;
import com.example.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OpenPostActivity extends AppCompatActivity {

    private TextView authorName, postDate, postTitle, tvOriginalPost;
    private TextInputEditText commentEditText;
    private Button sendCommentBtn;
    private MaterialToolbar topAppBar;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private MainViewModel viewModel;
    private CommentViewModel commentViewModel;
    private ContentRepo contentRepo;
    private CommentAdapter adapter;
    private ReadContentAdapter readContentAdapter;
    private String postId;
    private Boolean isRetweetedPost;
    private Post originalPost;



    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_post);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        contentRepo = ApplicationUtil.getContentRepo();

        //get postId
        postId = getIntent().getStringExtra("postId");
        if (postId == null) return;

        //adapter setup
        RecyclerView contentRecyclerView = findViewById(R.id.contentRecyclerView);
        contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        readContentAdapter = new ReadContentAdapter();
        contentRecyclerView.setAdapter(readContentAdapter);

        RecyclerView recyclerView = findViewById(R.id.commentRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommentAdapter(new CommentAdapter.OnCommentClickListener(){
            @Override
            public void onProfileClick(Comment comment) {
                Log.d("CommentAdapter", "onProfileClick");
            }
            @Override
            public void onLongClick(Comment comment) {
                Log.d("CommentAdapter", "onLongClick");
            }
            @Override
            public void onEditBtnClick(Comment comment, Editable newCommentContent) {
                if(newCommentContent == null) {
                    Toast.makeText(OpenPostActivity.this, "다시 댓글을 수정해주세요", Toast.LENGTH_SHORT).show();
                }else {
                    commentViewModel.updateComment(postId, comment.getCommentId()
                            , newCommentContent.toString());
                    Log.d("onEditBtnClick", "success");
                }
            }
            @Override
            public void onEditBtnClickDifferentUser() {
                Toast.makeText(OpenPostActivity.this, "수정 권한이 없습니다", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onDeleteBtnClick(Comment comment) {
                if(user.getUid().equals(comment.getAuthorId())){
                    Log.d("CommentAdapter", "onDeleteBtnClick");
                    commentViewModel.deleteComment(postId, comment.getCommentId(), user.getUid());
                }else{
                    Toast.makeText(OpenPostActivity.this, "삭제 권한이 없습니다", Toast.LENGTH_SHORT).show();
                }
            }
        });
        recyclerView.setAdapter(adapter);

        //viewModel setup
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        commentViewModel = new ViewModelProvider(this).get(CommentViewModel.class);
        commentViewModel.getComments(postId).observe(this, comments -> {
            Log.d("observe test", "Observe triggered: " + comments.size());
            Log.d("observe test", "observing...");
            adapter.submitList(new ArrayList<>(comments));
        });

        //View setup
        topAppBar = findViewById(R.id.topAppBar);
        postTitle = findViewById(R.id.postTitle);
        authorName = findViewById(R.id.authorName);
        postDate = findViewById(R.id.postDate);
        commentEditText = findViewById(R.id.commentEditText);
        sendCommentBtn = findViewById(R.id.sendCommentBtn);
        tvOriginalPost = findViewById(R.id.tvOriginalPost);

        tvOriginalPost.setOnClickListener(v->{
            String originalPostId = tvOriginalPost.getText().toString();
            if (originalPostId.equals("it is original")) return; //원본 post
            viewModel.getPostById(originalPostId, new MainViewModel.OnGetPostByIdListener() {
                @Override
                public void onSuccess(Post post) {
                    Toast.makeText(OpenPostActivity.this, "원본 페이지로 이동합니다."
                            , Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(OpenPostActivity.this, OpenPostActivity.class);
                    intent.putExtra("postId", originalPostId);
                    startActivity(intent);
                }
                @Override
                public void onError(String errorMessage) {
                    //원본 삭제되었다면 : post not found.
                    Toast.makeText(OpenPostActivity.this, errorMessage
                            , Toast.LENGTH_SHORT).show();
                }
            });
        });

        //뒤로가기
        topAppBar.setNavigationOnClickListener(v -> finish());
        //수정 / 삭제 버튼
        topAppBar.setOnMenuItemClickListener(item -> {
            Log.d("check", String.valueOf(isRetweetedPost));
            if (item.getItemId() == R.id.action_edit) {
                //리트윗 된 자식 post가 있을 경우 원본 수정 불가
                if(isRetweetedPost == true){
                    Toast.makeText(OpenPostActivity.this, "이미 리트윗 된 게시물입니다"
                            , Toast.LENGTH_SHORT).show();
                    return true;
                }

                // 수정 창으로 이동
                Intent intent = new Intent(OpenPostActivity.this, EditPostActivity.class);
                intent.putExtra("postId", postId);
                startActivity(intent);
                finish();
                return true;
            } else if (item.getItemId() == R.id.action_delete) {
                // 삭제 로직
                viewModel.deletePost(user.getUid(), postId);
                Intent intent = new Intent(OpenPostActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        //댓글 전송
        sendCommentBtn.setOnClickListener(v->{
            String commentText = commentEditText.getText().toString();
            if (!commentText.isEmpty()) {
                Comment comment = new Comment(postId, user.getUid(), "닉네임", "", commentText);
                commentViewModel.addComment(postId, comment, user.getUid());
                commentEditText.setText("");
            }else{
                Toast.makeText(OpenPostActivity.this, "댓글을 입력해주세요", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        // Firestore에서 최신 데이터 로드 후 UI 업데이트

        //post UI setup
        viewModel.getPostById(postId, new MainViewModel.OnGetPostByIdListener(){
            @Override
            public void onSuccess(Post post) {
                postTitle.setText(post.getTitle());
                authorName.setText(post.getCreatedBy());
                postDate.setText(post.getCreatedAt().toDate().toString());
                isRetweetedPost = post.getIsRetweeted();
                Log.d("isRetweetedPost", isRetweetedPost.toString());
                Log.d("retweetBy" , String.valueOf(post.getRetweetCount()));
                tvOriginalPost.setText(post.getParentPostId() != null ? post.getParentPostId() : "it is original");
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(OpenPostActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        //content UI setup
        contentRepo.getContentByPostId(postId, new ContentRepo.OnGetContentsListener(){
            @Override
            public void onSuccess(List<Content> contents) {
                readContentAdapter.submitList(new ArrayList<Content>(contents));
            }
            @Override
            public void onError(String errorMessage) {
                Toast.makeText(OpenPostActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
