package com.najunho.datecourserecommendapplication.Util;

import android.app.Activity;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class InterestLogic {
    public void getInterests(Activity activity, List<String> selectedInterests, ChipGroup chipGroupInterests, InterestCallback callback){
        String[] interests = {"카페", "전시회", "산책", "드라이브", "공연", "음식", "영화", "야경", "실내", "기타"};
        boolean[] checked = new boolean[interests.length];
        //selectedInterests(현재 선택되어 있는 interests, chipList와 동일) -> 복제 List 생성
        ArrayList<String> tempInterests = new ArrayList<>(selectedInterests);

        for (int i = 0; i < checked.length; i++) {
            checked[i] = selectedInterests.contains(interests[i]);
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle("카테고리")
                .setMultiChoiceItems(interests, checked, (dialog, which, isChecked) -> {
                    if(isChecked){
                        if (tempInterests.size() >= 3){
                            //선택 3개 초과 시 경고
                            Toast.makeText(activity, "최대 3개까지만 선택할 수 있습니다.", Toast.LENGTH_SHORT).show();
                            checked[which] = false;
                            //잠시 대기 후 버튼 취소 -> 내부 토글로 버튼 취소가 안 될 수 있기 때문
                            ((AlertDialog)dialog).getListView().post(()
                                    -> ((AlertDialog)dialog).getListView().setItemChecked(which, false));
                        }else {
                            tempInterests.add(interests[which]);
                            checked[which] = true;
                        }
                    }else {
                        tempInterests.remove(interests[which]);
                        checked[which] = false;
                    }
                })
                .setPositiveButton("확인", (dialog, which) ->{
                    chipGroupInterests.removeAllViews();

                    for (String interest : tempInterests) {
                        Chip chip = new Chip(activity);
                        chip.setText(interest);
                        chip.setCloseIconVisible(true);

                        final String interestName = interest;

                        chip.setOnCloseIconClickListener(v2 -> {
                            chipGroupInterests.removeView(chip);
                            tempInterests.remove(interestName);
                            callback.onChipClose(tempInterests);
                        });
                        chipGroupInterests.addView(chip);
                    }
                    callback.onChipConfirm(tempInterests);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    public interface InterestCallback{
        void onChipConfirm(List<String> interests);
        void onChipClose(List<String> interests);
    }
}
