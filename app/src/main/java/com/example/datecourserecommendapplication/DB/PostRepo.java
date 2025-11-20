package com.example.datecourserecommendapplication.DB;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PostRepo {
    private FirebaseFirestore db;
    private final MutableLiveData<List<Post>> postsLiveData = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration registration;
    private UserRepo userRepo = ApplicationUtil.getUserRepo();

    public PostRepo (FirebaseFirestore db){
        this.db = db;
    }
    public interface OnPostListener{
        void onSuccess(Post post);
        void onError(String errorMessage);
    }

    public LiveData<List<Post>> getPost(){
        if (registration == null) startListeningPosts();
        Log.d("getPost", "success");
        return postsLiveData; //업데이트 된 DB viewModel과 연결
    }

    private void startListeningPosts() {
        registration = db.collection("Posts")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("startListeningPosts", "failed" + e);
                        return;
                    }
                    if (querySnapshot != null)  {
                        List<Post> posts = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot) {
                            Post post = doc.toObject(Post.class);
                            post.setId(doc.getId()); // Firestore doc ID 저장
                            posts.add(post);
                        }
                        posts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                        postsLiveData.setValue(posts);
                        //DB 바뀔 때마다 postsLiveData 업데이트, orderBy시 CreateBy만 변경 감시가능 -> 삭제 후 정렬 코드 추가
                        //-> like, comment 등 다른 field까지 감시 가능 but doc 많아질수록 효율감소
                        //=> 따라서 추후 like, comment 등 주요 field 리스너 추가 예정
                    }
                });
    }
    public void stopListening() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
    public void addPost(Post post, String uid, List<Content> contentList, OnPostListener listener){
        WriteBatch batch = db.batch();

        // 1. Post 문서 ID 생성
        CollectionReference postsRef = db.collection("Posts");
        DocumentReference newPostRef = postsRef.document(); // 직접 문서 ID 생성
        String postId = newPostRef.getId(); // ID를 바로 확보
        post.setId(postId);

        // 2. Post 데이터 batch에 추가
        batch.set(newPostRef, post.toMap());

        // 3. Content SubCollection batch에 추가
        for (Content content : contentList) {
            // contentId 미리 생성
            DocumentReference contentRef = newPostRef.collection("Content").document();
            content.setContentId(contentRef.getId());
            // 리트윗 관련 필드 없으면 null 세팅
            if(content.getOriginalPostId() == null) content.setOriginalPostId(null);
            if(content.getOriginalContentId() == null) content.setOriginalContentId(null);
            //order Set
            content.setOrder(contentList.indexOf(content));
            batch.set(contentRef, content.toMap());
        }
        // 4. Batch commit
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("addPost", "Done Add Post");
                    // User DB에 postId 추가 (별도 write)
                    userRepo.addPostIdInUser(uid, postId, new UserRepo.OnUserAddedListener() {
                        @Override
                        public void onSuccess() {
                            listener.onSuccess(post);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            listener.onError(errorMessage);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                });
    }

    public void getPostById(String postId, OnPostListener listener) {
        db.collection("Posts").document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Post post = documentSnapshot.toObject(Post.class);
                        listener.onSuccess(post);
                    } else {
                        listener.onError("Post not found");
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                });
    }
    public void deletePost(String uid, String postId) {
        DocumentReference postRef = db.collection("Posts").document(postId);

        // 1. postDoc snapshot 가져오기
        postRef.get().addOnSuccessListener(documentSnapshot -> {
            if(!documentSnapshot.exists()){
                Log.d("deletePost", "Post not found");
                return;
            }

            String originalPostId = documentSnapshot.getString("parentPostId");

            // 2. batch 생성
            WriteBatch batch = db.batch();

            // 2-1. comments 삭제
            CollectionReference commentsRef = postRef.collection("Comments");
            commentsRef.get().addOnSuccessListener(querySnapshot -> {
                for(DocumentSnapshot doc : querySnapshot){
                    batch.delete(doc.getReference());
                }

                // 2-2. contents 삭제
                CollectionReference contentsRef = postRef.collection("Content");
                contentsRef.get().addOnSuccessListener(q -> {
                    for(DocumentSnapshot doc : q){
                        batch.delete(doc.getReference());
                    }

                    // 2-3. post 삭제
                    batch.delete(postRef);

                    // 3. batch commit
                    batch.commit().addOnSuccessListener(unused -> {
                        Log.d("deletePost", "Batch commit success");

                        // 4. originalPost retweet field 업데이트 (transaction)
                        if(originalPostId != null){
                            DocumentReference originalPostRef = db.collection("Posts").document(originalPostId);
                            db.runTransaction(transaction -> {
                                        DocumentSnapshot snapshot = transaction.get(originalPostRef);
                                        if(!snapshot.exists()) {
                                            Log.d("deletePost", "can't found original post");
                                            return null;
                                        }

                                        List<String> retweetBy = (List<String>) snapshot.get("retweetBy");
                                        if(retweetBy == null) retweetBy = new ArrayList<>();
                                        retweetBy.remove(postId);

                                        Boolean isRetweeted = !retweetBy.isEmpty();
                                        Long retweetCount = (long) retweetBy.size();

                                        transaction.update(originalPostRef, "retweetBy", retweetBy);
                                        transaction.update(originalPostRef, "isRetweeted", isRetweeted);
                                        transaction.update(originalPostRef, "retweetCount", retweetCount);
                                        return null;
                                    }).addOnSuccessListener(t -> Log.d("deletePost", "Retweet update success"))
                                    .addOnFailureListener(e -> Log.d("deletePost", "Retweet update failed: " + e));
                        }

                        // 5. userDB에서 postId 삭제
                        userRepo.deletePostIdInUser(uid, postId, new UserRepo.OnUserAddedListener() {
                            @Override
                            public void onSuccess() {
                                Log.d("deletePost", "User postId delete success");
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.d("deletePost", "User postId delete failed: " + errorMessage);
                            }
                        });

                    }).addOnFailureListener(e -> Log.d("deletePost", "Batch commit failed: " + e));

                });

            });

        }).addOnFailureListener(e -> Log.d("deletePost", "get post failed: " + e));
    }


    public void updatePost(String postId, Post post,List<Content> contentList, OnPostListener listener){
        WriteBatch batch = db.batch();

        DocumentReference postRef = db.collection("Posts").document(postId);
        CollectionReference contentRef = postRef.collection("Content");

        // 1) 기존 콘텐츠 전체 삭제
        contentRef.get().addOnSuccessListener(querySnapshot -> {
            for (DocumentSnapshot doc : querySnapshot) {
                batch.delete(doc.getReference());
            }
            // 2) 새로운 콘텐츠 전체 추가
            for (Content content : contentList) {
                DocumentReference newContentDoc = contentRef.document();
                batch.set(newContentDoc, content.toMap());
            }
            // 3) 게시글 자체 정보 업데이트(제목, 날짜 등)
            batch.update(postRef, post.toMap());
            // 4) batch 커밋
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        listener.onSuccess(post);
                    })
                    .addOnFailureListener(e -> {
                        listener.onError(e.getMessage());
                    });
        });
    }

    public void addLike(String postId, String uid, OnCompleteListener<Void> listener) {
        DocumentReference postRef = db.collection("Posts").document(postId);

        // Firestore 트랜잭션 (likesCount와 likesBy를 동시에 안전하게 업데이트)
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(postRef);

            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("Post not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            // 현재 데이터 가져오기
            Long currentLikes = snapshot.getLong("likesCount");
            List<String> likesBy = (List<String>) snapshot.get("likesBy");

            if (likesBy == null) likesBy = new ArrayList<>();
            if (currentLikes == null) currentLikes = 0L;

            // 이미 좋아요 누른 유저인지 확인 -> 아니면 좋아요 추가
            if (!likesBy.contains(uid)) {
                likesBy.add(uid);
                transaction.update(postRef, "likesCount", currentLikes + 1);
                transaction.update(postRef, "likesBy", likesBy);
                Log.d("addLike", "like add");
            }else {
                likesBy.remove(uid);
                transaction.update(postRef, "likesCount", currentLikes - 1);
                transaction.update(postRef, "likesBy", likesBy);
                Log.d("addLike", "like delete");
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("addLike", "좋아요 추가 성공");
            listener.onComplete(Tasks.forResult(null));
        }).addOnFailureListener(e -> {
            Log.e("addLike", "좋아요 추가 실패", e);
            listener.onComplete(Tasks.forException(e));
        });//addSnapshotListener 작동함 -> get 안해도 작동으로 UI 동적관리

    }

    public void addRetweetPost(Post post, String uid, List<Content> contentList, Post originalPost,
                               List<Content> originalContentList,
                               OnCompleteListener<Void> listener){

        //Retweet 전용 필드 세팅
        for(Content content : contentList){
            content.setOriginalPostId(originalPost.getId());
        }
        post.setParentPostId(originalPost.getId());

        addPost(post, uid, contentList, new OnPostListener() {
            @Override
            public void onSuccess(Post post) {
                DocumentReference originalPostRef = db.collection("Posts").document(originalPost.getId());
                db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(originalPostRef);

                    if (!snapshot.exists()) {
                        throw new FirebaseFirestoreException("user not found", FirebaseFirestoreException.Code.NOT_FOUND);
                    }

                    Boolean isRetweeted = snapshot.getBoolean("isRetweeted");
                    if (isRetweeted == null) {isRetweeted = false;}
                    Long retweetCount = snapshot.getLong("retweetCount");
                    if (retweetCount == null) {retweetCount = 0L;}
                    List<String> retweetBy = (List<String>) snapshot.get("retweetBy");
                    if (retweetBy == null) retweetBy = new ArrayList<>();

                    isRetweeted = true;
                    retweetBy.add(post.getId());
                    retweetCount = (long) retweetBy.size();
                    transaction.update(originalPostRef, "isRetweeted", isRetweeted);
                    transaction.update(originalPostRef, "retweetCount", retweetCount);
                    transaction.update(originalPostRef, "retweetBy", retweetBy);
                    return null;
                }).addOnSuccessListener(t->{
                    Log.d("originalPost Retweet Update", "success");
                    listener.onComplete(Tasks.forResult(null));
                }).addOnFailureListener(e ->{
                    Log.d("originalPost Retweet Update", Objects.requireNonNull(e.getMessage()));
                    listener.onComplete(Tasks.forException(e));
                });
            }
            @Override
            public void onError(String errorMessage) {
                Log.d("addPost",  errorMessage);
            }
        });
    }

    public void deleteRetweetField(DocumentReference post, DocumentReference originalPost){
        DocumentReference originalPostRef = db.collection("Posts").document(originalPost.getId());
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(originalPostRef);

            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException("user not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Boolean isRetweeted = snapshot.getBoolean("isRetweeted");
            if (isRetweeted == null) {isRetweeted = false;}
            Long retweetCount = snapshot.getLong("retweetCount");
            if (retweetCount == null) {retweetCount = 0L;}
            List<String> retweetBy = (List<String>) snapshot.get("retweetBy");
            if (retweetBy == null) retweetBy = new ArrayList<>();

            retweetBy.remove(post.getId());
            isRetweeted = true;
            retweetCount = (long) retweetBy.size();

            //삭제된 post가 마지막 자식 post일 때 -> 더이상 부모 post가 아님 -> 초기화
            if(retweetBy.isEmpty()){
                isRetweeted = false;
                retweetCount = 0L;
                retweetBy = new ArrayList<>();
            }

            transaction.update(originalPostRef, "isRetweeted", isRetweeted);
            transaction.update(originalPostRef, "retweetCount", retweetCount);
            transaction.update(originalPostRef, "retweetBy", retweetBy);
            return null;
        }).addOnSuccessListener(t->{
            Log.d("originalPost Retweet Update", "success");
        }).addOnFailureListener(e ->{
            Log.d("originalPost Retweet Update", Objects.requireNonNull(e.getMessage()));
        });
    }
}
