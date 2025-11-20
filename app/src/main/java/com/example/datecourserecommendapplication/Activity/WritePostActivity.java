package com.example.datecourserecommendapplication.Activity;

import static android.app.ProgressDialog.show;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.Location;
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
    private ActivityResultLauncher<Intent> locationSearchLauncher;
    private Location selectedPlace;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_post);

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        user = mAuth.getCurrentUser();
        timeCheck = new TimeCheck();
        selectedPlace = new Location();

        btnSave = findViewById(R.id.btnSave);
        btnAddCourse = findViewById(R.id.btnAddCourse);
        editTitle = findViewById(R.id.editTitle);

        //get location intent data
        locationSearchLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String name = data.getStringExtra("place_name");
                            String address = data.getStringExtra("address");
                            double lat = data.getDoubleExtra("lat", 0);
                            double lng = data.getDoubleExtra("lng", 0);
                            String placeId = data.getStringExtra("placeId");
                            int itemIndex = data.getIntExtra("itemIndex", -1);

                            setLocationInfo(name, address, lat, lng, placeId, itemIndex);
                        }
                    }
                });

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
                Toast.makeText(WritePostActivity.this, "핵심 데이트 코스로 선택했습니다.", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onSelectLocation(int position) {
                Log.d("ContentAdapter", "onSelectLocation success");
                Intent intent = new Intent(WritePostActivity.this, SearchLocationActivity.class);
                intent.putExtra("itemIndex", position);
                locationSearchLauncher.launch(intent);
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
            newContent.setLocation(new Location()); //new Odject 생성 시 필드는 전부 null (int, boolean...etc 제외)
            newContent.setContentId(java.util.UUID.randomUUID().toString()); // 임시 ID 생성
            adapter.submitList(new ArrayList<Content>(adapter.getCurrentList()) {{
                add(newContent);
            }});
        });
    }

    private void setLocationInfo(String name, String address, double lat, double lng, String placeId, int itemIndex) {
        selectedPlace.setName(name);
        selectedPlace.setAddress(address);
        selectedPlace.setLatitude(lat);
        selectedPlace.setLongitude(lng);
        selectedPlace.setPlaceId(placeId);
        selectedPlace.setItemIndex(itemIndex);
        Log.d("setLocationInfo", selectedPlace.toString());
        // UI에 표시 -> ContentList에 Location 추가하고 UI반영
        //-> DiffUtil에서 List뿐 아니라 인덱스 객체들도 새로운 객체여야 함. -> 다시 말해서 아예 새로운 객체, 인덱스 객체이여야 함
        List<Content> newList = new ArrayList<>();
        for (Content c : adapter.getCurrentList()) {
            newList.add(new Content(c)); // 깊은 복사
        }
        newList.get(itemIndex).setLocation(selectedPlace);
        adapter.submitList(newList);
    }

}
