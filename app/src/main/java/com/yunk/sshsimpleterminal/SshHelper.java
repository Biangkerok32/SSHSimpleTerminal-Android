package com.yunk.sshsimpleterminal;

import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bradley on 2018/12/19.
 */

public class SshHelper {
    private final static String TAG = "AI800M_SSHHELPER";
    private static final Logger LOGGER =
            Logger.getLogger(SshHelper.class.getName());
    private JSch jschSSHChannel;
    private String strUserName;
    private String strConnectionIP;
    private int intConnectionPort;
    private String strPassword;
    private Session sesConnection;
    private int intTimeOut;

    public SshHelper(String userName, String password,
                     String connectionIP, String knownHostsFileName) {
        doCommonConstructorActions(userName, password,
                connectionIP, knownHostsFileName);
        intConnectionPort = 22;
        intTimeOut = 60000;
    }

    public SshHelper(String userName, String password, String connectionIP,
                     String knownHostsFileName, int connectionPort) {
        doCommonConstructorActions(userName, password, connectionIP,
                knownHostsFileName);
        intConnectionPort = connectionPort;
        intTimeOut = 60000;
    }

    public SshHelper(String userName, String password, String connectionIP,
                     String knownHostsFileName, int connectionPort, int timeOutMilliseconds) {
        doCommonConstructorActions(userName, password, connectionIP,
                knownHostsFileName);
        intConnectionPort = connectionPort;
        intTimeOut = timeOutMilliseconds;
    }

    private void doCommonConstructorActions(String userName, String password,
                                            String connectionIP, String knownHostsFileName) {
        jschSSHChannel = new JSch();

        try {
            jschSSHChannel.setKnownHosts(knownHostsFileName);
        }
        catch(JSchException jschX) {
            Log.e(TAG, "doCommonConstructorActions, JSchException: "+jschX.getMessage());
        }

        strUserName = userName;
        strPassword = password;
        strConnectionIP = connectionIP;
    }

    public String connect() {
        String errorMessage = null;
        try {
            sesConnection = jschSSHChannel.getSession(strUserName,
                    strConnectionIP, intConnectionPort);
            sesConnection.setPassword(strPassword);
            // UNCOMMENT THIS FOR TESTING PURPOSES, BUT DO NOT USE IN PRODUCTION
            sesConnection.setConfig("StrictHostKeyChecking", "no");
            sesConnection.connect(intTimeOut);
            Log.i(TAG,"After connect...");
        }
        catch(JSchException jschX) {
            Log.e(TAG,"Connect failed!!!");
            errorMessage = jschX.getMessage();
        }

        return errorMessage;
    }

    public String sendCommand(String command) {
        StringBuilder outputBuffer = new StringBuilder();

        try {
            Channel channel = sesConnection.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);
            InputStream commandOutput = channel.getInputStream();
            channel.connect();
            int readByte = commandOutput.read();

            while(readByte != 0xffffffff)
            {
                outputBuffer.append((char)readByte);
                readByte = commandOutput.read();
            }

            channel.disconnect();

        } catch(IOException ioX) {
            Log.e(TAG, "sendCommand, IOException: "+ioX.getMessage());
            return null;
        } catch(JSchException jschX) {
            Log.e(TAG, "sendCommand, JSchException: "+jschX.getMessage());
            return null;
        }
        return outputBuffer.toString();
    }

    public void close() {
        sesConnection.disconnect();
    }

    private String logError(String errorMessage) {
        if(errorMessage != null) {
            LOGGER.log(Level.SEVERE, "{0}:{1} - {2}",
                    new Object[]{strConnectionIP, intConnectionPort, errorMessage});
        }
        return errorMessage;
    }

    private String logWarning(String warnMessage) {
        if(warnMessage != null) {
            LOGGER.log(Level.WARNING, "{0}:{1} - {2}",
                    new Object[]{strConnectionIP, intConnectionPort, warnMessage});
        }
        return warnMessage;
    }

    public String getStrConnectionIP() {
        return strConnectionIP;
    }

    public boolean isSpeakerConnected() {
        if (sesConnection != null) {
            return sesConnection.isConnected();
        } else {
            return false;
        }
    }
}
