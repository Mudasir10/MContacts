package com.mudasir.mcontacts;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<List<CloudContacts>> contacts;

    public LiveData<List<CloudContacts>> getUsers(String id) {
        if (contacts == null) {
            contacts = new MutableLiveData<List<CloudContacts>>();
            loadUsers(id);
        }
        return contacts;
    }

    private void loadUsers(String id) {
       new FetchContacts(id).execute();
    }


    public class FetchContacts extends AsyncTask<String ,Void ,Void>{

        DatabaseReference database=FirebaseDatabase.getInstance().getReference().child("Contacts");
        String uid;
        private List<CloudContacts> cloudContacts;

        public FetchContacts(String id) {
            uid=id;
            cloudContacts=new ArrayList<>();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {

            if (uid!=null){
                database.child(uid).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()) {
                            cloudContacts.clear();
                            for (DataSnapshot shot : snapshot.getChildren()) {
                                CloudContacts contacts = shot.getValue(CloudContacts.class);
                                cloudContacts.add(contacts);
                            }
                            contacts.setValue(cloudContacts);

                        }
                        else{

                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }



            return null;
        }
    }


}
