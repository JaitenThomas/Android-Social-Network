package com.j10.socialnetwork;


import android.app.ActionBar;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity
{

    private Toolbar ChatToolbar;
    private ImageButton SendMessageButton, SendImageFileButton;
    private EditText userMessageInput;

    private RecyclerView userMessagesList;
    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessagesAdapter messagesAdapter;


    private String messageReceiverID, messageReceiverName, messageSenderID, saveCurrentDate, saveCurrentTime;


    // TextView
    private TextView receiverName, userLastSeen;

    // Images
    private CircleImageView receiverProfileImage;

    private DatabaseReference RootRef, UsersRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        RootRef = FirebaseDatabase.getInstance().getReference();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        messageReceiverID = getIntent().getExtras().get("visit_user_id").toString();
        messageReceiverName = getIntent().getExtras().get("userName").toString();

        mAuth = FirebaseAuth.getInstance();
        messageSenderID = mAuth.getCurrentUser().getUid();

        IntializeFields();

        DisplayReceiverInfo();

        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendMessage();

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(userMessageInput.getWindowToken(), 0);

                userMessagesList.scrollToPosition(messagesList.size() - 1);

            }
        });

        updateUserStatus("online");

        FetchMessages();
    }

    private void FetchMessages()
    {
        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s)
            {
                if(dataSnapshot.exists())
                {
                    Messages messages = dataSnapshot.getValue(Messages.class);
                    messagesList.add(messages);
                    messagesAdapter.notifyDataSetChanged();
                    userMessagesList.scrollToPosition(messagesList.size() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendMessage()
    {

        final String messageText = userMessageInput.getText().toString();
        
        if(TextUtils.isEmpty(messageText))
        {
            Toast.makeText(this, "Please enter text before sending...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            String message_sender_ref = "Messages/" + messageSenderID + "/" + messageReceiverID;
            String message_receiver_ref = "Messages/" + messageReceiverID + "/" + messageSenderID;

            DatabaseReference user_message_key = RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).push();

            String message_push_id = user_message_key.getKey();

            Calendar calFordDate = Calendar.getInstance();
            SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
            saveCurrentDate = currentDate.format(calFordDate.getTime());

            SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm aa");
            saveCurrentTime = currentTime.format(calFordDate.getTime());

            Map messageTextBody = new HashMap();
            messageTextBody.put("message", messageText);
            messageTextBody.put("time", saveCurrentTime);
            messageTextBody.put("date", saveCurrentDate);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderID);

            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(message_sender_ref + "/" + message_push_id, messageTextBody);
            messageBodyDetails.put(message_receiver_ref + "/" + message_push_id, messageTextBody);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Toast.makeText(ChatActivity.this, "Message Sent Successfully", Toast.LENGTH_SHORT).show();
                        userMessageInput.setText("");
                    }
                    else {
                        String message = task.getException().getMessage();
                        Toast.makeText(ChatActivity.this, "" + message, Toast.LENGTH_SHORT).show();
                        userMessageInput.setText("");
                    }


                }
            });
        }
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

        UsersRef.child(messageSenderID).child("userState").updateChildren(currentStateMap);
    }

    private void DisplayReceiverInfo() {
        receiverName.setText(messageReceiverName);

        RootRef.child("Users").child(messageReceiverID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    final String profileImage = dataSnapshot.child("profileimage").getValue().toString();
                    final String type = dataSnapshot.child("userState").child("type").getValue().toString();
                    final String lastDate = dataSnapshot.child("userState").child("date").getValue().toString();
                    final String lastTime = dataSnapshot.child("userState").child("time").getValue().toString();

                    if (type.equals("online"))
                    {
                        userLastSeen.setText("online");
                    }
                    else
                    {
                        userLastSeen.setText("last seen: " + lastTime + " " + lastDate );
                    }

                    Picasso.get().load(profileImage).placeholder(R.drawable.profile).into(receiverProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void IntializeFields() {

        //Toolbar
        ChatToolbar = findViewById(R.id.chat_bar_layout);
        setSupportActionBar(ChatToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = layoutInflater.inflate(R.layout.chat_custom_bar, null);
        getSupportActionBar().setCustomView(action_bar_view);


        //TextView
        receiverName = findViewById(R.id.custom_profile_name);
        userLastSeen = findViewById(R.id.custom_user_last_seen);

        //Images
        receiverProfileImage = findViewById(R.id.custom_profile_image);

        SendMessageButton = findViewById(R.id.send_message_button);
        SendImageFileButton = findViewById(R.id.send_image_file_button);

        userMessageInput = findViewById(R.id.input_message);
        userMessagesList = findViewById(R.id.messages_list_users);


        messagesAdapter = new MessagesAdapter(messagesList);
        userMessagesList = findViewById(R.id.messages_list_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setHasFixedSize(true);
        userMessagesList.setLayoutManager(linearLayoutManager);

        userMessagesList.setAdapter(messagesAdapter);



    }
}
