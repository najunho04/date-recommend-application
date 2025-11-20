package com.example.datecourserecommendapplication.DB;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ContentRepo {
    private FirebaseFirestore db;
    public ContentRepo (FirebaseFirestore db){
        this.db = db;
    }

    public interface OnGetContentsListener{
        void onSuccess(List<Content> contents);
        void onError(String errorMessage);
    }

    public void getContentByPostId(String postId, OnGetContentsListener listener){
        db.collection("Posts").document(postId)
                .collection("Content")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Content> contents = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()){
                        Content content = doc.toObject(Content.class);
                        contents.add(content);
                    }
                    Log.d("getContentByPostId", "success " + contents.size());
                    listener.onSuccess(contents);
                }).addOnFailureListener(e->{
                    listener.onError(e.getMessage());
                });
    }
}
