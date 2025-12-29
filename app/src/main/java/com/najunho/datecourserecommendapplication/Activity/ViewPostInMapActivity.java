package com.najunho.datecourserecommendapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.najunho.datecourserecommendapplication.Activity.ForPost.OpenPostActivity;
import com.najunho.datecourserecommendapplication.Activity.User.UserSetUpActivity;
import com.najunho.datecourserecommendapplication.CloudFunction.CloudFunctionManager;
import com.najunho.datecourserecommendapplication.DB.Location;
import com.najunho.datecourserecommendapplication.DB.Post;
import com.najunho.datecourserecommendapplication.DB.PostRepo;
import com.najunho.datecourserecommendapplication.DB.User;
import com.najunho.datecourserecommendapplication.DB.UserRepo;
import com.najunho.datecourserecommendapplication.R;
import com.najunho.datecourserecommendapplication.Retrofit.KakaoRetrofitClient;
import com.najunho.datecourserecommendapplication.Retrofit.KakaoSearchResponse;
import com.najunho.datecourserecommendapplication.Util.ApplicationUtil;
import com.najunho.datecourserecommendapplication.Util.ClusterPreviewBottomSheet;
import com.najunho.datecourserecommendapplication.Util.KakaoKey;
import com.najunho.datecourserecommendapplication.Util.PostPreviewBottomSheet;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapReadyCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraAnimation;
import com.kakao.vectormap.camera.CameraUpdate;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelLayerOptions;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.label.LabelTextBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewPostInMapActivity extends AppCompatActivity {
    private EditText etSearchKeyword;
    private MapView mapView;
    private ImageView btnSearch;
    private ImageButton btnMainFeed, btnUserInfo;
    private ImageButton btnMyLocation;
    private String kakaoKey;
    private KakaoMap kakaoMap; // 지도 컨트롤용 객체 (onMapReady 내부에서 획득됨)
    private double lat, lng;
    private LabelLayer centerLayer, postLayer;
    private double minLat, maxLat, minLng, maxLng;
    private CloudFunctionManager cloudFunctionManager;
    private UserRepo userRepo;
    private PostRepo postRepo;
    private User currentUser;
    private Map<String, List<Post>> clusterMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_post_in_map);

        kakaoKey = KakaoKey.kakaoKey; //REST API KEY

        etSearchKeyword = findViewById(R.id.etSearchKeyword);
        btnSearch = findViewById(R.id.btnSearch);
        btnMainFeed = findViewById(R.id.btnMainFeed);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        btnUserInfo = findViewById(R.id.btnUserInfo);

        postRepo = ApplicationUtil.getPostRepo();

        userRepo = ApplicationUtil.getUserRepo();
        userRepo.getUser(new UserRepo.OnUserGetListener() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
            }
            @Override
            public void onError(String errorMessage) {
                Log.d("getUser" , "failed");
            }
        });

        cloudFunctionManager = new CloudFunctionManager();

        mapView = findViewById(R.id.mapView);
        mapView.start(mapLifeCycleCallback, mapReadyCallback);

        btnSearch.setOnClickListener(v->{
            //검색 키워드 -> 좌표로 변경 -> MapView UI 설정 -> 서버에 바운더리 전송 -> 경계 안에 있는 post load -> UI 변경
            String query = etSearchKeyword.getText().toString();

            if (query.isEmpty()) {
                etSearchKeyword.setError("위치 정보를 입력하세요");
                return;
            }

            String authHeader = "KakaoAK " + kakaoKey;

            Call<KakaoSearchResponse> call = KakaoRetrofitClient.getInstance()
                    .searchKeyword(authHeader, query, 1, 1);

            call.enqueue(new Callback<KakaoSearchResponse>() {
                @Override
                public void onResponse(Call<KakaoSearchResponse> call, Response<KakaoSearchResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        KakaoSearchResponse kakaoSearchResponse = response.body();
                        Location result = kakaoSearchResponse.getDocuments().get(0);
                        lat = result.getLatitude();
                        lng = result.getLongitude();
                        Log.d("KakaoSearchResponse", "좌표: " + lat + ", " + lng);

                        double distance = 500; // 500m

                        double deltaLat = distance / 111000.0;
                        double deltaLng = distance / (111000.0 * Math.cos(Math.toRadians(lat)));

                        minLat = lat - deltaLat;
                        maxLat = lat + deltaLat;
                        minLng = lng - deltaLng;
                        maxLng = lng + deltaLng;

                        if (kakaoMap != null) {
                            Log.d("moveMapToSearchedLocation", "start");
                            moveMapToSearchedLocation(lat, lng);
                        }
                    }
                }

                @Override
                public void onFailure(Call<KakaoSearchResponse> call, Throwable t) {
                    Log.d("KakaoSearchResponse", "error: " + t.getMessage());
                }
            });
        });

        btnMyLocation.setOnClickListener(v->{
            moveMapToSearchedLocation(lat, lng);
        });

        //mainFeed
        btnMainFeed.setOnClickListener(v-> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnUserInfo.setOnClickListener(v->{
            Intent intent = new Intent(this, UserSetUpActivity.class);
            startActivity(intent);
            finish();
        });

    }

    private final MapLifeCycleCallback mapLifeCycleCallback = new MapLifeCycleCallback() {
        @Override
        public void onMapDestroy() {
            // 지도 API 가 정상적으로 종료될 때 호출됨
            Log.d("mapView", "onMapDestroy");
        }
        @Override
        public void onMapError(Exception e) {
            // 인증 실패 및 지도 사용 중 에러가 발생할 때 호출됨
            Log.d("mapView", "onMapError: " + e.getMessage());
        }
    };

    private final MapReadyCallback mapReadyCallback = new KakaoMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull KakaoMap map) {
            // 인증 후 API 가 정상적으로 실행될 때 호출됨
            kakaoMap = map; // 지도 객체 저장
            Log.d("mapView", "onMapReady");

            //클릭 세팅
            kakaoMap.setOnLabelClickListener(new KakaoMap.OnLabelClickListener() {
                @Override
                public boolean onLabelClicked(KakaoMap kakaoMap, LabelLayer labelLayer, Label label) {
                    Log.d("onLabelClicked", "onLabelClicked : " + labelLayer.getLayerId());
                    Log.d("onLabelClicked", "label Tag: " + label.getTag());

                    List<Post> postList = (List<Post>) label.getTag();
                    if (postList.size() == 1) {
                        String postId = postList.get(0).getId();
                        postRepo.getPostById(postId, new PostRepo.OnPostListener() {
                            @Override
                            public void onSuccess(Post post) {
                                checkPurchase(post);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.d("getPostById", "failed");
                            }
                        });
                    }else {
                        //dialog 띄워서 post들 선택할 수 있게 만들어야 함.
                        Log.d("onLabelClicked", "dialog 띄워서 post들 선택할 수 있게 만들어야 함.");

                        ClusterPreviewBottomSheet clusterPreviewBottomSheet
                                = new ClusterPreviewBottomSheet(currentUser, postList);
                        clusterPreviewBottomSheet.show(getSupportFragmentManager(), "cluster");
                    }
                    return true;
                }
            });

            centerLayer = kakaoMap.getLabelManager().getLayer();
            centerLayer.setClickable(true);
            postLayer = kakaoMap.getLabelManager().addLayer(LabelLayerOptions.from("postLayer"));
            postLayer.setClickable(true);
        }

    };

    //키워드 검색 API에서 받은 좌표를 이용해 지도 이동
    private void moveMapToSearchedLocation(double lat, double lng) {

        if (kakaoMap == null) {
            // 지도가 아직 준비되지 않은 경우 (onMapReady 이전)
            Log.e("moveMapToSearchedLocation", "kakaoMap is null. Wait for onMapReady callback.");
            return;
        }

        runOnUiThread(() -> {
            try {
                // (1) LatLng 객체 생성 (카카오맵은 LatLng 사용)
                LatLng targetPosition = LatLng.from(lat, lng);

                // (2) 카메라 이동 (지도 중심 이동)
                CameraUpdate move = CameraUpdateFactory.newCenterPosition(targetPosition);
                Log.d("moveMapToSearchedLocation", "after move ");

                // (3) 줌 레벨 설정 (15 정도면 동네 단위 -> 건물 단위 표시)
                CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);

                kakaoMap.moveCamera(move, CameraAnimation.from(500, true, true));

                // //본인 좌표 추가
                LabelStyle iconStyle = LabelStyle.from(R.drawable.my_pin_32px);
                LabelStyles styles = kakaoMap.getLabelManager()
                        .addLabelStyles(LabelStyles.from(iconStyle));
                LabelOptions options = LabelOptions.from(targetPosition)
                        .setStyles(styles);

                centerLayer.remove(centerLayer.getAllLabels());

                centerLayer.addLabel(options);
                //바운더리 좌표 표시
                centerLayer.addLabel(LabelOptions.from(LatLng.from(maxLat, maxLng)).setStyles(styles));
                centerLayer.addLabel(LabelOptions.from(LatLng.from(maxLat, minLng)).setStyles(styles));
                centerLayer.addLabel(LabelOptions.from(LatLng.from(minLat, maxLng)).setStyles(styles));
                centerLayer.addLabel(LabelOptions.from(LatLng.from(minLat, minLng)).setStyles(styles));

                centerLayer.showAllLabels();

                cloudFunctionManager.callGetPostsInBoundary(minLat, maxLat, minLng, maxLng, new CloudFunctionManager.CloudFunctionCallback() {
                    @Override
                    public void onSuccess(boolean success, List<Post> posts) {

                        postLayer.removeAll();

                        //postLabel 생성
                        LabelStyles styles = kakaoMap.getLabelManager()
                                .addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.app_icon_color_64px)));

                        //clusterMap setup
                        for (Post post : posts) {
                            String key = post.getCoreLatitude() + "," + post.getCoreLongitude();
                            clusterMap.putIfAbsent(key, new ArrayList<>());
                            clusterMap.get(key).add(post);
                        }

                        for (String key : clusterMap.keySet()){
                            List<Post> group  = clusterMap.get(key);
                            Post first = group.get(0);

                            double lat1 = first.getCoreLatitude();
                            double lng1 = first.getCoreLongitude();

                            // 라벨 스타일
                            LabelOptions options = LabelOptions.from(LatLng.from(lat1, lng1))
                                    .setStyles(styles);

                            // 그룹이 여러 개면 숫자 표시
                            if (group.size() > 1) {
                                LabelTextBuilder textBuilder = new LabelTextBuilder();
                                textBuilder.setTexts(String.valueOf(group.size()));
                                options.setTexts(textBuilder);
                            }

                            Label label = postLayer.addLabel(options);
                            // label에 tag로 list 전체 전달
                            label.setTag(group);

                            Log.d("callGetPostsInBoundary",
                                    "label added at: " + key + " / size=" + group.size());
                        }
                        postLayer.showAllLabels();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.d("callGetPostsInBoundary", "error: " + e.getMessage() + "");
                    }
                });

                Log.d("moveMapToSearchedLocation", "layer.addLabel(options);");
            }catch (Exception e){
                Log.d("moveMapToSearchedLocation", "error: " + e.getMessage() );
            }
        });
    }

    private void checkPurchase(Post post){
        cloudFunctionManager.callCheckPurchase(post.getId(), new CloudFunctionManager.PointCallback() {
            @Override
            public void onSuccess(boolean success, String result) {
                if(Objects.equals(result, "noPurchasedPost")){
                    //구매해야함 -> PostPreviewBottomSheet로 이동.

                    Log.d("checkPurchase", "open PostPreviewBottomSheet");

                    PostPreviewBottomSheet sheet = new PostPreviewBottomSheet(post, currentUser);
                    sheet.show(getSupportFragmentManager(), "post_preview");

                }else if (Objects.equals(result, "purchasedPost") || Objects.equals(result, "myPost")) {
                    //구매했거나 내 게시물 -> openPost

                    Intent intent = new Intent(ViewPostInMapActivity.this, OpenPostActivity.class);
                    intent.putExtra("postId", post.getId());
                    startActivity(intent);
                    finish();

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
}
