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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;


public class PostActivity extends AppCompatActivity
{
    private Toolbar mToolbar;
    private ProgressDialog loadingBar;

    private ImageButton SelectPostImage;
    private Button UpdatePostButton;
    private EditText PostDescription;

    private static final int Gallery_Pick = 1;

    private StorageReference PostsImagesReference;
    private DatabaseReference UsersRef, PostsRef;
    private FirebaseAuth mAuth;

    private String description;

    private Uri ImageUri;

    private String saveCurrentDate, saveCurrentTime, postRandomName, downloadUrl, current_user_id;

    private long countPosts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);


        mAuth = FirebaseAuth.getInstance();
        current_user_id = mAuth.getCurrentUser().getUid();

        PostsImagesReference = FirebaseStorage.getInstance().getReference();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        PostsRef = FirebaseDatabase.getInstance().getReference().child("Posts");

        SelectPostImage = findViewById(R.id.select_post_image);
        UpdatePostButton = findViewById(R.id.update_post_button);
        PostDescription = findViewById(R.id.click_post_description);

        loadingBar = new ProgressDialog(this);

        mToolbar = findViewById(R.id.update_post_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Update Post");

        SelectPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenGallery();
            }
        });

        UpdatePostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValidatePostInfo();
            }
        });

    }

    private void ValidatePostInfo() {
        description = PostDescription.getText().toString();

        if(ImageUri == null){
            Toast.makeText(this, "Please select post image.", Toast.LENGTH_SHORT).show();
        }

        if(TextUtils.isEmpty(description)){
            Toast.makeText(this, "Post description is required", Toast.LENGTH_SHORT).show();
        }
        else {

            loadingBar.setTitle("Add New Post");
            loadingBar.setMessage("Please wait, while we are updating your new post...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            StoringImageToFirebaseStorage(description);
        }
    }

    private void StoringImageToFirebaseStorage(String description) {

        Calendar calFordDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
        saveCurrentDate = currentDate.format(calFordDate.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm");
        saveCurrentTime = currentTime.format(calFordDate.getTime());

        postRandomName = saveCurrentDate+saveCurrentTime;



        final StorageReference filePath = PostsImagesReference.child("Post Images").child(ImageUri.getLastPathSegment() + postRandomName + ".jpeg");


           filePath.putFile(ImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
               @Override
               public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                   if(task.isSuccessful()){

                       filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                           @Override
                           public void onSuccess(Uri uri) {
                               downloadUrl = uri.toString();
                               Toast.makeText(PostActivity.this, "image uploaded successfully to firebase storage", Toast.LENGTH_SHORT).show();
                               SavingPostInformationToDatabase();
                           }
                       });

                   } else {

                       String message = task.getException().getMessage();
                       Toast.makeText(PostActivity.this, message, Toast.LENGTH_SHORT).show();

                   }
               }
           });


    }

    private void SavingPostInformationToDatabase()
    {

        PostsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    countPosts = dataSnapshot.getChildrenCount();
                }
                else
                {
                    countPosts = 0;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });


        UsersRef.child(current_user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String userFullName = dataSnapshot.child("fullname").getValue().toString();

                    String userProfileImage;

                    HashMap postsMap = new HashMap();
                    postsMap.put("uid", current_user_id);
                    postsMap.put("date", saveCurrentDate);
                    postsMap.put("time", saveCurrentTime);
                    postsMap.put("description", description);
                    postsMap.put("postimage", downloadUrl);
                    postsMap.put("fullname", userFullName);
                    postsMap.put("counter", countPosts);

                    if(dataSnapshot.child("profileimage").getValue() != null){
                        userProfileImage = dataSnapshot.child("profileimage").getValue().toString();
                        postsMap.put("profileimage", userProfileImage);
                    }


                    PostsRef.child(current_user_id + postRandomName).updateChildren(postsMap).addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if(task.isSuccessful()){
                                SendUserToMainActivity();
                                Toast.makeText(PostActivity.this, "New Post is updated successfully.", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            } else {
                                String message = task.getException().getMessage();
                                Toast.makeText(PostActivity.this, message, Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void OpenGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, Gallery_Pick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(requestCode == Gallery_Pick && resultCode==RESULT_OK && data != null)
        {
            ImageUri = data.getData();
            SelectPostImage.setImageURI(ImageUri);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == android.R.id.home){
            SendUserToMainActivity();
        }

        return super.onOptionsItemSelected(item);
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(PostActivity.this, MainActivity.class);
        startActivity(mainIntent);
    }
}
