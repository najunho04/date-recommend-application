package com.example.datecourserecommendapplication.Util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Java에서 사용할 콜백 인터페이스 (Java에서 익명 클래스로 쉽게 구현 가능)
interface GoogleSignInCallback {
    fun onSuccess(idToken: String)
    fun onError(errorMessage: String)
}

object GoogleSignInHelper {

    @JvmStatic
    fun signInWithGoogleAsync(activity: Activity, webClientId: String, callback: GoogleSignInCallback) {
        // 코루틴을 메인(UI) 스레드에서 실행
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val idToken = signInWithGoogleSuspend(activity, webClientId)
                Log.d("afterIdToken", "IdToken : $idToken")
                if (idToken != null) {
                    callback.onSuccess(idToken)
                } else {
                    callback.onError("Failed to obtain Google ID token")
                }
            } catch (e: Exception) {
                Log.e("GoogleSignInHelper", "signIn failed", e)
                callback.onError(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun signInWithGoogleSuspend(activity: Activity, webClientId: String): String? {
        val credentialManager = CredentialManager.create(activity)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try{
            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    return googleIdTokenCredential.idToken
                }catch (e: GoogleIdTokenParsingException) {
                    Log.e("GoogleSignInHelper1", "Invalid google id token response", e)
                    null
                }
            }else{
                Log.e("GoogleSignInHelper2", "Unexpected credential type: ${credential.javaClass}")
                null
            }
        }catch (e: Exception) {
            Log.e("GoogleSignInHelper3", "signIn failed", e)
            null
        }
    }

    @JvmStatic
    fun getIdToken(context: Context, webClientId: String, callback: GoogleSignInCallback){
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val idToken : String? = fetchIdToken(context, webClientId)
                if(idToken != null){
                    callback.onSuccess(idToken) //코루틴 환경에서 return 값을 빼낼수가 없으니까 콜백 형태로 빼내버리기
                    //콜백으로 빼낼게 아니면 java환경에서 쓰레드를 작동시켜서 그안에서 로직 구현
                }else{
                    callback.onError("Failed to obtain Google ID token")
                }
            } catch (e: Exception) {
                Log.e("getIdToken", "coroutine failed", e)
            }
        }
    }
    suspend fun fetchIdToken(context: Context, webClientId: String) : String?{
        val credentialManager = CredentialManager.create(context)

        // Google ID 옵션 생성
        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(true) // 기존 로그인 계정만 필터링
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        return try {
            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )
            //response 처리
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            idToken
        }catch (e: GetCredentialException) {
            Log.e("GoogleSignInHelper", "getCredential failed", e)
            null
        }
    }

}