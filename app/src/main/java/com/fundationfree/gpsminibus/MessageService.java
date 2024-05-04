package com.fundationfree.gpsminibus;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MessageService extends Service {

    private LocationHandler locationHandler;
    private Socket socketClient;

    @Override
    public  void onCreate(){
        super.onCreate();
        locationHandler = new LocationHandler(this);
    }

    public void setSocketClient (Socket client){
        this.socketClient=client;
    }

//    @Override
//    public  int onStartCommand(Intent intent, int flag, int IdProcces){
//        Toast.makeText(this,"servicio iniciado", Toast.LENGTH_SHORT).show();
//        locationHandler.startLocationUpdates();
//        if(socketClient != null){
//            Log.e("mana null", "no es nulo");
//        }else{
//            Log.e("Joder", "es nulo piche android");
//        }
//        socketClient.on("getLocation", onNewMessage);
//        return START_STICKY;
//    }
    private final Emitter.Listener onNewMessage = args -> {
//        JSONObject data = (JSONObject) args[0];
//        Toast.makeText(this, "mensaje recibido", Toast.LENGTH_SHORT).show();
//        resendMessage();
    };
    private void resendMessage(){
        Object currentLocation = locationHandler.getLocation();
        socketClient.emit("sendLocation", currentLocation);
    }

    @Override
    public void  onDestroy(){
        locationHandler.stopLocationUpdates();
        socketClient.disconnect();
        super.onDestroy();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
