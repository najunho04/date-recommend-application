package com.najunho.datecourserecommendapplication.DB;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UserRepo {
    private FirebaseFirestore db;
    public UserRepo (FirebaseFirestore db){
        this.db = db;
    }
    public interface OnUserAddedListener{
        void onSuccess();
        void onError(String errorMessage);
    }

    public void addUser(String nickname, String location, String gender, int age, List<String> interests
            , OnUserAddedListener listener) {
        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        user.put("location", location);
        user.put("gender", gender);
        user.put("age", age);
        user.put("interests", interests);
        user.put("likesPost", new ArrayList<>());
        user.put("createdAt", Timestamp.now());
        user.put("comments", new ArrayList<>());
        user.put("postsId", new ArrayList<>());
        user.put("myPoint", 500);

        Log.d("addUser", "start Logic");

        db.collection("Users")
                .document(FirebaseAuth.getInstance().getUid()) // 로그인된 사용자 UID -> document 주소
                .set(user)
                .addOnSuccessListener(documentReference -> {
                    Log.d("addUser" , "success");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.d("addUser" , "failed" + e);
                    listener.onError(e.getMessage());
                });
    }//추후 유저 정보 수정 (nickname ~ interests 까지) + 좋아요, 댓글, 게시물 작성 추가 시 업데이트

    public interface OnUserGetListener{
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public void getUser(OnUserGetListener listener){
        Log.d("getUser", "start Logic");
        db.collection("Users").document(FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()){
                        Log.d("getNickName", "success");
                        User user = documentSnapshot.toObject(User.class);
                        listener.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d("getNickName", "failed");
                    listener.onError(e.getMessage());
                });
    }

    public void updateNicknameInFirestore(String newNickname, OnUserAddedListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("nickname", newNickname);

        db.collection("Users").document(FirebaseAuth.getInstance().getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("updateNicknameInFirestore", "success");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.d("updateNicknameInFirestore", "failed" + e);
                    listener.onError(e.getMessage());
                });
    }

    public void updateUser(String uid, String updateNickname, String updateLocation, String updateGender
            ,int updateAge, List<String> updateInterests, OnUserAddedListener listener){
        Map<String, Object> updateUser = new HashMap<>();
        updateUser.put("nickname", updateNickname);
        updateUser.put("location", updateLocation);
        updateUser.put("gender", updateGender);
        updateUser.put("age", updateAge);
        updateUser.put("interests", updateInterests);

        db.collection("Users").document(uid)
                .update(updateUser)
                .addOnSuccessListener(avoid ->{
                    Log.d("updateUser", "success");
                    listener.onSuccess();
                })
                .addOnFailureListener(e ->{
                    Log.d("updateUser", "failed");
                    listener.onError(e.getMessage());
                });
    }

    public void deleteUser(String uid) {
        db.collection("Users").document(uid)
                .delete()
                .addOnSuccessListener(avoid -> {
                    Log.d("deleteUser", "success");
                })
                .addOnFailureListener(e -> {
                    Log.d("deleteUser" , "failed" + e);
                });
    }


    public interface OnUserCheckListener{
        void onChecked();
        void onNotChecked();
        void onError(String errorMessage);
    }
    //지금 signIn, login이 같은 버튼에서 이루어지는 중. 그래서 DB가 존재하면 login, 없으면 회원가입 상황임

    public void checkUser(String uid, OnUserCheckListener listener){
        db.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("CheckUID", "로그인");
                        listener.onChecked();
                    } else {
                        Log.d("CheckUID", "회원가입");
                        listener.onNotChecked();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CheckUID", "UID 확인 중 오류 발생", e);
                    listener.onError(e.getMessage());
                });
    }

    public void addPostIdInUser(String uid, String postId, OnUserAddedListener listener){
        DocumentReference userRef = db.collection("Users").document(uid);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);

            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("user not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            List<String> postsId = (List<String>) snapshot.get("postsId");
            if (postsId == null) postsId = new ArrayList<>();
            postsId.add(postId);
            transaction.update(userRef, "postsId", postsId);
            return null;
        }).addOnSuccessListener(avoid -> {
            Log.d("addPostIdInUser", "success");
            listener.onSuccess();
        }).addOnFailureListener(e ->{
            listener.onError(e.getMessage());
        });
    }

    public void deletePostIdInUser(String uid, String postId, OnUserAddedListener listener){
        DocumentReference userRef = db.collection("Users").document(uid);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);

            if (!snapshot.exists()){
                throw new FirebaseFirestoreException("user not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            List<String> postsId = (List<String>) snapshot.get("postsId");
            if (postsId == null) postsId = new ArrayList<>();
            postsId.remove(postId);
            transaction.update(userRef, "postsId", postsId);
            return null;
        }).addOnSuccessListener(avoid -> {
            listener.onSuccess();
        }).addOnFailureListener(e ->{
            listener.onError(e.getMessage());
        });
    }

    public void addLikesInUser(String uid, String postId, OnUserAddedListener listener){
        DocumentReference userRef = db.collection("Users").document(uid);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);

            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("Post not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            List<String> likesPost = (List<String>) snapshot.get("likesPost");
            if(likesPost == null) {likesPost = new ArrayList<>();}

            if(likesPost.contains(postId)) {
                likesPost.remove(postId);
                transaction.update(userRef, "likesPost", likesPost);
            }else {
                likesPost.add(postId);
                transaction.update(userRef, "likesPost", likesPost);
            }

            return null;
        }).addOnSuccessListener(avoid -> {
            Log.d("addLikesInUser", "success");
            listener.onSuccess();
        }).addOnFailureListener(e ->{
            listener.onError(e.getMessage());
        });
    }
    public void addCommentInUser(String uid, String commentId){
        DocumentReference userRef = db.collection("Users").document(uid);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("user not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            List<String> comments = (List<String>) snapshot.get("comments");
            if (comments == null) {comments = new ArrayList<>();}
            comments.add(commentId);
            transaction.update(userRef, "comments", comments);
            return null;
        }).addOnSuccessListener(t->{
            Log.d("addCommentInUser", "success");
        }).addOnFailureListener(e -> {
            Log.d("addCommentInUser", e.getMessage());
        });
    }

    public void deleteCommentInUser(String uid, String commentId){
        DocumentReference userRef = db.collection("Users").document(uid);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("user not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            List<String> comments = (List<String>) snapshot.get("comments");
            if (comments == null) {
                comments = new ArrayList<>();
            }
            comments.remove(commentId);
            transaction.update(userRef, "comments", comments);
            return null;
        }).addOnSuccessListener(t->{
            Log.d("addCommentInUser", "success");
        }).addOnFailureListener(e -> {
            Log.d("addCommentInUser", e.getMessage());
        });
    }

    public interface OnGetMyPostsListener {
        void onSuccess(List<String> postsId);
        void onError(String errorMessage);
    }

    public void getMyPostsId(String uid, OnGetMyPostsListener listener){
        db.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.get("postsId") == null){
                        listener.onSuccess(new ArrayList<>());
                    }else {
                        List<String> myPostsId = (List<String>) documentSnapshot.get("postsId");
                        listener.onSuccess(myPostsId);
                    }
                });
    }

    public void getMyLikesPostsId(String uid, OnGetMyPostsListener listener){
        db.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot ->{
                    if(documentSnapshot.get("likesPost") == null){
                        listener.onSuccess(new ArrayList<>());
                    }else {
                        List<String> myPostsId = (List<String>) documentSnapshot.get("likesPost");
                        listener.onSuccess(myPostsId);
                    }
                });
    }
}
