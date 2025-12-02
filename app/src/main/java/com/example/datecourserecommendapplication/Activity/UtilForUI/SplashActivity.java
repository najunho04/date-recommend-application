package com.example.datecourserecommendapplication.Activity.UtilForUI;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.datecourserecommendapplication.Activity.LoginActivity;
import com.example.datecourserecommendapplication.Activity.MainActivity;
import com.example.datecourserecommendapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        // 1~2초 후 다음 화면으로 이동 (자연스러운 전환을 위함)
        new android.os.Handler().postDelayed(() -> checkLoginStatus(), 2000);
    }

    private void checkLoginStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        //회원 탈퇴 시 로그아웃 처리
        if (currentUser != null){
            currentUser.reload().addOnCompleteListener(task -> {
                if (currentUser.getUid() == null) {
                    // 서버에서 삭제됨 → 로그아웃 처리
                    FirebaseAuth.getInstance().signOut();
                }
            });
        }
        if (currentUser != null) {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            // 로그인 필요
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
        }
        finish(); // Splash 화면 제거
    }
}
