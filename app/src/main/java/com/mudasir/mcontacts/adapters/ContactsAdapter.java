package com.mudasir.mcontacts.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mudasir.mcontacts.AddContactsActivity;
import com.mudasir.mcontacts.CloudContacts;

import com.mudasir.mcontacts.R;
import com.mudasir.mcontacts.listeners.OnContactsClickListener;
import com.tsuryo.swipeablerv.SwipeableRecyclerView;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactsViewHolder> implements Filterable {

    private Context mContext;
    private List<CloudContacts> contactList;
    private List<CloudContacts> exampleListFull;
    RecyclerView recyclerView = null;
    String Uid;

    public ContactsAdapter(Context mContext, List<CloudContacts> contactList, String currentUserId) {
        this.mContext = mContext;
        this.contactList = contactList;
        exampleListFull = new ArrayList<>(contactList);
        Uid = currentUserId;
    }

    @NonNull
    @Override
    public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View root = LayoutInflater.from(mContext).inflate(R.layout.single_item_contact, parent, false);
        return new ContactsViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactsViewHolder holder, int position) {

        CloudContacts contact = contactList.get(position);
        holder.tvname.setText(contact.getName());
        holder.tvPhoneno.setText(contact.getPhone());

        boolean isExpanded = contactList.get(position).isExpanded();
        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    @Override
    public Filter getFilter() {
        return exampleFilter;
    }

    private Filter exampleFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            List<CloudContacts> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(exampleListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (CloudContacts item : exampleListFull) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;

        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            contactList.clear();
            contactList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };


    public class ContactsViewHolder extends RecyclerView.ViewHolder {

        TextView tvname, tvPhoneno;
        LinearLayout expandableLayout;
        Button btnDelete, btnUpdate;

        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvname = itemView.findViewById(R.id.tvname);
            tvPhoneno = itemView.findViewById(R.id.tvphone);
            expandableLayout = itemView.findViewById(R.id.expandablelayout);
            btnDelete = itemView.findViewById(R.id.btndelete);
            btnUpdate = itemView.findViewById(R.id.btnUpdate);

            btnDelete.setOnClickListener(v -> {

                if (haveNetworkConnection()) {

                    new AlertDialog.Builder(mContext)
                            .setTitle("Delete Contact")
                            .setMessage("Are You Sure You want to Delete Contact From Cloud.")
                            .setIcon(R.drawable.ic_delete)
                            .setPositiveButton("Yes", (dialog, which) -> {
                                dialog.dismiss();
                                int pos = getAdapterPosition();
                                String key = contactList.get(pos).getKey();
                                DatabaseReference mDatabaseRef = FirebaseDatabase.getInstance().getReference("Contacts");
                                DatabaseReference deleteRef = mDatabaseRef.child(Uid).child(key);
                                deleteRef.removeValue().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        notifyDataSetChanged();
                                        Toasty.success(mContext, "Successfully Delete Contacts", Toasty.LENGTH_SHORT, true).show();
                                    } else {
                                        Toasty.error(mContext, "Failed to  Delete Contacts", Toasty.LENGTH_SHORT, true).show();
                                    }
                                });

                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                dialog.cancel();

                            }).create().show();


                } else {
                    Toasty.error(mContext, "No Internet Connection!", Toasty.LENGTH_SHORT, true).show();
                }


            });

            btnUpdate.setOnClickListener(v -> {

                if (haveNetworkConnection()) {

                    int pos = getAdapterPosition();
                    String key = contactList.get(pos).getKey();
                    String name = contactList.get(pos).getName();
                    String phone = contactList.get(pos).getPhone();
                    Intent intent = new Intent(mContext, AddContactsActivity.class);
                    intent.putExtra("key", key);
                    intent.putExtra("name", name);
                    intent.putExtra("phone", phone);
                    mContext.startActivity(intent);

                } else {
                    Toasty.error(mContext, "No Internet Connection!", Toasty.LENGTH_SHORT, true).show();
                }


            });

            itemView.setOnClickListener(v -> {
                CloudContacts contacts = contactList.get(getAdapterPosition());
                contacts.setExpanded(!contacts.isExpanded());
                notifyItemChanged(getAdapterPosition());
            });
        }

        private boolean haveNetworkConnection() {
            boolean haveConnectedWifi = false;
            boolean haveConnectedMobile = false;

            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
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

}
