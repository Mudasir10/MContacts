package com.mudasir.mcontacts.reciever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import static com.mudasir.mcontacts.home.checkInternet;
public class NetworkChangeReceiver  extends BroadcastReceiver
{

    private boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            //should check null because in airplane mode it will be null
            return (netInfo != null && netInfo.isConnected());
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try
        {
            if (isOnline(context)) {
                checkInternet(true,context);
                Log.e("home : ", "Online Connect Intenet ");
            } else {
                checkInternet(false,context);
                Log.e("home : ", "Conectivity Failure !!! ");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}