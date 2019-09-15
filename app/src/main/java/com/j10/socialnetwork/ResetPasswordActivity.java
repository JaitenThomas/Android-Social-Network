package com.j10.socialnetwork;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;


public class ResetPasswordActivity extends AppCompatActivity
{

    private Toolbar mToolbar;
    private Button ResetPasswordSendEmailButton;
    private EditText ResetEmailInput;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);


        mAuth = FirebaseAuth.getInstance();

        mToolbar = findViewById(R.id.forget_password_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Reset Password");

        ResetPasswordSendEmailButton = findViewById(R.id.reset_email_password_button);
        ResetEmailInput = findViewById(R.id.reset_password_email);

        ResetPasswordSendEmailButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String userEmail = ResetEmailInput.getText().toString();

                if (TextUtils.isEmpty(userEmail)) {
                    Toast.makeText(ResetPasswordActivity.this, "Please write your valid email address first...", Toast.LENGTH_SHORT).show();
                } 
                else
                {
                    mAuth.sendPasswordResetEmail(userEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(ResetPasswordActivity.this, "Please check your Email Account, If you want to reset your password...", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                            }
                            else
                            {
                                String message = task.getException().getMessage();
                                Toast.makeText(ResetPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }
}
