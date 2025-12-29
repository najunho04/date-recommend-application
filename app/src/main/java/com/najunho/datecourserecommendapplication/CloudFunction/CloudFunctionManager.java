package com.najunho.datecourserecommendapplication.CloudFunction;

import android.util.Log;

import androidx.annotation.NonNull;

import com.najunho.datecourserecommendapplication.DB.Post;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudFunctionManager {
    private final FirebaseFunctions functions;

    public CloudFunctionManager() {
        functions = FirebaseFunctions.getInstance();
    }

    public void callGetPostsUnified(String sort, List<String> categories, List<String> locations, CloudFunctionCallback callback){
        Map<String, Object> data = new HashMap<>();
        data.put("sort", sort);
        data.put("categories", categories);
        data.put("locations", locations);
        data.put("limit", 50);

        FirebaseAuth.getInstance().getAccessToken(true)
                .addOnSuccessListener(token -> {
                    functions.getHttpsCallable("getPostsUnified")
                            .call(data)
                            .addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                                @Override
                                public void onSuccess(HttpsCallableResult httpsCallableResult) {
                                    Map<String, Object> result = (Map<String, Object>) httpsCallableResult.getData();

                                    Boolean success = (Boolean) result.get("success");
                                    if (success == null || !success) {
                                        return;
                                    }

                                    List<Map<String, Object>> postsMapList =
                                            (List<Map<String, Object>>) result.get("posts");

                                    Log.d("callGetPostsUnified", "postsMapList: " + postsMapList);
                                    List<Post> posts = new ArrayList<>();

                                    for (Map<String, Object> item : postsMapList) {
                                        Gson gson = new Gson();
                                        Post post = gson.fromJson(gson.toJson(item), Post.class);
                                        posts.add(post);
                                        Log.d("callGetPostsUnified", "post: " + post);
                                    }
                                    callback.onSuccess(success, posts);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    callback.onError(e);
                                }
                            });
                });
    }

    //제목,소제목 검색 로직
    public void callSearchPosts(String keyword, searchPostsCallback callback){
        Map<String, Object> data = new HashMap<>();
        data.put("keyword", keyword);
        functions.getHttpsCallable("searchPosts")
                .call(data)
                .addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                    @Override
                    public void onSuccess(HttpsCallableResult httpsCallableResult) {
                        Map<String, Object> result = (Map<String, Object>) httpsCallableResult.getData();

                        Boolean success = (Boolean) result.get("success");
                        if (success == null || !success) {
                            return;
                        }
                        Log.d("callSearchPosts", success.toString());

                        List<String> postIds = (List<String>) result.get("postIds");

                        callback.onSuccess(success, postIds);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }

    public void callCheckPurchase(String postId, PointCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);

        functions.getHttpsCallable("checkPurchase")
                .call(data)
                .addOnSuccessListener(httpsCallableResult -> {

                    Map<String, Object> result = (Map<String, Object>) httpsCallableResult.getData();

                    Boolean success = (Boolean) result.get("success");
                    String resultData = result.get("result").toString();

                    Log.d("callCheckPurchase", "result : " + resultData);
                    callback.onSuccess(success, resultData);
                }).addOnFailureListener(e -> Log.d("callCheckPurchase", "error : " + e));
    }

    public void callPurchasePost(String postId, PointCallback callback){
        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);

        functions.getHttpsCallable("purchasePost")
                .call(data)
                .addOnSuccessListener(httpsCallableResult -> {

                    Map<String, Object> result = (Map<String, Object>) httpsCallableResult.getData();

                    Boolean success = (Boolean) result.get("success");
                    String resultData = result.get("result").toString();

                    Log.d("callPurchasePost", "result : " + resultData);
                    callback.onSuccess(success, resultData);
                }).addOnFailureListener(e -> Log.d("callPurchasePost", "error : " + e));
    }

    public void callAddPoint(String postId, PointCallback callback){
        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);

        functions.getHttpsCallable("addPoint")
                .call(data)
                .addOnSuccessListener(httpsCallableResult -> {
                    Map<String, Object> result = (Map<String, Object>) httpsCallableResult.getData();

                    Boolean success = (Boolean) result.get("success");
                    String resultData = result.get("result").toString();

                    Log.d("callPurchasePost", "result : " + resultData);
                    callback.onSuccess(success, resultData); //resultData : addPoint시 성공

                }).addOnFailureListener(e -> Log.d("callPurchasePost", "error : " + e));
    }

    public void callGetPostsInBoundary(double minLat, double maxLat, double minLng, double maxLng, CloudFunctionCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("minLat", minLat);
        data.put("maxLat", maxLat);
        data.put("minLng", minLng);
        data.put("maxLng", maxLng);

        functions.getHttpsCallable("getPostsInBoundary")
                .call(data)
                .addOnSuccessListener(httpsCallableResult -> {
                    List<Post> posts = new ArrayList<>();
                    List<Map<String, Object>> result = (List<Map<String, Object>>) httpsCallableResult.getData();
                    for (Map<String, Object> item : result){
                        Gson gson = new Gson();
                        Post post = gson.fromJson(gson.toJson(item), Post.class);
                        posts.add(post);
                    }
                    Log.d("callGetPostsInBoundary", "posts: " + posts);
                    callback.onSuccess(true, posts);

                }).addOnFailureListener(callback::onError);
    }

    public void callVerifyPurchase(String purchaseToken, String productId, String orderId, PointCallback callback){
        Map<String, Object> data = new HashMap<>();
        data.put("purchaseToken", purchaseToken);
        data.put("productId", productId);
        data.put("orderId", orderId);

        functions.getHttpsCallable("verifyPurchase")
                .call(data)
                .addOnSuccessListener(httpsCallableResult -> {

                    Map<String, Object> result = (Map<String, Object>) httpsCallableResult.getData();
                    Boolean success = (Boolean) result.get("success");
                    String message = result.get("message").toString();
                    String verifiedProductId = result.get("verifiedProductId").toString();

                    Log.d("callVerifyPurchase", "callVerifyPurchase:" + verifiedProductId);
                    callback.onSuccess(success, message);
                }).addOnFailureListener(e -> Log.d("callVerifyPurchase", "failed: " + e));
    }

    public interface CloudFunctionCallback {
        void onSuccess(boolean success, List<Post> posts);
        void onError(Exception e);
    }
    public interface searchPostsCallback{
        void onSuccess(boolean success, List<String> postIds);
        void onError(Exception e);
    }

    public interface PointCallback{
        void onSuccess(boolean success, String result);
        void onError(Exception e);
    }
}
