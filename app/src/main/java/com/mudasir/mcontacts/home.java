package com.mudasir.mcontacts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mudasir.mcontacts.adapters.ContactsAdapter;
import com.mudasir.mcontacts.listeners.OnContactsClickListener;
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

public class home extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, SwipeRefreshLayout.OnRefreshListener{

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
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String TEXT = "text";
    static ProgressBar progressBar;

    String text;
    private int mpos;
    private NetworkChangeReceiver mNetworkReceiver;
    static ActionBar actionbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        getDatabaseRefAndUserId();

        init();
        if (haveNetworkConnection()){
            getDataFromDatabase(this);
        }
        else{
            progressBar.setVisibility(View.GONE);
            tvalert.setVisibility(View.VISIBLE);
            tvalert.setText(R.string.no_internet);
        }

        initSharedPreferences();


    }

    private void registerNetworkBroadcastForNougat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    protected void unregisterNetworkChanges() {
        try {
            unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


    public static boolean checkInternet(boolean value,Context context){
        if(value){
            tvalert.setText("We are back !!!");
            tvalert.setBackgroundColor(Color.GREEN);
            tvalert.setTextColor(Color.WHITE);
            Handler handler = new Handler();
            Runnable delayrunnable = new Runnable() {
                @Override
                public void run() {
                    tvalert.setVisibility(View.GONE);
                    getDataFromDatabase(context);
                }
            };
            handler.postDelayed(delayrunnable, 3000);
            return true;
        }else {
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
                    recyclerView.setAdapter(null);
                    dynamiccontactList.clear();
                }
            };
            handler.postDelayed(delayrunnable, 3000);

            return false;
        }
    }

    private static void fillintoRecyclerView(Context context) {

        mAdapter = new ContactsAdapter(context, dynamiccontactList,CurrentUserId);
        recyclerView.setAdapter(mAdapter);
       // getSupportActionBar().setSubtitle("Contacts Count: " + dynamiccontactList.size());
        // if has Data
        progressBar.setVisibility(View.GONE);
        tvalert.setVisibility(View.GONE);

    }

    public static void getDataFromDatabase(Context context) {

        mDatabaseRef.child(CurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    dynamiccontactList.clear();
                    for (DataSnapshot shot : snapshot.getChildren()) {
                        CloudContacts contacts = shot.getValue(CloudContacts.class);
                        dynamiccontactList.add(contacts);
                    }
                    fillintoRecyclerView(context);
                    swipLayout.setRefreshing(false);
                    actionbar.setSubtitle("Contacts Count: "+dynamiccontactList.size());


                } else {
                    swipLayout.setRefreshing(false);
                    // if has not Data
                    progressBar.setVisibility(View.GONE);
                    tvalert.setVisibility(View.VISIBLE);
                    tvalert.setBackgroundColor(Color.BLUE);
                    tvalert.setText(R.string.no_contacts);

                    Toasty.info(context, "No Data Found!", Toast.LENGTH_SHORT, true).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterNetworkChanges();
    }

    private void initSharedPreferences() {
        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    private void getDatabaseRefAndUserId() {
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("Contacts");
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        CurrentUserId = mCurrentUser.getUid();
    }

    private void init() {
        dynamiccontactList = new ArrayList<>();
        actionbar=getSupportActionBar();
        actionbar.setTitle("MContacts- Backup on Cloud");
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
                CallUser();
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onSwipedRight(int position) {
                //Message Code here
                mpos=position;
                sendSms();
                mAdapter.notifyDataSetChanged();
            }
        });

        progressBar = findViewById(R.id.progress);
        tvalert = findViewById(R.id.txtAlert);
        progressBar.setVisibility(View.VISIBLE);
        mNetworkReceiver = new NetworkChangeReceiver();
        registerNetworkBroadcastForNougat();
    }

    private static final String[] PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.home_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView searchView= (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (haveNetworkConnection()){
                    if (!mAdapter.equals(null)){
                        mAdapter.getFilter().filter(newText);
                    }
                }
                else{
                    Toasty.error(home.this,"No data!",Toasty.LENGTH_SHORT,true).show();
                }
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.btnSaveAll);

        text = sharedPreferences.getString(TEXT, "read");

        if (text.equals("read")) {
            item.setTitle("Save All Contacts");
        }
        if (text.equals("delete")) {
            item.setTitle("Delete All Contacts");
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
                if (haveNetworkConnection()){
                    Intent intent = new Intent(home.this, AccountDetails.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(this, "Enable Internet Connection to go to Account Details Activity!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnAddContact:
                if (haveNetworkConnection()){
                    Intent intent = new Intent(home.this, AddContactsActivity.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(this, "Enable Internet Connection to go to Account Details Activity!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btnSaveAll:
                if (text.equals("read")) {
                    if (haveNetworkConnection()){
                        ReadContacts();
                        editor.putString(TEXT, "delete");
                        editor.apply();
                        item.setTitle("Delete All Contacts");
                    }
                    else{
                        Toasty.error(this,"Can not Read And Store Contacts While You dont have internet Connection",Toasty.LENGTH_SHORT,true).show();
                    }


                }
                if (text.equals("delete")) {
                    if (haveNetworkConnection()){
                        DeleteAllContacts(CurrentUserId, item);
                    }else{
                        Toasty.error(this,"Can not Delete Contacts While You dont have internet Connection",Toasty.LENGTH_SHORT,true).show();
                    }

                }
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void DeleteAllContacts(String uid, MenuItem item) {

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
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(home.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    @AfterPermissionGranted(1)
    private void CallUser() {
        String[] perms = {Manifest.permission.CALL_PHONE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            call();
        } else {
            EasyPermissions.requestPermissions(this, "We need Call permissions to Call The User",
                    1, perms);
        }
    }

    private void call() {
        CloudContacts contact = dynamiccontactList.get(mpos);
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + contact.getPhone()));
        startActivity(callIntent);
    }


    @AfterPermissionGranted(2)
    private void sendSms() {
        String[] perms = {Manifest.permission.SEND_SMS};
        if (EasyPermissions.hasPermissions(this, perms)) {
            Sms();
        } else {
            EasyPermissions.requestPermissions(this, "We need Send_Sms permissions to send sms!",
                    2, perms);
        }
    }

    private void Sms() {
        final EditText taskEditText = new EditText(this);
        taskEditText.setHint("Type Msg here...");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Send Message")
                .setView(taskEditText)
                .setPositiveButton("Send", (dialog1, which) -> {
                    String task = String.valueOf(taskEditText.getText());
                    CloudContacts contact = dynamiccontactList.get(mpos);
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(contact.getPhone(), null, task, null, null);

                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

    }

    @Override
    public void onRefresh() {
        if (haveNetworkConnection()){
            getDataFromDatabase(this);
        }
        else{

        }

    }


    public class FetchAllContacts extends AsyncTask<String, Integer, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(home.this);
            progressDialog.setTitle("Fetching Contacts");
            progressDialog.setMessage("Wait until it finishes...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
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
                    while (cursor.moveToNext()) {
                        name = cursor.getString(nameIndex);
                        number = cursor.getString(numberIndex);
                        number = number.replace(" ", "");
                        if (!mobileNoSet.contains(number)) {
                            contactList.add(new Contact(name, number));
                            mobileNoSet.add(number);
                        }
                    }

                } finally {
                    cursor.close();
                    for (Contact contact : contactList) {
                        String key = mDatabaseRef.push().getKey();
                        CloudContacts contacts = new CloudContacts(key, contact.getName(), contact.getPhoneno());
                        Map<String, Object> postValues = contacts.toMap();
                        Map<String, Object> childUpdates = new HashMap<>();
                        childUpdates.put(key, postValues);
                        mDatabaseRef.child(CurrentUserId).updateChildren(childUpdates);
                    }
                    contactList.clear();
                    mobileNoSet.clear();

                }
            }

            return "done";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);


            Handler handler = new Handler();
            handler.postDelayed(() -> {

                progressDialog.dismiss();


            }, 5000);


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