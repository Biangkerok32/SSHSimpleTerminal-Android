package com.yunk.sshsimpleterminal;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "SSHSimpleMain";
    private Button connectHost, cmdSend;
    private TextView terminalText;
    private EditText inputText;
    private ImageView connectionStatus;
    private SshHelper sshHelper;
    private String selectedIP, username, password, connectResult, cmdResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectHost = (Button) findViewById(R.id.btn_connect);
        connectHost.setOnClickListener(buttonListener);
        cmdSend = (Button) findViewById(R.id.btn_send);
        cmdSend.setOnClickListener(buttonListener);
        terminalText = (TextView) findViewById(R.id.tv_terminal);
        terminalText.setMovementMethod(new ScrollingMovementMethod());
        inputText = (EditText) findViewById(R.id.et_input);
        connectionStatus = (ImageView) findViewById(R.id.iv_status);

    }

    private Button.OnClickListener buttonListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_connect:
                    setIpConnection();
                    break;

                case R.id.btn_send:
                    sendCommand();
                    break;
            }
        }
    };

    private void setIpConnection() {
        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        View dialogView = inflater.inflate(R.layout.dialog_custom_connect, null);
        final EditText input_address = (EditText) dialogView.findViewById(R.id.hostIP);
        final EditText input_id = (EditText) dialogView.findViewById(R.id.customUser);
        final EditText input_pw = (EditText) dialogView.findViewById(R.id.customPw);
        input_address.setText(getSharedPreferences("DebuggerConfig", MODE_PRIVATE).getString("Host", ""));
        input_id.setText(getSharedPreferences("DebuggerConfig", MODE_PRIVATE).getString("ID", ""));
        input_pw.setText(getSharedPreferences("DebuggerConfig", MODE_PRIVATE).getString("PW", ""));
        final Button setOK = (Button) dialogView.findViewById(R.id.buttonConnect);
        final Button setCancel = (Button) dialogView.findViewById(R.id.buttonCancel);
        setOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reConnectPreAction();
                selectedIP = input_address.getText().toString();
                username = input_id.getText().toString();
                password = input_pw.getText().toString();
                new ConnectTask().execute(selectedIP);
                SharedPreferences pref = getSharedPreferences("DebuggerConfig", MODE_PRIVATE);
                pref.edit()
                        .putString("Host", input_address.getText().toString())
                        .putString("ID", input_id.getText().toString())
                        .putString("PW", input_pw.getText().toString())
                        .commit();
                dialog.dismiss();
            }
        });
        setCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setView(dialogView);
        dialog.show();
    }

    private class ConnectTask extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... ip) {
            try {
                Log.i(TAG, "start connect to speaker: "+ip[0]);
                sshHelper = new SshHelper(username, password, ip[0], "");
                Log.d(TAG, "Do a new Connection now");
                connectResult = sshHelper.connect();
                Log.i(TAG, connectResult);
                return connectResult;
            } catch (Exception e) {
                return e.toString();
            }
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (sshHelper.isSpeakerConnected()) {
                Log.i(TAG, "After successfully connection, send check state cmd");
                Toast.makeText(MainActivity.this, "Successfully Connect to "+selectedIP, Toast.LENGTH_SHORT).show();
                cmdSend.setEnabled(true);
                connectionStatus.setImageResource(android.R.drawable.presence_online);
            } else {
                Log.i(TAG, "Connect Failed!");
                disableUIContrl();
                Toast.makeText(MainActivity.this, "Failed Connect to "+selectedIP, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendCommand(){
        cmdSend.setEnabled(false);
        if (sshHelper!=null && sshHelper.isSpeakerConnected()) {
            terminalText.append(inputText.getText().toString()+"\n");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    cmdResult = sshHelper.sendCommand(inputText.getText().toString());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            terminalText.append(cmdResult +"\n");
                            cmdSend.setEnabled(true);
                        }
                    });
                }
            }).start();
        } else {
            Toast.makeText(MainActivity.this, "Connection lost!! Please re-connect to device", Toast.LENGTH_SHORT).show();
        }
    }

    //If there's already a connection, before add new connection, this function must be called
    private void reConnectPreAction(){
        if (sshHelper!=null && sshHelper.isSpeakerConnected()) {
            sshHelper.close();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    disableUIContrl();
                }
            });
        }
    }

    private void disableUIContrl(){
        Log.i(TAG, "No Connection, disable UI");
        cmdSend.setEnabled(false);
        connectionStatus.setImageResource(android.R.drawable.presence_busy);
    }
}
