package com.najunho.datecourserecommendapplication.Util;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.najunho.datecourserecommendapplication.R;
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

public class MapViewPreviewDialogFragment extends DialogFragment {
    private KakaoMap kakaoMap;
    private LabelLayer centerLayer;
    private double lat, lng;

    // newInstance 패턴 (생성자 사용 절대 금지)
    public static MapViewPreviewDialogFragment newInstance(double lat, double lng) {
        MapViewPreviewDialogFragment frag = new MapViewPreviewDialogFragment();
        Bundle args = new Bundle();
        args.putDouble("lat", lat);
        args.putDouble("lng", lng);
        frag.setArguments(args);
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d("MapPreviewDialog", "onCreateView");

        View view = inflater.inflate(R.layout.dialog_map_view_preview, container, false);

        MapView mapView = view.findViewById(R.id.previewMapView);
        ImageButton closeBtn = view.findViewById(R.id.btnClosePreviewMap);

        closeBtn.setOnClickListener(v -> dismiss());

        // 좌표 불러오기
        lat = getArguments().getDouble("lat");
        lng = getArguments().getDouble("lng");

        // MapView 시작
        mapView.start(mapLifeCycleCallback, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                kakaoMap = map;
                Log.d("mapPreview", "onMapReady");

                centerLayer = kakaoMap.getLabelManager().getLayer();
                moveMap(lat, lng);
            }

            @Override
            public int getZoomLevel() {
                return 17;
            }
        });

        return view;
    }

    // 다이얼로그 크기 강제 고정 (BottomSheet-like size 유지)
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {

            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT   // xml의 400dp 유지됨
            );

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private final MapLifeCycleCallback mapLifeCycleCallback = new MapLifeCycleCallback() {
        @Override
        public void onMapDestroy() {
            Log.d("mapPreview", "onMapDestroy");
        }

        @Override
        public void onMapError(@NonNull Exception e) {
            Log.d("mapPreview", "onMapError: " + e.getMessage());
        }
    };

    private void moveMap(double lat, double lng) {
        if (kakaoMap == null) {
            Log.d("moveMap", "kakaoMap is null. Wait for onMapReady.");
            return;
        }

        LatLng pos = LatLng.from(lat, lng);

        CameraUpdate move = CameraUpdateFactory.newCenterPosition(pos);
        kakaoMap.moveCamera(move, CameraAnimation.from(500, true, true));

        LabelStyle iconStyle = LabelStyle.from(R.drawable.my_pin_32px);
        LabelStyles styles = kakaoMap.getLabelManager().addLabelStyles(LabelStyles.from(iconStyle));

        LabelOptions options = LabelOptions.from(pos).setStyles(styles);
        Label label = centerLayer.addLabel(options);

        label.show();
        Log.d("moveMap", "label.show()");
    }
}
