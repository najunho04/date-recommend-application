package com.example.datecourserecommendapplication.DB;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class Content {

    private String contentId;       // 문서 ID (optional)
    private String title;           // 소제목
    private Timestamp startTime;    // 시작 시간
    private String startTimeString;  // 시작 시간 문자열
    private Timestamp endTime;      // 종료 시간
    private String endTimeString;    // 종료 시간 문자열

    // 장소 정보 구조
    private Location location; //위치정보

    private String imageUrl;        // 이미지 URI
    private String description;     // 부가 설명
    private int order;              // 1~7 정렬 순서
    private boolean isCore; // true면 부모가 “핵심 데이트 코스”로 지정한 것

    // 리트윗 복제 시 원본 추적용 필드
    private String originalPostId; //자기가 오리지널 -> null
    private String originalContentId;

    // 빈 생성자 필수!
    public Content() {}

    public Content(String title,
                   Timestamp startTime,
                   Timestamp endTime,
                   String description) {

        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
    }

    public Content(Content other) {
        this.contentId = other.contentId;
        this.title = other.title;
        this.startTime = other.startTime;
        this.endTime = other.endTime;

        // Location deep copy
        if (other.location != null) {
            this.location = new Location(other.location);
        }

        this.imageUrl = other.imageUrl;
        this.description = other.description;
        this.order = other.order;
        this.originalPostId = other.originalPostId;
        this.originalContentId = other.originalContentId;
        this.isCore = other.isCore;
        this.startTimeString = other.startTimeString;
        this.endTimeString = other.endTimeString;
    }

    // ---------- Getter & Setter ----------
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public String getOriginalPostId() { return originalPostId; }
    public void setOriginalPostId(String originalPostId) { this.originalPostId = originalPostId; }

    public String getStartTimeString() { return startTimeString; }
    public void setStartTimeString(String startTimeString) { this.startTimeString = startTimeString; }

    public String getEndTimeString() { return endTimeString; }
    public void setEndTimeString(String endTimeString) { this.endTimeString = endTimeString; }

    public Boolean getIsCore(){return isCore;}
    public void setIsCore(Boolean isCore){this.isCore = isCore;}

    public String getOriginalContentId() { return originalContentId; }
    public void setOriginalContentId(String originalContentId) { this.originalContentId = originalContentId; }

    // ---------- Map 변환 ----------

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        putIfNotNull(map, "contentId", contentId);
        putIfNotNull(map, "title", title);
        putIfNotNull(map, "startTime", startTime);
        putIfNotNull(map, "startTimeString", startTimeString);
        putIfNotNull(map, "endTime", endTime);
        putIfNotNull(map, "endTimeString", endTimeString);
        putIfNotNull(map, "location", location);
        putIfNotNull(map, "imageUrl", imageUrl); //DB x
        putIfNotNull(map, "description", description);
        putIfNotNull(map, "order", order);
        putIfNotNull(map, "originalPostId", originalPostId);
        putIfNotNull(map, "originalContentId", originalContentId); //DB x
        putIfNotNull(map, "isCore", isCore);

        return map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    @Override
    public String toString(){
        return "Content{" +
                "contentId='" + contentId + '\'' +
                ", title='" + title + '\'' +
                ", startTime=" + (startTime != null ? startTime.toDate() : null) +
                ", startTimeString='" + startTimeString + '\'' +
                ", endTime=" + (endTime != null ? endTime.toDate() : null) +
                ", endTimeString='" + endTimeString + '\'' +
                ", location=" + (location != null ? location.toString() : null) +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", order=" + order +
                ", isCore=" + isCore +
                ", originalPostId='" + originalPostId + '\'' +
                ", originalContentId='" + originalContentId + '\'' +
                '}';
    }
}
