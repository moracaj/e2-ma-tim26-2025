package rs.ftn.rpgtracker.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rs.ftn.rpgtracker.R;
import rs.ftn.rpgtracker.model.Task;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private final Context context;
    private final List<Date> days;          // svi datumi za prikaz (npr. ceo mesec)
    private final List<Task> tasks;         // svi taskovi iz Firestore

    public CalendarAdapter(Context context, List<Date> days, List<Task> tasks) {
        this.context = context;
        this.days = days;
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        Date date = days.get(position);

        if (date == null) {
            // prazan slot (početak meseca)
            holder.tvDayNumber.setText("");
            holder.layoutTasks.removeAllViews();
            return;
        }

        // Formatiraj dan u mesecu (1–31)
        SimpleDateFormat sdfDay = new SimpleDateFormat("d", Locale.getDefault());
        holder.tvDayNumber.setText(sdfDay.format(date));

        // Očisti prethodne zadatke
        holder.layoutTasks.removeAllViews();

        for (Task task : tasks) {
            if (task.getStartDate() == null) continue;

            if (!task.isRecurring()) {
                // 👉 Jednokratni zadatak – samo na startDate
                SimpleDateFormat sdfCompare = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                String currentDateStr = sdfCompare.format(date);
                String taskDateStr = sdfCompare.format(task.getStartDate());

                if (taskDateStr.equals(currentDateStr)) {
                    addTaskView(holder, task);
                }
            } else {
                // 👉 Ponavljajući zadatak – prikazuj u intervalu
                Date start = task.getStartDate();
                Date end = task.getEndDate();
                if (start != null && end != null && !date.before(start) && !date.after(end)) {
                    long diffDays = (date.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);

                    if (task.getRepeatUnit() == Task.RepeatUnit.DAN) {
                        if (diffDays % task.getRepeatInterval() == 0) {
                            addTaskView(holder, task);
                        }
                    } else if (task.getRepeatUnit() == Task.RepeatUnit.NEDELJA) {
                        long diffWeeks = diffDays / 7;
                        if (diffWeeks % task.getRepeatInterval() == 0) {
                            addTaskView(holder, task);
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;
        LinearLayout layoutTasks;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            layoutTasks = itemView.findViewById(R.id.layoutTasks);
        }
    }

    // 🔹 pomoćna metoda za dodavanje taska u dan
    private void addTaskView(DayViewHolder holder, Task task) {
        TextView tv = new TextView(context);
        tv.setText(task.getName());
        tv.setTextSize(12);
        tv.setPadding(4, 2, 4, 2);
        tv.setTextColor(context.getResources().getColor(android.R.color.white));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12f);
        int color = task.getCategory() != null ? task.getCategory().getColor() : 0xFF9E9E9E;
        bg.setColor(color);
        tv.setBackground(bg);

        tv.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context)
                    .setTitle(task.getName())
                    .setMessage("Opis: " + (task.getDescription() != null ? task.getDescription() : "Nema opisa") +
                            "\nVreme: " + task.getExecutionTime() +
                            "\nStatus: " + task.getStatus());

            if (task.getStatus() == Task.Status.ACTIVE) {
                builder.setPositiveButton("Urađeno", (dialog, which) -> updateStatus(task, Task.Status.COMPLETED));
                builder.setNegativeButton("Otkazano", (dialog, which) -> updateStatus(task, Task.Status.CANCELED));
                if (task.isRecurring()) {
                    builder.setNeutralButton("Pauziraj", (dialog, which) -> updateStatus(task, Task.Status.PAUSED));
                }
            } else if (task.getStatus() == Task.Status.PAUSED) {
                builder.setPositiveButton("Aktiviraj ponovo", (dialog, which) -> updateStatus(task, Task.Status.ACTIVE));
            }

            builder.setCancelable(true).show();
        });


        holder.layoutTasks.addView(tv);
    }

    // 🔹 pomoćna metoda za update statusa
    private void updateStatus(Task task, Task.Status newStatus) {
        Date today = new Date();

        // Provera da li može da se menja
        if (!task.canBeUpdated(today)) {
            Toast.makeText(context, "Zadatak se više ne može menjati.", Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
            return;
        }

        // Pravila:
        if (task.getStatus() == Task.Status.ACTIVE) {
            if (newStatus == Task.Status.COMPLETED ||
                    newStatus == Task.Status.CANCELED ||
                    newStatus == Task.Status.PAUSED) {
                task.setStatus(newStatus);
            } else {
                Toast.makeText(context, "Nedozvoljena akcija!", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if (task.getStatus() == Task.Status.PAUSED) {
            if (newStatus == Task.Status.ACTIVE) {
                task.setStatus(Task.Status.ACTIVE); // reaktivacija
            } else {
                Toast.makeText(context, "Možeš samo ponovo aktivirati ovaj zadatak.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(context, "Ovaj zadatak se ne može menjati.", Toast.LENGTH_SHORT).show();
            return;
        }

        // XP logika
        int xpReward = task.calculateXpReward();
        if (xpReward > 0) {
            // dodaćemo korisniku XP
            addXpToUser(xpReward);
        }

        // Upis u Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tasks").document(task.getId())
                .update("status", task.getStatus().toString())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Status updated: " + task.getStatus(), Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Greška pri ažuriranju statusa", Toast.LENGTH_SHORT).show());
    }
    private void addXpToUser(int xp) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid)
                .update("xp", com.google.firebase.firestore.FieldValue.increment(xp))
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "+ " + xp + " XP!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Greška pri dodavanju XP", Toast.LENGTH_SHORT).show()
                );
    }

}
