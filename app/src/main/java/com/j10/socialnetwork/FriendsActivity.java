package com.j10.socialnetwork;

import android.app.Person;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendsActivity extends AppCompatActivity
{

    private RecyclerView myFriendList;
    private DatabaseReference FriendsRef, UsersRef;
    private FirebaseAuth mAuth;
    private String online_user_id;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        mAuth = FirebaseAuth.getInstance();
        online_user_id = mAuth.getCurrentUser().getUid();
        FriendsRef = FirebaseDatabase.getInstance().getReference().child("Friends").child(online_user_id);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        myFriendList = findViewById(R.id.friend_list);
        myFriendList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        myFriendList.setLayoutManager(linearLayoutManager);


        DisplayAllFriends();
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

        UsersRef.child(online_user_id).child("userState").updateChildren(currentStateMap);
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateUserStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateUserStatus("offline");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateUserStatus("offline");
    }

    private void DisplayAllFriends() {

        FirebaseRecyclerOptions<Friends> options = new FirebaseRecyclerOptions.Builder<Friends>().setQuery(FriendsRef, Friends.class).build();

        FirebaseRecyclerAdapter<Friends, FriendsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsViewHolder holder, int position, @NonNull Friends model) {

                holder.setDate(model.getDate());

                final String usersIDs = getRef(position).getKey();

                UsersRef.child(usersIDs).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            final String userName = dataSnapshot.child("fullname").getValue().toString();
                            final String profileImage = dataSnapshot.child("profileimage").getValue().toString();
                            final String type;

                            if (dataSnapshot.hasChild("userState")){
                                type = dataSnapshot.child("userState").child("type").getValue().toString();

                                if (type.equals("online")) {
                                    holder.onlineStatusView.setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    holder.onlineStatusView.setVisibility(View.INVISIBLE);
                                }
                            }

                            holder.setFullname(userName);
                            holder.setProfileimage(getApplicationContext(), profileImage);

                            holder.mView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    CharSequence options[] = new CharSequence[]
                                            {
                                                    userName + "'s Profile",
                                                    "Send Message"
                                            };

                                    AlertDialog.Builder builder = new AlertDialog.Builder(FriendsActivity.this);
                                    builder.setTitle("Select Option");
                                    builder.setItems(options, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (which == 0){
                                                Intent profileIntent = new Intent(FriendsActivity.this, PersonProfileActivity.class);
                                                profileIntent.putExtra("visit_user_id", usersIDs);
                                                startActivity(profileIntent);
                                            }

                                            if(which == 1){
                                                Intent chatIntent = new Intent(FriendsActivity.this, ChatActivity.class);
                                                chatIntent.putExtra("visit_user_id", usersIDs);
                                                chatIntent.putExtra("userName", userName);
                                                startActivity(chatIntent);
                                            }
                                        }
                                    });
                                    builder.show();

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.all_users_display_layout, viewGroup, false);
                FriendsActivity.FriendsViewHolder viewHolder = new FriendsActivity.FriendsViewHolder(view);
                return viewHolder;
            }
        };

        myFriendList.setAdapter(firebaseRecyclerAdapter);

       firebaseRecyclerAdapter.startListening();
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        ImageView onlineStatusView;

        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            onlineStatusView = (ImageView) itemView.findViewById(R.id.all_user_online_icon);
        }

        public void setProfileimage(Context ctx, String profileimage){
            CircleImageView myImage = mView.findViewById(R.id.all_users_profile_image);
            Picasso.get().load(profileimage).placeholder(R.drawable.profile).into(myImage);
        }

        public void setFullname(String fullname) {
            TextView myName = mView.findViewById(R.id.all_users_profile_full_name);
            myName.setText(fullname);
        }

        public void setDate(String date) {
            TextView friendsDate = mView.findViewById(R.id.all_users_status);
            friendsDate.setText("Friends Since: " + date);
        }


    }
}
