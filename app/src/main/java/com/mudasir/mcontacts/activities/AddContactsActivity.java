package com.mudasir.mcontacts.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mudasir.mcontacts.R;
import com.mudasir.mcontacts.models.CloudContacts;

import java.util.HashMap;
import java.util.Map;

import es.dmoral.toasty.Toasty;

public class AddContactsActivity extends AppCompatActivity {


    TextInputEditText etname, etphone;
    MaterialButton btnSave;

    String state = "save";

    DatabaseReference mDatabaseRef;
    FirebaseAuth mAuth;
    FirebaseUser mCurrentuser;
    String Uid;
    String key=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contacts);

        etname = findViewById(R.id.etname);
        etphone = findViewById(R.id.etphone);
        btnSave = findViewById(R.id.btnSave);
        mDatabaseRef= FirebaseDatabase.getInstance().getReference("Contacts");

        mAuth=FirebaseAuth.getInstance();
        mCurrentuser=mAuth.getCurrentUser();
        Uid=mCurrentuser.getUid();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add Contact");

        if (getIntent().getExtras() != null) {
            state = "update";
            key = getIntent().getStringExtra("key");
            String name = getIntent().getStringExtra("name");
            String phone = getIntent().getStringExtra("phone");
            btnSave.setText("Update");
            etname.setText(name);
            etphone.setText(phone);
            getSupportActionBar().setTitle("Update Contact");
        } else {
            state = "save";
        }

        btnSave.setOnClickListener(v -> {
            String name =etname.getText().toString();
            String phone =etphone.getText().toString();

            if (state.equals("save")) {

                if (!name.isEmpty() && !phone.isEmpty()){
                    // generate a new Key and Insert Contacts here
                    String newKey=mDatabaseRef.push().getKey();
                    CloudContacts cloudContacts=new CloudContacts(newKey,name,phone);
                    Map<String, Object> postValues = cloudContacts.toMap();
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(newKey, postValues);
                    mDatabaseRef.child(Uid).updateChildren(childUpdates);

                    Toast toast = Toasty.success(this,"Contact Added Successfully.",Toasty.LENGTH_SHORT,true);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    etname.setText("");
                    etphone.setText("");

                }
                else{
                    Toast toast =  Toasty.error(this,"Name and Phone Fields Can not be Empty",Toasty.LENGTH_SHORT,true);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }



            } else if (state.equals("update")) {

                if (!name.isEmpty() && !phone.isEmpty()){
                    // get key From Intent And Update Name And Phone
                    CloudContacts cloudContacts=new CloudContacts(key,name,phone);
                    Map<String, Object> postValues = cloudContacts.toMap();
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(key, postValues);
                    mDatabaseRef.child(Uid).updateChildren(childUpdates);

                    Toast toast = Toasty.success(this,"Contact Updated Successfully.",Toasty.LENGTH_SHORT,true);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                }
                else{
                    Toast toast =  Toasty.error(this,"Name and Phone Fields Can not be Empty",Toasty.LENGTH_SHORT,true);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    }


            }
        });


    }
}