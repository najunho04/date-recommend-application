package com.najunho.datecourserecommendapplication.RecycerView;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.najunho.datecourserecommendapplication.DB.Content;
import com.najunho.datecourserecommendapplication.R;
import com.kakao.vectormap.GestureType;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraAnimation;
import com.kakao.vectormap.camera.CameraUpdate;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReadContentAdapter extends ListAdapter<Content, ReadContentAdapter.ReadContentViewHolder> {

    public interface OnCLickMapViewDetailListener {
        void onClcik(double lat, double lng);
    }

    private final OnCLickMapViewDetailListener listener;


    //DiffUtil logic
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
            return oldTitle.equals(newTitle) && oldDesc.equals(newDesc);
        }
    };

    public ReadContentAdapter(OnCLickMapViewDetailListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReadContentAdapter.ReadContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_read_content, parent, false);
        Log.d("ReadContentAdapter", "onCreateViewHolder success");
        return new ReadContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReadContentAdapter.ReadContentViewHolder holder, int position) {
        Content item = getItem(position);
        Log.d("ReadContentAdapter", "onCreateViewHolder success");
        holder.bind(item, listener);
    }

    public static class ReadContentViewHolder extends RecyclerView.ViewHolder {
        private int localUriListIndex = 0;
        private List<String> localUriList = new ArrayList<>(Arrays.asList(null, null, null));
        private KakaoMap kakaoMap;
        private LabelLayer centerLayer;
        TextView tvTitle, tvStartTime, tvEndTime, tv_place_name, tv_place_address, tvDescription;
        ImageView imgPreview;
        ImageButton imgBackBtn, imgFrontBtn;
        MapView mapView;
        Button mapDetailBtn;
        public ReadContentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvStartTime = itemView.findViewById(R.id.tvStartTime);
            tvEndTime = itemView.findViewById(R.id.tvEndTime);
            tv_place_address = itemView.findViewById(R.id.tv_place_address);
            tv_place_name = itemView.findViewById(R.id.tv_place_name);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            imgBackBtn = itemView.findViewById(R.id.imgBackBtn);
            imgFrontBtn = itemView.findViewById(R.id.imgFrontBtn);
            mapView = itemView.findViewById(R.id.mapPreview);
            mapDetailBtn = itemView.findViewById(R.id.btnMapDetail);
        }

        @SuppressLint("ClickableViewAccessibility")
        public void bind(Content item, OnCLickMapViewDetailListener listener) {

            localUriList = item.getImageUrl();

            tvTitle.setText(item.getTitle());
            tvStartTime.setText(item.getStartTimeString());
            tvEndTime.setText(item.getEndTimeString());
            tv_place_name.setText(item.getLocation().getName());
            tv_place_address.setText(item.getLocation().getAddress());
            tvDescription.setText(item.getDescription());

            double lat = item.getLocation().getLatitude();
            double lng = item.getLocation().getLongitude();

            mapView.start(mapLifeCycleCallback, new KakaoMapReadyCallback() {
                @Override
                public void onMapReady(@NonNull KakaoMap map) {
                    // 인증 후 API 가 정상적으로 실행될 때 호출됨
                    kakaoMap = map; // 지도 객체 저장
                    Log.d("mapView", "onMapReady");

                    // 드래그(=Pan) 비활성화
                    kakaoMap.setGestureEnable(GestureType.Pan, false);
                    // 줌/회전/틸트 비활성화
                    kakaoMap.setGestureEnable(GestureType.Zoom, false);
                    kakaoMap.setGestureEnable(GestureType.Rotate, false);
                    kakaoMap.setGestureEnable(GestureType.Tilt, false);

                    centerLayer = kakaoMap.getLabelManager().getLayer();
                    moveMap(lat, lng);
                }
                @Override
                public int getZoomLevel() {
                    return 17;
                }
            });

            //img setup
            if (localUriList.get(localUriListIndex) != null) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl().get(localUriListIndex))
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(imgPreview);
            } else {
                imgPreview.setImageDrawable(null);
            }

            //back img btn setup
            imgBackBtn.setOnClickListener(v -> {
                if (localUriListIndex == 0) {
                    return;
                } else {
                    localUriListIndex -= 1;
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

            //front img btn setup
            imgFrontBtn.setOnClickListener(v -> {
                if (localUriListIndex == 2) {
                    return;
                } else {
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

            mapDetailBtn.setOnClickListener(v -> {
                listener.onClcik(lat, lng);
            });

        }

        private final MapLifeCycleCallback mapLifeCycleCallback = new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                // 지도 API 가 정상적으로 종료될 때 호출됨
                Log.d("mapView", "onMapDestroy");
            }
            @Override
            public void onMapError(Exception e) {
                // 인증 실패 및 지도 사용 중 에러가 발생할 때 호출됨
                Log.d("mapView", "onMapError: " + e.getMessage());
            }
        };

        public void moveMap(double lat, double lng){

            if(kakaoMap==null){
                Log.d("moveMap", "kakaoMap is null. Wait for onMapReady callback.");
                return;
            }

            LatLng targetPosition = LatLng.from(lat, lng);
            CameraUpdate move = CameraUpdateFactory.newCenterPosition(targetPosition);
            kakaoMap.moveCamera(move, CameraAnimation.from(500, true, true));

            LabelStyle iconStyle = LabelStyle.from(R.drawable.my_pin_32px);
            LabelStyles styles = kakaoMap.getLabelManager()
                    .addLabelStyles(LabelStyles.from(iconStyle));

            LabelOptions options = LabelOptions.from(targetPosition)
                    .setStyles(styles);

            Label label = centerLayer.addLabel(options);

            label.show();
        }
    }
}
