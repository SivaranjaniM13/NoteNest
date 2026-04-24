package com.example.notenest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.*;
import com.google.firebase.auth.*;
import com.google.android.gms.common.api.ApiException;

public class LoginActivity extends AppCompatActivity {
    FirebaseAuth auth;
    GoogleSignInClient googleClient;
    static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();

        // If already logged in, skip to Home
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        googleClient = GoogleSignIn.getClient(this, gso);

        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        Button loginBtn = findViewById(R.id.loginBtn);
        Button googleBtn = findViewById(R.id.googleBtn);
        TextView registerLink = findViewById(R.id.registerLink);

        loginBtn.setOnClickListener(v -> {
            auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                    .addOnSuccessListener(r -> { startActivity(new Intent(this, HomeActivity.class)); finish(); })
                    .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        googleBtn.setOnClickListener(v -> startActivityForResult(googleClient.getSignInIntent(), RC_SIGN_IN));
        registerLink.setOnClickListener(v -> { startActivity(new Intent(this, RegisterActivity.class)); finish(); });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                auth.signInWithCredential(credential).addOnSuccessListener(r -> {
                    startActivity(new Intent(this, HomeActivity.class)); finish();
                });
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}