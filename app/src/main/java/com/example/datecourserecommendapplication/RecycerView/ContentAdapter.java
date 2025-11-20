package com.example.datecourserecommendapplication.RecycerView;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datecourserecommendapplication.DB.Comment;
import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.Location;
import com.example.datecourserecommendapplication.R;

import java.util.ArrayList;
import java.util.List;

public class ContentAdapter extends ListAdapter<Content, ContentAdapter.ContentViewHolder> {
    private boolean isEditableMode; // 화면에서 수정 가능 여부 -> True : WritePost,updatePost / False : retweetPost

    //리스너 구현 -> 리사이클러뷰 아이템 클릭 리스너
    public interface OnContentActionListener {
        void onDeleteClick(int position);
        void onSelectImageClick();
        void onIsCoreClick(int position);
        void onSelectLocation(int position);
    }
    private OnContentActionListener listener;
    public void setOnContentActionListener(OnContentActionListener listener) {
        this.listener = listener;
    }

    //DiffUtil logic -> Object.equals() 대신 사용 예정
    private static final DiffUtil.ItemCallback<Content> DIFF_CALLBACK = new DiffUtil.ItemCallback<Content>() {
        @Override
        public boolean areItemsTheSame(@NonNull Content oldItem, @NonNull Content newItem) {
            String oldId = oldItem.getContentId();
            String newId = newItem.getContentId();
            return oldId != null && oldId.equals(newId);
        }
        @Override
        public boolean areContentsTheSame(@NonNull Content oldItem, @NonNull Content newItem) {
            String oldTitle = oldItem.getTitle() != null ? oldItem.getTitle() : "";
            String newTitle = newItem.getTitle() != null ? newItem.getTitle() : "";
            String oldDesc = oldItem.getDescription() != null ? oldItem.getDescription() : "";
            String newDesc = newItem.getDescription() != null ? newItem.getDescription() : "";
            Location oldLoc = oldItem.getLocation() != null ? oldItem.getLocation() : new Location("", "", 0, 0);
            Location newLoc = newItem.getLocation() != null ? newItem.getLocation() : new Location("new", "", 0, 0);
            //Location Class타입은 equal() 불가함으로 Location 필드들을 비교 -> 추후 equal() override해서 사용

            Log.d("areContentsTheSame", "oldLocation: " + oldLoc.toString());
            Log.d("areContentsTheSame", "newLocation: " + newLoc.toString());

            String oldName = oldLoc.getName() != null ? oldLoc.getName() : "";
            String newName = newLoc.getName() != null ? newLoc.getName() : "";

            String oldAddr = oldLoc.getAddress() != null ? oldLoc.getAddress() : "";
            String newAddr = newLoc.getAddress() != null ? newLoc.getAddress() : "";
            boolean isLocationSame = oldName.equals(newName) &&
                    oldAddr.equals(newAddr);
            Log.d("areContentsTheSame", "titlesTheSame: " + oldTitle.equals(newTitle));
            Log.d("areContentsTheSame", "descriptionsTheSame: " + oldDesc.equals(newDesc));
            Log.d("areContentsTheSame", "locationsTheSame: " + isLocationSame);

            return oldTitle.equals(newTitle) && oldDesc.equals(newDesc) && isLocationSame;
        }
    };
    public ContentAdapter(boolean isEditableMode, OnContentActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.isEditableMode = isEditableMode;
    }

    @NonNull
    @Override
    public ContentAdapter.ContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_content, parent, false);
        Log.d("ContentAdapter", "onCreateViewHolder success");
        return new ContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContentAdapter.ContentViewHolder holder, int position) {
        Content item = getItem(position);
        if (!isEditableMode && item.getIsCore()) {
            //리트윗 창 : original Post에서 핵심 데이트 코스들은 수정 불가
            Log.d("ContentAdapter", "onCreateViewHolder success, bindWhenIsCoreTrue");
            holder.bindWhenIsCoreTrue(item); // 수정 불가 모드 -> 해당 item: 부모 post의 핵심 데이트 코스
        } else if (item.getOriginalContentId() != null) {
            //자식 post 업데이트 창 : originalPost 핵심 데이트 코스들은 수정 불가
            holder.bindWhenIsCoreTrue(item); // 수정 불가 모드
        } else {
            Log.d("ContentAdapter", "onBindViewHolder success");
            holder.bind(item); // 기본 모드 (WritePost)
        }
    }

    public class ContentViewHolder extends RecyclerView.ViewHolder {
        // item_content.xml에 있는 View들
        ImageButton btnDelete, btnIsCore;
        EditText editTitle, editDescription, editStartTime, editEndTime;
        TextView tvLocation;
        ImageView imgPreview;
        Button btnSelectImage;

        public ContentViewHolder(@NonNull View itemView) {
            super(itemView);

            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnIsCore = itemView.findViewById(R.id.btnIsCore);
            editTitle = itemView.findViewById(R.id.editTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            editDescription = itemView.findViewById(R.id.editDescription);
            editStartTime = itemView.findViewById(R.id.tvStartTime);
            editEndTime = itemView.findViewById(R.id.tvEndTime);

            imgPreview = itemView.findViewById(R.id.imgPreview);
            btnSelectImage = itemView.findViewById(R.id.btnSelectImage);
        }

        public void bind(Content item) {

            //현재 isCore 상태에 따라 초기 tint 설정
            btnIsCore.setColorFilter(
                    item.getIsCore() ? Color.RED : Color.GRAY,
                    PorterDuff.Mode.SRC_IN
            );

            // 현재는 값만 세팅 (로직은 추후 구현 예정)
            editTitle.setText(item.getTitle());
            tvLocation.setText(
                    item.getLocation().getName() != null ? item.getLocation().getName() : ""
            );
            editDescription.setText(item.getDescription());
            editStartTime.setText(
                    item.getStartTimeString() != null ? item.getStartTimeString() : ""
            );
            editEndTime.setText(
                    item.getEndTimeString() != null ? item.getEndTimeString() : ""
            );

            // 시간은 String → TimeStamp 변환 로직 나중에 구현
            editStartTime.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    getItem(getLayoutPosition()).setStartTimeString(charSequence.toString());
                }
            });
            editEndTime.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    getItem(getLayoutPosition()).setEndTimeString(charSequence.toString());
                }
            });

            // 이미지 로딩 로직도 추후 글라이드로 추가 예정
            // Glide.with(holder.itemView).load(item.getImageUrl()).into(holder.imgPreview);

            // TextWatcher를 등록하여 입력 시 Content 객체에 바로 반영
            editTitle.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    getItem(getLayoutPosition()).setTitle(s.toString());
                }
            });

            //Description Text
            editDescription.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    getItem(getLayoutPosition()).setDescription(s.toString());
                }
            });

            //location 작성 창으로 이동
            tvLocation.setOnClickListener(v->{
                if (listener != null) listener.onSelectLocation(getLayoutPosition()); //location 선택
            });

            // 버튼 클릭 리스너 연결 (구조만)
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(getLayoutPosition()); //item view 삭제
            });
            btnSelectImage.setOnClickListener(v -> {
                if (listener != null) listener.onSelectImageClick(); //이미지 선택
            });
            btnIsCore.setOnClickListener(v -> {
                boolean newValue = !item.getIsCore();
                item.setIsCore(newValue);   // isCore 값 변경

                // UI 색상 즉시 반영
                btnIsCore.setColorFilter(
                        newValue ? Color.RED : Color.GRAY,
                        PorterDuff.Mode.SRC_IN
                );
                if (listener != null) listener.onIsCoreClick(getLayoutPosition());
            });
        }

        private void bindWhenIsCoreTrue(Content item) {
            //현재 isCore 상태에 따라 초기 tint 설정
            btnIsCore.setColorFilter(
                    item.getIsCore() ? Color.RED : Color.GRAY,
                    PorterDuff.Mode.SRC_IN
            );
            // 현재는 값만 세팅 (로직은 추후 구현 예정)
            editTitle.setText(item.getTitle());
            tvLocation.setText(item.getLocation().getName());
            editDescription.setText(item.getDescription());
            editStartTime.setText(item.getStartTimeString());
            editEndTime.setText(item.getEndTimeString());

            // 2) 모든 EditText 수정 불가
            editTitle.setEnabled(false);
            editTitle.setFocusable(false);
            editTitle.setKeyListener(null);

            tvLocation.setEnabled(false);
            tvLocation.setFocusable(false);
            tvLocation.setKeyListener(null);

            editDescription.setEnabled(false);
            editDescription.setFocusable(false);
            editDescription.setKeyListener(null);

            editStartTime.setEnabled(false);
            editStartTime.setFocusable(false);
            editStartTime.setKeyListener(null);

            editEndTime.setEnabled(false);
            editEndTime.setFocusable(false);
            editEndTime.setKeyListener(null);

            // 3) 버튼 비활성화
            btnDelete.setEnabled(false);
            btnSelectImage.setEnabled(false);
            btnIsCore.setEnabled(false);
        }
    }

    // 단순화한 TextWatcher (다른 메서드는 빈 구현)
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void afterTextChanged(Editable s) {}
    }
    public List<Content> getContentList() {
        return new ArrayList<>(getCurrentList());
    }
}
