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

        // uvek očisti prethodne task-ove
        holder.layoutTasks.removeAllViews();

        // ako je prazan slot (za poravnanje kalendara) → prikaži prazno i izađi
        if (date == null) {
            holder.tvDayNumber.setText("");
            return;
        }

        // 1) dan u mesecu
        SimpleDateFormat sdfDay = new SimpleDateFormat("d", Locale.getDefault());
        holder.tvDayNumber.setText(sdfDay.format(date));

        // 2) poređenje datuma
        SimpleDateFormat sdfCompare = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String currentDateStr = sdfCompare.format(date);

        for (Task task : tasks) {
            Date taskStart = task.getStartDate();
            if (taskStart == null) continue; // zaštita

            String taskDateStr = sdfCompare.format(taskStart);
            if (currentDateStr.equals(taskDateStr)) {
                TextView tv = new TextView(context);
                tv.setText(task.getName());
                tv.setTextSize(12);
                tv.setPadding(4, 2, 4, 2);
                tv.setTextColor(context.getResources().getColor(android.R.color.white));

                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(12f);
                int color = (task.getCategory() != null) ? task.getCategory().getColor() : 0xFF9E9E9E;
                bg.setColor(color);
                tv.setBackground(bg);

                tv.setOnClickListener(v -> {
                    new android.app.AlertDialog.Builder(context)
                            .setTitle(task.getName())
                            .setMessage("Opis: " + (task.getDescription() != null ? task.getDescription() : "Nema opisa") +
                                    "\nVreme: " + task.getExecutionTime() +
                                    "\nStatus: " + task.getStatus())
                            .setPositiveButton("Urađeno", (d, w) -> updateStatus(task, Task.Status.COMPLETED))
                            .setNegativeButton("Otkazano", (d, w) -> updateStatus(task, Task.Status.CANCELED))
                            .setNeutralButton("Pauzirano", (d, w) -> updateStatus(task, Task.Status.PAUSED))
                            .show();
                });

                holder.layoutTasks.addView(tv);
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

    // 🔹 pomoćna metoda za update statusa
    private void updateStatus(Task task, Task.Status newStatus) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tasks").document(task.getId())
                .update("status", newStatus.toString())
                .addOnSuccessListener(aVoid -> {
                    task.setStatus(newStatus);
                    Toast.makeText(context, "Status updated: " + newStatus, Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Greška pri ažuriranju statusa", Toast.LENGTH_SHORT).show());
    }
}
