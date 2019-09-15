package com.j10.socialnetwork;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity
{

    private Button LoginButton;
    private ImageView googleSignInButton;
    private EditText UserEmail, UserPassword;
    private TextView NeedNewAccountLink, ForgetPasswordLink;
    private ProgressDialog loadingBar;

    private FirebaseAuth mAuth;

    private Boolean emailAddressChecker;

    private static final int RC_SIGN_IN = 1;
    private GoogleApiClient mGoogleSignInClient;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        NeedNewAccountLink = findViewById(R.id.register_account_link);
        ForgetPasswordLink = findViewById(R.id.forgot_password_link);
        UserEmail = findViewById(R.id.login_email);
        UserPassword = findViewById(R.id.login_password);
        LoginButton = findViewById(R.id.login_button);
        googleSignInButton = findViewById(R.id.google_signin_button);

        mAuth = FirebaseAuth.getInstance();

        loadingBar = new ProgressDialog(this);

        NeedNewAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                SendUserToRegisterActivity();
            }
        });

        ForgetPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
            }
        });

        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                attemptUserLogin();
            }
        });


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = new GoogleApiClient.Builder(this).enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) 
            {
                Toast.makeText(LoginActivity.this, "Connection to Google Sign In Failed...", Toast.LENGTH_SHORT).show();
            }
        }).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();

        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                signIn();
            }
        });

    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleSignInClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN)
        {
            loadingBar.setTitle("Google Sign In");
            loadingBar.setMessage("Please wait, while we are allowing you to login using your Google Account...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();

            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
                Toast.makeText(this, "Please wait, while we are getting your auth result...", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            } 
            else
            {
                Toast.makeText(this, "Can't get Auth result.", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful())
                        {
                            Log.d(TAG, "signInWithCredential:success");
                            SendUserToMainActivity();
                            loadingBar.dismiss();
                        }
                        else
                        {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            String message = task.getException().getMessage();
                            SendUserToLoginActivity();
                            Toast.makeText(LoginActivity.this, "Not Authenticated: " + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }

                    }
                });
    }



    @Override
    protected void onStart()
    {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null)
        {
            SendUserToMainActivity();
        }
    }

    private void attemptUserLogin(){

        String email = UserEmail.getText().toString();
        String password = UserPassword.getText().toString();

        if(TextUtils.isEmpty(email))
        {
            UserEmail.setError("Email Required");
        }

        else if(TextUtils.isEmpty(password))
        {
            UserPassword.setError("Password Required");
        }
        else
        {

            loadingBar.setTitle("Login");
            loadingBar.setMessage("Please wait, while we are login you in to your account...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful())
                    {
                        VerifyEmailAddress();
                        loadingBar.dismiss();
                    }
                    else
                    {
                        String message = task.getException().getMessage();
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        SendUserToLoginActivity();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    private void VerifyEmailAddress () {
        FirebaseUser user = mAuth.getCurrentUser();

        emailAddressChecker = user.isEmailVerified();

        // MAKE SURE TO CHANGE LATER
        if (emailAddressChecker || !emailAddressChecker)
        {
            SendUserToMainActivity();

            Toast.makeText(LoginActivity.this, "You are Logged In!", Toast.LENGTH_SHORT).show();
        } 
        else
        {
            Toast.makeText(this, "Please verify your account first...", Toast.LENGTH_SHORT).show();
            mAuth.signOut();
        }

    }

    private void SendUserToMainActivity()
    {
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void SendUserToRegisterActivity()
    {
        Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(registerIntent);
    }

    private void SendUserToLoginActivity() {
        Intent selfIntent = new Intent(LoginActivity.this, LoginActivity.class);
        selfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(selfIntent);
        finish();
    }
}
