package com.example.datecourserecommendapplication.Activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.Activity.ForPost.OpenPostActivity;
import com.example.datecourserecommendapplication.Activity.ForPost.RetweetActivity;
import com.example.datecourserecommendapplication.Activity.ForPost.WritePostActivity;
import com.example.datecourserecommendapplication.Activity.User.UserSetUpActivity;
import com.example.datecourserecommendapplication.CloudFunction.CloudFunctionManager;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.DB.PostRepo;
import com.example.datecourserecommendapplication.DB.User;
import com.example.datecourserecommendapplication.DB.UserRepo;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.RecycerView.PostAdapter;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.example.datecourserecommendapplication.Util.Area;
import com.example.datecourserecommendapplication.Util.PointLogic;
import com.example.datecourserecommendapplication.ViewModel.MainViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private Button btnUserInfo, btnMainFeed, btnViewMap;
    private ImageButton filterExpandButton, searchButton ,btnSelectInterestsCategory, btnSelectLocationCategory;
    private EditText searchEditText;
    private ImageButton btnAddPost;
    private AutoCompleteTextView sortMenu;
    private UserRepo userRepo;
    private PostRepo postRepo;
    private MainViewModel viewModel;
    private PostAdapter adapter;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private User currentUser;
    private String sort = "latest";
    private ArrayList<String> selectedInterests = new ArrayList<>();
    private ArrayList<String> selectedLocations = new ArrayList<>();
    private CloudFunctionManager cloudFunctionManager = new CloudFunctionManager();
    private boolean isExpanded = false; //검색 로직에서 UI 확장을 위한 변수

    // 정렬 옵션 배열
    private final String[] sortOptions = {"최신순", "좋아요순", "인기순"};

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();

        //sortMenu setup
        sortMenu = findViewById(R.id.sortMenu);
        ArrayAdapter<String> menuAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                sortOptions
        );
        sortMenu.setAdapter(menuAdapter);

        //adapter setup
        RecyclerView recyclerView = findViewById(R.id.recyclerViewPosts);
        // 2열로 표시
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        adapter = new PostAdapter((post, anchorView, actionType) -> {
            switch (actionType) {
                case LIKE:
                    viewModel.addLike(post.getId(), mAuth.getUid(), task ->{
                        //adapter.submitList();
                        //좋아요 로직 필요.
                    });
                    break;
                case RETWEET:
                    if(post.getParentPostId() != null){
                        //게시물이 자식 post일 때
                        Toast.makeText(MainActivity.this, "이미 리트윗 게시물입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(mAuth.getCurrentUser().toString().equals(post.getCreatedBy())){
                        //자기 게시물 리트윗 시도 시
                        Toast.makeText(MainActivity.this, "본인이 작성한 게시물은 리트윗 불가합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    checkPurchase(post, "retweet");

                    break;
                case COMMENT:
                    checkPurchase(post, "viewPost");
                case CLICK:
                    checkPurchase(post, "viewPost");
            }
        });
        recyclerView.setAdapter(adapter);


        //viewModel setup
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        /*/viewModel.getPosts().observe(this,posts -> {
            Log.d("test", "test");
            adapter.submitList(new ArrayList<>(posts)); //새 객체로 감싸야 Diff가 감지 가능.
        });
        /*/

        //Repo setup
        postRepo = ApplicationUtil.getPostRepo();
        //초기 UI 생성
        postRepo.getPosts(new PostRepo.OnPostsListener() {
            @Override
            public void onSuccess(List<Post> posts) {
                adapter.submitList(posts);
            }
            @Override
            public void onError(String errorMessage) {
                Log.d("getPosts" , errorMessage);
            }
        });

        userRepo = ApplicationUtil.getUserRepo();
        userRepo.getUser(new UserRepo.OnUserGetListener() {
            @Override
            public void onSuccess(User user) {
                String nickName = user.getNickname();
                Toast.makeText(MainActivity.this, nickName + "님 환영합니다", Toast.LENGTH_LONG).show();
                currentUser = user;
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

        //btnViewMap setup
        btnViewMap = findViewById(R.id.btnViewMap);
        btnViewMap.setOnClickListener(v->{
            Intent intent = new Intent(this, ViewPostInMapActivity.class);
            startActivity(intent);
            finish();
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

        sortMenu.setOnItemClickListener((parent, view, position, id) ->{
            String selected = sortOptions[position];
            switch (selected) {
                case "최신순":
                    sortMenu.setText("최신순", false);
                    Log.d("sortMenu", "최신순");
                    sort = "latest";

                    cloudFunctionManager.callGetPostsUnified(sort, selectedInterests, selectedLocations, new CloudFunctionManager.CloudFunctionCallback() {
                        @Override
                        public void onSuccess(boolean success, List<Post> posts) {
                            Log.d("callGetPostsUnified latest", "posts: " + posts);
                            adapter.submitList(posts);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.d("callGetPostsUnified latest", "failed: " + e.getMessage());
                        }
                    });

                    break;
                case "좋아요순":
                    sortMenu.setText("좋아요순", false);
                    Log.d("sortMenu", "좋아요순");
                    sort = "likes";

                    cloudFunctionManager.callGetPostsUnified(sort, selectedInterests, selectedLocations, new CloudFunctionManager.CloudFunctionCallback() {
                        @Override
                        public void onSuccess(boolean success, List<Post> posts) {
                            Log.d("callGetPostsUnified likes", "posts: " + posts);
                            adapter.submitList(posts);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.d("callGetPostsUnified likes", "failed: " + e.getMessage());
                        }
                    });

                    break;
                case "인기순":
                    sortMenu.setText("인기순", false);
                    Log.d("sortMenu", "인기순");
                    sort = "popular";

                    cloudFunctionManager.callGetPostsUnified(sort, selectedInterests, selectedLocations, new CloudFunctionManager.CloudFunctionCallback() {
                        @Override
                        public void onSuccess(boolean success, List<Post> posts) {
                            Log.d("callGetPostsUnified popular", "posts: " + posts);
                            adapter.submitList(posts);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.d("callGetPostsUnified popular", "failed: " + e.getMessage());
                        }
                    });

                    break;
            }
        });

        //btnSelectInterestsCategory
        btnSelectInterestsCategory = findViewById(R.id.btnSelectInterestsCategory);
        btnSelectInterestsCategory.setOnClickListener(v->{
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
                            if (tempInterests.size() >= 8){
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
                        LinearLayout interestsContainer = findViewById(R.id.interestsContainer);
                        interestsContainer.removeAllViews();

                        for (String item : tempInterests) {
                            TextView textView = new TextView(this);
                            textView.setText(item);
                            textView.setTextSize(14);
                            textView.setTextColor(Color.BLACK);
                            textView.setPadding(16, 8, 16, 8);

                            //margin
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(8, 0, 8, 0);
                            textView.setLayoutParams(params);

                            // 컨테이너에 추가
                            interestsContainer.addView(textView);
                        }
                        selectedInterests = tempInterests;
                        //post load logic
                        cloudFunctionManager.callGetPostsUnified(sort, selectedInterests, selectedLocations, new CloudFunctionManager.CloudFunctionCallback() {
                            @Override
                            public void onSuccess(boolean success, List<Post> posts) {
                                Log.d("callGetPostsUnified popular", "posts: " + posts);
                                adapter.submitList(posts);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.d("callGetPostsUnified popular", "failed: " + e.getMessage());
                            }
                        });
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        //btnSelectLocationCategory
        btnSelectLocationCategory = findViewById(R.id.btnSelectLocationCategory);
        btnSelectLocationCategory.setOnClickListener(v->{
            String[] areas = Area.AREAS;
            boolean[] checked = new boolean[areas.length];
            ArrayList<String> tempAreas = new ArrayList<>(selectedLocations);

            for (int i = 0; i < checked.length; i++) {
                checked[i] = selectedLocations.contains(areas[i]);
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("장소")
                    .setMultiChoiceItems(areas, checked, (dialog, which, isChecked) -> {
                        if(isChecked){
                            if (tempAreas.size() >= 8){
                                //선택 3개 초과 시 경고
                                Toast.makeText(this, "최대 3개까지만 선택할 수 있습니다.", Toast.LENGTH_SHORT).show();
                                checked[which] = false;
                                //잠시 대기 후 버튼 취소 -> 내부 토글로 버튼 취소가 안 될 수 있기 때문
                                ((AlertDialog)dialog).getListView().post(()
                                        -> ((AlertDialog)dialog).getListView().setItemChecked(which, false));
                            }else {
                                tempAreas.add(areas[which]);
                                checked[which] = true;
                            }
                        }else {
                            tempAreas.remove(areas[which]);
                            checked[which] = false;
                        }
                    }).setPositiveButton("확인", (dialog, which) ->{
                        LinearLayout locationsContainer = findViewById(R.id.locationsContainer);
                        locationsContainer.removeAllViews();

                        for (String item : tempAreas) {
                            TextView textView = new TextView(this);
                            textView.setText(item);
                            textView.setTextSize(14);
                            textView.setTextColor(Color.BLACK);
                            textView.setPadding(16, 8, 16, 8);

                            //margin
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(8, 0, 8, 0);
                            textView.setLayoutParams(params);

                            // 컨테이너에 추가
                            locationsContainer.addView(textView);
                        }
                        selectedLocations = tempAreas;

                        //post load logic
                        cloudFunctionManager.callGetPostsUnified(sort, selectedInterests, selectedLocations, new CloudFunctionManager.CloudFunctionCallback() {
                            @Override
                            public void onSuccess(boolean success, List<Post> posts) {
                                Log.d("callGetPostsUnified popular", "posts: " + posts);
                                adapter.submitList(posts);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.d("callGetPostsUnified popular", "failed: " + e.getMessage());
                            }
                        });
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        //search Logic -> 검색 로직 실행 -> 제목/content 제목/content Location 제목/주소 총 4필드 쿼리 로직
        searchButton = findViewById(R.id.searchButton);
        searchEditText = findViewById(R.id.searchEditText);
        searchButton.setOnClickListener(v -> {
            String keyword = searchEditText.getText().toString();
            if(keyword.isEmpty()){
                Toast.makeText(MainActivity.this, "검색어를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            //검색 로직
            cloudFunctionManager.callSearchPosts(keyword, new CloudFunctionManager.searchPostsCallback() {
                @Override
                public void onSuccess(boolean success, List<String> postIds) {
                    Log.d("searchButton", "postIds: " + postIds);
                    postRepo.getPostsById(postIds, new PostRepo.OnPostsListener() {
                        @Override
                        public void onSuccess(List<Post> posts) {
                            Log.d("searchButton", "posts: " + posts.toString());
                            adapter.submitList(posts);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.d("searchButton", "getPostsById : " + errorMessage);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.d("searchButton", "callSearchPosts : " + e.getMessage());
                }
            });
        });

        //filterExpandButton -> 검색 UI 확장 버튼
        filterExpandButton = findViewById(R.id.filterExpandButton);
        LinearLayout filterExpandLayout = findViewById(R.id.filterExpandLayout);
        filterExpandButton.setOnClickListener(v->{
            if (isExpanded) {
                // 닫기
                Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                filterExpandLayout.startAnimation(slideUp);
                filterExpandLayout.setVisibility(View.GONE);
                Log.d("slideUp", "success");
            } else {
                // 열기
                Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
                filterExpandLayout.setVisibility(View.VISIBLE);
                filterExpandLayout.startAnimation(slideDown);
                Log.d("slideDown", "success");
            }
            isExpanded = !isExpanded;
        });
    }

    private void openPost(Post post) {
        Intent intent = new Intent(MainActivity.this, OpenPostActivity.class);
        intent.putExtra("postId", post.getId());
        startActivity(intent);
        finish();
    }

    private void openRetweetActivity(Post post){
        Intent intent = new Intent(MainActivity.this, RetweetActivity.class);
        intent.putExtra("originalPostId", post.getId());
        startActivity(intent);
        finish();
    }

    private void checkPurchase(Post post, String state){
        cloudFunctionManager.callCheckPurchase(post.getId(), new CloudFunctionManager.PointCallback() {
            @Override
            public void onSuccess(boolean success, String result) {
                if(Objects.equals(result, "noPurchasedPost")){

                    //state가 retweet일 때
                    if(Objects.equals(state, "retweet")){
                        Toast.makeText(MainActivity.this, "열람이 필요한 게시물입니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //dialog 생성
                    Log.d("checkPurchase", "noPurchasedPost. dialog 창으로 이동합니다");

                    PointLogic.showPointConsumeDialog(MainActivity.this, 50, currentUser.getMyPoint()
                            , () -> {
                                //"예" 클릭 시
                                purchaseAndOpenPost(post.getId(), post);
                            });

                }else if (Objects.equals(result, "purchasedPost") || Objects.equals(result, "myPost")) {
                    if(Objects.equals(state, "retweet")){
                        //state가 retweet일 때
                        openRetweetActivity(post);
                    }else {
                        //state가 viewPost일 때
                        openPost(post);
                    }
                }else {
                    Log.d("checkPurchase", "error. result is not expected");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.d("checkPurchase", "error: " + e.getMessage());
            }
        });
    }

    public void purchaseAndOpenPost(String postId, Post post){
        cloudFunctionManager.callPurchasePost(postId, new CloudFunctionManager.PointCallback() {

            @Override
            public void onSuccess(boolean success, String result) {
                openPost(post);
            }

            @Override
            public void onError(Exception e) {
                Log.d("purchaseAndOpenPost", e.getMessage());
            }
        });
    }
}
