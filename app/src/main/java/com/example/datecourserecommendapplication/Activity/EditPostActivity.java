package com.example.datecourserecommendapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.ContentRepo;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.ContentAdapter;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.example.datecourserecommendapplication.Util.TimeCheck;
import com.example.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class EditPostActivity extends AppCompatActivity {

    private Button btnSave, btnAddCourse;
    private EditText editTitle;
    private MainViewModel viewModel;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ContentAdapter contentAdapter;
    private ContentRepo contentRepo;
    private TimeCheck timeCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        user = mAuth.getCurrentUser();
        contentRepo = ApplicationUtil.getContentRepo();
        timeCheck = new TimeCheck();

        btnSave = findViewById(R.id.btnSave);
        btnAddCourse = findViewById(R.id.btnAddCourse);
        editTitle = findViewById(R.id.editTitle);

        //adapter setup
        RecyclerView recyclerView = findViewById(R.id.rvContents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        contentAdapter = new ContentAdapter(true, new ContentAdapter.OnContentActionListener() {
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
                Toast.makeText(EditPostActivity.this, "핵심 데이트 코스로 선택했습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSelectLocation(int position) {
                Log.d("ContentAdapter", "onSelectLocation success");
                Intent intent = new Intent(EditPostActivity.this, SearchLocationActivity.class);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(contentAdapter);

        //postId get
        String postId = getIntent().getStringExtra("postId");
        if (postId == null) return;

        //초기 UI 반영
        viewModel.getPostById(postId, new MainViewModel.OnGetPostByIdListener() {
            @Override
            public void onSuccess(Post post) {
                editTitle.setText(post.getTitle());
            }
            @Override
            public void onError(String errorMessage) {
                Log.d("getPostById", "failed");
            }
        });
        //초기 UI (content) 반영
        contentRepo.getContentByPostId(postId, new ContentRepo.OnGetContentsListener() {
            @Override
            public void onSuccess(List<Content> contents) {
                contentAdapter.submitList(new ArrayList<Content>(contents));
                Log.d("getContentByPostId", "success " + contents.size());
            }
            @Override
            public void onError(String errorMessage) {
                Log.d("getContentByPostId", errorMessage);
            }
        });

        //update button logic
        btnSave.setOnClickListener(v -> {
            //null check
            String title = editTitle.getText().toString();
            if (title.isEmpty()) {
                editTitle.setError("제목을 입력하세요");
                return;
            }
            List<Content> contentList = new ArrayList<>(contentAdapter.getCurrentList());
            if(contentList.isEmpty()){
                Toast.makeText(EditPostActivity.this, "데이트 코스를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            //시간 데이터 확인 및 설정
            for(Content content : contentList){
                String startTime = content.getStartTimeString();
                String endTime = content.getEndTimeString();
                if(timeCheck.isValidTimeFormat(startTime)){
                    content.setStartTime(timeCheck.convertToTimestamp(startTime));
                }else{
                    Toast.makeText(EditPostActivity.this, "시간 형식이 올바르지 않습니다"
                            , Toast.LENGTH_SHORT).show();
                    return;
                }
                if(timeCheck.isValidTimeFormat(endTime)){
                    content.setEndTime(timeCheck.convertToTimestamp(endTime));
                }else{
                    Toast.makeText(EditPostActivity.this, "시간 형식이 올바르지 않습니다"
                            , Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            //post update
            viewModel.getPostById(postId, new MainViewModel.OnGetPostByIdListener(){
                @Override
                public void onSuccess(Post post) {
                    post.setTitle(title);
                    //content update
                    Log.d("update", "success");
                    //추후 수정 시간, 좋아요 등 추가 데이터 수정 예정
                    viewModel.updatePost(post, postId, contentList, new MainViewModel.OnPostListener() {
                        @Override
                        public void onSuccess(Post post) {
                            Log.d("update", "success2");
                            Toast.makeText(EditPostActivity.this, "성공적으로 저장되었습니다", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(EditPostActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(EditPostActivity.this, "failed"+errorMessage, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(EditPostActivity.this, "failed"+errorMessage, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });

        //데이트 코스 추가
        btnAddCourse.setOnClickListener(v -> {
            Content newContent = new Content();
            newContent.setContentId(java.util.UUID.randomUUID().toString()); // 임시 ID 생성
            contentAdapter.submitList(new ArrayList<Content>(contentAdapter.getCurrentList()) {{
                add(newContent);
            }});
        });
    }
}
