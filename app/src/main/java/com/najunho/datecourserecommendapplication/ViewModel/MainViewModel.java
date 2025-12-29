package com.najunho.datecourserecommendapplication.ViewModel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.najunho.datecourserecommendapplication.CloudFunction.CloudFunctionManager;
import com.najunho.datecourserecommendapplication.DB.Content;
import com.najunho.datecourserecommendapplication.DB.Post;
import com.najunho.datecourserecommendapplication.DB.PostRepo;
import com.najunho.datecourserecommendapplication.DB.UserRepo;
import com.najunho.datecourserecommendapplication.Util.ApplicationUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MainViewModel extends ViewModel {
    private FirebaseFirestore db = ApplicationUtil.getFirestore();
    private PostRepo postRepo = ApplicationUtil.getPostRepo();
    private UserRepo userRepo = ApplicationUtil.getUserRepo();

    //postRepo랑 연결되어 있는 postList : DB변경 시 sourcePosts도 변경
    private final LiveData<List<Post>> sourcePosts = postRepo.getPost();

    //필터, 검색, 좋아요.. 등 클라이언트 로직과 연결되어 UI에 반영되는 postList
    private final MutableLiveData<List<Post>> uiPosts = new MutableLiveData<>();

    public LiveData<List<Post>> getUiPosts(){
        return uiPosts; //observe와 연결
    }

    public void getPosts(){
        postRepo.getPosts(new PostRepo.OnPostsListener() {
            @Override
            public void onSuccess(List<Post> posts) {
                uiPosts.setValue(posts);
                Log.d("getPosts", "success");
            }

            @Override
            public void onError(String errorMessage) {
                Log.d("getPosts", "failed");
            }
        });
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

    public void addLike(Post post, String uid, OnCompleteListener<Void> listener){
        List<Post> currentUi = uiPosts.getValue();
        if (currentUi == null) return;

        List<Post> updatedUi = new ArrayList<>();
        Post rollbackSnapshot = null; // 🔥 실패 시 되돌릴 원본

        for (Post p : currentUi) {

            if (!p.getId().equals(post.getId())) {
                updatedUi.add(p);
                continue;
            }

            // 롤백용 스냅샷
            rollbackSnapshot = new Post(p);

            //true -> 이미 좋아요 로직 선반영 중
            if (p.getLikePending()){
                Log.d("addLike", "이미 좋아요 로직 선반영 중");
                return;
            }
            p.setLikePending(true);

            // Post 깊은 복사 (sourcePosts 오염 방지)
            Post copy = new Post(p);

            boolean liked = copy.getLikesBy().contains(uid);

            if (liked) {
                copy.getLikesBy().remove(uid);
                copy.setLikesCount(copy.getLikesCount() - 1);
            } else {
                copy.getLikesBy().add(uid);
                copy.setLikesCount(copy.getLikesCount() + 1);
            }
            updatedUi.add(copy);
        }
        //UI 즉시 반영
        uiPosts.setValue(updatedUi);
        Log.d("addLike", "낙관적 ui 반영 완료");

        Post finalRollbackSnapshot = rollbackSnapshot;

        postRepo.addLike(post.getId(), uid, task -> {
            if (task.isSuccessful()) {
                Log.d("addLike", "좋아요 추가 성공");
                userRepo.addLikesInUser(FirebaseAuth.getInstance().getUid(), post.getId(), new UserRepo.OnUserAddedListener(){
                    @Override
                    public void onSuccess() {
                        Log.d("addLike", "User DB에 좋아요 추가 성공");

                        //likePending -> false 만들어서 활성화 시켜야 함..
                        List<Post> afterUiPosts = uiPosts.getValue();
                        if (afterUiPosts == null) return;

                        List<Post> newPosts = new ArrayList<>();
                        for (Post p: afterUiPosts){
                            if(p.getLikePending()){
                                p.setLikePending(false);
                                newPosts.add(new Post(p));
                            }else {
                                newPosts.add(new Post(p));
                            }
                        }
                        uiPosts.setValue(newPosts);
                        Log.d("addLike", "likePending -> false 완료");
                        Log.d("addLike", "uiPosts: " + uiPosts.getValue());
                        listener.onComplete(Tasks.forResult(null));
                    }
                    @Override
                    public void onError(String errorMessage) {
                        Log.d("addLike", errorMessage);
                    }
                });
            }else {
                Log.e("addLike", "좋아요 추가 실패", task.getException());
                // 실패: UI 롤백
                List<Post> currentRollBackUi = uiPosts.getValue();
                if (currentRollBackUi == null) return;

                List<Post> rolledBack = new ArrayList<>();

                for (Post p : currentRollBackUi) {
                    if (p.getId().equals(post.getId())) {
                        rolledBack.add(finalRollbackSnapshot); // 이전 상태로 복원
                    } else {
                        rolledBack.add(p);
                    }
                }
                uiPosts.setValue(rolledBack);
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
