package com.matrix_maeny.onlinenotes.registerActivities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.matrix_maeny.onlinenotes.MainActivity;
import com.matrix_maeny.onlinenotes.R;
import com.matrix_maeny.onlinenotes.databinding.ActivityLoginBinding;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog dialog;

    private String email = null, password = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        FirebaseApp.initializeApp(LoginActivity.this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance());
        initialize();
    }

    private void initialize() {
        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        dialog = new ProgressDialog(LoginActivity.this);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage("Please wait few seconds");
        dialog.setTitle("Logging in...");

        binding.loginSignUp.setOnClickListener(loginSignUpListener);
        binding.loginBtn.setOnClickListener(loginBtnListener);


    }

    View.OnClickListener loginSignUpListener = v -> {
        startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        finish();
    };
    View.OnClickListener loginBtnListener = v -> login();


    private void login() {
        if (checkEmail() && checkPassword()) {
            dialog.show();
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

    }

    private boolean checkEmail() {
        try {
            email = Objects.requireNonNull(binding.loginEmail.getText()).toString();
            if (!email.equals("")) return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Please enter Email", Toast.LENGTH_SHORT).show();
        return false;
    }

    private boolean checkPassword() {
        try {
            password = Objects.requireNonNull(binding.loginPassword.getText()).toString();
            if (!password.equals("")) return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Please enter Password", Toast.LENGTH_SHORT).show();
        return false;
    }

}