package com.j10.socialnetwork;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


public class SettingsActivity extends AppCompatActivity {


    private Toolbar mToolbar;

    private EditText userName, userProfileName, userStatus, userCountry, userGender, userRelation, userDOB;
    private Button UpdateAccountSettingsButton;
    private CircleImageView userProfileImage;

    private DatabaseReference SettingsUserRef;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private ProgressDialog loadingBar;

    private StorageReference UserProfileImageRef;



    final static int Gallery_Pick = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        SettingsUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);

        mToolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");



        loadingBar = new ProgressDialog(this);


        userName = findViewById(R.id.settings_username);
        userProfileName = findViewById(R.id.settings_profile_full_name);
        userStatus = findViewById(R.id.settings_status);
        userCountry = findViewById(R.id.settings_country);
        userGender = findViewById(R.id.settings_gender);
        userRelation = findViewById(R.id.settings_relationship_status);
        userDOB = findViewById(R.id.settings_dob);
        userProfileImage = findViewById(R.id.settings_profile_image);
        UpdateAccountSettingsButton = findViewById(R.id.update_account_settings_button);

        SettingsUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    String myUserName = dataSnapshot.child("username").getValue().toString();
                    String myProfileName = dataSnapshot.child("fullname").getValue().toString();
                    String myProfileStatus = dataSnapshot.child("status").getValue().toString();
                    String myDOB = dataSnapshot.child("dob").getValue().toString();
                    String myCountry = dataSnapshot.child("countryname").getValue().toString();
                    String myGender = dataSnapshot.child("gender").getValue().toString();
                    String myRelationStatus = dataSnapshot.child("relationshipstatus").getValue().toString();
                    if(dataSnapshot.child("profileimage").exists()){
                        String myProfileImage = dataSnapshot.child("profileimage").getValue().toString();
                        Picasso.get().load(myProfileImage).placeholder(R.drawable.profile).into(userProfileImage);
                    } else {
                        userProfileImage.setImageResource(R.drawable.profile);
                    }

                    userName.setText(myUserName);
                    userProfileName.setText(myProfileName);
                    userStatus.setText(myProfileStatus);
                    userDOB.setText(myDOB);
                    userCountry.setText(myCountry);
                    userGender.setText(myGender);
                    userRelation.setText(myRelationStatus);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        UpdateAccountSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValidateAccountInfo();
            }
        });


        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, Gallery_Pick);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Gallery_Pick && resultCode == RESULT_OK && data != null) {
            Uri ImageUri = data.getData();

            CropImage.activity(ImageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);
        }

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK) {

                loadingBar.setTitle("Profile Image");
                loadingBar.setMessage("Please wait, while we are updating your profile image...");
                loadingBar.show();
                loadingBar.setCanceledOnTouchOutside(true);

                Uri resultUri = result.getUri();

                final StorageReference filePath = UserProfileImageRef.child(currentUserId + ".jpeg");

                //Saving the crop image into firebase storage
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(SettingsActivity.this, "Profile Image stored successfully to Firebase storage...", Toast.LENGTH_SHORT).show();

                            //Get the URI of the uploaded image
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    //Save the uri under the correct user profile
                                    SettingsUserRef.child("profileimage").setValue(uri.toString())
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Intent selfIntent = new Intent(SettingsActivity.this, SettingsActivity.class);
                                                        startActivity(selfIntent);

                                                        Toast.makeText(SettingsActivity.this, "Profile Image stored to Firebase Database Successfully...", Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    } else {
                                                        String message = task.getException().getMessage();
                                                        Toast.makeText(SettingsActivity.this, "Error Occured: " + message, Toast.LENGTH_SHORT).show();
                                                        loadingBar.dismiss();
                                                    }
                                                }
                                            });
                                }
                            });
                        }
                        else {
                            String message = task.getException().getMessage();
                            Toast.makeText(SettingsActivity.this, "Error Occured: " + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }

                });
            }
        }
    }

    private void ValidateAccountInfo() {
        String username = userName.getText().toString();
        String profilename = userProfileName.getText().toString();
        String status = userStatus.getText().toString();
        String dob = userDOB.getText().toString();
        String country = userCountry.getText().toString();
        String gender = userGender.getText().toString();
        String relation = userRelation.getText().toString();

        if(TextUtils.isEmpty(username))
        {
            Toast.makeText(this, "Please enter your username...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(profilename))
        {
            Toast.makeText(this, "Please enter your profile name...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(status))
        {
            Toast.makeText(this, "Please enter your status...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(dob))
        {
            Toast.makeText(this, "Please enter your date of birth...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(country))
        {
            Toast.makeText(this, "Please enter your country...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(gender))
        {
            Toast.makeText(this, "Please enter your gender...", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty(relation))
        {
            Toast.makeText(this, "Please enter your relationship status...", Toast.LENGTH_SHORT).show();
        }
        else
        {

            loadingBar.setTitle("Account Settings");
            loadingBar.setMessage("Please wait, while we are updating your account settings...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            UpdateAccountInformation(username, profilename, status, dob, country, gender, relation);
        }
    }

    private void UpdateAccountInformation(String username, String profilename, String status, String dob, String country, String gender, String relation)
    {
        HashMap userMap = new HashMap();

        userMap.put("countryname", country);
        userMap.put("dob", dob);
        userMap.put("fullname", profilename);
        userMap.put("gender", gender);
        userMap.put("relationshipstatus", relation);
        userMap.put("status", status);
        userMap.put("username", username);

        SettingsUserRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if(task.isSuccessful()){
                    loadingBar.dismiss();
                    SendUserToMainActivity();
                    Toast.makeText(SettingsActivity.this, "Account Settings Updated Successfully", Toast.LENGTH_SHORT).show();
                } else {
                    String message = task.getException().getMessage();
                    Toast.makeText(SettingsActivity.this, "" + message, Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }

    private void SendUserToMainActivity()
    {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }


}
