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

public class SignupActivity extends AppCompatActivity {

    EditText signUpname, signupemail, signupusername, signuppassword;
    TextView loginRedirectText;
    Button signupButton;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        signUpname = findViewById(R.id.name_signup);
        signupemail = findViewById(R.id.email_signup);
        signupusername = findViewById(R.id.username_signup);
        signuppassword = findViewById(R.id.password_signup);
        loginRedirectText = findViewById(R.id.login);
        signupButton = findViewById(R.id.signup_button);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateAllFields()) {
                    checkEmailExists();
                }
            }
        });

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean validateAllFields() {
        String name = signUpname.getText().toString().trim();
        String email = signupemail.getText().toString().trim();
        String username = signupusername.getText().toString().trim();
        String password = signuppassword.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            signUpname.setError("Name is required");
            signUpname.requestFocus();
            isValid = false;
        } else {
            signUpname.setError(null);
        }

        if (email.isEmpty()) {
            signupemail.setError("Email is required");
            if (isValid) signupemail.requestFocus();
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signupemail.setError("Please enter a valid email");
            if (isValid) signupemail.requestFocus();
            isValid = false;
        } else {
            signupemail.setError(null);
        }

        if (username.isEmpty()) {
            signupusername.setError("Username is required");
            if (isValid) signupusername.requestFocus();
            isValid = false;
        } else {
            signupusername.setError(null);
        }

        if (password.isEmpty()) {
            signuppassword.setError("Password is required");
            if (isValid) signuppassword.requestFocus();
            isValid = false;
        } else if (password.length() < 6) {
            signuppassword.setError("Password must be at least 6 characters");
            if (isValid) signuppassword.requestFocus();
            isValid = false;
        } else {
            signuppassword.setError(null);
        }

        return isValid;
    }

    private void checkEmailExists() {
        String email = signupemail.getText().toString().trim();

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");

        // Query to check if any user has this email
        Query query = databaseReference.orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // No users with this email exist, proceed
                    registerUser();
                } else {
                    // Email already registered
                    Toast.makeText(SignupActivity.this, "User already exists with this email", Toast.LENGTH_SHORT).show();
                    signupemail.setError("This email is already registered");
                    signupemail.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SignupActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser() {
        String name = signUpname.getText().toString().trim();
        String email = signupemail.getText().toString().trim();
        String username = signupusername.getText().toString().trim();
        String password = signuppassword.getText().toString().trim();

        HelperClass helperClass = new HelperClass(name, email, username, password);

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");

        databaseReference.child(username).setValue(helperClass);

        Toast.makeText(SignupActivity.this, "You have signed up successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
