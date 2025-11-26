package com.example.datecourserecommendapplication.DB;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Post implements Serializable {
    private String id; //post마다 고유 id
    private String title;
    private String previewText;
    private String createdBy; // 작성자 UID
    private Timestamp createdAt; //추후 수정 시간까지 추가 예정
    private int likesCount;
    private int commentsCount;
    private int retweetCount;
    private List<String> likesBy;    // 좋아요 누른 유저 UID 리스트
    // comment:  전용 서브 collection 에서 관리
    private List<String> retweetBy;  // 리트윗 누른 유저 UID 리스트
    private String parentPostId; //자신이 부모 post인지 자식 post인지 확인하는 용도 부모 post일때: null / 자식: 부모 postId
    private boolean isRetweeted; //리트윗 확인 목적

    private String thumbnail; //Uri String


    // 🔹 Firestore 직렬화용 기본 생성자 (필수)
    public Post() {}

    // 🔹 생성자
    public Post(String title, String createdBy, Timestamp createdAt) {
        this.title = title;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.likesCount = 0;
        this.commentsCount = 0;
    }

    // 🔹 Getter / Setter

    public String getId(){return id;}
    public void setId(String id){this.id = id;}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPreviewText() { return previewText; }
    public void setPreviewText(String previewText) { this.previewText = previewText; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public int getRetweetCount() {return retweetCount; }
    public void setRetweetCount(int retweetCount) {this.retweetCount = retweetCount;}


    public List<String> getLikesBy() { return likesBy; }
    public void setLikesBy(List<String> likesBy) { this.likesBy = likesBy; }

    public List<String> getRetweetBy() {return retweetBy; }
    public void setRetweetBy(List<String> retweetBy) {this.retweetBy = retweetBy;}

    public String getParentPostId() {
        return parentPostId;
    }

    public void setParentPostId(String parentPostId) {
        this.parentPostId = parentPostId;
    }

    public boolean getIsRetweeted() {
        return isRetweeted;
    }

    public void setRetweet(boolean retweeted) {
        this.isRetweeted = retweeted;
    }

    public String getThumbnail() {return thumbnail;}
    public void setThumbnail(String thumbnail) {this.thumbnail = thumbnail;}



    // 🔹 Firestore에 저장할 때 Map 변환용
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        //id는 firestore 고유 doc Id를 가져올거임.
        map.put("id", id);
        map.put("title", title);
        map.put("createdBy", createdBy);
        map.put("createdAt", createdAt);
        map.put("previewText", previewText);
        map.put("likesCount", likesCount);
        map.put("commentsCount", commentsCount);
        map.put("retweetCount", retweetCount);
        map.put("retweetBy", retweetBy);
        map.put("likesBy", likesBy);
        map.put("isRetweeted", isRetweeted);
        map.put("parentPostId", parentPostId);
        map.put("thumbnail", thumbnail);
        return map;
    }
}