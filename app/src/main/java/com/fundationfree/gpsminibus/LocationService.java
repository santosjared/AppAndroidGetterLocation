package com.fundationfree.gpsminibus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Socket socket;
    private String email;
    private String password;
    private String server;
    boolean connected = false;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Servicio de localizacion activo")
                .setContentText("Se esta enviando la localizacion de teléfono")
                .setSmallIcon(R.drawable.baseline_directions_bus_24)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    if(connected){
                        sendLocationToServer(location);
                    }
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void connectToSocket(String server) {
        sendSocketConnectionStatus(true, false,false,false, false);
        if (!server.startsWith("http://") && !server.startsWith("https://")) {
            server = "http://" + server;
        }
        try {
            socket = IO.socket(server); // Reemplaza "your_server_url" con la URL de tu servidor Socket.io
            socket.connect();
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    initChat();
                }
            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    sendSocketConnectionStatus(false, false,false,true, false);
                }
            }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener(){
                @Override
                public void call(Object... args){
                    sendSocketConnectionStatus(false, false,true,false, false);
                }
            });
        } catch (URISyntaxException e) {
            sendSocketConnectionStatus(false, false,true,false, false);
            e.printStackTrace();
        }
    }

    private void sendLocationToServer(Location location) {
        JSONObject data = new JSONObject();
        try {
            data.put("latitude", location.getLatitude());
            data.put("longitude", location.getLongitude());
            socket.emit("sendLocation", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendSocketConnectionStatus(boolean isLoading, boolean isSuccess, boolean isError, boolean isDisconnected, boolean credencial){
        Intent intent = new Intent("socket_connection_status");
        intent.putExtra("isLoading", isLoading);
        intent.putExtra("isSuccess", isSuccess);
        intent.putExtra("isError", isError);
        intent.putExtra("isDisconnected", isDisconnected);
        intent.putExtra("credentials",credencial);
        sendBroadcast(intent);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (socket != null) {
            socket.emit("disconnectedDivice","");
            socket.disconnect();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent !=null)
        {
            email = intent.getStringExtra("email");
            password = intent.getStringExtra("password");
            server = intent.getStringExtra("server");
            connectToSocket(server);
            createNotificationChannel();
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            createLocationRequest();
            startForeground(1, createNotification());
        }
        return START_STICKY;
    }
    private void initChat(){
        if(socket !=null){
            JSONObject data = new JSONObject();
            try {
                data.put("email", email);
                data.put("password", password);
                socket.emit("divice", data);
                socket.on("resDivice", response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    private final Emitter.Listener response = args -> {
       if (args.length>0){
          String message = args[0].toString();
            if (message.equals("success")){
                connected=true;
                sendSocketConnectionStatus(false, true,false,false,false);
            }else {
                sendSocketConnectionStatus(false, false,false,false,true);
            }
        }else {
           sendSocketConnectionStatus(false, false,false,false,true);
        }
    };
}