package com.example.datecourserecommendapplication.Retrofit;

import com.example.datecourserecommendapplication.DB.Location;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class KakaoSearchResponse {
    //json 파일 받는 용도
    @SerializedName("documents")
    private List<Location> documents;

    public List<Location> getDocuments() {
        return documents;
    }
}