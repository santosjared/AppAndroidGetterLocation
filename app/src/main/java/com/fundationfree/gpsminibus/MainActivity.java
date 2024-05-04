package com.fundationfree.gpsminibus;

import android.annotation.SuppressLint;
import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity implements SocketManager.SocketConnectionListener{
    private LocationHandler locationHandler;
    private Socket socketClient;
    private TextInputEditText textUser;
    private TextInputEditText textKey;
    private String user;
    private String key;
    private  String server;
    private TextInputEditText textServer;
    private TextView tvuser;
    private TextView tvkey;
    private TextView fail;
    private TextView tvTitle;
    private Button connect;
    private Button editable;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @SuppressLint({"CutPasteId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        locationHandler = new LocationHandler(this);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        textServer = (TextInputEditText) findViewById(R.id.serverEdit);
        textUser = (TextInputEditText) findViewById(R.id.textUser);
        textKey = (TextInputEditText) findViewById(R.id.textKey);
        fail = (TextView) findViewById(R.id.fail);
        tvkey = (TextView) findViewById(R.id.textVKey);
        tvuser = (TextView) findViewById(R.id.tvUser);
        tvTitle = (TextView) findViewById(R.id.title);
        connect = (Button) findViewById(R.id.connect);
        editable = (Button) findViewById(R.id.NewRegister);
        editable.setOnClickListener(v -> {
            textUser.setEnabled(true);
            textKey.setEnabled(true);
            textServer.setEnabled(true);
        });
        if(!verifyCredentiales()){
            editable.setVisibility(View.GONE);
        }
        connect.setOnClickListener(v -> {
            String severText = Objects.requireNonNull(textServer.getText()).toString();
            if(SocketManager.isConnected()){
                connect.setText("Desconectando....");
                socketClient.emit("disconnectedDivice",user);
                socketClient.on("resdisconnected", disconnetedMessage);
            }else{
                SocketManager.iniSocket(this, severText);
            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private final Emitter.Listener disconnetedMessage = args -> {
        SocketManager.disconnectSocket();
        socketClient.disconnect();
    };

    @SuppressLint("SetTextI18n")
    @Override
    public void onConnectionLoading() {
        connect.setText("conectando...");
        connect.setEnabled(false);
        textServer.setEnabled(false);
        textKey.setEnabled(false);
        textUser.setEnabled(false);
    }

    @Override
    public void onConnectionSuccess() {
        socketClient = SocketManager.getSocket();
        user = Objects.requireNonNull(textUser.getText()).toString();
        key = Objects.requireNonNull(textKey.getText()).toString();
        server = Objects.requireNonNull(textServer.getText()).toString();
        socketClient.emit("divice",user,key);
        socketClient.on("resDivice", response);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fail.setText("");
            }
        });

//        messageService.setSocketClient(socketClient);
//        Intent serviceIntent = new Intent(this, MessageService.class);
//        startService(serviceIntent);
//        if(messageService !=null){
//            Log.e("MainAcskjdsdhsh", "El servicio eniciado");
//        }else {
//            Log.e("MainActivityklaskdasdasjkl", "El servicio MessageService no está inicializado correctamente");
//        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onConnectionError() {
        if(socketClient !=null){
            socketClient.disconnect();
        }
        if(SocketManager.getSocket() !=null)
        {
            SocketManager.disconnectSocket();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connect.setText("conectar");
                connect.setEnabled(true);
                fail.setText("No se puede establecer la conexion con socket");
                fail.setTextColor(Color.RED);
                tvTitle.setText("Conectar con el sistema web Microbuses");
                tvTitle.setTextColor(Color.BLACK);
                if(verifyCredentiales()){
                    textServer.setEnabled(true);
                    textKey.setEnabled(true);
                    textUser.setEnabled(true);
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onConnectionConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connect.setText("desconectar");
                connect.setEnabled(true);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onConnectionDisconnected() {
        socketClient.disconnect();
        locationHandler.stopLocationUpdates();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connect.setText("Conectar");
                connect.setEnabled(true);
                tvTitle.setText("Conectar con el sistema web Microbuses");
                tvTitle.setTextColor(Color.BLACK);
            }
        });
    }

    private final Emitter.Listener onNewMessage = args -> {
        Location currentLocation = locationHandler.getLocation();
        if(currentLocation !=null)
        {
            socketClient.emit("sendLocation", currentLocation.getLatitude(),currentLocation.getLongitude(), user);
        }
    };
    @SuppressLint("SetTextI18n")
    private final Emitter.Listener response = args -> {
       if (args.length>0){
          String message = args[0].toString();
            if (message.equals("success")){
                saveCredentiales();
                initSendLocation();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvkey.setText("");
                        tvuser.setText("");
                    }
                });

            }else {
                socketClient.disconnect();
                SocketManager.disconnectSocket();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvuser.setText("Usuario incorrecto");
                        tvuser.setTextColor(Color.RED);
                        tvkey.setText("Clave incorrecta");
                        tvkey.setTextColor(Color.RED);
                        if(verifyCredentiales()){
                            textServer.setEnabled(true);
                            textKey.setEnabled(true);
                            textUser.setEnabled(true);
                        }
                    }
                });
            }
        }else {
            SocketManager.disconnectSocket();
            socketClient.disconnect();
        }
    };
    private void saveCredentiales(){
        SharedPreferences sharedPreferences = getSharedPreferences("qwer", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("clave", key);
        editor.putString("usuario", user);
        editor.putString("url", server);
        editor.apply();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editable.setVisibility(View.VISIBLE);
            }
        });
    }
    @SuppressLint("SetTextI18n")
    private void initSendLocation(){
        locationHandler.startLocationUpdates();

        socketClient.on("getlocation", onNewMessage);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvTitle.setText("* Conectado con el sistema web Microbuses");
                tvTitle.setTextColor(Color.rgb(0,157,113));
            }
        });
    }
    private boolean verifyCredentiales (){
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("qwer", Context.MODE_PRIVATE);
            String clave = sharedPreferences.getString("clave", null);
            String usuario = sharedPreferences.getString("usuario", null);
            String url = sharedPreferences.getString("url",null);
            if (clave != null && usuario != null && url !=null) {
                textUser.setText(usuario);
                textKey.setText(clave);
                textServer.setText(url);
                textUser.setEnabled(false);
                textKey.setEnabled(false);
                textServer.setEnabled(false);
                editable.setVisibility(View.VISIBLE);
                return true;
            } else {
                textUser.setEnabled(true);
                textKey.setEnabled(true);
                textServer.setEnabled(true);
                return false;
            }

        }catch (Exception e){
            return false;
        }
    }
}