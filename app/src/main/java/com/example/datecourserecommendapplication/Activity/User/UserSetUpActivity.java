package com.example.datecourserecommendapplication.Activity.User;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.datecourserecommendapplication.Activity.LoginActivity;
import com.example.datecourserecommendapplication.Activity.MainActivity;
import com.example.datecourserecommendapplication.Activity.UtilForUI.ViewMyLikesActivity;
import com.example.datecourserecommendapplication.Activity.UtilForUI.ViewMyPostsActivity;
import com.example.datecourserecommendapplication.Activity.ViewPostInMapActivity;
import com.example.datecourserecommendapplication.DB.UserRepo;
import com.example.datecourserecommendapplication.R;
import com.example.datecourserecommendapplication.Util.ApplicationUtil;
import com.example.datecourserecommendapplication.Util.GoogleSignInCallback;
import com.example.datecourserecommendapplication.Util.GoogleSignInHelper;
import com.example.datecourserecommendapplication.Util.GoogleSignOutCallback;
import com.example.datecourserecommendapplication.Util.GoogleSignOutHelper;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserSetUpActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    private Button btnMainFeed, btnViewMap;
    private Button btnChangeNickname, btnMyPosts, btnMyLikes, btnMyInfo, btnLogout, btnWithdraw;
    private UserRepo userRepo;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setup);

        userRepo = ApplicationUtil.getUserRepo();
        mAuth = FirebaseAuth.getInstance();

        btnChangeNickname = findViewById(R.id.btnChangeNickname);//O
        btnMyPosts = findViewById(R.id.btnMyPosts);//postDB 생성 후
        btnMyLikes = findViewById(R.id.btnMyLikes);//postDB 생성 후
        btnMyInfo = findViewById(R.id.btnMyInfo);//지역, 나이, 카테고리 수정란 -> repo로직 + UI 생성 + intent
        btnLogout = findViewById(R.id.btnLogout);//O
        btnWithdraw = findViewById(R.id.btnWithdraw);//O
        btnMainFeed = findViewById(R.id.btnMainFeed);

        //메인화면 이동
        btnMainFeed.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        //btnViewMap setup
        btnViewMap = findViewById(R.id.btnViewMap);
        btnViewMap.setOnClickListener(v->{
            Intent intent = new Intent(this, ViewPostInMapActivity.class);
            startActivity(intent);
            finish();
        });

        //닉네임 변경
        btnChangeNickname.setOnClickListener(v->{
            showChangeNicknameDialog();
        });

        //정보 수정
        btnMyInfo.setOnClickListener(v->{
            Intent intent = new Intent(this, UserProfileEditActivity.class);
            startActivity(intent);
            finish();
        });

        //자기 게시물 보기
        btnMyPosts.setOnClickListener(v->{
            Intent intent = new Intent(this, ViewMyPostsActivity.class);
            startActivity(intent);
        });

        //좋아요 누른 게시물 보기
        btnMyLikes.setOnClickListener(v->{
            Intent intent = new Intent(this, ViewMyLikesActivity.class);
            startActivity(intent);
        });

        //회원 탈퇴
        btnWithdraw.setOnClickListener(v->{
            GoogleSignInHelper.getIdToken(this, getString(R.string.default_web_client_id), new GoogleSignInCallback() {
                @Override
                public void onSuccess(@NotNull String idToken) {
                    drawLogic(idToken);
                    Log.d("getIdToken", "success");
                }
                @Override
                public void onError(@NotNull String errorMessage) {
                    Log.d("getIdToken", "failed" + errorMessage);
                }
            });
        });

        //로그아웃
        btnLogout.setOnClickListener(v-> {
            //추후 DB리스너 생성할 때 리스너 remove 로직 필요
            GoogleSignOutHelper.signOutWithGoogleAsync(this, new GoogleSignOutCallback(){
                @Override
                public void onError(@Nullable String error) {
                    Toast.makeText(UserSetUpActivity.this, "failed" + error
                            , Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onSuccess() {
                    Toast.makeText(UserSetUpActivity.this, "로그아웃 되었습니다"
                            , Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    Intent intent = new Intent(UserSetUpActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        });
    }

    //회원 탈퇴 로직
    private void drawLogic(String idToken){
        //1. 유저 재인증
        //2. OAuth 연결해제
        //3. DB 삭제
        //4. firebase Auth 탈퇴
        //5. logout + intent
        FirebaseUser user = mAuth.getCurrentUser();
        if( user == null){
            Log.d("drawLogic", "failed");
        }
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        user.reauthenticate(credential).addOnCompleteListener(task -> { //재인증
            if (task.isSuccessful()) {
                GoogleSignOutHelper.revokeGoogleAccess(idToken); //OAuth 해제
                GoogleSignOutHelper.revokeUserDb(user.getUid()); //User Db 삭제
                user.delete().addOnCompleteListener(deleteTask -> {//userAuth 삭제
                    if(deleteTask.isSuccessful()){
                        Log.d("drawLogic_AuthDelete", "success");
                        mAuth.signOut(); //logout
                        Toast.makeText(UserSetUpActivity.this, "탈퇴 되었습니다"
                                , Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UserSetUpActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }else {
                        Log.d("drawLogic_AuthDelete", "failed");
                    }
                });
            }else {
                Log.d("drawLogic_ReAuth", "failed");
            }
        });
    }

    //change NickName Logic
    private void showChangeNicknameDialog(){
        // 다이얼로그 커스텀 뷰 인플레이트
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_nickname, null);
        EditText etNewNickname = dialogView.findViewById(R.id.etNewNickname);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("닉네임 변경")
                .setView(dialogView)
                .setPositiveButton("저장", null) // 나중에 직접 override
                .setNegativeButton("취소", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v-> {
                String newNickname = etNewNickname.getText().toString().trim();
                if (newNickname.isEmpty()) {
                    etNewNickname.setError("닉네임을 입력하세요");
                    return;
                }
                // Firestore 업데이트
                userRepo.updateNicknameInFirestore(newNickname, new UserRepo.OnUserAddedListener() {
                    @Override
                    public void onSuccess() {
                        etNewNickname.setText(newNickname);
                        dialog.dismiss();
                    }
                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(UserSetUpActivity.this, "failed" + errorMessage
                                , Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
            });
        });
        dialog.show();
    }
}
