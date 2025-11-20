package com.example.datecourserecommendapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.ContentRepo;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.DB.PostRepo;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.ContentAdapter;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.example.datecourserecommendapplication.Util.TimeCheck;
import com.example.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class RetweetActivity extends AppCompatActivity {
    private String originalPostId;
    private Button btnSave, btnAddCourse;
    private TextView tvOriginalPost;
    private MainViewModel viewModel;
    private EditText editTitle;
    private ContentAdapter contentAdapter;
    private ContentRepo contentRepo;
    private PostRepo postRepo;
    private TimeCheck timeCheck;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private Post originalPost;
    private List<Content> newContentList;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retweet);

        //get postId
        originalPostId = getIntent().getStringExtra("originalPostId");
        if (originalPostId == null) return;

        contentRepo = ApplicationUtil.getContentRepo();
        postRepo = ApplicationUtil.getPostRepo();
        timeCheck = new TimeCheck();
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        btnSave = findViewById(R.id.btnSave);
        btnAddCourse = findViewById(R.id.btnAddCourse);
        editTitle = findViewById(R.id.editTitle);
        tvOriginalPost = findViewById(R.id.tvOriginalPost);

        //adapter setup
        RecyclerView recyclerView = findViewById(R.id.rvContents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        contentAdapter = new ContentAdapter(false, new ContentAdapter.OnContentActionListener() {
            @Override
            public void onDeleteClick(int position) {
                Log.d("ContentAdapter", "onDeleteClick success");
                contentAdapter.submitList(new ArrayList<Content>(contentAdapter.getCurrentList()) {{
                    remove(position);
                }});
            }
            @Override
            public void onSelectImageClick() {
                Log.d("ContentAdapter", "onSelectImageClick success");
            }

            @Override
            public void onIsCoreClick(int position) {
                Toast.makeText(RetweetActivity.this, "핵심 데이트 코스로 선택했습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSelectLocation(int position) {
                Log.d("ContentAdapter", "onSelectLocation success");
                Intent intent = new Intent(RetweetActivity.this, SearchLocationActivity.class);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(contentAdapter);

        //UI Setup
        viewModel.getPostById(originalPostId, new MainViewModel.OnGetPostByIdListener() {
            @Override
            public void onSuccess(Post post) {
                originalPost = post;
                editTitle.setText(originalPost.getTitle());
                tvOriginalPost.setText(originalPost.getId() != null ? originalPost.getId() : "no id setup");
            }

            @Override
            public void onError(String errorMessage) {
                Log.d("getPostById", "failed");
            }
        });

        contentRepo.getContentByPostId(originalPostId, new ContentRepo.OnGetContentsListener() {
            @Override
            public void onSuccess(List<Content> contents) {
                newContentList = contents;
                //originalContent를 리트윗하는 post 내 contents에도 연결 -> 추후 리트윗post 수정시 부모 content는 수정 불가 위함
                for (Content newContent : newContentList){
                    if(newContent.getIsCore() == true){
                        newContent.setOriginalContentId(newContent.getContentId());
                    }
                }
                contentAdapter.submitList(new ArrayList<Content>(newContentList));
            }
            @Override
            public void onError(String errorMessage) {
                Log.d("getContentByPostId", errorMessage);
            }
        });

        //Add Course button logic
        btnAddCourse.setOnClickListener(v->{
            Content newContent = new Content();
            newContent.setContentId(java.util.UUID.randomUUID().toString()); // 임시 ID 생성
            contentAdapter.submitList(new ArrayList<Content>(contentAdapter.getCurrentList()) {{
                add(newContent);
            }});
        });

        //retweet button logic
        btnSave.setOnClickListener(v-> {
            //Post Class 중 리트윗 관련 필드 : retweetCount, retweetBy, parentPostId, isRetweet 추가해서 addPost()
            //Content Class 중 리트윗 관련 필드 : originalPostId, originalContentId
            String title = editTitle.getText().toString();
            if (title.isEmpty()) {
                Toast.makeText(RetweetActivity.this, "제목이 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            Post post = new Post(title, user.getUid(), Timestamp.now());
            List<Content> contentList = new ArrayList<>(contentAdapter.getCurrentList());

            //preView 설정
            post.setPreviewText(contentList.get(0).getDescription());

            //시간 데이터 확인 및 설정
            for (Content content : contentList) {
                String startTime = content.getStartTimeString();
                String endTime = content.getEndTimeString();
                if (timeCheck.isValidTimeFormat(startTime)) {
                    content.setStartTime(timeCheck.convertToTimestamp(startTime));
                } else {
                    Toast.makeText(RetweetActivity.this, "시간 형식이 올바르지 않습니다"
                            , Toast.LENGTH_SHORT).show();
                    return;
                }
                if (timeCheck.isValidTimeFormat(endTime)) {
                    content.setEndTime(timeCheck.convertToTimestamp(endTime));
                } else {
                    Toast.makeText(RetweetActivity.this, "시간 형식이 올바르지 않습니다"
                            , Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            //retweet 관련 필드들 채운 채로 new (retweet) post 추가 + originalPost에 retweet post connect
            viewModel.addRetweetPost(post, user.getUid(), contentList, originalPost, newContentList, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Toast.makeText(RetweetActivity.this, "성공적으로 저장되었습니다", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RetweetActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    Log.d("addRetweetPost", "success");
                }
            });


        });

    }

}
