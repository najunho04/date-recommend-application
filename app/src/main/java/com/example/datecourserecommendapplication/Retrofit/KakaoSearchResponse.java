package com.example.datecourserecommendapplication.Retrofit;

import com.example.datecourserecommendapplication.DB.Location;

import java.util.List;

public class KakaoSearchResponse {
    //json 파일 받는 용도
    private List<Location> documents;

    public List<Location> getDocuments() {
        return documents;
    }
}