package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArrayMap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;

import java.util.HashMap;
import java.util.Map;


public class ContactActivity extends AppCompatActivity {
    //recreate socket
    private Socket mSocket;// socket to connect
    static ContactActivity ActivityC;
    Button logout, sendnew; // btn2 for messaging and btn1 for logging out
    // stores username and password of current user( retrieved from MainActivity )

    JSONArray myContacts; // stores contact information in form [{"user":user, "uuid":uuid}] builds contactlist
    RequestQueue mqueue;// queueu for request
    makeSocket app; // used to retrieve app state information
    String URL; // url for requests
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact);
        ActivityC = this;// used to destroy
        mqueue = Volley.newRequestQueue(this); // assign mqueue request
        // retrieve userName data transfered from MainActivity.java( from signin )
        app = (makeSocket) getApplication();
        mSocket = app.getmSocket();
        URL = app.getUrl();

        logout = (Button) findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.disconnect();// disconnect from socket and return to login screen
                mSocket.disconnect();
                Intent Login = new Intent(getBaseContext(), MainActivity.class);
                startActivity(Login);
            }
        });

        /*
         * navigates to join a room
         * */
        sendnew = (Button) findViewById(R.id.generalMessage);
        sendnew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newRoom = new Intent(getBaseContext(), roomActivity.class);
                startActivity(newRoom);
            }
        });
    }// onCreate

    public void onStart(){
        super.onStart();
        if (mSocket.connected()){
            mSocket.on("join", invitedMessage);
            mSocket.on("privateMessage", privateMessage);
            Toast.makeText(ContactActivity.this, "Connected!! As user: " + app.getMyUsername()
                    + " socketid: "+mSocket.id(), Toast.LENGTH_SHORT).show();
        }

        JSONObject tryUser = new JSONObject();
        try{
            tryUser.put("name", app.getMyUsername());
            tryUser.put("uuid", app.getmyUUID());
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        contactList(tryUser);// make users request with tryUser obj as param
    }// onstart

    /*
    * @params tryUser obj defined in onstart
    * sets headers for authentication to make requests
    * calls buildbuttons upon successful response
    * makes request at url/users
    * */
    public void contactList(JSONObject tryUser){
        JsonObjectRequest reqContacts = new JsonObjectRequest(Request.Method.GET, URL+"/users", tryUser, new Response.Listener<JSONObject>(){            @Override
            public void onResponse(JSONObject response) {// handles response
                try {// try to retrieve array of json object named users in server
                    myContacts = response.getJSONArray("users");
                    buildButtons(myContacts);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }// onResponse
        }, new Response.ErrorListener(){// handles response error
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }//onErrorResponse
        }){
            @Override
            public Map<String, String> getHeaders() {// attaches authentication headers
                Map<String, String> mHeaders = new HashMap<String, String>();
                mHeaders.put("x-access-token", app.getToken());// set token access head
                mHeaders.put("authorization", app.getMyUsername());// set authorization header
                return mHeaders;
            }
        };// end of reqLogin
        mqueue.add(reqContacts);// add reqLogin to mqueue request
    }// contactList();

    /*
    * @params listener obj from server
    * attempt to invite user to a room, lets user know on screen
    * sets room value in app's setRoom(response) to store for joining room
    *
    * */
    private final Emitter.Listener invitedMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            final String response = (String) args[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ContactActivity.this, "Received Invitation, to Join click Join Room: "
                            + response , Toast.LENGTH_LONG).show();
                    app.setRoom(response);// toRoom in socket class
                }
            });
        }
    };

    /*
    * will store messages into unreadMsgs in makesocket class
    *  * */
    private final Emitter.Listener privateMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            final String msg = (String) args[0];
            final String from = (String) args[1];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ContactActivity.this, "New Message from: " + from , Toast.LENGTH_LONG).show();
                    app.storeMessage(from, msg);
                }// run
            });// on ui
        }
    };// privateMessage emitter

    /*
    * @ params temp json array, response list from url/users request
    * builds buttons according to user returned from server requests
    * sets new buttons on scrollview with parent paramaters
    * calls button onclick listener btnHandler
    * */
    public void buildButtons(JSONArray temp) throws JSONException {
        LinearLayout ly = (LinearLayout) findViewById(R.id.contactListView);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        for(int i=0; i<temp.length(); ++i){
            final String toUser = temp.getJSONObject(i).getString("username");
            final int toUserUUID = temp.getJSONObject(i).getInt("uuid");

            if(!toUser.equals(app.getMyUsername())){ // checks if same user as app state user
                Button newButton = new Button(this);
                newButton.setText(toUser);
                if(!app.getRequestedUsers())
                    app.addUserList(toUser, toUserUUID);// save to list of users in socket class
                newButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        btnHandler(toUser, toUserUUID);// btnHandler function
                    }
                });
                ly.addView(newButton, btnParams);
            }
        }// for loop creating buttons
        app.setRequestedUsers();// sets to true
    }// buildBtns()

    public void btnHandler(String toUser, int toUserUUID){
        Intent messagePage = new Intent(getBaseContext(), Message.class);// creates activity Message
        //sets uuid and user of recipient
        app.setToUser(toUserUUID, toUser);
        startActivity(messagePage);// starts activity message
    }

    public static ContactActivity getInstance(){
        return ActivityC;// returns contact activity to be destroyed in MainActivity onResume
    }
    public void onPause(){
        super.onPause();
        mSocket.off("privateMessage");
        mSocket.off("inviteMe");// join emitter from server
    }
    public void onRestart(){
        super.onRestart();
    }
    public void onResume(){
        super.onResume();
    }

    public void onStop(){
        super.onStop();
    }
    public void onDestroy(){
        super.onDestroy();
    }
}// class ContactActivity
