package rs.ftn.rpgtracker.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.List;
import java.util.Map;

import rs.ftn.rpgtracker.NewTaskActivity;
import rs.ftn.rpgtracker.R;
import rs.ftn.rpgtracker.model.Task;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final Context context;

    public TaskAdapter(List<Task> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.tvTaskName.setText(task.getName());
        holder.tvTaskDescription.setText(task.getDescription());
        holder.tvTaskCategory.setText("Category: " + (task.getCategory() != null ? task.getCategory().getName() : ""));
        holder.tvTaskTime.setText("Execution time: " + task.getExecutionTime());

        // 🔹 Status spinner
        String[] statuses = holder.itemView.getContext().getResources().getStringArray(R.array.task_status);
        int selectedIndex = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equals(task.getStatus().toString())) {
                selectedIndex = i;
                break;
            }
        }
        holder.spinnerStatus.setSelection(selectedIndex);

        // 🔹 Vidljivost Delete dugmeta
        if (task.getStatus() == Task.Status.COMPLETED ||
                task.getStatus() == Task.Status.NOT_COMPLETED) {
            holder.btnDeleteTask.setVisibility(View.GONE);
        } else {
            holder.btnDeleteTask.setVisibility(View.VISIBLE);
        }

        // 🔹 Listener za status
        holder.spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int currentPos = holder.getAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;

                Task currentTask = taskList.get(currentPos);
                String selected = parent.getItemAtPosition(pos).toString();

                if (!selected.equals(currentTask.getStatus().toString())) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("tasks").document(currentTask.getId())
                            .update("status", selected)
                            .addOnSuccessListener(aVoid -> {
                                currentTask.setStatus(Task.Status.valueOf(selected));
                                Toast.makeText(context, "Status updated to " + selected, Toast.LENGTH_SHORT).show();

                                // ✅ Ako je task kompletiran, proveri kvotu i dodaj XP
                                if (currentTask.getStatus() == Task.Status.COMPLETED) {
                                    String userId = "trenutniUserId"; // TODO: ovde ubaci pravi ID korisnika
                                    completeTask(currentTask, userId);
                                }

                                notifyItemChanged(currentPos);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Error updating status", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        // 🔹 Edit
        holder.btnEditTask.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            Task currentTask = taskList.get(currentPos);
            Intent intent = new Intent(context, NewTaskActivity.class);
            intent.putExtra("taskId", currentTask.getId());
            context.startActivity(intent);
        });

        // 🔹 Delete
        holder.btnDeleteTask.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            Task currentTask = taskList.get(currentPos);

            new android.app.AlertDialog.Builder(context)
                    .setTitle("Brisanje zadatka")
                    .setMessage("Da li ste sigurni da želite da obrišete ovaj zadatak?")
                    .setPositiveButton("Da", (dialog, which) -> {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("tasks").document(currentTask.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    taskList.remove(currentPos);
                                    notifyItemRemoved(currentPos);
                                    Toast.makeText(context, "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Greška pri brisanju: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Ne", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }


    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvTaskDescription, tvTaskCategory, tvTaskTime;
        Spinner spinnerStatus;
        Button btnEditTask, btnDeleteTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvTaskCategory = itemView.findViewById(R.id.tvTaskCategory);
            tvTaskTime = itemView.findViewById(R.id.tvTaskTime);
            spinnerStatus = itemView.findViewById(R.id.spinnerStatus);
            btnEditTask = itemView.findViewById(R.id.btnEditTask);
            btnDeleteTask = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
    private void completeTask(Task task, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Date startRange = task.getQuotaStartDate(); // koristi helper iz Task modela
        if (startRange == null) {
            startRange = new Date(0); // ako je UNLIMITED
        }

        db.collection("taskExecutions")
                .whereEqualTo("taskId", task.getId())
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("executionDate", startRange)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    if (count < task.getMaxQuota()) {
                        int xp = task.getTotalXP();

                        // Dodaj XP korisniku
                        db.collection("users").document(userId)
                                .update("xp", com.google.firebase.firestore.FieldValue.increment(xp));

                        // Snimi izvršenje
                        Map<String, Object> execution = new java.util.HashMap<>();
                        execution.put("taskId", task.getId());
                        execution.put("userId", userId);
                        execution.put("executionDate", new Date());
                        db.collection("taskExecutions").add(execution);

                        Toast.makeText(context, "Task completed! +" + xp + " XP", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Quota reached for this task", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error checking quota: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

}
