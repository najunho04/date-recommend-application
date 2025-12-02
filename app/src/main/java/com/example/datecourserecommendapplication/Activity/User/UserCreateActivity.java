package com.example.datecourserecommendapplication.Activity.User;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.datecourserecommendapplication.Activity.MainActivity;
import com.example.datecourserecommendapplication.DB.UserRepo;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class UserCreateActivity extends AppCompatActivity {

    private ChipGroup chipGroupInterests;
    private TextInputEditText editNickName, editAge;
    private Button btnSelectLocation, btnSelectGender, btnSelectInterests, btnSaveUser;
    private ArrayList<String> selectedInterests = new ArrayList<>(); // 현재 화면에 보이는 선택 상태
    private ArrayList<String> userInterestsList = new ArrayList<>(); // 저장용 리스트(최종)
    private String selectedLocation, selectedGender, nickName;
    private int age;
    private UserRepo userRepo;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_create);

        editNickName = findViewById(R.id.editNickName);
        editAge = findViewById(R.id.editAge);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        btnSelectGender = findViewById(R.id.btnSelectGender);
        btnSelectInterests = findViewById(R.id.btnSelectInterests);
        btnSaveUser = findViewById(R.id.btnSaveUser);
        chipGroupInterests = findViewById(R.id.chipGroupInterests);

        userRepo = ApplicationUtil.getUserRepo();
        nickName = null;
        age = 0;
        selectedLocation = null;
        selectedGender = null;

        btnSelectLocation.setOnClickListener(v -> {
            String[] areas = {"서울", "대전", "부산", "대구", "경기", "광주", "강원", "기타"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("지역 선택")
                    .setItems(areas, (dialog, which) -> {
                        btnSelectLocation.setText(areas[which]);
                        selectedLocation = areas[which];
                    })
                    .show();
        });
        btnSelectGender.setOnClickListener(v-> {
            String[] genders = {"남성", "여성"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle("성별")
                    .setItems(genders, (dialog, which) -> {
                        btnSelectGender.setText(genders[which]);
                        selectedGender = genders[which];
                    })
                    .show();
        });

        btnSelectInterests.setOnClickListener(v-> {
            String[] interests = {"카페", "전시회", "산책", "드라이브", "공연", "음식", "영화", "야경", "실내", "기타"};
            boolean[] checked = new boolean[interests.length];
            //selectedInterests(현재 선택되어 있는 interests, chipList와 동일) -> 복제 List 생성
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
                        userInterestsList.remove(interestName);
                    });
                    chipGroupInterests.addView(chip);
                }
                selectedInterests = tempInterests;
                // 선택을 저장용 리스트로 복사 (Firestore에 저장할 때 사용)
                userInterestsList = new ArrayList<>(selectedInterests);
            })
                    .setNegativeButton("취소", null)
                    .show();
        });


        //저장 버튼: User Collection 생성 + intent
        btnSaveUser.setOnClickListener(v-> {

            if(editNickName.getText().toString().isEmpty() || editAge.getText().toString().isEmpty()){
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if(selectedInterests.isEmpty()) {
                Toast.makeText(this, "흥미 카테고리를 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if(selectedLocation == null || selectedGender == null) {
                Toast.makeText(this, "지역과 성별을 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            String nickName = editNickName.getText().toString();
            int age = Integer.parseInt(editAge.getText().toString());

            userRepo.addUser(nickName, selectedLocation, selectedGender, age, userInterestsList
                    , new UserRepo.OnUserAddedListener(){//success시 에만 intent해야 하니까 callback 사용
                @Override
                public void onSuccess() {
                    Log.d("SaveBtn", "success");
                    Intent intent = new Intent(UserCreateActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                @Override
                public void onError(String errorMessage) {
                    Log.d("SaveBtn", errorMessage);
                }
            });
        });
    }
}
