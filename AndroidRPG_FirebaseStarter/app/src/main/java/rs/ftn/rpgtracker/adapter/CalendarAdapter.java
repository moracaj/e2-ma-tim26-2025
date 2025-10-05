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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rs.ftn.rpgtracker.R;
import rs.ftn.rpgtracker.model.Task;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private final Context context;
    private final List<Date> days;          // svi datumi u mesecu
    private final List<Task> tasks;         // svi taskovi iz baze

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
            holder.tvDayNumber.setText("");
            holder.layoutTasks.removeAllViews();
            return;
        }

        // dan u mesecu
        SimpleDateFormat sdfDay = new SimpleDateFormat("d", Locale.getDefault());
        holder.tvDayNumber.setText(sdfDay.format(date));

        // obriši stare taskove iz holdera
        holder.layoutTasks.removeAllViews();

        List<Task> tasksForDay = getTasksForDate(date);

        // prikaži max 3 taska, ostalo stavi u "+X"
        int maxVisible = 3;
        for (int i = 0; i < tasksForDay.size(); i++) {
            if (i < maxVisible) {
                addTaskView(holder, tasksForDay.get(i));
            } else if (i == maxVisible) {
                TextView more = new TextView(context);
                more.setText("+ " + (tasksForDay.size() - maxVisible) + " više...");
                more.setTextSize(11);
                more.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                holder.layoutTasks.addView(more);
                break;
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

    //filtriraj taskove za odabrani datum
    private List<Task> getTasksForDate(Date date) {
        List<Task> result = new ArrayList<>();

        for (Task task : tasks) {
            if (task.getStartDate() == null) continue;

            if (!task.isRecurring()) {
                // jednokratni
                if (isSameDay(task.getStartDate(), date)) {
                    result.add(task);
                }
            } else {
                // ponavljajući
                Date start = task.getStartDate();
                Date end = task.getEndDate();
                if (start != null && end != null && !date.before(start) && !date.after(end)) {
                    long diffDays = (date.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);

                    if (task.getRepeatUnit() == Task.RepeatUnit.DAY) {
                        if (diffDays % task.getRepeatInterval() == 0) {
                            result.add(task);
                        }
                    } else if (task.getRepeatUnit() == Task.RepeatUnit.WEEK) {
                        long diffWeeks = diffDays / 7;
                        if (diffWeeks % task.getRepeatInterval() == 0) {
                            result.add(task);
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(d1);
        c2.setTime(d2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    //dodaj task u dan
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

        tv.setOnClickListener(v -> showTaskDialog(task));
        holder.layoutTasks.addView(tv);
    }

    //klik na task → status dijalog
    private void showTaskDialog(Task task) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context)
                .setTitle(task.getName())
                .setMessage("Description: " + (task.getDescription() != null ? task.getDescription() : "Nema opisa") +
                        "\nTime: " + task.getExecutionTime() +
                        "\nStatus: " + task.getStatus())
                .setCancelable(true);

        if (task.getStatus() == Task.Status.ACTIVE) {
            builder.setPositiveButton("Completed", (d, w) -> updateStatus(task, Task.Status.COMPLETED));
            builder.setNegativeButton("Cancelled", (d, w) -> updateStatus(task, Task.Status.CANCELLED));
            if (task.isRecurring()) {
                builder.setNeutralButton("Paused", (d, w) -> updateStatus(task, Task.Status.PAUSED));
            }
        } else if (task.getStatus() == Task.Status.PAUSED) {
            builder.setPositiveButton("Activate", (d, w) -> updateStatus(task, Task.Status.ACTIVE));
        }

        builder.show();
    }

    //update statusa i XP
    private void updateStatus(Task task, Task.Status newStatus) {
        Date today = new Date();

        if (!task.canBeUpdated(today)) {
            Toast.makeText(context, "Task cannot be changed.", Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
            return;
        }

        if (task.getStatus() == Task.Status.ACTIVE) {
            if (newStatus == Task.Status.COMPLETED ||
                    newStatus == Task.Status.CANCELLED ||
                    newStatus == Task.Status.PAUSED) {
                task.setStatus(newStatus);
            }
        } else if (task.getStatus() == Task.Status.PAUSED && newStatus == Task.Status.ACTIVE) {
            task.setStatus(Task.Status.ACTIVE);
        }

        int xpReward = task.calculateXpReward();
        if (xpReward > 0) addXpToUser(xpReward);

        FirebaseFirestore.getInstance()
                .collection("tasks")
                .document(task.getId())
                .update("status", task.getStatus().toString())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Status → " + task.getStatus(), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error updating.", Toast.LENGTH_SHORT).show());

        notifyDataSetChanged();
    }

    private void addXpToUser(int xp) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("xp", com.google.firebase.firestore.FieldValue.increment(xp));
    }
}
