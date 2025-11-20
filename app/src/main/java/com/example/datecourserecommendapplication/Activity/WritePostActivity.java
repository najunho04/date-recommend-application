package com.example.datecourserecommendapplication.Activity;

import static android.app.ProgressDialog.show;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.ContentAdapter;
import com.example.datecourserecommendapplication.Util.TimeCheck;
import com.example.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WritePostActivity extends AppCompatActivity {

    private Button btnSave, btnAddCourse;
    private EditText editTitle;
    private MainViewModel viewModel;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ContentAdapter adapter;
    private TimeCheck timeCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_post);

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        user = mAuth.getCurrentUser();
        timeCheck = new TimeCheck();

        btnSave = findViewById(R.id.btnSave);
        btnAddCourse = findViewById(R.id.btnAddCourse);
        editTitle = findViewById(R.id.editTitle);

        //adapter setup
        RecyclerView recyclerView = findViewById(R.id.rvContents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContentAdapter(true, new ContentAdapter.OnContentActionListener(){
            @Override
            public void onDeleteClick(int position) {
                //delete logic
                Log.d("ContentAdapter", "onDeleteClick success");
                adapter.submitList(new ArrayList<Content>(adapter.getCurrentList()) {{
                    remove(position);
                }});
            }
            @Override
            public void onSelectImageClick() {
                Log.d("ContentAdapter", "onSelectImageClick success");
            }

            @Override
            public void onIsCoreClick(int position) {

            }
        });
        recyclerView.setAdapter(adapter);

        //Save button logic
        btnSave.setOnClickListener(v -> {
            String title = editTitle.getText().toString();
            if(title.isEmpty()){
                editTitle.setError("제목을 입력하세요");
                return;
            }
            Post post = new Post(title, user.getUid(), Timestamp.now());
            List<Content> contentList = new ArrayList<>(adapter.getCurrentList());

            //preView 설정
            post.setPreviewText(contentList.get(0).getDescription());

            //시간 데이터 확인 및 설정
            for(Content content : contentList){
                String startTime = content.getStartTimeString();
                String endTime = content.getEndTimeString();
                if(timeCheck.isValidTimeFormat(startTime)){
                    content.setStartTime(timeCheck.convertToTimestamp(startTime));
                }else{
                    Toast.makeText(WritePostActivity.this, "시간 형식이 올바르지 않습니다"
                            , Toast.LENGTH_SHORT).show();
                    return;
                }
                if(timeCheck.isValidTimeFormat(endTime)){
                    content.setEndTime(timeCheck.convertToTimestamp(endTime));
                }else{
                    Toast.makeText(WritePostActivity.this, "시간 형식이 올바르지 않습니다"
                            , Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            viewModel.addPost(post, user.getUid(), contentList);

            Toast.makeText(this, "성공적으로 저장되었습니다", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        //Add Course button logic
        btnAddCourse.setOnClickListener(v -> {
            Content newContent = new Content();
            newContent.setContentId(java.util.UUID.randomUUID().toString()); // 임시 ID 생성
            adapter.submitList(new ArrayList<Content>(adapter.getCurrentList()) {{
                add(newContent);
            }});
        });
    }

}
