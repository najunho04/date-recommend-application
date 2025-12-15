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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.Activity.MainActivity;
import com.example.datecourserecommendapplication.Activity.UtilForUI.SearchLocationActivity;
import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.Location;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.ContentAdapter;
import com.example.datecourserecommendapplication.Util.ContentItemTouchHelperCallback;
import com.example.datecourserecommendapplication.Util.TimeCheck;
import com.example.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class WritePostActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private FirebaseStorage storage;
    private Button btnSave, btnAddCourse, btnSelectInterests;
    private ChipGroup chipGroupInterests;
    private ImageButton btnBack;
    private EditText editTitle;
    private MainViewModel viewModel;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ContentAdapter adapter;
    private TimeCheck timeCheck;
    private ActivityResultLauncher<Intent> locationSearchLauncher;
    private Location selectedPlace;
    private ArrayList<String> selectedInterests = new ArrayList<>();
    private int selectedItemIndex = -1;
    private List<String> localUriList = new ArrayList<>(3);
    private int localUriListIndex = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_post);

        storage = FirebaseStorage.getInstance();

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        user = mAuth.getCurrentUser();
        timeCheck = new TimeCheck();
        selectedPlace = new Location();

        btnSave = findViewById(R.id.btnSave);
        btnAddCourse = findViewById(R.id.btnAddCourse);
        btnBack = findViewById(R.id.btn_back);
        btnSelectInterests = findViewById(R.id.btnSelectInterests);

        chipGroupInterests = findViewById(R.id.chipGroupInterests);

        editTitle = findViewById(R.id.editTitle);

        //list 초기화
        localUriList = new ArrayList<>(Arrays.asList(null, null, null));

        //get img intent data
        registerPickImageLauncher();

        //get location intent data
        registerPickLocationLauncher();

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
            public void onSelectImageClick(int position, int localUriIndex, List<String> list) {
                localUriListIndex = localUriIndex;
                localUriList = new ArrayList<>(list); //item별 UriList 개인화 -> 무조건 adapter 객체 사용할때는 새 객체 참조
                Log.d("ContentAdapter", "onSelectImageClick success");
                selectedItemIndex = position;
                openGalleryForItem(selectedItemIndex);
            }

            @Override
            public void onIsCoreClick(int position) {
                Toast.makeText(WritePostActivity.this, "핵심 데이트 코스로 선택했습니다.", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onSelectLocation(int position) {
                Log.d("ContentAdapter", "onSelectLocation success");
                Intent intent = new Intent(WritePostActivity.this, SearchLocationActivity.class);
                selectedItemIndex = position;
                intent.putExtra("itemIndex", position);
                locationSearchLauncher.launch(intent);
            }
        });
        recyclerView.setAdapter(adapter);
        //드래그 로직 setup
        //ItemTouchHelper : 드래그 인식만 함 -> callback으로 전달 -> adapter에 리스너 통해서 로직 구현
        //결국 우린 ItemTouchHelper를 쓰기 위해서 callback을 만든거고 (ContentItemTouchHelperCallback), callback과 onMove 리스너를
        //연결해서 어댑터에서 로직을 구현함.
        ItemTouchHelper.Callback callback = new ContentItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        //뒤로가기
        btnBack.setOnClickListener(v->{
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        //Save button logic
        btnSave.setOnClickListener(v -> {
            String title = editTitle.getText().toString();
            if(title.isEmpty()){
                editTitle.setError("제목을 입력하세요");
                return;
            }

            Post post = new Post(title, user.getUid(), Timestamp.now());
            List<Content> contentList = new ArrayList<>(adapter.getCurrentList());
            for (int i = 0; i < contentList.size(); i++) {
                contentList.get(i).setOrder(i);
            }
            Log.d("after Drag", contentList.get(0).toString());

            //preView 설정
            post.setPreviewText(contentList.get(0).getDescription());

            //postInterests 설정
            List<String> newInterests = new ArrayList<>(selectedInterests);
            post.setPostInterests(newInterests);

            //location filter query 위한 대표 region 설정 + post Core좌표 설정
            for (Content content : contentList) {
                if(content.getIsCore() == true){
                    post.setCoreRegion(content.getLocation().getRegion1());

                    post.setCoreLatitude(content.getLocation().getLatitude());
                    post.setCoreLongitude(content.getLocation().getLongitude());

                    Log.d("setCoreRegion", post.getCoreRegion());
                    break;
                }
            }
            if (post.getCoreRegion() == null){
                post.setCoreRegion(contentList.get(0).getLocation().getRegion1());

                post.setCoreLatitude(contentList.get(0).getLocation().getLatitude());
                post.setCoreLongitude(contentList.get(0).getLocation().getLongitude());

                Log.d("setCoreRegion", post.getCoreRegion());
            }

            //post_thumbnail 설정
            if(contentList.get(0).getImageUrl() != null) {
                post.setThumbnail(contentList.get(0).getImageUrl().get(0));
            }else {
                post.setThumbnail(null);
            }

            //시간 데이터 확인 및 설정
            for(Content content : contentList) {
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

            //list 초기화
            localUriList = new ArrayList<>(Arrays.asList(null, null, null));

            List<String> imgList = new ArrayList<>(Arrays.asList(null, null, null));
            newContent.setImageUrl(imgList);

            adapter.submitList(new ArrayList<Content>(adapter.getCurrentList()) {{
                add(newContent);
            }});
        });

        //select Interests button
        btnSelectInterests.setOnClickListener(v->{
            String[] interests = {"카페", "전시회", "산책", "드라이브", "공연", "음식", "영화", "야경", "실내", "기타"};
            boolean[] checked = new boolean[interests.length];
            //selectedInterests(현재 선택되어 있는 interests, chipList와 동일) -> 복제 List 생성
            ArrayList<String> tempInterests = new ArrayList<>(selectedInterests);

            for (int i = 0; i < checked.length; i++) {
                checked[i] = selectedInterests.contains(interests[i]);
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("카테고리")
                    .setMultiChoiceItems(interests, checked, (dialog, which, isChecked) -> {
                        if(isChecked){
                            if (tempInterests.size() >= 3){
                                //선택 3개 초과 시 경고
                                Toast.makeText(this, "최대 3개까지만 선택할 수 있습니다.", Toast.LENGTH_SHORT).show();
                                checked[which] = false;
                                //잠시 대기 후 버튼 취소 -> 내부 토글로 버튼 취소가 안 될 수 있기 때문
                                ((AlertDialog)dialog).getListView().post(()
                                        -> ((AlertDialog)dialog).getListView().setItemChecked(which, false));
                            }else {
                                tempInterests.add(interests[which]);
                                checked[which] = true;
                            }
                        }else {
                            tempInterests.remove(interests[which]);
                            checked[which] = false;
                        }
                    })
                    .setPositiveButton("확인", (dialog, which) ->{
                        chipGroupInterests.removeAllViews();

                        for (String interest : tempInterests) {
                            Chip chip = new Chip(this);
                            chip.setText(interest);
                            chip.setCloseIconVisible(true);

                            final String interestName = interest;

                            chip.setOnCloseIconClickListener(v2 -> {
                                chipGroupInterests.removeView(chip);
                                selectedInterests.remove(interestName);
                            });
                            chipGroupInterests.addView(chip);
                        }
                        selectedInterests = tempInterests;
                    })
                    .setNegativeButton("취소", null)
                    .show();
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

                            setLocationInfo(name, address, lat, lng, placeId);
                        }
                    }
                });
    }

    //location 정보 UI 반영
    private void setLocationInfo(String name, String address, double lat, double lng, String placeId) {
        selectedPlace.setName(name);
        selectedPlace.setAddress(address);
        selectedPlace.setLatitude(lat);
        selectedPlace.setLongitude(lng);
        selectedPlace.setPlaceId(placeId);
        selectedPlace.splitAddressIntoRegions();
        Log.d("setLocationInfo", selectedPlace.getRegion1());
        Log.d("setLocationInfo", selectedPlace.toString());
        // UI에 표시 -> ContentList에 Location 추가하고 UI반영
        //-> DiffUtil에서 List뿐 아니라 인덱스 객체들도 새로운 객체여야 함. -> 다시 말해서 아예 새로운 객체, 인덱스 객체이여야 함
        List<Content> newList = new ArrayList<>();
        for (Content c : adapter.getCurrentList()) {
            newList.add(new Content(c)); // 깊은 복사
        }
        Location newLocation = new Location(selectedPlace);//전역변수를 list에 추가 시 같은 객체 참조 이슈 발생.
        newList.get(selectedItemIndex).setLocation(newLocation);
        adapter.submitList(newList);
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
                                        Log.d("WritePostActivity", "uri 업로드 성공");
                                        storageRef.getDownloadUrl()
                                                .addOnSuccessListener(downloadUri  ->{
                                                    Log.d("WritePostActivity", "uri 불러오기 성공");

                                                    // RecyclerView의 리스트(uri) 업데이트
                                                    List<Content> newList = new ArrayList<>();
                                                    for (Content c : adapter.getCurrentList()) {
                                                        newList.add(new Content(c)); // 깊은 복사
                                                    }

                                                    localUriList.set(localUriListIndex, downloadUri.toString());

                                                    List<String> newUriList = new ArrayList<>(localUriList);

                                                    newList.get(selectedItemIndex).setImageUrl(newUriList);

                                                    Log.d("DiffUtil", "old: " + adapter.getCurrentList().get(selectedItemIndex).getImageUrl());
                                                    Log.d("DiffUtil", "localUriList == oldUriList ? " +
                                                            (localUriList == adapter.getCurrentList().get(selectedItemIndex).getImageUrl()));

                                                    adapter.submitList(newList);
                                                }).addOnFailureListener(e->{
                                                    Log.d("WritePostActivity", "uri 가져오기 실패" + e.getMessage());
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        // Check if the exception is a StorageException
                                        if (e instanceof StorageException) {
                                            StorageException se = (StorageException) e;
                                            int errorCode = se.getErrorCode();
                                            int httpResultCode = se.getHttpResultCode();

                                            Log.e("STORAGE_ERROR", "ErrorCode: " + errorCode);
                                            Log.e("STORAGE_ERROR", "HTTP Result Code: " + httpResultCode);
                                            Log.e("STORAGE_ERROR", "Message: " + se.getMessage());

                                            // Show a toast for immediate feedback
                                            Toast.makeText(WritePostActivity.this,
                                                    "Upload Failed: " + se.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            // Handle generic errors
                                            Log.e("STORAGE_ERROR", "Unknown error", e);
                                            Toast.makeText(WritePostActivity.this,
                                                    "Upload Error: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
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
