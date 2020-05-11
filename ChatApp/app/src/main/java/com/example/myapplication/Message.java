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
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;


public class Message extends AppCompatActivity {
    // recreate socket
    private Socket mSocket;
    Intent intent;// creates activity contacts
    static Message ActivityM;
    makeSocket app;
    TextView showMyReceivedMsg, showMyMsgSent, toUserView;// textviews to show received/sent/connecting user
    EditText keyField;// field for decryption key
    Button toContacts, sendMsg, keyBtn;// btns to return, send, and submit decryption key
    JSONArray unreadMsg = null;// stores received messages from user whom conversation is not current
    // stored as [{user: user, message: [msg, msg2]}] in fctn storeMessage(int, String)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityM = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        // retrieve info from makeSocket class
        app = (makeSocket) getApplication();
        mSocket = app.getmSocket();
        toUserView = (TextView) findViewById(R.id.sendingtouser);
        toUserView.setText(app.getToUser());


        // if socket is connected show on screen
        if (mSocket.connected()){
            mSocket.on("privateMessage", privateMessage);

            // show user socketid on screen pop up
            Toast.makeText(Message.this, "Connected!! As user: " + app.getMyUsername()
                    + ", Will Send to: " + app.getToUser()
                    + " socketid: "+mSocket.id(), Toast.LENGTH_SHORT).show();
        }// end of checking socket

        toContacts = (Button) findViewById(R.id.btn1);// button to go back to contacts
        toContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {// back to contacts
                intent = new Intent(getBaseContext(), ContactActivity.class);
                startActivity(intent);// starts activity contacts
            }
        });// end of button btn1 to go back into contacts

        showMyMsgSent = (TextView) findViewById(R.id.msgSentBox); // display TextView for my messages sent
        showMyReceivedMsg = (TextView) findViewById(R.id.msgReceivedBox);// display TextView for messages received
        keyField = (EditText) findViewById(R.id.keyField);

        // clicker events defined in onStart
        sendMsg = (Button) findViewById(R.id.sendMsg);// button to send message
        keyBtn = (Button) findViewById(R.id.keybtn);// button to process key

    }// onCreate
    public void onStart(){
        super.onStart();
        System.out.println("************ Message onStart invoked");
        addMessages(app.getUnreadMsgs());// returns list of received messages as strings and sets to convo page
        keyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String decryptMessage = showMyReceivedMsg.getText().toString();
                System.out.println(decryptMessage);
            }
        });

        sendMsg.setOnClickListener(new View.OnClickListener(){// set click
            @Override
            public void onClick(View v) { // when this button is clicked a message is sent
                EditText getMsgToSend = findViewById(R.id.msgText); // text field where message is written
                String sendingText = getMsgToSend.getText().toString(); // gets the message in EditText converts it to string
                getMsgToSend.setText(new String("")); // erase string on field where msg is written
                showMyMsgSent.append("\n\n" + sendingText);// display message that you sent
                /*
                 *
                 * need to encrypt message sendingText here before calling
                 * emitToUser
                 *
                 *
                 * */

                emitToUser(sendingText);

            }// onClick
        });// end of btn2 aka button to send Message
    }// onStart

    /*emits to user associated with btn form = json obj
    * @params message to send
    * emit on privateMessage
    * */

    public void emitToUser(String sendingText){// emits to user
        JSONObject data = new JSONObject();
        try{// try to develop a JSON Object to send into web server
            data.put("msgObj", sendingText);// json object has an item, message:sendMsg
            data.put("username", app.getToUser());
            data.put("uuid", app.getToUUID());
            mSocket.emit("privateMessage", data);
        } catch (Exception e) {
            e.printStackTrace();
        } // end of try and catch
    }

    public final Emitter.Listener recKeys = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

        }
    };

    /*@ params string message and sender name
    * privateMessage listener to receive response from server /
    * displays to screen if corresponding conversatio user otherwise stores message
    * */
    private final Emitter.Listener privateMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            final String msg = (String) args[0];
            final String from = (String) args[1];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(from.equals(app.getToUser())){// checks if received message from same user as convo page
                        showMyReceivedMsg.append("\n\n" + msg);
                    }else{// otherwise store message in unread with user
                        Toast.makeText(Message.this, "Received a new Message from uuid: " + from,
                                Toast.LENGTH_SHORT).show();
                    }
                }// run
            });// on ui
            app.storeMessage(from, msg);
        }
    };// privateMessage emitter

    // adds messages to screen
    public void addMessages(String unread){
        try{
            unreadMsg = new JSONArray(unread);// convert back to array to print accordingly
            for(int i=0; i<unreadMsg.length(); ++i){
                if(unreadMsg.getJSONObject(i).getString("fromUser").equals(app.getToUser())){
                    System.out.println(unreadMsg.getJSONObject(i).getJSONArray("messages"));
                    for(int j=0; j<unreadMsg.getJSONObject(i).getJSONArray("messages").length(); ++j){
                        showMyReceivedMsg.append("\n\n" + unreadMsg.getJSONObject(i).getJSONArray("messages").getString(j));
                    }
                }
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }// addMessages

    public static Message getInstance(){
        return ActivityM;// return this activity to delete when logging in in ContactActivity;
    }
    /*
    *disconnects socket listeners
    * */
    public void onPause(){
        super.onPause();
        mSocket.off("privateMessage");
    }
    public void onResume(){
        super.onResume();
        ContactActivity.getInstance().finish();
    }
    public void onStop(){
        super.onStop();
    }
    public void onDestroy(){
        super.onDestroy();
        mSocket.off("privateMessage");
    }
}// class Message
