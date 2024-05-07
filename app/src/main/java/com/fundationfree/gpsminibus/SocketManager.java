package com.fundationfree.gpsminibus;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;

public class SocketManager {
    private static Socket socket;
    private static boolean isLoading = false;
    private static boolean isSuccess = false;
    private static boolean isError = false;
    private  static  boolean isConnected = false;
    private static  boolean isDisconnected = false;

    public interface SocketConnectionListener {
        void onConnectionLoading();
        void onConnectionSuccess();
        void onConnectionError();
        void onConnectionConnected();
        void onConnectionDisconnected();
    }
    public static  void  iniSocket(SocketConnectionListener listener, String serverUrl){
        isLoading = true;
        listener.onConnectionLoading();

        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            serverUrl = "http://" + serverUrl;
        }

        try {
                socket = IO.socket(serverUrl);
                socket.connect();
            socket.on(Socket.EVENT_CONNECT, args -> {
                isSuccess = true;
                isError = false;
                isLoading = false;
                isConnected = true;
                isDisconnected = true;
                listener.onConnectionSuccess();
                listener.onConnectionConnected();
            }).on(Socket.EVENT_CONNECT_ERROR, args -> {
                isSuccess = false;
                isConnected =false;
                isError = true;
                isLoading = false;
                isDisconnected=true;
                socket.disconnect();
               listener.onConnectionError();
            }).on(Socket.EVENT_DISCONNECT, args -> {
                isSuccess = false;
                isConnected = false;
                isError = false;
                isLoading = false;
                isDisconnected = true;
                listener.onConnectionDisconnected();
            });
        } catch (URISyntaxException e) {
            isSuccess = false;
            isError = true;
            isConnected = false;
            isLoading = false;
            isDisconnected=true;
            listener.onConnectionError();
        }
    }

    public static Socket getSocket() {
        return socket;
    }

    public static boolean isLoading() {
        return isLoading;
    }

    public static boolean isSuccess() {
        return isSuccess;
    }

    public static boolean isError() {
        return isError;
    }

    public static boolean isConnected(){
        return isConnected;
    }
    public  static  boolean isDisconnected (){return isDisconnected;}

    public static void disconnectSocket() {
        if (socket != null && isConnected && socket.connected()) {
            socket.disconnect();
            isLoading = false;
            isError = false;
            isSuccess = false;
            isConnected = false;
            isDisconnected=true;

        }else{
            isDisconnected=true;
            isConnected = false;
            isLoading = false;
            isError = false;
            isSuccess = false;
        }
    }
}

