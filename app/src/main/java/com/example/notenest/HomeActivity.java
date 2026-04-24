package com.example.notenest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.*;
import java.util.*;

public class HomeActivity extends AppCompatActivity {

    FirebaseFirestore db;
    FirebaseAuth auth;
    FirebaseStorage storage;
    RecyclerView recyclerView;
    NotesAdapter adapter;
    List<Map<String, Object>> notes = new ArrayList<>();
    static final int PICK_IMAGE = 1001;
    static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // ── FCM: get device token and log it ──────────────────────────
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d("FCM_TOKEN", "Device token: " + token);
                    Toast.makeText(this, "FCM token logged!", Toast.LENGTH_SHORT).show();

                    // Also save token to Firestore under this user
                    String uid = auth.getCurrentUser().getUid();
                    Map<String, Object> tokenData = new HashMap<>();
                    tokenData.put("fcmToken", token);
                    db.collection("users").document(uid).update(tokenData)
                            .addOnSuccessListener(a -> Log.d(TAG, "Token saved to Firestore"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to save token: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e("FCM_TOKEN", "Failed to get token: " + e.getMessage()));
        // ──────────────────────────────────────────────────────────────

        // RecyclerView setup
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotesAdapter(notes, db, auth.getCurrentUser().getUid());
        recyclerView.setAdapter(adapter);

        // Views
        EditText noteInput = findViewById(R.id.noteInput);
        Button addBtn     = findViewById(R.id.addBtn);
        Button uploadBtn  = findViewById(R.id.uploadBtn);
        Button logoutBtn  = findViewById(R.id.logoutBtn);

        // ── Real-time Firestore listener (READ) ───────────────────────
        db.collection("notes")
                .whereEqualTo("uid", auth.getCurrentUser().getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed: " + e.getMessage());
                        return;
                    }
                    notes.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Map<String, Object> note = doc.getData();
                        note.put("id", doc.getId());
                        notes.add(note);
                    }
                    adapter.notifyDataSetChanged();
                });
        // ──────────────────────────────────────────────────────────────

        // ── ADD note (CREATE) ─────────────────────────────────────────
        addBtn.setOnClickListener(v -> {
            String text = noteInput.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter a note", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> note = new HashMap<>();
            note.put("text", text);
            note.put("uid", auth.getCurrentUser().getUid());
            note.put("timestamp", FieldValue.serverTimestamp());

            db.collection("notes").add(note)
                    .addOnSuccessListener(ref -> {
                        Log.d(TAG, "Note added: " + ref.getId());
                        noteInput.setText("");
                        Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(err -> {
                        Log.e(TAG, "Error adding note: " + err.getMessage());
                        Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show();
                    });
        });
        // ──────────────────────────────────────────────────────────────

        // ── UPLOAD image to Firebase Storage ─────────────────────────
        uploadBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });
        // ──────────────────────────────────────────────────────────────

        // ── LOGOUT ────────────────────────────────────────────────────
        logoutBtn.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        // ──────────────────────────────────────────────────────────────
    }

    // ── Image picker result ───────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            String fileName = "images/" + UUID.randomUUID().toString();
            StorageReference storageRef = storage.getReference().child(fileName);

            Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get download URL after upload
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                            Map<String, Object> note = new HashMap<>();
                            note.put("imageUrl", downloadUrl.toString());
                            note.put("uid", auth.getCurrentUser().getUid());
                            note.put("timestamp", FieldValue.serverTimestamp());

                            db.collection("notes").add(note)
                                    .addOnSuccessListener(ref -> {
                                        Log.d(TAG, "Image note saved: " + downloadUrl);
                                        Toast.makeText(this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Upload failed: " + e.getMessage());
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}