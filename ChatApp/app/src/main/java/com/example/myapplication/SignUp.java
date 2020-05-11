package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.socketio.client.Socket;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;

public class SignUp extends AppCompatActivity implements View.OnClickListener{
    String newUserName="tempUser2", newPassword="tempPassword2", newEmail="tempemail2@temp.com"; // variables holding new user information
    Button SignUp;// signing up button
    RequestQueue signqueue;// stores request headers
    String url = "http://192.168.1.137:9000";// main url, domain and port
    private Socket mySocket;// socket for connection
    EditText getUserName,getPassWord, getEmail;// used to retrieve input in edittext fields

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        getUserName = findViewById(R.id.newUsername);
        getPassWord = findViewById(R.id.newPassword);
        getEmail = findViewById(R.id.newUsrEmail);
        signqueue = Volley.newRequestQueue(this); // assign mqueue request

        // when clicked will perform post at /login/signup
        SignUp = (Button) findViewById(R.id.createBtn);
        SignUp.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                /// get information from edit text boxes in activity_sign_up

                newUserName = getUserName.getText().toString();
                newPassword = getPassWord.getText().toString();
                newEmail = getEmail.getText().toString();

                if(checkInfo()) {// if information input is empty or not to par return
                    return;
                }
                else if(!checkInfo()) {
                    reqSignUp(); // attempts header and if error returns
                }// if info is not empty
            }
        });// end of SignUp button

    }// onCreate

    public boolean checkInfo(){
        if(newUserName.length() == 0 || newUserName == null)
            return true;
        else if(newPassword.length() == 0 || newPassword == null)
            return true;
        else if (newEmail.length() == 0 || newEmail == null)
            return true;
        else
            return false;
    }// checkInfo

    public void reqSignUp(){
        JSONObject newUser = new JSONObject();// create newUser object with information
        try{
            newUser.put("email",newEmail);
            newUser.put("username",newUserName);
            newUser.put("password",newPassword);
            System.out.println(newUser);
        }catch(JSONException e){
            e.printStackTrace();
        }

        JsonObjectRequest reqASignUp = new JsonObjectRequest(Request.Method.POST, url + "/login/register", newUser, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                SharedPreferences sharedprefs = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedprefs.edit();
                editor.putString("username", newUserName);
                editor.putString("email", newEmail);
                editor.apply();
                System.out.println("*****Setting gotocontacts to true");
                System.out.println("****************Here");
                System.out.println(response);
                // Sign up successful go to contacts page
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);

            }// onResponse
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });// end of parameters

        signqueue.add(reqASignUp);// add reqSignUp to mqueue
    }// reqSignUp

    @Override
    public void onClick(View v) {

    }
    public void onResume(){
        super.onResume();
        System.out.println("************ Signup onResume invoked");
    }
    public void onStart(){
        super.onStart();
        System.out.println("************ Signup onStart invoked");

    }
    public void onRestart(){
        super.onRestart();
        System.out.println("************ Signup onRestart invoked");
    }

    public void onDestroy(){
        super.onDestroy();
    }
}
