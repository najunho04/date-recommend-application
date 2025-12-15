package com.example.datecourserecommendapplication.ViewModel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.datecourserecommendapplication.CloudFunction.CloudFunctionManager;
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
import java.util.Objects;


public class MainViewModel extends ViewModel {
    private FirebaseFirestore db = ApplicationUtil.getFirestore();
    private PostRepo postRepo = ApplicationUtil.getPostRepo();
    private UserRepo userRepo = ApplicationUtil.getUserRepo();

    //postRepo랑 연결되어 있는 postList : DB변경 시 sourcePosts도 변경
    private final LiveData<List<Post>> sourcePosts = postRepo.getPost();

    //필터, 검색, 좋아요.. 등 클라이언트 로직과 연결되어 UI에 반영되는 postList
    private final MutableLiveData<List<Post>> uiPosts = new MutableLiveData<>();

    public LiveData<List<Post>> getPosts(){
        return uiPosts; //observe와 연결
    }
    private final CloudFunctionManager cloudFunctionManager = new CloudFunctionManager();

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

                Log.d("callAddPoint", "postId: " + post.getId());

                cloudFunctionManager.callAddPoint(post.getId(), new CloudFunctionManager.PointCallback(){

                    @Override
                    public void onSuccess(boolean success, String result) {
                        if(Objects.equals(result, "addPoint")){
                            Log.d("callAddPoint", "success add Point");
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.d("callAddPoint", "failed add Point" + e.getMessage());
                    }
                });
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

    public interface OnPostsListener{
        void onSuccess(List<Post> posts);
        void onError(String errorMessage);
    }
    public void getPostsInUser(List<String> postsId, OnPostsListener listener){
        postRepo.getPostsById(postsId, new PostRepo.OnPostsListener(){
            @Override
            public void onSuccess(List<Post> posts) {
                listener.onSuccess(posts);
            }
            @Override
            public void onError(String errorMessage) {
                listener.onError(errorMessage);
            }
        });
    }

    public void addRetweetPost(Post post, String uid, List<Content> contentList, Post originalPost, List<Content> originalContentList
            , OnCompleteListener<Void> listener){
        postRepo.addRetweetPost(post, uid, contentList, originalPost, originalContentList,
                task -> listener.onComplete(Tasks.forResult(null)));
    }
}
