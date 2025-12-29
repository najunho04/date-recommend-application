package com.najunho.datecourserecommendapplication.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.najunho.datecourserecommendapplication.DB.Comment;
import com.najunho.datecourserecommendapplication.DB.CommentRepo;
import com.najunho.datecourserecommendapplication.Util.ApplicationUtil;

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
