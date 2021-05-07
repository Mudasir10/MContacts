package com.mudasir.mcontacts.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mudasir.mcontacts.R;
import com.mudasir.mcontacts.models.CloudContacts;
import com.mudasir.mcontacts.models.Contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class uploadContacts extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{


    Button btnUploadContactsOnCloud,btnCancel;
    List<Contact> contactList = new ArrayList<>();
     DatabaseReference mDatabaseRef;
     String id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_contacts);



        btnUploadContactsOnCloud=findViewById(R.id.btnUpload);
        btnCancel=findViewById(R.id.btnCancel);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference("Contacts");


        if (getIntent().getExtras() != null) {
            id = getIntent().getStringExtra("id");
        }


        btnUploadContactsOnCloud.setOnClickListener(v -> {


            ReadContacts();



        });


        btnCancel.setOnClickListener(v -> {

            Intent intent= new Intent();
            intent.putExtra("msg","not done");
            setResult(Activity.RESULT_OK,intent );
            finish();


        });


    }


    private void getContactList() {
        FetchAllContacts fetchAllContacts = new FetchAllContacts();
        fetchAllContacts.execute();
    }

    private static final String[] PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };



    public class FetchAllContacts extends AsyncTask<String, Integer, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // setup a progress dialog
            progressDialog = new ProgressDialog(uploadContacts.this);
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
                            mDatabaseRef.child(id).updateChildren(childUpdates);
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

            Intent intent= new Intent();
            intent.putExtra("msg","done");
            setResult(Activity.RESULT_OK,intent );
            finish();
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

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }
}