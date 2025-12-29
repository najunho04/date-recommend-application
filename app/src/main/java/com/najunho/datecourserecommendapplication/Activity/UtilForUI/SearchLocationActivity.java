package com.najunho.datecourserecommendapplication.Activity.UtilForUI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.najunho.datecourserecommendapplication.DB.Location;
import com.najunho.datecourserecommendapplication.R;
import com.najunho.datecourserecommendapplication.RecycerView.PlaceAdapter;
import com.najunho.datecourserecommendapplication.Retrofit.KakaoRetrofitClient;
import com.najunho.datecourserecommendapplication.Retrofit.KakaoSearchResponse;
import com.najunho.datecourserecommendapplication.Util.KakaoKey;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchLocationActivity extends AppCompatActivity {
    private Button btnSelect, btnSearch;
    private Location selectedPlace;
    private TextInputEditText etSearch;
    private PlaceAdapter adapter;
    private List<Location> placeList;
    private int itemIndex;

    private String kakaoKey; //git commit 시 없애야 함.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_location);

        btnSelect = findViewById(R.id.btnSelect);
        btnSearch = findViewById(R.id.btnSearch);
        etSearch = findViewById(R.id.etSearch);

        selectedPlace = new Location();
        placeList = new ArrayList<>();

        kakaoKey = KakaoKey.kakaoKey;
        itemIndex = getIntent().getIntExtra("itemIndex", -1);

        //adapter setup
        RecyclerView placeRecyclerView = findViewById(R.id.rv_place);
        placeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlaceAdapter((position, location) -> {
            Toast.makeText(SearchLocationActivity.this, "선택한 장소: " + location.getName()
                    , Toast.LENGTH_SHORT).show();
            selectedPlace = location;
        });
        placeRecyclerView.setAdapter(adapter);

        //검색 버튼
        btnSearch.setOnClickListener(v->{
            Log.d("SearchLocationActivity", "btnSearch Click");
            searchKeyword();
            //retrofit -> res -> recyclerView, Map
        });

        //장소 선택 완료
        btnSelect.setOnClickListener(v -> {
            //위치 정보와 함께 intent? activity 종료?
            //장소 선택 시 setResult()로 데이터를 전달
            Intent data = new Intent();
            selectedPlace.splitAddressIntoRegions();
            data.putExtra("place_name", selectedPlace.getName());
            data.putExtra("address", selectedPlace.getAddress());
            data.putExtra("lat", selectedPlace.getLatitude());
            data.putExtra("lng", selectedPlace.getLongitude());
            data.putExtra("placeId", selectedPlace.getPlaceId());
            data.putExtra("itemIndex", itemIndex);
            Log.d("btnSelect", "intent success");
            setResult(RESULT_OK, data);
            finish(); // 원래 Activity로 복귀
        });
    }

    private void searchKeyword() {
        if (etSearch == null){
            Toast.makeText(SearchLocationActivity.this, "No Data Here", Toast.LENGTH_SHORT).show();
            return;
        }
        String query = etSearch.getText().toString();

        // BuildConfig에서 Kakao Key 가져오기
        String authHeader = "KakaoAK " + kakaoKey;

        //call : Kakao API(KakaoRetrofitClient) 호출 ->
        Call<KakaoSearchResponse> call = KakaoRetrofitClient.getInstance()
                .searchKeyword(authHeader, query, 1, 15);

        //get 응답 시 callback으로 데이터 받기
        call.enqueue(new Callback<KakaoSearchResponse>() {
            @Override
            public void onResponse(Call<KakaoSearchResponse> call, Response<KakaoSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("SearchLocationActivity", "callback success " + response.body().getDocuments().toString());
                    placeList.clear();
                    placeList.addAll(response.body().getDocuments());
                    runOnUiThread(() -> adapter.setPlaceList(placeList));
                    Log.d("SearchLocationActivity", "placeList: " + placeList.get(0).toString());
                    Log.d("SearchLocationActivity", "adapter item size: " + adapter.getItemCount());

                }
            }

            @Override
            public void onFailure(Call<KakaoSearchResponse> call, Throwable t) {
                Log.e("KAKAO", "API ERROR: " + t.getMessage());
            }
        });
    }
}
