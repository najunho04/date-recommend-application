package com.najunho.datecourserecommendapplication.Retrofit;

import com.najunho.datecourserecommendapplication.DB.Location;
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