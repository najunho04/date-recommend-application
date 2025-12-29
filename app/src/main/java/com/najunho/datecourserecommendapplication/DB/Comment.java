package com.najunho.datecourserecommendapplication.DB;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;

public class Comment implements Serializable {

    @Exclude
    private String commentId;       // Firestore 문서 ID
    private String postId;          // 연결된 게시글 ID
    private String authorId;        // 작성자 UID
    private String authorName;      // 작성자 이름
    private String content;         // 댓글 내용

    @ServerTimestamp
    private Timestamp createdAt;    // 작성 시각

    @ServerTimestamp
    private Timestamp updatedAt;

    // 🔹 Firestore용 기본 생성자 (필수)
    public Comment() {}

    // 🔹 생성자 (댓글 작성 시 사용)
    public Comment(String postId, String authorId, String authorName, String authorImageUrl, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
    }

    // ✅ Getter & Setter
    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getContent(){return content;}
    public void setContent(String content){this.content = content;}

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // 🔹 Firestore에 저장할 때 ID 자동 제외
    @Exclude
    public boolean isValid() {
        return content != null && !content.trim().isEmpty();
    }
}
