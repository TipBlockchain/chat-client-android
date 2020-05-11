package com.example.myapplication;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import java.util.Vector;

public class roomActivity extends AppCompatActivity {
    static roomActivity roomAct;
    private Socket mSocket;
    makeSocket app;// stores information on user and socket //
    Button sendbtn, leaveRoom;// sending, leaving, joining ongoing chat
    TextView showMyReceivedMsg, showMyMsgSent;// for displaying incoming/ sent messages
    EditText msgText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        roomAct = this;

        // activity gui fields
        showMyMsgSent = (TextView) findViewById(R.id.msgSentBox);
        showMyReceivedMsg = (TextView) findViewById(R.id.msgReceivedBox);
        msgText = (EditText) findViewById(R.id.msgText);
        sendbtn = (Button) findViewById(R.id.sendMsg);
        leaveRoom = (Button) findViewById(R.id.leave);

        // app socket retrieval
        app = (makeSocket) getApplication();
        mSocket = app.getmSocket();
        if(app.getRoomInvited() == null) {// if you were invited you received room Val in contacts page
            app.setHost();// if not then you are host!!!
            System.out.println("**** settign as host");
        }
        else// otherwise you are joining room
            mSocket.emit("joinHere", app.getRoomInvited());

        if(mSocket.connected()){// if connected listen for events
            mSocket.on("privateMessage", privateMessage);
            mSocket.on("newMessage", newMessage);
            mSocket.on("join", joinedUser); // handles join event diff from contacts page, here it only notifies
        }
        try{// try to develop options as buttons into scroll view on page
            JSONArray userList = app.getUserList();
            buildButtons(userList);// builds options menu/ like contacts screen
        }catch (JSONException e){
            e.printStackTrace();
        }
    }// onCreate

    public void onStart(){
        super.onStart();

        // leaving room sends you back to contacts
        leaveRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSocket.emit("leaveThisRoom", app.getRoomInvited());// make socket leave room
                app.setRoom(null);// once you leave room you need to be reinvited
                Intent contacts = new Intent(getBaseContext(), ContactActivity.class);
                startActivity(contacts);
            }
        });

        /*
        *retrieves sending message from textfield first
        * emits 'sendMessage' responsible for room message emitter on server
        * data sent in form of json obj msgObj:str, roomNo:#
        * */
        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = msgText.getText().toString();
                if(str.length() == 0)
                    return; // nothing written
                else {
                    JSONObject data = new JSONObject();
                    try {
                        data.put("msgObj", str);
                        data.put("roomNo", app.getRoomInvited());// retrieves room info
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showMyMsgSent.append("\n\n" +str);
                    mSocket.emit("sendMessage", data);// send msg to room
                }
            }
        });
    }

    /*
    * handles join listener join
    * notifies current users of a user joining
    *
    * */

    private final Emitter.Listener joinedUser = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            final String response = (String) args[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {// lets you knpw user joined channel
                    Toast.makeText(roomActivity.this, "User invited to " +
                           response , Toast.LENGTH_LONG).show();
                    if(app.getHost()){// if you are host then upon first invited user, your
                        app.setRoom(response);// room val is generated otherwise it will always have value here
                        mSocket.emit("joinHere", response);// need to join the socket on server

                    }
                }
            });
        }
    };

    /*
    * any personal messages are currently stored for later reading
    * */
    private final Emitter.Listener privateMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String fromUser = (String) args[0];
                    String msg = (String) args[1];
                    app.storeMessage(fromUser, msg);// store incoming messages// does not store sent ones
                }
            });
        }
    };

    /*
    * handles incoming new messages to room
    * */
    private final Emitter.Listener newMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String msg = (String) args[0];
                    showMyReceivedMsg.append("\n\n" + msg);
                }
            });
        }
    };

    /*
    * builds list of users to invite similar to how contact list is built
    * */
    public void buildButtons(JSONArray userList) throws JSONException {
        LinearLayout ly = (LinearLayout) findViewById(R.id.userList);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        // for loop to create buttons according to number of contacts in temp jsonarray
        for(int i=0; i<userList.length(); ++i){
            final String toUser = userList.getJSONObject(i).getString("username");
            final int toUserUUID = userList.getJSONObject(i).getInt("uuid");
            if(!toUser.equals(app.getMyUsername())){
                Button newButton = new Button(this);
                newButton.setText(toUser);
                newButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //transfer userName value
                        btnHandler(toUser, toUserUUID);// btnHandler function
                    }
                });
                ly.addView(newButton, btnParams);
            }
        }// for loop creating buttons
    }// buildBtns()

    /**
     * invites chosen user, only host may invite others
     * sends server roomVal along with invitee's socketid to generate room if needed
     * emits inviteMe response
     * */
    private void btnHandler(String toUser, int toUserUUID) {
        if(app.getHost()){// only host can invite
            try{
                JSONObject inviting = new JSONObject();
                inviting.put("socket", mSocket);
                inviting.put("username", toUser);
                inviting.put("uuid", toUserUUID);
                inviting.put("roomVal", app.getRoomInvited());
                mSocket.emit("inviteMe", inviting);// emit to chosen user
            }catch(JSONException e){
                e.printStackTrace();
            }
        }else{// let them know they are not host
            Toast.makeText(roomActivity.this, "You are not host, Ask host to invite user",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void onResume(){
        super.onResume();
        ContactActivity.getInstance().finish();
    }

    public void onPause(){
        super.onPause();
        mSocket.off("privateMessage");
        mSocket.off("newMessage");
    }
    public void onDestroy(){
        super.onDestroy();
    }
}
