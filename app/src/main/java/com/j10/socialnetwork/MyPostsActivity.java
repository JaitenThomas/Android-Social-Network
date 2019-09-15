package com.j10.socialnetwork;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


public class MyPostsActivity extends AppCompatActivity
{

    private Toolbar mToolbar;

    private RecyclerView myPostsList;

    // Authentication
    private FirebaseAuth mAuth;
    private String currentUserID;

    //Database references
    private DatabaseReference PostsRef, UsersRef, LikesRef;

    Boolean LikeCheaker = false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();


        // Database References
        PostsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        // Toolbar Configeration
        mToolbar = (Toolbar) findViewById(R.id.my_post_bar_layout);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("My Posts");

        myPostsList = findViewById(R.id.my_all_post_list);
        myPostsList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        myPostsList.setLayoutManager(linearLayoutManager);

        DisplayMyAllPosts();


    }

    private void DisplayMyAllPosts()
    {

        Query myPostsQuery = PostsRef.orderByChild("uid")
            .startAt(currentUserID).endAt(currentUserID + "\uf8ff");

        FirebaseRecyclerOptions<Posts> options = new FirebaseRecyclerOptions.Builder<Posts>().setQuery(myPostsQuery, Posts.class).build();

        FirebaseRecyclerAdapter<Posts, MyPostsActivity.MyPostsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Posts, MyPostsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MyPostsViewHolder holder, int position, @NonNull Posts model) {

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
                        Intent clickPostIntent = new Intent(MyPostsActivity.this, ClickPostActivity.class);
                        clickPostIntent.putExtra("PostKey", PostKey);
                        startActivity(clickPostIntent);
                    }
                });

                holder.CommentPostButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        Intent commentsIntent = new Intent(MyPostsActivity.this, CommentsActivity.class);
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
            public MyPostsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.all_posts_layout, viewGroup, false);
                MyPostsViewHolder viewHolder = new MyPostsViewHolder(view);
                return viewHolder;
            }
        };

        myPostsList.setAdapter(firebaseRecyclerAdapter);
        firebaseRecyclerAdapter.startListening();
    }

    public static class MyPostsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        ImageButton LikePostButton, CommentPostButton;
        TextView DisplayNumberOfLikes;
        int countLikes;
        String currentUserId;
        DatabaseReference LikesRef;

        public MyPostsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            mView = itemView;

            LikePostButton = mView.findViewById(R.id.like_button);
            CommentPostButton = mView.findViewById(R.id.comment_button);
            DisplayNumberOfLikes = mView.findViewById(R.id.display_no_of_likes);

            LikesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

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
    }
}
