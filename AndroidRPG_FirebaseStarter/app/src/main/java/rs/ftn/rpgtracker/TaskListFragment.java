package rs.ftn.rpgtracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import rs.ftn.rpgtracker.adapter.TaskAdapter;
import rs.ftn.rpgtracker.model.Task;

public class TaskListFragment extends Fragment {

    private static final String ARG_IS_RECURRING = "isRecurring";
    private boolean isRecurring;

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList = new ArrayList<>();
    private FirebaseFirestore db;

    public static TaskListFragment newInstance(boolean isRecurring) {
        TaskListFragment fragment = new TaskListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_RECURRING, isRecurring);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isRecurring = getArguments().getBoolean(ARG_IS_RECURRING);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        taskAdapter = new TaskAdapter(taskList, getContext());
        recyclerView.setAdapter(taskAdapter);

        loadTasksFromDb();

        return view;
    }

    private void loadTasksFromDb() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tasks")
                .whereEqualTo("userId", uid)
                .whereEqualTo("recurring", isRecurring)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        taskList.add(task);
                    }
                    taskAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Greška pri učitavanju zadataka: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
