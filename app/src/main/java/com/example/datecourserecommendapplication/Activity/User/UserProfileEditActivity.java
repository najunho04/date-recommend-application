package com.example.datecourserecommendapplication.Activity.User;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.datecourserecommendapplication.Activity.MainActivity;
import com.example.datecourserecommendapplication.DB.User;
import com.example.datecourserecommendapplication.DB.UserRepo;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class UserProfileEditActivity extends AppCompatActivity {
    private UserRepo userRepo;
    private FirebaseAuth mAuth;

    private TextInputEditText editNickName, editAge;
    private boolean isNickEditing = false;
    private boolean isAgeEditing = false;
    private TextView tvNickName, tvAge, tvLocation, tvGender, tvInterests;
    private Button btnEditNick, btnEditAge, btnSelectLocation, btnSelectGender
            , btnSelectInterests, btnSaveProfile;
    private ChipGroup chipGroupInterests;
    private User currentUser;

    // 수정 대상 데이터 (Firestore 반영 전까지 임시 보관)
    private String updateLocation, updateGender, updateNickName;
    private int updateAge;
    private ArrayList<String> selectedInterests = new ArrayList<>();
    private ArrayList<String> updateUserInterestsList = new ArrayList<>();

    /*/해야할거
    1. location, Interests uI 로직 구현 -> Done
    2. MVVM 구조 viewModel (LD)+ DiffUtil 활용한 recyclerview
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_edit);
        mAuth = FirebaseAuth.getInstance();

        editNickName = findViewById(R.id.editNickName);
        editAge = findViewById(R.id.editAge);

        btnEditNick = findViewById(R.id.btnEditNick);
        btnEditAge = findViewById(R.id.btnEditAge);
        btnSelectGender = findViewById(R.id.btnEditGender);
        btnSelectLocation = findViewById(R.id.btnEditLocation);
        btnSelectInterests = findViewById(R.id.btnInterests);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        chipGroupInterests = findViewById(R.id.chipGroupInterests);

        tvNickName = findViewById(R.id.tvNickName);
        tvAge = findViewById(R.id.tvAge);
        tvGender = findViewById(R.id.tvGender);
        tvLocation = findViewById(R.id.tvLocation);


        userRepo = ApplicationUtil.getUserRepo();
        //UI 갱신
        userRepo.getUser(new UserRepo.OnUserGetListener(){
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                //비동기 로직이기 때문에 어느시점까지는 currentUser = null 일수 있음.
                tvNickName.setText(currentUser.getNickname());
                tvAge.setText(String.valueOf(currentUser.getAge()));
                tvLocation.setText(currentUser.getLocation());
                tvGender.setText(currentUser.getGender());

                selectedInterests = (ArrayList<String>) currentUser.getInterests();
                btnSelectInterests.setText(getIndexInList(selectedInterests));

                for (String interest : selectedInterests) {
                    Chip chip = new Chip(UserProfileEditActivity.this);
                    chip.setText(interest);
                    chip.setCloseIconVisible(true);

                    final String interestName = interest;
                    //Chip 취소 로직
                    chip.setOnCloseIconClickListener(v2 -> {
                        chipGroupInterests.removeView(chip);
                        selectedInterests.remove(interestName);
                        btnSelectInterests.setText(getIndexInList(selectedInterests));
                        //동적관리 가능?
                    });

                    chipGroupInterests.addView(chip);
                }
            }
            @Override
            public void onError(String errorMessage) {
                Log.d("getUser", "failed" + errorMessage);
            }
        });

        // 닉네임 수정 버튼 클릭 로직
        btnEditNick.setOnClickListener(v -> {
            if (!isNickEditing) {
                // 수정 모드 진입
                editNickName.setText("");
                tvNickName.setVisibility(View.GONE);
                editNickName.setVisibility(View.VISIBLE);
                btnEditNick.setText("완료");
                isNickEditing = true;
            } else {
                // 수정 완료 모드
                String newNick = editNickName.getText().toString().trim();
                if (!newNick.isEmpty()) {
                    tvNickName.setText(newNick);
                }
                editNickName.setVisibility(View.GONE);
                tvNickName.setVisibility(View.VISIBLE);
                btnEditNick.setText("수정");
                isNickEditing = false;
            }
        });

        // 나이 수정 버튼 클릭 로직
        btnEditAge.setOnClickListener(v -> {
            if (!isAgeEditing) {
                editAge.setText("");
                tvAge.setVisibility(View.GONE);
                editAge.setVisibility(View.VISIBLE);
                btnEditAge.setText("완료");
                isAgeEditing = true;
            } else {
                String newAge = editAge.getText().toString().trim();
                if (!newAge.isEmpty()) {
                    tvAge.setText(newAge);
                }
                editAge.setVisibility(View.GONE);
                tvAge.setVisibility(View.VISIBLE);
                btnEditAge.setText("수정");
                isAgeEditing = false;
            }
        });

        //성별 수정
        btnSelectGender.setOnClickListener(v->{
            btnSelectGender.setText("완료");
            String[] genders = {"남성", "여성"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("성별")
                    .setItems(genders, (dialog, which) -> {
                        tvGender.setText(genders[which]);
                    })
                    .show();
        });

        //지역 수정
        btnSelectLocation.setOnClickListener(v-> {
            btnSelectLocation.setText("완료");
            String[] areas = {"서울", "대전", "부산", "대구", "경기", "광주", "강원", "기타"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("지역")
                    .setItems(areas, (dialog , which) -> {
                        tvLocation.setText(areas[which]);
                        btnSelectLocation.setText("수정");
                    })
                    .show();
        });

        //흥미 카테고리 수정
        btnSelectInterests.setOnClickListener(v->{
            String[] interests = {"카페", "전시회", "산책", "드라이브", "공연", "음식", "영화", "야경", "실내", "기타"};
            boolean[] checked = new boolean[interests.length];
            ArrayList<String> tempInterests = new ArrayList<>(selectedInterests);

            // checked[] 초기화: 이미 선택되어 있는 항목이 있으면 미리 체크해주기
            for (int i = 0; i < checked.length; i++) {
                checked[i] = selectedInterests.contains(interests[i]);
            }

            new MaterialAlertDialogBuilder(this)
                    .setTitle("카테고리")
                    .setMultiChoiceItems(interests, checked, (dialog, which, isChecked) -> {
                        if(isChecked){
                            if (tempInterests.size() >= 3){
                                //선택 3개 초과 시 경고
                                Toast.makeText(this, "최대 3개까지만 선택할 수 있습니다.", Toast.LENGTH_SHORT).show();
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
                    .setPositiveButton("확인", (dialog, which) -> {
                        chipGroupInterests.removeAllViews();
                        btnSelectInterests.setText(getIndexInList(tempInterests));
                        //선택된 항목을 Chip 형태로 표시
                        for (String interest : tempInterests) {
                            Chip chip = new Chip(this);
                            chip.setText(interest);
                            chip.setCloseIconVisible(true);

                            // 중요한 부분: 람다 안에서 루프 변수(interest)를 안전하게 사용하려면
                            // final 로컬 변수를 만들어 캡처하도록 한다.
                            final String interestName = interest;

                            //Chip 취소 로직
                            chip.setOnCloseIconClickListener(v2 -> {
                                chipGroupInterests.removeView(chip);
                                selectedInterests.remove(interestName);
                                btnSelectInterests.setText(getIndexInList(selectedInterests));
                                //동적관리 가능?
                            });
                            chipGroupInterests.addView(chip);
                        }
                        selectedInterests = tempInterests;
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        //유저 정보 업데이트
        btnSaveProfile.setOnClickListener(v-> {
            updateLocation = tvLocation.getText().toString();
            updateGender = tvGender.getText().toString();
            updateNickName = tvNickName.getText().toString();
            updateAge = Integer.parseInt(tvAge.getText().toString());
            updateUserInterestsList = selectedInterests;

            userRepo.updateUser(mAuth.getCurrentUser().getUid(), updateNickName, updateLocation, updateGender
                    , updateAge, updateUserInterestsList, new UserRepo.OnUserAddedListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(UserProfileEditActivity.this, "저장되었습니다" , Toast.LENGTH_SHORT).show();
                            //intent()
                            Intent intent = new Intent(UserProfileEditActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(UserProfileEditActivity.this, "failed" + errorMessage , Toast.LENGTH_SHORT).show();
                        }
                    });
        });

    }

    private String getIndexInList(ArrayList list){
        if(list.isEmpty()){
            return "선택한 카테고리가 없습니다";
        }else {
            String result = String.join(", ", list);
            return result;
        }
    }
}
