package com.example.datecourserecommendapplication.ViewModel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.DB.PostRepo;
import com.example.datecourserecommendapplication.DB.User;
import com.example.datecourserecommendapplication.DB.UserRepo;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainViewModel extends ViewModel {
    private FirebaseFirestore db = ApplicationUtil.getFirestore();
    private PostRepo postRepo = ApplicationUtil.getPostRepo();
    private UserRepo userRepo = ApplicationUtil.getUserRepo();

    private final LiveData<List<Post>> posts = postRepo.getPost();

    public LiveData<List<Post>> getPosts(){
        return posts; //observe와 연결
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        postRepo.stopListening();
    }

    public void addPost(Post post, String uid, List<Content> contents){
        postRepo.addPost(post, uid, contents, new PostRepo.OnPostListener() {
            @Override
            public void onSuccess(Post post) {
                Log.d("addPost", "success addPost, content and add post In User DB. All Success.");
            }
            @Override
            public void onError(String errorMessage) {
                Log.d("addPost", "failed Add Post " + errorMessage);
            }
        });
    }

    public interface OnGetPostByIdListener{
        void onSuccess(Post post);
        void onError(String errorMessage);
    }
    public void getPostById(String postId, OnGetPostByIdListener listener){
        postRepo.getPostById(postId, new PostRepo.OnPostListener() {
            @Override
            public void onSuccess(Post post) {
                listener.onSuccess(post);
            }

            @Override
            public void onError(String errorMessage) {
                listener.onError(errorMessage);
            }
        });
    }
    public void deletePost(String uid ,String postId){
        postRepo.deletePost(uid, postId);
    }

    public interface OnPostListener{
        void onSuccess(Post post);
        void onError(String errorMessage);
    }
    public void updatePost(Post post, String postId, List<Content> contents ,OnPostListener listener){
        postRepo.updatePost(postId, post, contents, new PostRepo.OnPostListener() {
            @Override
            public void onSuccess(Post post) {
                listener.onSuccess(post);
            }
            @Override
            public void onError(String errorMessage) {
                listener.onError(errorMessage);
            }
        });
    }

    public void addLike(String postId, String uid, OnCompleteListener<Void> listener){
        postRepo.addLike(postId, uid, task -> {
            if (task.isSuccessful()) {
                Log.d("addLike", "좋아요 추가 성공");
                userRepo.addLikesInUser(FirebaseAuth.getInstance().getUid(), postId, new UserRepo.OnUserAddedListener(){
                    @Override
                    public void onSuccess() {
                        Log.d("addLike", "User DB에 좋아요 추가 성공");
                        listener.onComplete(Tasks.forResult(null));
                    }
                    @Override
                    public void onError(String errorMessage) {
                        Log.d("addLike", errorMessage);
                    }
                });
            }else {
                Log.e("addLike", "좋아요 추가 실패", task.getException());
            }
        });
    }

    public void getPostInUser(Post post){
        //실시간으로 add하려니 null발생. 이슈 : addPost 후 get 리스너 로직에서 postId가 생성되는데 로직 시간이 엇갈려서 null발생
    }

    public void addRetweetPost(Post post, String uid, List<Content> contentList, Post originalPost, List<Content> originalContentList
            , OnCompleteListener<Void> listener){
        postRepo.addRetweetPost(post, uid, contentList, originalPost, originalContentList,
                new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                listener.onComplete(Tasks.forResult(null));
            }
        });
    }
}
