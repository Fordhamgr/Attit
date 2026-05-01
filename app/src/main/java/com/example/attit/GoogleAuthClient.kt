package com.example.attit

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class GoogleAuthClient(private val context: Context) {

    // PASTE YOUR WEB CLIENT ID HERE ↓
    private val webClientId = context.getString(com.example.attit.R.string.default_web_client_id)

    private val auth = Firebase.auth

    // Configure Google Sign In
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()

    private val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // 1. Check if user is ALREADY logged in (Auto-login)
    fun getSignedInUser(): String? {
        return auth.currentUser?.uid // Returns ID if logged in, null if not
    }

    // 2. Start the Sign In (Get the Intent to launch)
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    // 3. Handle the result after user clicks their email
    suspend fun signInWithIntent(intent: Intent): Boolean {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.await()
            val idToken = account.idToken ?: return false

            // Swap Google Token for Firebase Token
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 4. Sign Out
    suspend fun signOut() {
        try {
            auth.signOut()
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}