package com.example.datecourserecommendapplication.Retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class KakaoRetrofitClient {
    private static final String BASE_URL = "https://dapi.kakao.com/";
    private static KakaoApi kakaoApi;

    // Singleton Pattern -> 나중에 build 따로 나눌수도
    public static KakaoApi getInstance() {
        if (kakaoApi == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            kakaoApi = retrofit.create(KakaoApi.class);
        }
        return kakaoApi;
    }
}