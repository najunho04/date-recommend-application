package com.example.datecourserecommendapplication.Util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.datecourserecommendapplication.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PointLogic {

    public static void showPointConsumeDialog(
            Context context,
            int price,
            int currentPoint,
            Runnable onConfirm
    ) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_point_use, null);

        TextView tvMessage = view.findViewById(R.id.tvMessage);
        TextView tvCurrentPoint = view.findViewById(R.id.tvCurrentPoint);

        tvMessage.setText(price + "p를 소진하여 게시물을 열람하시겠습니까?");
        tvCurrentPoint.setText("현재 보유 포인트: " + currentPoint + "p");

        new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(true)
                .setPositiveButton("예, " + price + "p 사용하기", (dialog, which) -> {
                    if (onConfirm != null) onConfirm.run();
                })
                .setNegativeButton("아니오", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
}
