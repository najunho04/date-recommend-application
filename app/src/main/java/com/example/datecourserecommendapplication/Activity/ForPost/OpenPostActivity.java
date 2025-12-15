package com.example.datecourserecommendapplication.Activity.ForPost;

import static java.lang.String.valueOf;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.Activity.MainActivity;
import com.example.datecourserecommendapplication.DB.Comment;
import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.ContentRepo;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.DB.User;
import com.example.datecourserecommendapplication.DB.UserRepo;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.CommentAdapter;
import com.example.datecourserecommendapplication.RecycerView.ReadContentAdapter;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.example.datecourserecommendapplication.Util.MapViewPreviewDialogFragment;
import com.example.datecourserecommendapplication.ViewModel.CommentViewModel;
import com.example.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OpenPostActivity extends AppCompatActivity {

    private TextView authorName, postDate, postTitle, tvOriginalPost, likeCount, commentCount, retweetCount;
    private TextInputEditText commentEditText;
    private Button sendCommentBtn;
    private ImageButton likeBtn, commentBtn, retweetBtn;
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
    private String originalPostId;
    private UserRepo userRepo;
    private User postUser;
    private Post myPost;
    private SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
    boolean isLiked = false;
    int likes;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_post);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        contentRepo = ApplicationUtil.getContentRepo();
        userRepo = ApplicationUtil.getUserRepo();

        //get postId
        postId = getIntent().getStringExtra("postId");
        if (postId == null) return;

        //adapter setup
        RecyclerView contentRecyclerView = findViewById(R.id.contentRecyclerView);
        contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        readContentAdapter = new ReadContentAdapter(new ReadContentAdapter.OnCLickMapViewDetailListener() {
            @Override
            public void onClcik(double lat, double lng) {
                MapViewPreviewDialogFragment dialog =
                        MapViewPreviewDialogFragment.newInstance(lat, lng);

                dialog.show(getSupportFragmentManager(), "MapPreviewDialog");
            }
        });
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
        likeBtn = findViewById(R.id.likeBtn);
        commentBtn = findViewById(R.id.commentBtn);
        retweetBtn = findViewById(R.id.retweetBtn);
        likeCount = findViewById(R.id.likeCount);
        commentCount = findViewById(R.id.commentCount);
        retweetCount = findViewById(R.id.retweetCount);

        likeBtn.setOnClickListener(v->{
            viewModel.addLike(postId, mAuth.getUid(), task ->{
                if (task.isSuccessful()) {
                    if(isLiked) {
                        //좋아요 취소
                        likeBtn.setImageResource(R.drawable.heart_empty);
                        isLiked = false;
                        likes--;
                        likeCount.setText(String.valueOf(likes));
                    } else if (!isLiked) {
                        //좋아요 추가
                        likeBtn.setImageResource(R.drawable.heart_full);
                        isLiked = true;
                        likes++;
                        likeCount.setText(String.valueOf(likes));
                    }
                }
            });
        });
        
        commentBtn.setOnClickListener(v->{
            //댓글 창으로 이동
            commentEditText.requestFocus();
        });

        retweetBtn.setOnClickListener(v->{
            boolean isMyPost = mAuth.getCurrentUser().getUid().toString().equals(myPost.getCreatedBy());

            if(originalPostId == null && !isMyPost){
                Intent intent = new Intent(OpenPostActivity.this, RetweetActivity.class);
                intent.putExtra("originalPostId", myPost.getId());
                startActivity(intent);
                finish();
            }
            if(isMyPost){
                //자기 게시물 리트윗 시도 시
                Toast.makeText(OpenPostActivity.this, "본인이 작성한 게시물은 리트윗 불가합니다.", Toast.LENGTH_SHORT).show();
            }else {
                //게시물이 자식 post일 때
                Toast.makeText(OpenPostActivity.this, "이미 리트윗 게시물입니다.", Toast.LENGTH_SHORT).show();
            }
            return;
        });

        //부모 게시물로 이동
        tvOriginalPost.setOnClickListener(v->{
            if (originalPostId == null) return; //원본 post
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

        //수정 / 삭제 버튼 / 뒤로가기
        topAppBar.setOnMenuItemClickListener(item -> {
            Log.d("check", valueOf(isRetweetedPost));
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
            } else if (item.getItemId() == R.id.action_back) {
                // 뒤로가기 로직
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

        userRepo.getUser(new UserRepo.OnUserGetListener() {
            @Override
            public void onSuccess(User user) {
                postUser = user;
                authorName.setText(postUser.getNickname());
            }

            @Override
            public void onError(String errorMessage) {
                Log.d("getUser", errorMessage);
            }
        });

        //post UI setup
        viewModel.getPostById(postId, new MainViewModel.OnGetPostByIdListener(){
            @Override
            public void onSuccess(Post post) {
                myPost = post;
                postTitle.setText(post.getTitle());

                Date date = post.getCreatedAt().toDate();
                String result = sdf.format(date);

                likes = post.getLikesCount();

                List<String> likesBy = post.getLikesBy();
                if(likesBy == null){likesBy = new ArrayList<>();}
                if(likesBy.contains(user.getUid())){
                    likeBtn.setImageResource(R.drawable.heart_full);
                    isLiked = true;
                }else {
                    likeBtn.setImageResource(R.drawable.heart_empty);
                    isLiked = false;
                }

                postDate.setText(result);
                isRetweetedPost = post.getIsRetweeted();
                Log.d("isRetweetedPost", isRetweetedPost.toString());
                Log.d("retweetBy" , valueOf(post.getRetweetCount()));
                originalPostId = post.getParentPostId();
                tvOriginalPost.setText(post.getParentPostId() != null ? "클릭 시 원본 게시물로 이동합니다." : "원본 게시물입니다");

                likeCount.setText(String.valueOf(likes));
                commentCount.setText(String.valueOf(post.getCommentsCount()));
                retweetCount.setText(String.valueOf(post.getRetweetCount()));
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
