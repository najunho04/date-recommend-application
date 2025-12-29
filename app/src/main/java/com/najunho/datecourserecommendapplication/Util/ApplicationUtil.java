package com.najunho.datecourserecommendapplication.Util;

import android.app.Application;

import com.najunho.datecourserecommendapplication.DB.CommentRepo;
import com.najunho.datecourserecommendapplication.DB.ContentRepo;
import com.najunho.datecourserecommendapplication.DB.PostRepo;
import com.najunho.datecourserecommendapplication.DB.UserRepo;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class ApplicationUtil extends Application {
    private static FirebaseFirestore db;
    private static UserRepo userRepo;
    private static PostRepo postRepo;
    private static ContentRepo contentRepo;
    private static CommentRepo commentRepo;

    @Override
    public void onCreate() {
        super.onCreate();
        // Firebase 초기화
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        userRepo = new UserRepo(db);
        postRepo = new PostRepo(db);
        commentRepo = new CommentRepo(db);
        contentRepo = new ContentRepo(db);

        //kakaoMap 초기화
        com.kakao.vectormap.KakaoMapSdk.init(this, "REDACTED_KAKAO_NATIVE_APP_KEY");
    }

    public static FirebaseFirestore getFirestore(){
        return db;
    }
    public static UserRepo getUserRepo() {
        return userRepo;
    }
    public static PostRepo getPostRepo() {return postRepo; }
    public static CommentRepo getCommentRepo() {return commentRepo; }
    public static ContentRepo getContentRepo() {return contentRepo; }
}
