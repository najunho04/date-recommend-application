package com.example.datecourserecommendapplication.RecycerView;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.example.datecourserecommendapplication.DB.Post;

import java.util.Objects;

public class PostDiffCallback extends DiffUtil.ItemCallback<Post>{

    @Override
    public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
        // 같은 항목인지 (id 기준)
        return Objects.equals(oldItem.getId(), newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
        // 화면에 보여지는 실제 내용 동일한지
        return oldItem.getTitle().equals(newItem.getTitle())
                && oldItem.getLikesCount() == newItem.getLikesCount()
                && oldItem.getCommentsCount() == newItem.getCommentsCount()
                && oldItem.getRetweetCount() == newItem.getRetweetCount();
    }

    @Override
    public Object getChangePayload(@NonNull Post oldItem, @NonNull Post newItem) {
        // 부분 변경 최적화용: 변경된 필드 정보를 반환할 수 있음
        Bundle diff = new Bundle();
        if (!oldItem.getTitle().equals(newItem.getTitle())) diff.putBoolean("title", true);
        if (!oldItem.getPreviewText().equals(newItem.getPreviewText())) diff.putBoolean("previewText", true);
        return diff.isEmpty() ? null : diff;
    }
}
