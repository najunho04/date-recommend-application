package com.example.datecourserecommendapplication.Activity.ForPost;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.Activity.MainActivity;
import com.example.datecourserecommendapplication.Activity.UtilForUI.SearchLocationActivity;
import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.ContentRepo;
import com.example.datecourserecommendapplication.DB.Location;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.ContentAdapter;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.example.datecourserecommendapplication.Util.ContentItemTouchHelperCallback;
import com.example.datecourserecommendapplication.Util.TimeCheck;
import com.example.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditPostActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> locationSearchLauncher;
    private Location selectedPlace;
    private int selectedItemIndex = -1;

    private Button btnSave, btnAddCourse;
    private ImageButton btnBack;
    private EditText editTitle;
    private MainViewModel viewModel;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ContentAdapter contentAdapter;
    private ContentRepo contentRepo;
    private TimeCheck timeCheck;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        storage = FirebaseStorage.getInstance();

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        user = mAuth.getCurrentUser();
        contentRepo = ApplicationUtil.getContentRepo();
        timeCheck = new TimeCheck();
        selectedPlace = new Location();

        btnSave = findViewById(R.id.btnSave);
        btnAddCourse = findViewById(R.id.btnAddCourse);
        btnBack = findViewById(R.id.btnBack);
        editTitle = findViewById(R.id.editTitle);

        //get img intent
        registerPickImageLauncher();
        //get location intent data
        registerPickLocationLauncher();

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
            public void onSelectImageClick(int position) {
                Log.d("ContentAdapter", "onSelectImageClick success");
                selectedItemIndex = position;
                openGalleryForItem(selectedItemIndex);
            }

            @Override
            public void onIsCoreClick(int position) {
                Toast.makeText(EditPostActivity.this, "핵심 데이트 코스로 선택했습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSelectLocation(int position) {
                Log.d("ContentAdapter", "onSelectLocation success");
                Intent intent = new Intent(EditPostActivity.this, SearchLocationActivity.class);
                intent.putExtra("itemIndex", position);
                locationSearchLauncher.launch(intent);
            }
        });
        recyclerView.setAdapter(contentAdapter);
        //드래그 로직 setup
        ItemTouchHelper.Callback callback = new ContentItemTouchHelperCallback(contentAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

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
            Log.d("after Drag", contentList.get(0).toString());
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

                    //preView 설정
                    post.setPreviewText(contentList.get(0).getDescription());

                    //post_thumbnail 설정
                    if(contentList.get(0).getImageUrl() != null) {
                        post.setThumbnail(contentList.get(0).getImageUrl());
                    }else {
                        post.setThumbnail(null);
                    }


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
            newContent.setLocation(new Location());
            newContent.setContentId(java.util.UUID.randomUUID().toString()); // 임시 ID 생성
            contentAdapter.submitList(new ArrayList<Content>(contentAdapter.getCurrentList()) {{
                add(newContent);
            }});
        });

        btnBack.setOnClickListener(v->{
            Intent intent = new Intent(EditPostActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

    }

    //locationLauncher setup : intent callback이라고 생각하면 편함. intent 속성, intent 후 callback data 처리
    private void registerPickLocationLauncher() {
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
    }

    //location 정보 UI 반영
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
        for (Content c : contentAdapter.getCurrentList()) {
            newList.add(new Content(c)); // 깊은 복사
        }
        newList.get(itemIndex).setLocation(selectedPlace);
        contentAdapter.submitList(newList);
    }

    //imgLauncher setup
    private void registerPickImageLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && selectedItemIndex != -1) {
                            //로컬 환경에서 uri 영구 보관
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            //storage 통해서 uri 전송 및 url 생성.
                            String filename = UUID.randomUUID().toString() + ".jpg";
                            //StorageReference
                            StorageReference storageRef = storage.getReference()
                                    .child("postImages/" + filename);

                            storageRef.putFile(uri)
                                    .addOnSuccessListener(task ->{
                                        Log.d("EditPostActivity", "uri 업로드 성공");
                                        storageRef.getDownloadUrl()
                                                .addOnSuccessListener(downloadUri  ->{
                                                    Log.d("EditPostActivity", "uri 불러오기 성공");

                                                    // RecyclerView의 리스트(uri) 업데이트
                                                    List<Content> newList = new ArrayList<>();
                                                    for (Content c : contentAdapter.getCurrentList()) {
                                                        newList.add(new Content(c)); // 깊은 복사
                                                    }
                                                    newList.get(selectedItemIndex).setImageUrl(downloadUri.toString());
                                                    contentAdapter.submitList(newList);
                                                }).addOnFailureListener(e->{
                                                    Log.d("EditPostActivity", "uri 가져오기 실패" + e.getMessage());
                                                });
                                    }).addOnFailureListener(e->{
                                        Log.d("EditPostActivity", e.getMessage());
                                    });
                        }
                    }
                }
        );
    }

    //img 폴더로 intent
    private void openGalleryForItem(int index) {
        selectedItemIndex = index;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        pickImageLauncher.launch(intent);
    }
}
