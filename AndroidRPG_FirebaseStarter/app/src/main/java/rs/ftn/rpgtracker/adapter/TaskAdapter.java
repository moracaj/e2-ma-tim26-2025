package rs.ftn.rpgtracker.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.List;

import rs.ftn.rpgtracker.NewTaskActivity;
import rs.ftn.rpgtracker.R;
import rs.ftn.rpgtracker.model.Task;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final Context context;
    private final String[] statusArray; // niz iz strings.xml

    public TaskAdapter(List<Task> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
        this.statusArray = context.getResources().getStringArray(R.array.task_status);
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
        holder.tvTaskTime.setText("Time: " + task.getExecutionTime());

        //koristimo status iz string-array umesto enum.toString()
        String statusText = statusArray[task.getStatus().ordinal()];
        holder.tvTaskStatus.setText("Status: " + statusText);

        // Klik na zadatak → dijalog sa opcijama
        holder.itemView.setOnClickListener(v -> showTaskDialog(task, holder.getAdapterPosition()));

        //Edit
        holder.btnEditTask.setOnClickListener(v -> {
            Intent intent = new Intent(context, NewTaskActivity.class);
            intent.putExtra("taskId", task.getId());
            context.startActivity(intent);
        });


        holder.btnDeleteTask.setOnClickListener(v -> {

            //Ne dozvoli brisanje završenih zadataka
            if (task.getStatus() == Task.Status.COMPLETED) {
                Toast.makeText(context, "You cannot delete completed tasks.", Toast.LENGTH_LONG).show();
                return;
            }

            new android.app.AlertDialog.Builder(context)
                    .setTitle("Delete task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Yes", (dialog, which) -> {

                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        // Ako je zadatak ponavljajuci obrisi sva buduca ponavljanja
                        if (task.isRecurring()) {
                            Date now = new Date();

                            db.collection("tasks")
                                    .whereEqualTo("userId", task.getUserId())
                                    .whereEqualTo("name", task.getName()) // ili neki sharedGroupId ako ga imaš
                                    .whereEqualTo("recurring", true)
                                    .get()
                                    .addOnSuccessListener(query -> {
                                        for (var doc : query.getDocuments()) {
                                            Task recurringTask = doc.toObject(Task.class);
                                            if (recurringTask != null && recurringTask.getStartDate() != null) {
                                                if (recurringTask.getStartDate().after(now)) {
                                                    db.collection("tasks").document(doc.getId()).delete();
                                                }
                                            }
                                        }

                                        // Ukloni i originalni zadatak iz prikaza
                                        taskList.remove(holder.getAdapterPosition());
                                        notifyItemRemoved(holder.getAdapterPosition());
                                        Toast.makeText(context, "Future recurrences deleted.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(context, "Error deleting recurring tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                        else {
                            // Ako nije ponavljajuci – obrisi samo ovaj
                            db.collection("tasks").document(task.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        taskList.remove(holder.getAdapterPosition());
                                        notifyItemRemoved(holder.getAdapterPosition());
                                        Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(context, "Something went wrong: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }

                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });


        // Boje za status
        switch (task.getStatus()) {
            case COMPLETED:
                holder.tvTaskStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                break;
            case CANCELLED:
                holder.tvTaskStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                break;
            case PAUSED:
                holder.tvTaskStatus.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case NOT_COMPLETED:
                holder.tvTaskStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                break;
            default: // ACTIVE
                holder.tvTaskStatus.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
                break;
        }
    }

    private void showTaskDialog(Task task, int position) {
        String statusText = statusArray[task.getStatus().ordinal()];

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context)
                .setTitle(task.getName())
                .setMessage("Description: " + (task.getDescription() != null ? task.getDescription() : "No description") +
                        "\nTime: " + task.getExecutionTime() +
                        "\nStatus: " + statusText)
                .setCancelable(true);

        if (task.getStatus() == Task.Status.ACTIVE) {
            builder.setPositiveButton("Completed", (dialog, which) -> updateStatus(task, Task.Status.COMPLETED, position));
            builder.setNegativeButton("Cancelled", (dialog, which) -> updateStatus(task, Task.Status.CANCELLED, position));
            if (task.isRecurring()) {
                builder.setNeutralButton("Paused", (dialog, which) -> updateStatus(task, Task.Status.PAUSED, position));
            }
        } else if (task.getStatus() == Task.Status.PAUSED) {
            builder.setPositiveButton("Activate again", (dialog, which) -> updateStatus(task, Task.Status.ACTIVE, position));
        }

        builder.show();
    }

    private void updateStatus(Task task, Task.Status newStatus, int position) {
        Date today = new Date();

        if (!task.canBeUpdated(today)) {
            Toast.makeText(context, "The task can no longer be changed.", Toast.LENGTH_SHORT).show();
            notifyItemChanged(position);
            return;
        }

        if (task.getStatus() == Task.Status.ACTIVE) {
            if (newStatus == Task.Status.COMPLETED ||
                    newStatus == Task.Status.CANCELLED ||
                    newStatus == Task.Status.PAUSED) {
                task.setStatus(newStatus);
            } else {
                Toast.makeText(context, "Not allowed!", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (task.getStatus() == Task.Status.PAUSED) {
            if (newStatus == Task.Status.ACTIVE) {
                task.setStatus(Task.Status.ACTIVE);
            } else {
                Toast.makeText(context, "You can only activate task again.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(context, "This task can't be changed.", Toast.LENGTH_SHORT).show();
            return;
        }

        int xpReward = task.calculateXpReward();
        if (xpReward > 0) {
            addXpToUser(xpReward);
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tasks").document(task.getId())
                .update("status", task.getStatus().toString())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Status updated: " + statusArray[task.getStatus().ordinal()], Toast.LENGTH_SHORT).show();
                    notifyItemChanged(position);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error updating status", Toast.LENGTH_SHORT).show());
    }

    private void addXpToUser(int xp) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                long currentXp = doc.getLong("xp") != null ? doc.getLong("xp") : 0;
                long nextLevelXp = doc.getLong("nextLevelXp") != null ? doc.getLong("nextLevelXp") : 100;
                long level = doc.getLong("level") != null ? doc.getLong("level") : 1;

                currentXp += xp; // dodaj XP nagradu

                if (currentXp >= nextLevelXp) {
                    level++;
                    currentXp = 0; // reset XP jer je prešao nivo
                    db.collection("users").document(uid)
                            .update("level", level,
                                    "xp", currentXp,
                                    "nextLevelXp", nextLevelXp * 1.5,
                                    "canFightBoss", true)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(context, "🎉 Level up! You can fight the boss now!", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Error leveling up: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    db.collection("users").document(uid)
                            .update("xp", currentXp)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(context, "+ " + xp + " XP!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Error adding XP", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvTaskDescription, tvTaskCategory, tvTaskTime, tvTaskStatus;

        Button btnEditTask, btnDeleteTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvTaskCategory = itemView.findViewById(R.id.tvTaskCategory);
            tvTaskTime = itemView.findViewById(R.id.tvTaskTime);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
            btnEditTask = itemView.findViewById(R.id.btnEditTask);
            btnDeleteTask = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
}
