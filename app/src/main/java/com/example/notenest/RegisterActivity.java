package com.example.notenest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        EditText name = findViewById(R.id.name);
        Button registerBtn = findViewById(R.id.registerBtn);
        TextView loginLink = findViewById(R.id.loginLink);

        registerBtn.setOnClickListener(v -> {
            String e = email.getText().toString();
            String p = password.getText().toString();
            String n = name.getText().toString();
            auth.createUserWithEmailAndPassword(e, p).addOnSuccessListener(result -> {
                String uid = result.getUser().getUid();
                Map<String, Object> user = new HashMap<>();
                user.put("name", n);
                user.put("email", e);
                db.collection("users").document(uid).set(user);
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            }).addOnFailureListener(ex -> Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show());
        });
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}