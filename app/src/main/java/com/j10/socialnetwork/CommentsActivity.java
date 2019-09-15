package com.j10.socialnetwork;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Comment;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class CommentsActivity extends AppCompatActivity {


    private RecyclerView CommentsList;
    private ImageButton PostCommentButton;
    private EditText CommentInputText;

    private DatabaseReference UsersRef, PostsRef;
    private FirebaseAuth mAuth;


    private String Post_Key, current_user_id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        Post_Key = getIntent().getExtras().get("PostKey").toString();

        UsersRef = (FirebaseDatabase.getInstance().getReference().child("Users"));
        PostsRef = FirebaseDatabase.getInstance().getReference().child("Posts").child(Post_Key).child("Comments");
        mAuth = FirebaseAuth.getInstance();

        current_user_id = mAuth.getCurrentUser().getUid();

        CommentsList = findViewById(R.id.comments_list);
        CommentsList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        CommentsList.setLayoutManager(linearLayoutManager);

        PostCommentButton = findViewById(R.id.post_comment_button);
        CommentInputText = findViewById(R.id.comment_input);

        PostCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                UsersRef.child(current_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists()){
                            String userName = dataSnapshot.child("username").getValue().toString();

                            ValidateComment(userName);

                            CommentInputText.setText(null);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Comments> options = new FirebaseRecyclerOptions.Builder<Comments>().setQuery(PostsRef, Comments.class).build();


        FirebaseRecyclerAdapter<Comments, CommentsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Comments, CommentsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CommentsViewHolder holder, int position, @NonNull Comments model)
            {
                holder.setComment(model.getComment());
                holder.setDate(model.getDate());
                holder.setTime(model.getTime());
                holder.setUsername(model.getUsername());
            }

            @NonNull
            @Override
            public CommentsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
            {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.all_comments_layout, viewGroup, false);
                CommentsActivity.CommentsViewHolder viewHolder = new CommentsActivity.CommentsViewHolder(view);
                return viewHolder;
            }
        };

        CommentsList.setAdapter(firebaseRecyclerAdapter);

        firebaseRecyclerAdapter.startListening();
    }

    public static class CommentsViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        public CommentsViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;

        }

        public void setComment(String comment) {
            TextView Comment = mView.findViewById(R.id.comment_text);
            Comment.setText(comment);

        }

        public void setDate(String date) {
            TextView Date = mView.findViewById(R.id.comment_date);
            Date.setText("  Date: " + date);
        }

        public void setTime(String time) {
            TextView Time = mView.findViewById(R.id.comment_time);
            Time.setText("   Time: " + time);
        }

        public void setUsername(String username) {
            TextView Username = mView.findViewById(R.id.comment_username);
            Username.setText("@" + username + "   ");
        }


    }

    private void ValidateComment(String userName)
    {
        String commentText = CommentInputText.getText().toString();

        if(TextUtils.isEmpty(commentText))
        {
            Toast.makeText(this, "text required...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Calendar calFordDate = Calendar.getInstance();
            SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
            final String saveCurrentDate = currentDate.format(calFordDate.getTime());

            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
            final String saveCurrentTime = currentTime.format(calFordDate.getTime());

            final String RandomKey = current_user_id + saveCurrentDate + saveCurrentTime;

            HashMap commentsMap = new HashMap();

            commentsMap.put("uid", current_user_id);
            commentsMap.put("comment", commentText);
            commentsMap.put("date", saveCurrentDate);
            commentsMap.put("time", saveCurrentTime);
            commentsMap.put("username", userName);

            PostsRef.child(RandomKey).updateChildren(commentsMap).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Toast.makeText(CommentsActivity.this, "Comment added successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        String message = task.getException().getMessage();
                        Toast.makeText(CommentsActivity.this, "" + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
