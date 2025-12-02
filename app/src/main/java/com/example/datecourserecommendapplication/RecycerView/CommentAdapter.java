package com.example.datecourserecommendapplication.RecycerView;

import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.DB.Comment;
import com.example.datecourserecommendapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class CommentAdapter extends ListAdapter<Comment, CommentAdapter.CommentViewHolder> {

    public interface OnCommentClickListener {
        void onProfileClick(Comment comment);
        void onLongClick(Comment comment);
        void onEditBtnClick(Comment comment, Editable newCommentContent);
        void onEditBtnClickDifferentUser();
        void onDeleteBtnClick(Comment comment);
    }

    private final OnCommentClickListener listener;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    private static final DiffUtil.ItemCallback<Comment> DIFF_CALLBACK = new DiffUtil.ItemCallback<Comment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return oldItem.getCommentId().equals(newItem.getCommentId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return oldItem.getContent().equals(newItem.getContent())
                    && oldItem.getAuthorName().equals(newItem.getAuthorName());
        }
    };

    public CommentAdapter(OnCommentClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        Log.d("observe test", "onCreating...");
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Log.d("observe test", "onBinding...");
        holder.bind(getItem(position));
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAuthor;
        TextView tvAuthorName, tvContent, tvCreatedAt;
        EditText tvCommentEditField;
        Button tvBtnEdit, tvBtnDelete;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAuthor = itemView.findViewById(R.id.commentAuthorImage);
            tvAuthorName = itemView.findViewById(R.id.commentAuthorName);
            tvContent = itemView.findViewById(R.id.commentContent);
            tvCommentEditField = itemView.findViewById(R.id.commentEditField);
            tvCreatedAt = itemView.findViewById(R.id.commentCreatedAt);
            tvBtnEdit = itemView.findViewById(R.id.btnEdit);
            tvBtnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Comment comment) {
            tvAuthorName.setText(comment.getAuthorName());
            tvContent.setText(comment.getContent());

            // 작성 시간 포맷
            if (comment.getCreatedAt() != null) {
                String dateStr = new SimpleDateFormat("MM/dd HH:mm", Locale.KOREA)
                        .format(comment.getCreatedAt().toDate());
                tvCreatedAt.setText(dateStr);
            }

            // 클릭 리스너
            imgAuthor.setOnClickListener(v -> {
                if (listener != null) listener.onProfileClick(comment);
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(comment);
                return true;
            });
            tvBtnEdit.setOnClickListener(v->{
                if(user.getUid().equals(comment.getAuthorId())){
                    if(tvCommentEditField.getVisibility() == View.GONE){
                        //일반 상태
                        tvCommentEditField.setVisibility(View.VISIBLE);
                        tvContent.setVisibility(View.GONE);
                        tvBtnEdit.setText("저장");
                        Log.d("onEditBtnClick", "일반 -> 수정 상태 전환");
                    }else if(tvCommentEditField.getVisibility() == View.VISIBLE){
                        //수정 상태
                        tvCommentEditField.setVisibility(View.GONE);
                        tvContent.setVisibility(View.VISIBLE);
                        tvBtnEdit.setText("수정");
                        listener.onEditBtnClick(comment, tvCommentEditField.getText());
                        Log.d("onEditBtnClick", "일반 -> 수정 상태 전환");
                    }
                }else {
                    listener.onEditBtnClickDifferentUser();
                }
            });
            tvBtnDelete.setOnClickListener(v->{
                if (listener != null) listener.onDeleteBtnClick(comment);
            });
        }
    }
}
