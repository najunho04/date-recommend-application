package com.example.datecourserecommendapplication.ViewModel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.datecourserecommendapplication.DB.Comment;
import com.example.datecourserecommendapplication.DB.CommentRepo;
import com.example.datecourserecommendapplication.DB.Post;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;

import java.util.List;

public class CommentViewModel extends ViewModel {
    private final CommentRepo commentRepo = ApplicationUtil.getCommentRepo();

    public LiveData<List<Comment>> getComments(String postId) {
        return commentRepo.getComment(postId);
    }

    public void addComment(String postId, Comment comment, String uid) {
        commentRepo.addComment(postId, comment, uid);
    }

    public void deleteComment(String postId, String commentId, String uid) {
        commentRepo.deleteComment(postId, commentId, uid);
    }

    public void updateComment(String postId, String commentId, String newContent) {
        commentRepo.updateComment(postId, commentId, newContent);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        commentRepo.removeCommentsObserver();
    }
}
