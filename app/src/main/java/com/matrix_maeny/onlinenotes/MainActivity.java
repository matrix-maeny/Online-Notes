package com.matrix_maeny.onlinenotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.matrix_maeny.onlinenotes.databinding.ActivityMainBinding;
import com.matrix_maeny.onlinenotes.notes.NotesAdapter;
import com.matrix_maeny.onlinenotes.notes.NotesEditorActivity;
import com.matrix_maeny.onlinenotes.notes.NotesModel;
import com.matrix_maeny.onlinenotes.registerActivities.LoginActivity;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NotesAdapter.NotesAdapterListener {

    private ActivityMainBinding binding; // binding to hold views
    private ArrayList<NotesModel> list; // list for notes
    private NotesAdapter adapter; // adapter to hold notes

    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth auth;
    private String currentUserUid = "";
    private ProgressDialog progressDialog;

    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseApp.initializeApp(MainActivity.this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance());

        initialize(); // the function for first initialization
    }

    private void initialize() {

        firebaseDatabase = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserUid = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        list = new ArrayList<>(); // initializing list
        adapter = new NotesAdapter(MainActivity.this, list); // initializing adapter


        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("Loading...");
        progressDialog.setMessage("Fetching data...");

        getCurrentUserData();


//        list.add(new NotesModel("Baktha","Hello world"));
//        list.add(new NotesModel("Baktha1","Hello world"));
//        list.add(new NotesModel("Baktha2","Hello world"));
//        list.add(new NotesModel("Baktha3","Hello world"));

        // setting layout manager and adapter to recycler view to hold data
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        binding.recyclerView.setAdapter(adapter);

        fetchNotesDataFromOnline();

    }

    private void fetchNotesDataFromOnline() {
        firebaseDatabase.getReference().child("Notes").child(currentUserUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        list.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot s : snapshot.getChildren()) {
                                NotesModel model = s.getValue(NotesModel.class);
                                list.add(model);
                            }


                        }
                        if (list.isEmpty()) {
                            binding.emptyTxtTv.setVisibility(View.VISIBLE);
                        } else {
                            binding.emptyTxtTv.setVisibility(View.GONE);

                        }
                        refreshAdapter();
                        try {
                            progressDialog.dismiss(); // dismissing the waiting dialog
                        } catch (Exception e) {
                            e.printStackTrace(); // in case of any illegal state exception
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshAdapter() {
        try {
            handler.post(() -> adapter.notifyDataSetChanged());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void getCurrentUserData() {
        progressDialog.show();
        firebaseDatabase.getReference().child("Users").child(Objects.requireNonNull(auth.getCurrentUser()).getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserModel model = snapshot.getValue(UserModel.class);
                        if (model != null) {
                            String temp = "<u>" + model.getUsername() + "  </u>"; // creating an underlined String from html
                            Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml(temp)); // setting title of the toolbar
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    // to inflate toolbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    // to function for each option in toolbar options
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_:
                // go to about activity
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                break;
            case R.id.log_out:
                // log out from the device
                auth.signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                break;
            case R.id.new_note:
                // create a new note
                addNewNote();
                break;
            case R.id.delete_all:
                deleteAll();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAll() {
        progressDialog.show();
        firebaseDatabase.getReference().child("Notes").child(currentUserUid).removeValue().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Toast.makeText(MainActivity.this, "All deleted", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(MainActivity.this, "Error: "+ Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }

            fetchNotesDataFromOnline();
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addNewNote() {
        startActivity(new Intent(MainActivity.this, NotesEditorActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        fetchNotesDataFromOnline();
    }

    @Override
    public void refreshAfterDelete() {
        fetchNotesDataFromOnline();
    }
}