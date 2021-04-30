package com.mudasir.mcontacts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import android.content.pm.PackageManager;
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
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;


import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
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
import com.mudasir.mcontacts.adapters.ContactsAdapter;

import com.mudasir.mcontacts.reciever.NetworkChangeReceiver;
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

public class home extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, SwipeRefreshLayout.OnRefreshListener {

    private static final int MY_REQUEST_CODE = 1025;
    static SwipeableRecyclerView recyclerView;
    static SwipeRefreshLayout swipLayout;
    static ContactsAdapter mAdapter;
    List<Contact> contactList = new ArrayList<>();

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

    public static HomeViewModel homeViewModel;
    public static List<CloudContacts> cloudContactsList=new ArrayList<>();
    private AdView mAdView;
    private AdRequest mAdRequest;
    private InterstitialAd mInterstitialAd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);


        try {
            getDatabaseRefAndUserId();
            init();
            if (haveNetworkConnection()) {


                 homeViewModel.getUsers(CurrentUserId).observe(this, new Observer<List<CloudContacts>>() {
                     @Override
                     public void onChanged(List<CloudContacts> cloudContacts) {

                         if (!cloudContacts.isEmpty()){
                             cloudContactsList=cloudContacts;
                             mAdapter=new ContactsAdapter(home.this,cloudContacts,CurrentUserId);
                             recyclerView.setAdapter(mAdapter);

                             progressBar.setVisibility(View.GONE);
                             actionBar.setSubtitle("Contacts Size: "+cloudContacts.size());
                             swipLayout.setRefreshing(false);
                         }
                         else{
                             swipLayout.setRefreshing(false);
                             // if has not Data
                             progressBar.setVisibility(View.GONE);
                             tvalert.setVisibility(View.VISIBLE);
                             tvalert.setBackgroundColor(Color.BLUE);
                             tvalert.setText(R.string.no_contacts);

                             Toasty.info(home.this, "No Data Found!", Toast.LENGTH_SHORT, true).show();
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


            } else {
                progressBar.setVisibility(View.GONE);
                tvalert.setVisibility(View.VISIBLE);
                tvalert.setText(R.string.no_internet);
                actionBar.setSubtitle("No Internet!");

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
    protected void onStart() {
        super.onStart();

        try {
                if (mAuth.getCurrentUser()!=null){

                }
                else{
                    Intent intent=new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            if(haveNetworkConnection()){

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
                    .setAction("RESTART", view ->appUpdateManager.completeUpdate())
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


    public static boolean  checkInternet(boolean value, Context context) {

        try {
            if (value) {
                tvalert.setText("We are back !!!");
                tvalert.setBackgroundColor(Color.GREEN);
                tvalert.setTextColor(Color.WHITE);
                Handler handler = new Handler();
                Runnable delayrunnable = new Runnable() {
                    @Override
                    public void run() {

                        tvalert.setVisibility(View.GONE);
                        mAdapter=new ContactsAdapter(context,cloudContactsList,CurrentUserId);
                        recyclerView.setAdapter(mAdapter);
                        actionBar.setSubtitle("Contacts Size: "+cloudContactsList.size());

                    }
                };
                handler.postDelayed(delayrunnable, 3000);
                return true;
            } else {
                tvalert.setVisibility(View.VISIBLE);
                tvalert.setText("Could not Connect to internet");
                tvalert.setBackgroundColor(Color.RED);
                tvalert.setTextColor(Color.WHITE);

                Handler handler = new Handler();
                Runnable delayrunnable = new Runnable() {
                    @Override
                    public void run() {
                        tvalert.setVisibility(View.VISIBLE);
                        tvalert.setText(R.string.no_internet);
                        actionBar.setSubtitle("No Internet!");
                        recyclerView.setAdapter(null);
                        dynamiccontactList.clear();
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
            cloudContactsList.clear();
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
            dynamiccontactList = new ArrayList<>();
            actionBar=getSupportActionBar();
            actionBar.setTitle("MContacts- Backup on Cloud");
            recyclerView = findViewById(R.id.rv_contacts);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setHasFixedSize(true);

            swipLayout = findViewById(R.id.swipe_layout);
            swipLayout.setOnRefreshListener(this);

            swipLayout.setColorSchemeResources(R.color.colorAccent,
                    android.R.color.holo_green_dark,
                    android.R.color.holo_orange_dark,
                    android.R.color.holo_blue_dark);


            recyclerView.setListener(new SwipeLeftRightCallback.Listener() {
                @Override
                public void onSwipedLeft(int position) {
                    //call Code Here
                    mpos = position;
                    String phone = dynamiccontactList.get(mpos).getPhone();
                    dialContactPhone(phone);
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onSwipedRight(int position) {
                    //Message Code here
                    mpos = position;
                    String phone = dynamiccontactList.get(mpos).getPhone();
                    sendSms(phone);
                    mAdapter.notifyDataSetChanged();
                }
            });

            progressBar = findViewById(R.id.progress);
            tvalert = findViewById(R.id.txtAlert);
            progressBar.setVisibility(View.VISIBLE);
            mNetworkReceiver = new NetworkChangeReceiver();
            registerNetworkBroadcastForNougat();

            initSharedPreferences();
        } catch (Exception exception) {
            exception.printStackTrace();
        }


    }

    private static final String[] PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };


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
                logout();
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
                        ReadContacts();
                        editor.putString(TEXT, "delete");
                        editor.apply();
                        item.setTitle("Delete All Contacts");
                        showInterstialAd();
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
                homeViewModel.getUsers(CurrentUserId).observe(this, new Observer<List<CloudContacts>>() {
                    @Override
                    public void onChanged(List<CloudContacts> cloudContacts) {

                        if (!cloudContacts.isEmpty()){

                            mAdapter=new ContactsAdapter(home.this,cloudContacts,CurrentUserId);
                            recyclerView.setAdapter(mAdapter);

                            progressBar.setVisibility(View.GONE);
                            actionBar.setSubtitle("Contacts Size: "+cloudContacts.size());
                            swipLayout.setRefreshing(false);
                        }
                        else{
                            swipLayout.setRefreshing(false);
                            // if has not Data
                            progressBar.setVisibility(View.GONE);
                            tvalert.setVisibility(View.VISIBLE);
                            tvalert.setBackgroundColor(Color.BLUE);
                            tvalert.setText(R.string.no_contacts);

                            Toasty.info(home.this, "No Data Found!", Toast.LENGTH_SHORT, true).show();
                        }


                    }
                });

            } else {

            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }


    }


    public class FetchAllContacts extends AsyncTask<String, Integer, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // setup a progress dialog
            progressDialog = new ProgressDialog(home.this);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle("Creating A backUp");
            progressDialog.setMessage("please wait we are making a backup of your contacts on cloud...");
            // make a button
            progressDialog.show();

        }

        @Override
        protected String doInBackground(String... strings) {

            ContentResolver cr = getContentResolver();

            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            if (cursor != null) {
                HashSet<String> mobileNoSet = new HashSet<String>();
                try {
                    final int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    final int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String name, number;
                    int contactsCounter = 0;
                    while (cursor.moveToNext()) {
                        name = cursor.getString(nameIndex);
                        number = cursor.getString(numberIndex);
                        number = number.replace(" ", "");
                        if (!mobileNoSet.contains(number)) {
                            String key = mDatabaseRef.push().getKey();
                            CloudContacts contacts = new CloudContacts(key, name, number);
                            Map<String, Object> postValues = contacts.toMap();
                            Map<String, Object> childUpdates = new HashMap<>();
                            childUpdates.put(key, postValues);
                            mDatabaseRef.child(CurrentUserId).updateChildren(childUpdates);
                            contactList.add(new Contact(name, number));
                            mobileNoSet.add(number);
                        }
                    }


                } finally {
                    cursor.close();
                    contactList.clear();
                    mobileNoSet.clear();
                }
            }

            return "done";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressDialog.dismiss();
        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @AfterPermissionGranted(123)
    private void ReadContacts() {
        String[] perms = {android.Manifest.permission.INTERNET, Manifest.permission.READ_CONTACTS};
        if (EasyPermissions.hasPermissions(this, perms)) {
            getContactList();
        } else {
            EasyPermissions.requestPermissions(this, "We need permissions because this and that",
                    123, perms);
        }
    }

    private void getContactList() {
        FetchAllContacts fetchAllContacts = new FetchAllContacts();
        fetchAllContacts.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
        }
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Toasty.error(this,"Update flow failed.",Toasty.LENGTH_SHORT,true).show();
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