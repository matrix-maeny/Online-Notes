package com.matrix_maeny.onlinenotes.notes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;
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
import com.matrix_maeny.onlinenotes.R;
import com.matrix_maeny.onlinenotes.databinding.ActivityNotesEditorBinding;

import java.util.Objects;

public class NotesEditorActivity extends AppCompatActivity {

    private ActivityNotesEditorBinding binding; // normal binding
    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private String currentUserUid = "";
    private ProgressDialog progressDialog;
    private boolean isHave = false;

    private String noteHeading = null, noteContent = null; // global values to have note heading and content
    private boolean update = false; // gives the existence of note, initially not existed, i.e false;
    private boolean isSaved = true; // checks whether the current content saved or not, initially true

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotesEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        Objects.requireNonNull(getSupportActionBar()).setTitle("Editor"); // default title of the toolbar

        FirebaseApp.initializeApp(NotesEditorActivity.this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance());

        noteHeading = getIntent().getStringExtra("heading"); // getting note heading
        noteContent = getIntent().getStringExtra("content"); // getting note content
        update = getIntent().getBooleanExtra("update", false); // getting that the note is already existed or not

        // function to setup values
        initialize();
        setupData();

        // listener when text is changed in the content section, note: listener is added after setting up data
        // if you setup listener before setupData() function, if you back press it will ask dialog
        // if you setup after function, it won't; because when you setting up data the listener will catch because the listener
        // is already setup before it;
        binding.editorContentEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isSaved = false;
            }

            @Override
            public void afterTextChanged(Editable s) {
                isSaved = false;
            }
        });

    }

    private void initialize() {
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserUid = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        progressDialog = new ProgressDialog(NotesEditorActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("Saving data...");
        progressDialog.setMessage("Please wait few seconds...");
    }

    private void setupData() {
        if (update) { // if existed
            binding.editorHeadingEt.setVisibility(View.GONE); // hide heading edit text

            if (noteHeading != null) {
                Objects.requireNonNull(getSupportActionBar()).setTitle("Note: " + noteHeading); // set the title of the toolbar
            }
            if (noteContent != null) {
                binding.editorContentEt.setText(noteContent); // set the content
            }
        } else { // if not existed
            binding.editorHeadingEt.setVisibility(View.VISIBLE); // show heading edit text, "Editor" will be the title of the toolbar
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_editor_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        saveData(); // run function to save
        return super.onOptionsItemSelected(item);
    }

    //Saves the data note
    private void saveData() { // to save data

        boolean checkH;// = checkHeading();
        boolean checkC = checkContent();

        if (update) {
            checkH = true;
        } else {
            checkH = checkHeading();
        }

        if (!checkH && !checkC) {
            finish();
            return;
        }

        if (!checkH) {
            Toast.makeText(NotesEditorActivity.this, "Please enter note Heading", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkC) {
            Toast.makeText(this, "Please enter note Content", Toast.LENGTH_SHORT).show();
            return;
        }

        // checking data
        isHave = false;
        progressDialog.show();

        database.getReference().child("Notes").child(currentUserUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!update && snapshot.exists()) {
                    for (DataSnapshot s : snapshot.getChildren()) {
                        NotesModel model = s.getValue(NotesModel.class);

                        if (model != null && noteHeading.equals(model.getHeading())) {
                            isHave = true;
                            break;
                        }
                    }
                }

                if (!isHave) {
//                    Toast.makeText(NotesEditorActivity.this, "Heading: " + noteHeading, Toast.LENGTH_SHORT).show();
                    NotesModel model = new NotesModel(noteHeading, noteContent);
                    database.getReference().child("Notes").child(currentUserUid)
                            .child(noteHeading).setValue(model).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    try {
                                        progressDialog.dismiss();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    isSaved = true;
                                    finish();
                                } else {
                                    Toast.makeText(NotesEditorActivity.this, "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(e -> Toast.makeText(NotesEditorActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());


                } else {
                    Toast.makeText(NotesEditorActivity.this, "Heading already taken", Toast.LENGTH_SHORT).show();
                    try {
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotesEditorActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                try {
                    progressDialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


    }

    private boolean checkHeading() {

        try {
            noteHeading = binding.editorHeadingEt.getText().toString().trim();
            if (!noteHeading.equals("")) return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean checkContent() {

        try {
            noteContent = binding.editorContentEt.getText().toString().trim();
            if (!noteContent.equals("")) return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    @Override
    public void onBackPressed() {
        checkSaved();
    }

    private void checkSaved() {
        if (isSaved) { // if is saved
            super.onBackPressed(); // exit without asking any thing
        } else { // if not
            new AlertDialog.Builder(NotesEditorActivity.this) // ask dialog to save or not
                    .setTitle("Do you want to save?")
                    .setMessage("Note is not saved..!")
                    .setPositiveButton("save", (dialog, which) -> {
                        saveData(); // save data if yes
                    })
                    .setNegativeButton("discard", (dialog, which) -> finish()).create().show(); // if not finish the activity without saving
        }
    }
}