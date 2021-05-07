package com.mudasir.mcontacts.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mudasir.mcontacts.R;
import com.mudasir.mcontacts.adapters.ContactsAdapter;

import com.mudasir.mcontacts.reciever.NetworkChangeReceiver;
import com.mudasir.mcontacts.models.CloudContacts;
import com.mudasir.mcontacts.models.Contact;
import com.tsuryo.swipeablerv.SwipeLeftRightCallback;
import com.tsuryo.swipeablerv.SwipeableRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import es.dmoral.toasty.Toasty;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE;

public class home extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final int MY_REQUEST_CODE = 1025;
    static SwipeableRecyclerView recyclerView;
    static SwipeRefreshLayout swipLayout;
    static ContactsAdapter mAdapter;


    static List<CloudContacts> dynamiccontactList;

    static DatabaseReference mDatabaseRef;
    static FirebaseAuth mAuth;
    static FirebaseUser mCurrentUser;
    static String CurrentUserId;
    static TextView tvalert;
    static ProgressBar progressBar;


    private DatabaseReference mRefStatus;
    String text;
    private int mpos;
    private NetworkChangeReceiver mNetworkReceiver;
    static ActionBar actionBar;


    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String TEXT = "text";

    private AppUpdateManager appUpdateManager;



    private int check_IF_USER_IMPORTS_CONTACTS = 10;


    private static List<CloudContacts> cloudContacts;


    private AdView mAdView;
    private AdRequest mAdRequest;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


            getDatabaseRefAndUserId();
            init();
            if (haveNetworkConnection()) {

                getUsersFromDataBase(CurrentUserId,this);

                appUpdateManager = AppUpdateManagerFactory.create(this);

                // Returns an intent object that you use to check for an update.
                Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
                // Checks that the platform will allow the specified type of update.

                appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            // This example applies an immediate update. To apply a flexible update
                            // instead, pass in AppUpdateType.FLEXIBLE
                            && appUpdateInfo.isUpdateTypeAllowed(IMMEDIATE)) {
                        // Request the update.
                        try {

                            appUpdateManager.startUpdateFlowForResult(
                                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                                    appUpdateInfo,
                                    // The current activity making the update request.
                                    this,
                                    // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                                    // flexible updates.
                                    AppUpdateOptions.newBuilder(IMMEDIATE)
                                            .setAllowAssetPackDeletion(true)
                                            .build(),
                                    // Include a request code to later monitor this update request.
                                    MY_REQUEST_CODE);

                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }

                    }
                });


                MobileAds.initialize(this, initializationStatus -> {

                });




                mAdView = findViewById(R.id.adView);
                mAdRequest = new AdRequest.Builder().build();
                mAdView.loadAd(mAdRequest);

                mAdView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Toasty.error(home.this,loadAdError.getMessage(),Toasty.LENGTH_SHORT,true).show();
                    }
                });



            } else {
                progressBar.setVisibility(View.GONE);
                tvalert.setVisibility(View.VISIBLE);
                tvalert.setText(R.string.no_internet);
                actionBar.setSubtitle("No Internet!");

            }


    }

   public static void getUsersFromDataBase(String Id,Context mContext){

        if (Id!=null){
            mDatabaseRef.child(Id).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (snapshot.exists()) {
                        cloudContacts.clear();
                        for (DataSnapshot shot : snapshot.getChildren()) {
                            CloudContacts contacts = shot.getValue(CloudContacts.class);
                            cloudContacts.add(contacts);
                        }

                        tvalert.setVisibility(View.GONE);
                        mAdapter=new ContactsAdapter(mContext,cloudContacts,CurrentUserId);
                        recyclerView.setAdapter(mAdapter);
                        progressBar.setVisibility(View.GONE);
                        actionBar.setSubtitle("Contacts Size: " + cloudContacts.size());
                        swipLayout.setRefreshing(false);

                    }
                    else{
                        swipLayout.setRefreshing(false);
                        progressBar.setVisibility(View.GONE);
                        tvalert.setVisibility(View.VISIBLE);
                        tvalert.setBackgroundColor(Color.BLUE);
                        tvalert.setTextColor(Color.WHITE);
                        tvalert.setText(R.string.no_contacts);
                        actionBar.setSubtitle("No Contacts!");
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }



    }


    @Override
    protected void onStart() {
        super.onStart();

        try {
            if (mAuth.getCurrentUser() != null) {

            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void showInterstialAd(){

        if (mInterstitialAd != null) {
            mInterstitialAd.show(home.this);
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            if (haveNetworkConnection()) {

                appUpdateManager
                        .getAppUpdateInfo()
                        .addOnSuccessListener(appUpdateInfo -> {
                            // If the update is downloaded but not installed,
                            // notify the user to complete the update.
                            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                                popupSnackbarForCompleteUpdate();
                            }
                            if (appUpdateInfo.updateAvailability()
                                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                // If an in-app update is already running, resume the update.
                                try {
                                    appUpdateManager.startUpdateFlowForResult(
                                            appUpdateInfo,
                                            IMMEDIATE,
                                            this,
                                            MY_REQUEST_CODE);
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                }
                            }
                        });


            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }


    }

    // Displays the snackbar notification and call to action.
    private void popupSnackbarForCompleteUpdate() {


        try {
            Snackbar.make(findViewById(R.id.home_Activity), "An update has just been downloaded.",
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("RESTART", view -> appUpdateManager.completeUpdate())
                    .setActionTextColor(
                            getResources().getColor(R.color.snackbar_action_text_color))
                    .show();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }


    }

    private void initSharedPreferences() {

        try {
            sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
            editor = sharedPreferences.edit();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

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


    public static boolean checkInternet(boolean value, Context context) {

        try {
            if (value) {

                progressBar.setVisibility(View.VISIBLE);

                Handler handler = new Handler();
                Runnable delayrunnable = new Runnable() {
                    @Override
                    public void run() {

                        progressBar.setVisibility(View.GONE);
                        tvalert.setVisibility(View.GONE);
                        getUsersFromDataBase(CurrentUserId,context);
                        actionBar.setSubtitle("Contacts Size: " + cloudContacts.size());

                    }
                };
                handler.postDelayed(delayrunnable, 3000);
                return true;
            } else {
                progressBar.setVisibility(View.VISIBLE);



                Handler handler = new Handler();
                Runnable delayrunnable = new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        tvalert.setBackgroundColor(Color.RED);
                        tvalert.setTextColor(Color.WHITE);
                        tvalert.setVisibility(View.VISIBLE);
                        tvalert.setText(R.string.no_internet);
                        actionBar.setSubtitle("No Internet!");
                        recyclerView.setAdapter(null);

                    }
                };
                handler.postDelayed(delayrunnable, 3000);


            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unregisterNetworkChanges();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }


    private void getDatabaseRefAndUserId() {

        try {
            mDatabaseRef = FirebaseDatabase.getInstance().getReference("Contacts");
            mRefStatus = mDatabaseRef;
            mAuth = FirebaseAuth.getInstance();
            mCurrentUser = mAuth.getCurrentUser();
            CurrentUserId = mCurrentUser.getUid();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    private void init() {


        try {
            cloudContacts=new ArrayList<>();
            actionBar = getSupportActionBar();
            actionBar.setTitle("MContacts- Backup on Cloud");
            recyclerView = findViewById(R.id.rv_contacts);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setHasFixedSize(true);

            swipLayout = findViewById(R.id.swipe_layout);
            swipLayout.setOnRefreshListener(this);

            swipLayout.setColorSchemeResources(R.color.green,
                    android.R.color.holo_green_dark,
                    android.R.color.holo_orange_dark,
                    android.R.color.holo_blue_dark);


            recyclerView.setListener(new SwipeLeftRightCallback.Listener() {
                @Override
                public void onSwipedLeft(int position) {
                    //call Code Here
                    mpos = position;
                    String phone = cloudContacts.get(mpos).getPhone();
                    dialContactPhone(phone);
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onSwipedRight(int position) {
                    //Message Code here
                    mpos = position;
                    String phone = cloudContacts.get(mpos).getPhone();
                    sendSms(phone);
                    mAdapter.notifyDataSetChanged();
                }
            });

            progressBar = findViewById(R.id.progress);
            tvalert = findViewById(R.id.txtAlert);
            mNetworkReceiver = new NetworkChangeReceiver();
            progressBar.setVisibility(View.VISIBLE);
            registerNetworkBroadcastForNougat();

            initSharedPreferences();
        } catch (Exception exception) {
            exception.printStackTrace();
        }


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        try {
            getMenuInflater().inflate(R.menu.home_menu, menu);

            MenuItem searchItem = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {

                    try {
                        if (haveNetworkConnection()) {
                            if (!mAdapter.equals(null)) {
                                mAdapter.getFilter().filter(newText);
                            } else {
                                Toasty.error(home.this, "No Data!", Toasty.LENGTH_SHORT, true).show();
                            }
                        } else {
                            Toasty.error(home.this, "No Internet Connection!", Toasty.LENGTH_SHORT, true).show();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    return false;

                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        try {
            MenuItem item = menu.findItem(R.id.btnSaveAll);
            text = sharedPreferences.getString(TEXT, "read");

            if (text.equals("read")) {
                item.setTitle("Save All Contacts");
            }
            if (text.equals("delete")) {
                item.setTitle("Delete All Contacts");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btnLogout:
                new AlertDialog.Builder(this)
                        .setTitle("Confirmation Message")
                        .setMessage("Are You Sure You Want to LogOut")
                        .setPositiveButton("YES", (dialog, which) -> {
                            dialog.dismiss();
                            logout();
                        }).setNegativeButton("No", (dialog, which) -> {
                            dialog.cancel();
                        }).create().show();
                break;
            case R.id.btnAccountDetails:
                if (haveNetworkConnection()) {
                    Intent intent = new Intent(home.this, AccountDetails.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Enable Internet Connection to go to Account Details Activity!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnAddContact:
                if (haveNetworkConnection()) {
                    Intent intent = new Intent(home.this, AddContactsActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Enable Internet Connection to go to Account Details Activity!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnSaveAll:
                if (text.equals("read")) {
                    if (haveNetworkConnection()) {

                        Intent intent=new Intent(home.this,uploadContacts.class);
                        intent.putExtra("id",CurrentUserId);
                        startActivityForResult(intent,10);


                    } else {
                        Toasty.error(this, "Can not Read And Store Contacts While You dont have internet Connection", Toasty.LENGTH_SHORT, true).show();
                    }


                }
                if (text.equals("delete")) {
                    if (haveNetworkConnection()) {
                        DeleteAllContacts(CurrentUserId, item);
                    } else {
                        Toasty.error(this, "Can not Delete Contacts While You dont have internet Connection", Toasty.LENGTH_SHORT, true).show();
                    }

                }
                break;

        }

        return super.onOptionsItemSelected(item);
    }


    private void DeleteAllContacts(String uid, MenuItem item) {


        try {
            new AlertDialog.Builder(this)
                    .setTitle("Confirmation Message")
                    .setMessage("Are You Sure You Want to Delete All Contacts From Cloud")
                    .setPositiveButton("YES", (dialog, which) -> {

                        ProgressDialog progressDialog = new ProgressDialog(this);
                        progressDialog.setCancelable(false);
                        progressDialog.setTitle("Deleting...");
                        progressDialog.setMessage("Deleting Contacts From Cloud");
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.show();
                        mDatabaseRef.child(uid).removeValue().addOnSuccessListener(aVoid -> {

                            recyclerView.setAdapter(null);
                            getSupportActionBar().setSubtitle("");

                            editor.putString(TEXT, "read");
                            editor.apply();
                            item.setTitle("Save All Contacts");
                            progressDialog.dismiss();


                        });


                    })
                    .setNegativeButton("NO", (dialog, which) -> dialog.cancel()).create().show();
        } catch (Exception exception) {
            exception.printStackTrace();
        }


    }


    private void logout() {

        try {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(home.this, MainActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception exception) {
            exception.printStackTrace();
        }


    }


    private void dialContactPhone(final String phoneNumber) {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null)));
    }

    private void sendSms(final String number) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, null)));
    }

    @Override
    public void onRefresh() {


        try {
            if (haveNetworkConnection()) {
                getUsersFromDataBase(CurrentUserId,this);
            } else {

            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }


    }







    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {

        }
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Toasty.error(this, "Update flow failed.", Toasty.LENGTH_SHORT, true).show();
            }
        }

        if (requestCode == check_IF_USER_IMPORTS_CONTACTS) {
            if (resultCode == RESULT_OK && data != null) {
                String value = data.getStringExtra("msg");

                if (value.equals("done")){
                    editor.putString(TEXT, "delete");
                    editor.apply();
                    //item.setTitle("Delete All Contacts");
                    AdRequest adRequest = new AdRequest.Builder().build();
                    InterstitialAd.load(this,getString(R.string.interstitial_full_screen), adRequest, new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            // The mInterstitialAd reference will be null until
                            // an ad is loaded.
                            mInterstitialAd = interstitialAd;
                            // Log.i(TAG, "onAdLoaded");
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error

                            //  Log.i(TAG, loadAdError.getMessage());
                            mInterstitialAd = null;
                        }
                    });

                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Called when fullscreen content is dismissed.
                            //    Log.d("TAG", "The ad was dismissed.");
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            // Called when fullscreen content failed to show.
                            //  Log.d("TAG", "The ad failed to show.");
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when fullscreen content is shown.
                            // Make sure to set your reference to null so you don't
                            // show it a second time.
                            mInterstitialAd = null;
                            // Log.d("TAG", "The ad was shown.");
                        }
                    });



                    showInterstialAd();
                }
                else{

                }
                Toasty.success(this, value, Toasty.LENGTH_SHORT, true).show();
            }
        }


    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }


}