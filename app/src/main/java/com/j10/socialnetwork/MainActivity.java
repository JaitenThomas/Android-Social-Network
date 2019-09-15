package com.j10.socialnetwork;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;


import org.w3c.dom.Comment;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
{

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private RecyclerView postList;
    private Toolbar mToolbar;
    private CircleImageView NavProfileImage;
    private TextView NavProfileUserName;
    private ImageButton AddNewPostButton;


    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef, PostsRef, LikesRef;
    private String currentUserID;

    Boolean LikeCheaker = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        // Database references
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        PostsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        // Toolbar
        mToolbar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Home");

        // Recycler Setup
        postList = findViewById(R.id.all_users_post_list);
        postList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        postList.setLayoutManager(linearLayoutManager);


        //Buttons
        AddNewPostButton = findViewById(R.id.add_new_post_button);

        drawerLayout = findViewById(R.id.drawable_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView = findViewById(R.id.navigation_view);

        View navView = navigationView.inflateHeaderView(R.layout.navigation_header);
        NavProfileImage = navView.findViewById(R.id.nav_profile_image);
        NavProfileUserName = navView.findViewById(R.id.nav_user_full_name);



        UsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    if(dataSnapshot.hasChild("fullname"))
                    {
                        String fullname = dataSnapshot.child("fullname").getValue().toString();
                        NavProfileUserName.setText(fullname);
                    }

                    if(dataSnapshot.hasChild("profileimage"))
                    {
                        String image = dataSnapshot.child("profileimage").getValue().toString();
                        Picasso.get().load(image).placeholder(R.drawable.profile).into(NavProfileImage);
                    }
                    
                    else 
                    {
                        Toast.makeText(MainActivity.this, "Profile name do not exits", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull final DatabaseError databaseError) {

            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
            {
                UserMenuSelector(menuItem);
                return false;
            }
        });

        AddNewPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                SendUserToPostActivity();
            }
        });

        DisplayAllUsersPosts();

    }

    public void updateUserStatus (String state)
    {
        String saveCurrentDate, saveCurrentTime;

        Calendar calForDate =  Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calForDate.getTime());

        Calendar calForTime =  Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calForTime.getTime());

        Map currentStateMap = new HashMap();
        currentStateMap.put("time", saveCurrentTime);
        currentStateMap.put("date", saveCurrentDate);
        currentStateMap.put("type", state);

        UsersRef.child(currentUserID).child("userState").updateChildren(currentStateMap);
    }

    private void DisplayAllUsersPosts()
    {

        Query SortPostsInDesOrder = PostsRef.orderByChild("counter");

        FirebaseRecyclerOptions<Posts> options = new FirebaseRecyclerOptions.Builder<Posts>().setQuery(SortPostsInDesOrder, Posts.class).build();


        FirebaseRecyclerAdapter<Posts, PostsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Posts, PostsViewHolder>(options) {


            @Override
            protected void onBindViewHolder(@NonNull PostsViewHolder holder, int position, @NonNull Posts model) {
                final String PostKey = getRef(position).getKey();

                holder.setFullName(model.getFullname());
                holder.setTime(model.getTime());
                holder.setDate(model.getDate());
                holder.setDescription(model.getDescription());
                holder.setProfileimage(getApplicationContext(), model.getProfileimage());
                holder.setPostimage(getApplicationContext(), model.getPostimage());

                holder.setLikeButtonStatus(PostKey);

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent clickPostIntent = new Intent(MainActivity.this, ClickPostActivity.class);
                        clickPostIntent.putExtra("PostKey", PostKey);
                        startActivity(clickPostIntent);
                    }
                });

                holder.CommentPostButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        Intent commentsIntent = new Intent(MainActivity.this, CommentsActivity.class);
                        commentsIntent.putExtra("PostKey", PostKey);
                        startActivity(commentsIntent);
                    }
                });

                holder.LikePostButton.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                      LikeCheaker = true;

                      LikesRef.addValueEventListener(new ValueEventListener() {
                          @Override
                          public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                              if(LikeCheaker.equals(true)){
                                  if (dataSnapshot.child(PostKey).hasChild(currentUserID))
                                  {
                                      LikesRef.child(PostKey).child(currentUserID).removeValue();
                                      LikeCheaker = false;
                                  }
                                  else
                                  {
                                      LikesRef.child(PostKey).child(currentUserID).setValue(true);
                                      LikeCheaker = false;
                                  }
                              }
                          }

                          @Override
                          public void onCancelled(@NonNull DatabaseError databaseError) {

                          }
                      });
                  }
                });
            }

            @NonNull
            @Override
            public PostsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.all_posts_layout, viewGroup, false);
                PostsViewHolder viewHolder = new PostsViewHolder(view);
                return viewHolder;
            }


        };

        postList.setAdapter(firebaseRecyclerAdapter);

        firebaseRecyclerAdapter.startListening();

        updateUserStatus("online");
    }

    public static class PostsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        ImageButton LikePostButton, CommentPostButton;
        TextView DisplayNumberOfLikes;
        int countLikes;
        String currentUserId;
        DatabaseReference LikesRef;

        public PostsViewHolder(View itemView) {

            super(itemView);
            mView = itemView;

            LikePostButton = mView.findViewById(R.id.like_button);
            CommentPostButton = mView.findViewById(R.id.comment_button);
            DisplayNumberOfLikes = mView.findViewById(R.id.display_no_of_likes);

            LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        public void setLikeButtonStatus(final String PostKey){
            LikesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child(PostKey).hasChild(currentUserId)) {
                        countLikes = (int) dataSnapshot.child(PostKey).getChildrenCount();
                        LikePostButton.setImageResource(R.drawable.like);
                        DisplayNumberOfLikes.setText((Integer.toString(countLikes)) + (" Likes"));
                    }
                    else
                    {
                        countLikes = (int) dataSnapshot.child(PostKey).getChildrenCount();
                        LikePostButton.setImageResource(R.drawable.dislike);
                        DisplayNumberOfLikes.setText((Integer.toString(countLikes)) + (" Likes"));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        public void setFullName(String fullName){
            TextView username = mView.findViewById(R.id.post_user_name);
            username.setText(fullName);
        }

        public void setProfileimage(Context ctx, String profileImage){

            CircleImageView image = mView.findViewById(R.id.post_profile_image);

            if(profileImage == null)
            {
                image.setImageResource(R.drawable.profile);
            }
            else
                {
                Picasso.get().load(profileImage).into(image);
            }
        }

        public void setTime(String time){
            TextView PostTime = mView.findViewById(R.id.post_time);
            PostTime.setText("   " + time);
        }

        public void setDate(String date){
            TextView PostDate = mView.findViewById(R.id.post_date);
            PostDate.setText("   " + date);
        }

        public void setDescription(String description){
            TextView PostDescription = mView.findViewById(R.id.click_post_description);
            PostDescription.setText(description);
        }

        public void setPostimage(Context ctx1, String postimage){
            ImageView PostImage = mView.findViewById(R.id.click_post_image);
            Picasso.get().load(postimage).into(PostImage);
        }
    }

    private void SendUserToPostActivity()
    {
        Intent addNewPostIntent = new Intent(MainActivity.this, PostActivity.class);
        startActivity(addNewPostIntent);
    }

    @Override
    protected void onStart()
    {
        super.onStart();



        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null)
        {
            SendUserToLoginActivity();
        }
        else
        {
            CheckUserExistence();
        }
    }

    private void CheckUserExistence() {
        final String current_user_id = mAuth.getCurrentUser().getUid();

        UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(!dataSnapshot.hasChild(current_user_id))
                {
                    SendUserToSetupActivity();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }

    private void SendUserToSetupActivity() {
        Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(setupIntent);
        finish();
    }

    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(actionBarDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void UserMenuSelector(MenuItem menuItem)
    {
        switch (menuItem.getItemId())
        {
            case R.id.nav_post:
                SendUserToPostActivity();
                break;
            case R.id.nav_profile:
                SendUserToProfileActivity();
                break;
            case R.id.nav_home:
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_friends:
                SendUserToFriendsActivity();
                break;
            case R.id.nav_find_friends:
                SendUserToFindFriendsActivity();
                break;
            case R.id.nav_messages:
                SendUserToFriendsActivity();
                break;
            case R.id.nav_settings:
                SendUserToSettingsActivity();
                break;
            case R.id.nav_Logout:
                updateUserStatus("offline");
                mAuth.signOut();
                SendUserToLoginActivity();
                break;
        }
    }

    private void SendUserToFriendsActivity() {
        Intent friendsIntent = new Intent(MainActivity.this, FriendsActivity.class);
        startActivity(friendsIntent);
    }

    private void SendUserToFindFriendsActivity() {
        Intent settingIntent = new Intent(MainActivity.this, FindFriendsActivity.class);
        startActivity(settingIntent);
    }

    private void SendUserToProfileActivity() {
        Intent settingIntent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(settingIntent);
    }

    private void SendUserToSettingsActivity() {
        Intent settingIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(settingIntent);
    }
}
