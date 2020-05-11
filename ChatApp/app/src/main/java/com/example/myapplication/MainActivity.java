package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    // upon signing in(button1), if userName and passWord are null then app does not continue
    Button login, gotosignup;
    // username used for testing   // stores password entered by user
    String passWord = "",userName = "tempUser", userEmail = "", myToken;
    int UUID;

    //declare socket variable
    private Socket mySocket;
    makeSocket app;
    // Instantiate http request will save requests on mqueue
    RequestQueue mqueue;
    static MainActivity mainActivity;
    String URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mainActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mqueue = Volley.newRequestQueue(this); // assign mqueue request
        login = findViewById(R.id.signIn); // upon clicking will attempt a login request
        // head over to signup
        gotosignup = findViewById(R.id.createbtn);// user goes to create acct
        app = (makeSocket) getApplication();// initialize app as type of makeSocket
        URL = app.getUrl();// retrieve url of makeSocket
    }// onCreate

    public void onStart(){
        super.onStart();
        // sets listener for clicking login
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //retrieve userName and passWord from app
                EditText getUser = findViewById(R.id.userEmail);// retrieve info of email
                userEmail = getUser.getText().toString(); // extract email
                EditText getPword = findViewById(R.id.pword);
                passWord = getPword.getText().toString();// extract password

                // check if userName and password entered in checkUserInfor method
                if(!checkUserInfo()){
                    Toast.makeText(MainActivity.this, "Invalid username or password !!!"
                            + userName, Toast.LENGTH_SHORT).show();
                }
                else {// if filled will attempt a login request through loginCallback
                    loginCallback();
                }
            }// onClick
        });// end of login button

        gotosignup.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(getBaseContext(), SignUp.class);
                startActivity(intent);// start signup page
            }
        });// endof gotosignup button
        //print variables for reference
    }// onStart()

    public static MainActivity getInstance(){
        return mainActivity;
    }
    /*
    * @ params login request response on success , onlogin interface to reqALogin fct
    * sets state of application
    *  onLogin interface calls login request
    *
    * */
    public void loginCallback() {
        // callback function for login request with interface onLogin
        reqALogin(new onLogin() {
            @Override
            public void onLoginResponse(JSONObject response) {
                try{// the following are used to state on app for user creds
                    myToken = response.getString("token");
                    userName = response.getString("username");
                    UUID = response.getInt("uuid");
                    app.setToken(myToken);// sets token on socket

                    Handler socketHandler = new Handler();// used to give the socket time to connect
                    socketHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() { // set state on application
                            mySocket = app.getmSocket();
                            app.setMyUser(UUID, userName);
                            mySocket.emit("login", userName, UUID);// emit login sets users socket id in server
                            // used for connecting to other clients
                            Intent intent = new Intent(getBaseContext(), ContactActivity.class);
                            startActivity(intent);
                        }
                    }, 2000);// delay, give socket time to connect

                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
        });

    }// callback

    public interface onLogin{
        void onLoginResponse(JSONObject response);
    }
/*
*   @param onLogin interface, on successful response from server handled by callback fctn loginCallback
*   makes a login request to server at path url/login with tryUser obj
*  response in form of json obj
*
* */
    public void reqALogin(final onLogin loginListener){
        JSONObject tryUser = new JSONObject();// will store login information as json object
        try{// try to set tryUser variable values
                tryUser.put("email", userEmail);
                tryUser.put("password", passWord);
        }
        catch(JSONException e){
                e.printStackTrace();
        }// end of try/catch for tryuser

            final JsonObjectRequest reqLogin = new JsonObjectRequest(Request.Method.POST, URL+"/login", tryUser, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                        loginListener.onLoginResponse(response);// call interface
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    Toast.makeText(MainActivity.this, "Wrong email or password ", Toast.LENGTH_SHORT).show();
                }
            });
            mqueue.add(reqLogin);// add reqLogin to queue
    }// reqALogin()
    /*
    * checks user Infirmation
    */
    public boolean checkUserInfo(){
        if(userEmail == null || userEmail.length() == 0){
            return false;// return username empty
        }
        else return passWord != null && passWord.length() != 0;// return true or false
    }// end of checkUserInfo

    public void onResume(){
        super.onResume();
    }
    public void onRestart(){
        super.onRestart();
        ContactActivity.getInstance().finish();// destroy contact activity since new login will commence
    }
    public void onStop(){
        super.onStop();
    }

    public void onDestroy(){
        super.onDestroy();
    }
}
