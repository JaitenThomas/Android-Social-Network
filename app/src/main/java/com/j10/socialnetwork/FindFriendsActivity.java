package com.j10.socialnetwork;

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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


public class FindFriendsActivity extends AppCompatActivity
{

    private Toolbar mToolbar;
    private ImageButton SearchButton;
    private EditText SearchInputText;

    private RecyclerView SearchResultList;

    private DatabaseReference allUsersDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        allUsersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users");

        mToolbar = findViewById(R.id.find_friends_appbar_layout);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Find Friends");

        SearchResultList = findViewById(R.id.search_result_list);
        SearchResultList.setHasFixedSize(true);
        SearchResultList.setLayoutManager(new LinearLayoutManager(this));

        SearchButton = findViewById(R.id.search_people_friends_button);
        SearchInputText = findViewById(R.id.search_box_input);

        SearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String searchBoxInput = SearchInputText.getText().toString();
                SearchPeopleAndFriends(searchBoxInput);
            }
        });
    }

    private void SearchPeopleAndFriends(String searchBoxInput) {

        Toast.makeText(this, "Searching...", Toast.LENGTH_LONG).show();

        // Query
        Query searchPeopleandFriendsQuery = allUsersDatabaseRef.orderByChild("fullname").startAt(searchBoxInput).endAt(searchBoxInput + "\uf8ff");

        FirebaseRecyclerOptions<FindFriends> options = new FirebaseRecyclerOptions.Builder<FindFriends>().setQuery(searchPeopleandFriendsQuery, FindFriends.class).build();

        FirebaseRecyclerAdapter<FindFriends, FindFriendsViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<FindFriends, FindFriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FindFriendsViewHolder holder, final int position, @NonNull FindFriends model) {
                holder.setFullname(model.getFullname());
                holder.setStatus(model.getStatus());
                holder.setProfileimage(model.getProfileimage());

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String visit_user_id = getRef(position).getKey();

                        Intent profileIntent = new Intent(FindFriendsActivity.this, PersonProfileActivity.class);
                        profileIntent.putExtra("visit_user_id", visit_user_id);
                        startActivity(profileIntent);
                    }
                });
            }

            @NonNull
            @Override
            public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.all_users_display_layout, viewGroup, false);
                FindFriendsActivity.FindFriendsViewHolder viewHolder = new FindFriendsActivity.FindFriendsViewHolder(view);
                return viewHolder;
            }
        };

        SearchResultList.setAdapter(firebaseRecyclerAdapter);

        firebaseRecyclerAdapter.startListening();
    }

    public static class FindFriendsViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setProfileimage(String profileimage){
            CircleImageView myImage = mView.findViewById(R.id.all_users_profile_image);
            Picasso.get().load(profileimage).placeholder(R.drawable.profile).into(myImage);
        }

        public void setFullname(String fullname) {
            TextView myName = mView.findViewById(R.id.all_users_profile_full_name);
            myName.setText(fullname);
        }

        public void setStatus(String status) {
            TextView myStatus = mView.findViewById(R.id.all_users_status);
            myStatus.setText(status);
        }


    }
}
