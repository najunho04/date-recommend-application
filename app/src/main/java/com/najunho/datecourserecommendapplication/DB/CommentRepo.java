package com.najunho.datecourserecommendapplication.DB;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.najunho.datecourserecommendapplication.Util.ApplicationUtil;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentRepo {
    private FirebaseFirestore db;
    private final MutableLiveData<List<Comment>> commentsLiveData = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration commentsListener;
    private UserRepo userRepo = ApplicationUtil.getUserRepo();
    private String currentPostId = ""; // 현재 감시 중인 postId 저장용

    public CommentRepo (FirebaseFirestore db){
        this.db = db;
    }

    public LiveData<List<Comment>> getComment(String postId){
        if (commentsListener == null|| !postId.equals(currentPostId)) observeComments(postId);
        Log.d("getComment", "psotID : " + postId);
        Log.d("getComment", "success");
        Log.d("getComment", "commentsLiveData: " + commentsLiveData.getValue());
        return commentsLiveData; //업데이트 된 DB viewModel과 연결
    }

    public void observeComments(String postId) {
        currentPostId = postId; // 현재 ID 업데이트
        CollectionReference commentsRef = db.collection("Posts").document(postId).collection("Comments");
        commentsListener = commentsRef.orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        // handle error
                        return;
                    }
                    List<Comment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Comment c = doc.toObject(Comment.class);
                        c.setCommentId(doc.getId());
                        comments.add(c);
                    }
                    Log.d("observe test", "observeComments");
                    commentsLiveData.setValue(comments);
                    // ViewModel의 LiveData에 new ArrayList<>(comments)로 set -> Adapter.submitList
                });
    }

    public void removeCommentsObserver() {
        if (commentsListener != null) commentsListener.remove();
    }

    public void addComment(String postId, Comment comment, String uid) {
        DocumentReference postRef = db.collection("Posts").document(postId);

        CollectionReference commentsRef = db.collection("Posts").document(postId)
                .collection("Comments");

        DocumentReference newCommentRef = commentsRef.document(); // 직접 문서 ID 생성
        String commentId = newCommentRef.getId(); // ID를 바로 확보

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.update(postRef, "commentsCount", FieldValue.increment(1));
            transaction.set(newCommentRef, comment);
            return null;
        }).addOnSuccessListener(avoid -> {
            Log.d("addComment", "success");
            userRepo.addCommentInUser(uid, commentId);
        }).addOnFailureListener(e->{
            Log.d("addComment", "failed"+ e);
        });

    }

    public void deleteComment(String postId, String commentId, String uid) {
        if (postId == null || commentId == null) {
            Log.e("deleteComment", "postId or commentId is null");
            return;
        }
        DocumentReference postRef = db.collection("Posts").document(postId);
        DocumentReference newCommentRef = postRef.collection("Comments").document(commentId); // 자동 ID

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // delete comment
            transaction.delete(newCommentRef);
            // update commentsCount (use snapshot if needed)
            transaction.update(postRef, "commentsCount", FieldValue.increment(-1));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("deleteComment", "success");
            userRepo.deleteCommentInUser(uid, commentId);
        }).addOnFailureListener(e -> {
            Log.d("deleteComment", "failed"+ e);
        });
    }

    public void updateComment(String postId, String commentId, String newContent){
        DocumentReference commentRef = db.collection("Posts")
                .document(postId)
                .collection("Comments")
                .document(commentId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("content", newContent);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        commentRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CommentRepo", "updateComment success: " + commentId);
                })
                .addOnFailureListener(e -> {
                    Log.e("CommentRepo", "updateComment fail", e);
                });
    }

}
