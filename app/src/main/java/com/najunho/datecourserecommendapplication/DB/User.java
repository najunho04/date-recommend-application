package com.najunho.datecourserecommendapplication.DB;

import com.google.firebase.Timestamp;

import java.util.List;

// Firestore에 저장할 User 모델 클래스
public class User {

    private String nickname;            // 닉네임
    private String location;            // 지역
    private String gender;              // 성별
    private int age;                    // 나이
    private List<String> interests;     // 흥미 리스트
    private List<String> likesPost;     // 좋아요한 게시물 ID 리스트
    private Timestamp createdAt;        // 가입 시간
    private List<String> comments;      // 내가 쓴 댓글 ID 리스트
    private List<String> postsId;       // 내가 쓴 게시물 ID 리스트
    private int myPoint;

    //Firestore 직렬화를 위해 반드시 빈 생성자가 필요함
    public User() {}

    //생성자
    public User(String nickname, String location, String gender, int age,
                List<String> interests) {
        this.nickname = nickname;
        this.location = location;
        this.gender = gender;
        this.age = age;
        this.interests = interests;
    }

    // Getter & Setter
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public List<String> getLikesPost() { return likesPost; }
    public void setLikesPost(List<String> likesPost) { this.likesPost = likesPost; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public List<String> getComments() { return comments; }
    public void setComments(List<String> comments) { this.comments = comments; }

    public List<String> getPostsId() { return postsId; }
    public void setPostsId(List<String> postsId) { this.postsId = postsId; }
    public int getMyPoint(){return myPoint;}
    public void setMyPoint(int myPoint){this.myPoint = myPoint;}
}
