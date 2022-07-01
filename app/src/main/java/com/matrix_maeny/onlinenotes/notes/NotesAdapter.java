package com.matrix_maeny.onlinenotes.notes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.matrix_maeny.onlinenotes.R;
import com.matrix_maeny.onlinenotes.databinding.NotesModelBinding;

import java.util.ArrayList;
import java.util.Objects;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.viewHolder> {

    private Context context;
    private ArrayList<NotesModel> list;

    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private String currentUserUid = "";
    private NotesAdapterListener listener;

    public NotesAdapter(Context context, ArrayList<NotesModel> list) {
        this.context = context;
        this.list = list;

        FirebaseApp.initializeApp(context.getApplicationContext());
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance());

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserUid = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        try {
            listener = (NotesAdapterListener) context;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notes_model,parent,false);
        return new viewHolder(view);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {

        NotesModel model = list.get(position);
        holder.modelBinding.modelNoteHeadingTv.setText(model.getHeading());

        holder.modelBinding.cardView.setOnClickListener(v->{
            gotoEditing(model);
        });

        holder.modelBinding.cardView.setOnLongClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context.getApplicationContext(),holder.modelBinding.cardView);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()){
                    case R.id.share_note:
                        // share note content;
                        shareNotes(model);
                        break;
                    case R.id.delete_note:
                        // delete specific note;
                        deleteNote(model.getHeading());
                        break;
                }
                return true;
            });
            popupMenu.show();
            return true;
        });
    }

    private void shareNotes(NotesModel model) {
        Intent intent = new Intent();
        intent.setType("text/plain");
        intent.setAction(Intent.ACTION_SEND);

        String shareTxt = "******* "+model.getHeading()+" ******"+"\n\n"+model.getContent()+"\n\n@matrix";
        intent.putExtra(Intent.EXTRA_TEXT,shareTxt);

        context.startActivity(Intent.createChooser(intent,"Share using"));
    }

    private void deleteNote(String heading) {
        database.getReference().child("Notes").child(currentUserUid)
                .child(heading).removeValue().addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Toast.makeText(context, "deleted successfully", Toast.LENGTH_SHORT).show();
                        listener.refreshAfterDelete();
                    }
                }).addOnFailureListener(e -> Toast.makeText(context, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void gotoEditing(NotesModel model) {
       Intent intent = new Intent(context.getApplicationContext(),NotesEditorActivity.class);
       intent.putExtra("heading",model.getHeading());
       intent.putExtra("content",model.getContent());
       intent.putExtra("update",true);
       context.startActivity(intent);
    }

    public interface NotesAdapterListener{
        void refreshAfterDelete();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class viewHolder extends RecyclerView.ViewHolder {

        NotesModelBinding modelBinding;

        public viewHolder(@NonNull View itemView) {
            super(itemView);

            modelBinding = NotesModelBinding.bind(itemView);
        }
    }
}
