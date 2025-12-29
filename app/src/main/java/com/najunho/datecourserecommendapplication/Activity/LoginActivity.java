package com.najunho.datecourserecommendapplication.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.najunho.datecourserecommendapplication.Activity.User.UserCreateActivity;
import com.najunho.datecourserecommendapplication.DB.UserRepo;
import com.najunho.datecourserecommendapplication.R;
import com.najunho.datecourserecommendapplication.Util.ApplicationUtil;
import com.najunho.datecourserecommendapplication.Util.GoogleSignInCallback;
import com.najunho.datecourserecommendapplication.Util.GoogleSignInHelper;
import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.jetbrains.annotations.NotNull;

public class LoginActivity extends AppCompatActivity {

    private Button btnGoogleSignIn;
    private Button btnGoogleLogin;
    private FirebaseAuth mAuth;
    private UserRepo userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        userRepo = ApplicationUtil.getUserRepo();

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        btnGoogleSignIn.setOnClickListener(v -> {
            GoogleSignInHelper.signInWithGoogleAsync(this, getString(R.string.default_web_client_id), new GoogleSignInCallback(){
                @Override
                public void onError(@NotNull String errorMessage) {
                    Log.d("loginActivity", "failed" + errorMessage);
                }

                @Override
                public void onSuccess(@NotNull String idToken) {
                    handleGoogleSignInResult(idToken);
                }
            });
        });
    }

    private void handleGoogleSignInResult(String idToken){
        if (idToken == null) {
            Log.d("handleGoogleSignInResult", "idToken is null");
            return;
        }
        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
        //Sign in to Firebase with the credential.
        mAuth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(authTask -> {
                    if(authTask.isSuccessful()){
                        Log.d("handleGoogleSignInResult", "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        userRepo.checkUser(user.getUid(), new UserRepo.OnUserCheckListener(){
                            @Override
                            public void onChecked() {
                                //전에 로그인 함
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                            @Override
                            public void onNotChecked() {
                                //DB에 유저 정보 없음 -> 처음 가입
                                Intent intent = new Intent(LoginActivity.this, UserCreateActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                            @Override
                            public void onError(String errorMessage) {
                                Log.d("onError", "error: "+ errorMessage);
                            }
                        });
                    }else{
                        Log.w("handleGoogleSignInResult", "signInWithCredential:failure", authTask.getException());
                    }
                });
    }
}
