package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@SuppressLint("Registered")
public class makeSocket extends Application {
    private Socket mSocket;// mSocket is my socket, roomInvited is room I've been invited to
    IO.Options opts;
    private boolean host=false, requestedUsers = false;// host defines whether you're a host for the room
    private String toUser, myUsername, myToken, userEmail, toRoom;
    private int toUUID, myUUID;// touuid and myuuid are used in socket to identify receipient/ sender
    private JSONArray unreadMsg;// stores messages incoming
    private JSONArray userList;// used to store list of total users
    private KeyPair RSAKP;
    private byte[] BobPub;

    private final String URL = "http://192.168.1.137:3000";
    @Override
    public void onCreate() {
        super.onCreate();
        userList = new JSONArray();
        unreadMsg = new JSONArray();
        toRoom = null;
        try {// creates socket. does not connect until token is set in setToken method
            opts = new IO.Options();
            opts.forceNew = true;
            mSocket = IO.socket(URL, opts);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    public void disconnect(){
        this.mSocket.disconnect();
    }
    public boolean getRequestedUsers(){
        return this.requestedUsers;
    }
    public void setRequestedUsers(){
        this.requestedUsers = true;
    }
    public void setHost(){
        this.host = !this.host;
    }
    public boolean getHost(){
        return this.host;
    }
    // adds to user list stores user as object with creds username and uuid
    public void addUserList(String user, int uuid) throws JSONException {
        JSONObject temp = new JSONObject();// creating user object
        temp.put("username", user);// setting values
        temp.put("uuid", uuid);
        if(userList.length() == 0)// checks if list is empty
            userList.put(0, temp);
        else
            userList.put(userList.length(), temp);
    }
    public JSONArray getUserList(){
        return this.userList;
    }

    public void setRoom(String temp){// sets room for multiple user room chat
        System.out.println("******** setting invited socket ");
        this.toRoom = temp;
    }
    public String getRoomInvited(){// returns value of room chat
        return this.toRoom;
    }
    // sets creds for my user as username and uuid for socket identification in server
    public void setMyUser(int myUUID, String myUsername){
        this.myUUID = myUUID;
        this.myUsername = myUsername;
        System.out.println("****************" + this.myUUID +"$" + this.myUsername);
    }
    // sets creds for server to id which socket/ uuser to look for
    public void setToUser(int toUUID, String toUser){
        this.toUUID = toUUID;
        this.toUser = toUser;
        System.out.println("****************" + this.toUUID +"$" + this.toUser);
    }
    // sets token, connects socket once token is received in MainActivity class
    public void setToken(String myToken){
        opts.query = "xAccessToken="+myToken;
        this.myToken = myToken;
        mSocket.connect();
    }
    public String getToken(){
        return this.myToken;
    }
    public String getMyUsername(){
        return this.myUsername;
    }
    public int getmyUUID(){
        return this.myUUID;
    }
    public String getToUser(){
        return this.toUser;
    }
    public int getToUUID(){
        return this.toUUID;
    }
    public String getUrl(){
        return this.URL;
    }
    public Socket getmSocket()
    {
        System.out.println(this.opts.query);
        return mSocket;
    }
    public String getUnreadMsgs(){
        return unreadMsg.toString();
    }
    public void setNewUnreadMsgs(JSONArray temp){
        this.unreadMsg = temp;
    }
    // stores message incoming in accordance to user sending
    public void storeMessage(String from, String msg){// stores msg in jsonarray unreadMsg
        try {//
            if (unreadMsg.length() == 0) {// first msg received unread
                addNew(from, msg);// if list is empty adds sender as first to list
            } else {// will have to look if user is already saved
                for (int i = 0; i < unreadMsg.length(); ++i) {// traverses list to search for sender
                    if (unreadMsg.getJSONObject(i).getString("fromUser").equals(from)) {
                        unreadMsg.getJSONObject(i).getJSONArray("messages").put(msg);
                        break;
                    }// list is not empty but sender's info was not found
                    else if (i==unreadMsg.length()-1){// end of list and no unread msgs from incoming user
                        addNew(from, msg);// will add sender to list as new obj
                        break;
                    }
                }
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
    public void addNew(String from, String msg) throws JSONException{
        JSONObject newTemp = new JSONObject();
        JSONArray messages = new JSONArray();
        messages.put(0, msg);
        newTemp.put("fromUser", from);
        newTemp.put("messages",messages);
        unreadMsg.put(newTemp);// add to user w/msg to array
    }

}
