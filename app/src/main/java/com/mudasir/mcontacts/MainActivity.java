package com.mudasir.mcontacts;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.mudasir.mcontacts.reciever.NetworkChangeReceiver;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1;
    private FirebaseAuth mAuth;

    private GoogleSignInClient mGoogleSignInClient;
    private NetworkChangeReceiver mNetworkReceiver;

    public static boolean connection=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNetworkReceiver = new NetworkChangeReceiver();
        registerNetworkBroadcastForNougat();


        if (connection){
            mAuth = FirebaseAuth.getInstance();
            createRequest();
        }else{
            Toasty.info(this,"please connect internet then try again!",Toasty.LENGTH_SHORT).show();
        }

        findViewById(R.id.sign_in_button).setOnClickListener(v -> {
            if (connection){
                signIn();
            }
            else{
                Toasty.info(this,"Cannot signIn without internet connection!",Toasty.LENGTH_SHORT).show();
            }
        });

    }

    private void createRequest() {
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient= GoogleSignIn.getClient(this,gso);
    }

    private void signIn() {

            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);


    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("Sign In", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("Sign In", "Google sign in failed", e);
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Intent intent=new Intent(MainActivity.this, home.class);
                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(MainActivity.this, "Error While Signing In.", Toast.LENGTH_SHORT).show();
                    }

                    // ...
                });
    }

    public static boolean CheckIsOnline(boolean value,Context context){

        if (value){
            connection=true;
           // Toasty.success(context,"Everything Is Fine",Toasty.LENGTH_SHORT,true).show();
        }else{
            connection=false;
           // Toasty.error(context,"connect to internet!",Toasty.LENGTH_SHORT,true).show();
        }
        return false;
    }


    private void registerNetworkBroadcastForNougat() {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    protected void unregisterNetworkChanges() {
        try {
            unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterNetworkChanges();
    }
}