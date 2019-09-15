package com.j10.socialnetwork;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity
{

    private EditText UserEmail, UserPassword, UserConfirmPassword;
    private Button CreateAccountButton;
    private ProgressDialog loadingBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        UserEmail = findViewById(R.id.register_email);
        UserPassword = findViewById(R.id.register_password);
        UserConfirmPassword = findViewById(R.id.register_confirm_password);
        CreateAccountButton = findViewById(R.id.register_create_account);

        loadingBar = new ProgressDialog(this);

        CreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                attemptRegistration();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null)
        {
            SendUserToMainActivity();
        }
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void attemptRegistration()
    {
        String email = UserEmail.getText().toString();
        String password = UserPassword.getText().toString();
        String confirmPassword = UserConfirmPassword.getText().toString();

        UserEmail.setError(null);
        UserPassword.setError(null);

        if(TextUtils.isEmpty(email))
        {
            UserEmail.setError("Email Required");
            Toast.makeText(this, "Please write your email...", Toast.LENGTH_SHORT).show();
        }

        if(TextUtils.isEmpty(password))
        {
            UserPassword.setError("Password Required");
            Toast.makeText(this, "Please write your password...", Toast.LENGTH_SHORT).show();
        }

        if(TextUtils.isEmpty(confirmPassword))
        {
            UserConfirmPassword.setError("Confirm Password Required");
            Toast.makeText(this, "Please confirm password...", Toast.LENGTH_SHORT).show();
        }

        else if (!password.equals(confirmPassword))
        {
            UserConfirmPassword.setError("Passwords Must Match");
            Toast.makeText(this, "Password does not match confirm password.", Toast.LENGTH_SHORT).show();
        }

        else
        {
            loadingBar.setTitle("Creating New Account");
            loadingBar.setMessage("Please wait, while we are creating your new account...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            createFirebaseUser();
        }
    }



    private void createFirebaseUser(){

        String email = UserEmail.getText().toString();
        String password = UserPassword.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task)
            {
                if(task.isSuccessful())
                {

                    SendEmailVerificationMessage();

                    loadingBar.dismiss();
                }
                else
                {
                    String message = task.getException().getMessage();
                    Toast.makeText(RegisterActivity.this, "Error Occured: " + message, Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }

    private void SendEmailVerificationMessage ()
    {
        FirebaseUser user = mAuth.getCurrentUser();

        if(user != null){
            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {
                    if(task.isSuccessful()){
                        Toast.makeText(RegisterActivity.this, "Registration Success, Please check and verify your account", Toast.LENGTH_SHORT).show();
                        SendUserToLoginActivity();
                        mAuth.signOut();
                    }
                    else {
                        String message = task.getException().getMessage();
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                }
            });
        }
    }

    private void SendUserToLoginActivity(){
        Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
}
