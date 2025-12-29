package com.najunho.datecourserecommendapplication.Activity.ForPost;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.najunho.datecourserecommendapplication.Activity.MainActivity;
import com.najunho.datecourserecommendapplication.Activity.UtilForUI.SearchLocationActivity;
import com.najunho.datecourserecommendapplication.DB.Content;
import com.najunho.datecourserecommendapplication.DB.ContentRepo;
import com.najunho.datecourserecommendapplication.DB.Location;
import com.najunho.datecourserecommendapplication.DB.Post;
import com.najunho.datecourserecommendapplication.R;
import com.najunho.datecourserecommendapplication.RecycerView.ContentAdapter;
import com.najunho.datecourserecommendapplication.Util.ApplicationUtil;
import com.najunho.datecourserecommendapplication.Util.ContentItemTouchHelperCallback;
import com.najunho.datecourserecommendapplication.Util.InterestLogic;
import com.najunho.datecourserecommendapplication.Util.PostSetUpLogic;
import com.najunho.datecourserecommendapplication.Util.TimeCheck;
import com.najunho.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class EditPostActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> locationSearchLauncher;
    private Location selectedPlace;
    private int selectedItemIndex = -1;
    private Button btnAddCourse, btnSelectInterests;
    private ChipGroup chipGroupInterests;
    private TextView btnBack, btnSave;
    private EditText editTitle;
    private MainViewModel viewModel;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ContentAdapter contentAdapter;
    private ContentRepo contentRepo;
    private TimeCheck timeCheck;
    private FirebaseStorage storage;
    private List<String> localUriList = new ArrayList<>(3);
    private int localUriListIndex = 0;
    private List<String> selectedInterests = new ArrayList<>();
    private List<Content> contentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        storage = FirebaseStorage.getInstance();

        //list 초기화
        localUriList = new ArrayList<>(Arrays.asList(null, null, null));

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        user = mAuth.getCurrentUser();
        contentRepo = ApplicationUtil.getContentRepo();
        timeCheck = new TimeCheck();
        selectedPlace = new Location();

        btnSave = findViewById(R.id.btn_save);
        btnAddCourse = findViewById(R.id.btnAddCourse);
        btnBack = findViewById(R.id.btn_delete);
        editTitle = findViewById(R.id.editTitle);
        btnSelectInterests = findViewById(R.id.btnSelectInterests);
        chipGroupInterests = findViewById(R.id.chipGroupInterests);

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
            public void onSelectImageClick(int position, int localUriIndex, List<String> list) {
                localUriListIndex = localUriIndex;
                localUriList = new ArrayList<>(list);
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
                selectedInterests = post.getPostInterests();

                for (String interest : selectedInterests) {
                    Chip chip = new Chip(EditPostActivity.this);
                    chip.setText(interest);
                    chip.setCloseIconVisible(true);

                    final String interestName = interest;
                    //Chip 취소 로직
                    chip.setOnCloseIconClickListener(v2 -> {
                        chipGroupInterests.removeView(chip);
                        selectedInterests.remove(interestName);
                        //동적관리 가능?
                    });

                    chipGroupInterests.addView(chip);
                }
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

            contentList = new ArrayList<>(contentAdapter.getCurrentList());
            if(contentList.isEmpty()){
                Toast.makeText(EditPostActivity.this, "데이트 코스를 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            //post update
            viewModel.getPostById(postId, new MainViewModel.OnGetPostByIdListener(){
                @Override
                public void onSuccess(Post post) {
                    PostSetUpLogic.postSetUp(EditPostActivity.this, title, post, contentList, selectedInterests, (realPost, realContentList) -> {
                        viewModel.updatePost(realPost, postId, realContentList, new MainViewModel.OnPostListener() {
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
            //list 초기화
            localUriList = new ArrayList<>(Arrays.asList(null, null, null));
            List<String> imgList = new ArrayList<>(Arrays.asList(null, null, null));

            newContent.setImageUrl(imgList);
            newContent.setLocation(new Location());
            newContent.setContentId(java.util.UUID.randomUUID().toString()); // 임시 ID 생성
            contentAdapter.submitList(new ArrayList<Content>(contentAdapter.getCurrentList()) {{
                add(newContent);
            }});
        });

        //뒤로 가기 버튼
        btnBack.setOnClickListener(v-> {
            Intent intent = new Intent(EditPostActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        //select Interests button
        btnSelectInterests.setOnClickListener(v->{
            InterestLogic interestLogic = new InterestLogic();
            interestLogic.getInterests(this, selectedInterests, chipGroupInterests, new InterestLogic.InterestCallback() {
                @Override
                public void onChipConfirm(List<String> interests) {
                    selectedInterests = (ArrayList<String>) interests;
                }
                @Override
                public void onChipClose(List<String> interests) {
                    selectedInterests = (ArrayList<String>) interests;
                }
            });
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
        selectedPlace.splitAddressIntoRegions();
        Log.d("setLocationInfo", selectedPlace.toString());
        // UI에 표시 -> ContentList에 Location 추가하고 UI반영
        //-> DiffUtil에서 List뿐 아니라 인덱스 객체들도 새로운 객체여야 함. -> 다시 말해서 아예 새로운 객체, 인덱스 객체이여야 함
        List<Content> newList = new ArrayList<>();
        for (Content c : contentAdapter.getCurrentList()) {
            newList.add(new Content(c)); // 깊은 복사
        }
        Location newLocation = new Location(selectedPlace);
        newList.get(itemIndex).setLocation(newLocation);
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

                                                    localUriList.set(localUriListIndex, downloadUri.toString());

                                                    List<String> newUriList = new ArrayList<>(localUriList);

                                                    newList.get(selectedItemIndex).setImageUrl(newUriList);

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