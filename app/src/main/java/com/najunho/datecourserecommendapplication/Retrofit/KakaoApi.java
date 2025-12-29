package com.najunho.datecourserecommendapplication.Retrofit;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface KakaoApi {

    // Kakao Local - Keyword Search -> kakao측으로 보내는 API
    @GET("v2/local/search/keyword.json")
    Call<KakaoSearchResponse> searchKeyword(
            @Header("Authorization") String auth, //-> 카카오 키 사용
            @Query("query") String query, //키워드
            @Query("page") int page,
            @Query("size") int size
    );
}
