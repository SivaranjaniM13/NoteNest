package com.example.notenest;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {
    List<Map<String, Object>> notes;
    FirebaseFirestore db;
    String uid;

    public NotesAdapter(List<Map<String, Object>> notes, FirebaseFirestore db, String uid) {
        this.notes = notes; this.db = db; this.uid = uid;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView noteText;
        ImageView noteImage;
        Button deleteBtn;
        ViewHolder(View view) {
            super(view);
            noteText = view.findViewById(R.id.noteText);
            noteImage = view.findViewById(R.id.noteImage);
            deleteBtn = view.findViewById(R.id.deleteBtn);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Map<String, Object> note = notes.get(position);
        String text = (String) note.get("text");
        String imageUrl = (String) note.get("imageUrl");

        if (text != null) { holder.noteText.setText(text); holder.noteImage.setVisibility(View.GONE); }
        else if (imageUrl != null) {
            holder.noteText.setText("📷 Image note");
            holder.noteImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(imageUrl).into(holder.noteImage);
        }

        holder.deleteBtn.setOnClickListener(v -> db.collection("notes").document((String) note.get("id")).delete());
    }

    @Override
    public int getItemCount() { return notes.size(); }
}