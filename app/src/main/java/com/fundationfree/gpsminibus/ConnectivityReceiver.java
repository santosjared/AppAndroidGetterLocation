package com.fundationfree.gpsminibus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityReceiver extends BroadcastReceiver {
    private static boolean connectInternet = false;

    private  interface ConectivityListener {
        void onConnectInternet();
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        // Obtiene información sobre la red
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        // Verifica si hay conexión de red disponible
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

        // Muestra un mensaje al usuario dependiendo del estado de la conexión
        if (isConnected) {
            connectInternet = true;
//            Toast.makeText(context, "Conectado a Internet", Toast.LENGTH_SHORT).show();
        } else {
//            Toast.makeText(context, "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
        }
    }
}

