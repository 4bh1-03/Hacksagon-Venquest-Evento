package com.example.evento;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    EditText loginusername, loginpassword;
    TextView signupRedirectText;
    Button loginButton;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        loginusername = findViewById(R.id.username_login);
        loginpassword = findViewById(R.id.password_login);
        signupRedirectText = findViewById(R.id.signup);
        loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Fixed: Proper validation logic
                if (validateUser() && validatepassword()) {
                    Toast.makeText(LoginActivity.this, "User authenticated", Toast.LENGTH_SHORT).show();
                    checkUser();
                } else {
                    Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });

        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public boolean validateUser() {
        String val = loginusername.getText().toString().trim();
        if (val.isEmpty()) {
            loginusername.setError("Username cannot be empty");
            return false;
        } else {
            loginusername.setError(null);
            return true;
        }
    }

    public boolean validatepassword() {
        String pass = loginpassword.getText().toString().trim();
        if (pass.isEmpty()) {
            loginpassword.setError("Password cannot be empty");
            return false;
        } else {
            loginpassword.setError(null);
            return true;
        }
    }

    // Problem 3: Fixed login authentication logic
    public void checkUser() {
        String userUsername = loginusername.getText().toString().trim();
        String userPassword = loginpassword.getText().toString().trim();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        // Fixed: Remove quotes around userUsername variable
        Query checkUserDatabase = reference.orderByChild("username").equalTo(userUsername);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    loginusername.setError(null);
                    // Get the user data from snapshot
                    String passwordFromDatabase = null;
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        passwordFromDatabase = userSnapshot.child("password").getValue(String.class);
                        break; // Get the first (and should be only) match
                    }

                    // Fixed: Correct password comparison logic
                    if (Objects.equals(passwordFromDatabase, userPassword)) {
                        // Password matches - login successful
                        loginusername.setError(null);
                        loginpassword.setError(null);

                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // Close login activity
                    } else {
                        // Password doesn't match
                        loginpassword.setError("Wrong Password");
                        loginpassword.requestFocus();
                    }
                } else {
                    // User doesn't exist
                    loginusername.setError("User does not exist");
                    loginusername.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
