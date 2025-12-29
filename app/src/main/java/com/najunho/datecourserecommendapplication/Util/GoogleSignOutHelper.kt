package com.najunho.datecourserecommendapplication.Util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

interface GoogleSignOutCallback{
    fun onSuccess()
    fun onError(error: String?)
}

object GoogleSignOutHelper {
    @JvmStatic
    fun signOutWithGoogleAsync(activity: Activity, callback: GoogleSignOutCallback){
        CoroutineScope(Dispatchers.Main).launch {
            try {
                clearCredentialState(activity)
                callback.onSuccess()
            }catch (e: Exception){
                callback.onError(e.message);
            }
        }
    }

    //로컬 환경에서 구글계정 로그인 캐시를 없앰 -> firebaseAuth에는 유지
    suspend fun clearCredentialState(context: Context) {
        val credentialManager = CredentialManager.create(context)
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    @JvmStatic
    //인증 서버(OAuth 서버)에서 아예 회원 UID 삭제 로직.
    fun revokeGoogleAccess(idToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://oauth2.googleapis.com/revoke?token=$idToken"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            val responseCode = conn.responseCode
            Log.d("revokeGoogleAccess", "Google revoke response: $responseCode")
        }
    }

    //UserDB delete
    @JvmStatic
    fun revokeUserDb(uid : String?) {
        if (uid != null) {
            ApplicationUtil.getUserRepo().deleteUser(uid);
            Log.d("revokeUserDb", "success");
        } else {
            Log.d("revokeUserDb", "failed. no Uid")
        }
    }


}