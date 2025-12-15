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

import com.bumptech.glide.Glide;
import com.example.datecourserecommendapplication.DB.Content;
import com.example.datecourserecommendapplication.DB.Location;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.Util.ItemTouchHelperListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ContentAdapter extends ListAdapter<Content, ContentAdapter.ContentViewHolder>
        implements ItemTouchHelperListener {

    //드래그 리스너 구현
    @Override
    public boolean onItemMove(int fromPos, int toPos) {
        if (fromPos < getItemCount() && toPos < getItemCount()) {
            List<Content> newList = getDeepCopiedList(); //이새끼 때문인듯?
            Log.d("before onItemMove", newList.toString());
            Collections.swap(newList, fromPos, toPos); // 순서 변경
            Log.d("onItemMove", newList.toString());
            submitList(newList); // 리스트 재반영
            Log.d("onItemMove after submitList", newList.toString());
            return true;
        }
        return false;
    }

    private List<Content> getDeepCopiedList() {
        List<Content> copy = new ArrayList<>();
        Log.d("getDeepCopiedList", getCurrentList().toString());
        for (Content c : getCurrentList()) {
            copy.add(new Content(c)); // copy constructor 필요
        }
        Log.d("getDeepCopiedList",copy.toString());
        return copy;
    }

    private boolean isEditableMode; // 화면에서 수정 가능 여부 -> True : WritePost,updatePost / False : retweetPost

    //리스너 구현 -> 리사이클러뷰 아이템 클릭 리스너
    public interface OnContentActionListener {
        void onDeleteClick(int position);
        void onSelectImageClick(int position, int localUriListIndex, List<String> localUriList);
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
            //title 필드 비교
            String oldTitle = oldItem.getTitle() != null ? oldItem.getTitle() : "";
            String newTitle = newItem.getTitle() != null ? newItem.getTitle() : "";

            //Description 필드 비교
            String oldDesc = oldItem.getDescription() != null ? oldItem.getDescription() : "";
            String newDesc = newItem.getDescription() != null ? newItem.getDescription() : "";

            //Uri 필드 비교
            List<String> oldUriList = oldItem.getImageUrl();
            List<String> newUriList = newItem.getImageUrl();

            //Location 비교를 위한 객체 생성
            Location oldLoc = oldItem.getLocation() != null ? oldItem.getLocation() : new Location("", "", 0, 0);
            Location newLoc = newItem.getLocation() != null ? newItem.getLocation() : new Location("", "", 0, 0);

            //Location 필드 비교 (equal() override 안 됐을 때)
            String oldName = oldLoc.getName() != null ? oldLoc.getName() : "";
            String newName = newLoc.getName() != null ? newLoc.getName() : "";

            String oldAddr = oldLoc.getAddress() != null ? oldLoc.getAddress() : "";
            String newAddr = newLoc.getAddress() != null ? newLoc.getAddress() : "";

            boolean isLocationSame = oldName.equals(newName) && oldAddr.equals(newAddr);

            Log.d("DiffUtil", "oldUriList: " + oldUriList);
            Log.d("DiffUtil", "newUriList: " + newUriList);
            Log.d("DiffUtil", "result: " + Objects.equals(oldUriList, newUriList) );

            // 최종 비교 / 참고: Qbject.equals 사용하면 null 체크 필요 없음
            return Objects.equals(oldTitle, newTitle)
                    && Objects.equals(oldDesc, newDesc)
                    //&& Objects.equals(oldItem.getLocation(), newItem.getLocation())
                    && isLocationSame
                    && Objects.equals(oldUriList, newUriList);
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
        if (!isEditableMode && item.getIsCore() && item.getOriginalContentId() != null) {
            //리트윗 창 전용
            holder.bindWhenIsCoreTrue(item); //수정 불가 Bind
        } else if (item.getOriginalContentId() != null) {
            //자식 Post -> parentContent Edit 창 전용
            holder.bindWhenIsCoreTrue(item);
        } else {
            holder.bind(item); // 기본 모드 (WritePost)
        }
    }

    public class ContentViewHolder extends RecyclerView.ViewHolder {
        // item_content.xml에 있는 View들

        private int localUriListIndex;
        private List<String> localUriList;
        ImageButton btnDelete, btnIsCore, imgBackBtn, imgFrontBtn;
        EditText editTitle, editDescription, editStartTime, editEndTime;
        TextView tvLocation;
        ImageView imgPreview;
        Button btnSelectImage;

        public ContentViewHolder(@NonNull View itemView) {
            super(itemView);

            localUriListIndex = 0;
            localUriList = new ArrayList<>(Arrays.asList(null, null, null));

            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnIsCore = itemView.findViewById(R.id.btnIsCore);
            editTitle = itemView.findViewById(R.id.editTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            editDescription = itemView.findViewById(R.id.editDescription);
            editStartTime = itemView.findViewById(R.id.tvStartTime);
            editEndTime = itemView.findViewById(R.id.tvEndTime);

            imgPreview = itemView.findViewById(R.id.imgPreview);
            btnSelectImage = itemView.findViewById(R.id.btnSelectImage);
            imgBackBtn = itemView.findViewById(R.id.imgBackBtn);
            imgFrontBtn = itemView.findViewById(R.id.imgFrontBtn);
        }

        public void bind(Content item) {

            localUriList = item.getImageUrl();

            //현재 isCore 상태에 따라 초기 tint 설정
            btnIsCore.setColorFilter(
                    item.getIsCore() ? Color.YELLOW : Color.GRAY,
                    PorterDuff.Mode.SRC_IN
            );

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

            //Glide : Uri -> String이여도 변환후 자동 반영 -> 개섹스
            if (localUriList.get(localUriListIndex) != null) {
                Glide.with(itemView.getContext())
                        .load(localUriList.get(localUriListIndex))
                        .into(imgPreview);
            } else {
                imgPreview.setImageDrawable(null);
            }

            imgBackBtn.setOnClickListener(v->{
                if(localUriListIndex == 0){
                    return;
                }else{
                    localUriListIndex -= 1 ;
                    Log.d("localUriListIndex", "localUriListIndex: " + localUriListIndex);
                    if (localUriList.get(localUriListIndex) != null) {
                        Glide.with(itemView.getContext())
                                .load(localUriList.get(localUriListIndex))
                                .into(imgPreview);
                    } else {
                        imgPreview.setImageDrawable(null);
                    }
                }
            });

            imgFrontBtn.setOnClickListener(v->{
                if(localUriListIndex == 2){
                    return;
                }else {
                    localUriListIndex += 1;
                    Log.d("localUriListIndex", "localUriListIndex: " + localUriListIndex);
                    if (localUriList.get(localUriListIndex) != null) {
                        Glide.with(itemView.getContext())
                                .load(item.getImageUrl().get(localUriListIndex))
                                .into(imgPreview);
                    } else {
                        imgPreview.setImageDrawable(null);
                    }
                }
            });

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
                if (listener != null) listener.onSelectImageClick(getLayoutPosition(), localUriListIndex, localUriList); //이미지 선택
            });
            btnIsCore.setOnClickListener(v -> {
                boolean newValue = !item.getIsCore();
                item.setIsCore(newValue);   // isCore 값 변경

                // UI 색상 즉시 반영
                btnIsCore.setColorFilter(
                        newValue ? Color.YELLOW : Color.GRAY,
                        PorterDuff.Mode.SRC_IN
                );
                if (listener != null) listener.onIsCoreClick(getLayoutPosition());
            });
        }

        private void bindWhenIsCoreTrue(Content item) {

            localUriList = item.getImageUrl();

            //현재 isCore 상태에 따라 초기 tint 설정
            btnIsCore.setColorFilter(
                    item.getIsCore() ? Color.YELLOW : Color.GRAY,
                    PorterDuff.Mode.SRC_IN
            );
            // 현재는 값만 세팅 (로직은 추후 구현 예정)
            editTitle.setText(item.getTitle());

            tvLocation.setText(item.getLocation().getName());

            editDescription.setText(item.getDescription());
            editStartTime.setText(item.getStartTimeString());
            editEndTime.setText(item.getEndTimeString());

            if (localUriList.get(localUriListIndex) != null) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .into(imgPreview);
            } else {
                imgPreview.setImageDrawable(null);
            }

            imgBackBtn.setOnClickListener(v->{
                if(localUriListIndex == 0){
                    return;
                }else{
                    localUriListIndex -= 1 ;
                    Log.d("localUriListIndex", "localUriListIndex: " + localUriListIndex);
                    if (localUriList.get(localUriListIndex) != null) {
                        Glide.with(itemView.getContext())
                                .load(item.getImageUrl().get(localUriListIndex))
                                .into(imgPreview);
                    } else {
                        imgPreview.setImageDrawable(null);
                    }
                }
            });

            imgFrontBtn.setOnClickListener(v->{
                if(localUriListIndex == 2){
                    return;
                }else {
                    localUriListIndex += 1;
                    Log.d("localUriListIndex", "localUriListIndex: " + localUriListIndex);
                    if (localUriList.get(localUriListIndex) != null) {
                        Glide.with(itemView.getContext())
                                .load(item.getImageUrl().get(localUriListIndex))
                                .into(imgPreview);
                    } else {
                        imgPreview.setImageDrawable(null);
                    }
                }
            });

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
