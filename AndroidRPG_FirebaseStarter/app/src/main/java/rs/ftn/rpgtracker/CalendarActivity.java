package rs.ftn.rpgtracker;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import rs.ftn.rpgtracker.adapter.CalendarAdapter;
import rs.ftn.rpgtracker.model.Category;
import rs.ftn.rpgtracker.model.Task;

public class CalendarActivity extends AppCompatActivity {

    private RecyclerView calendarRecyclerView;
    private TextView tvMonthYear;
    private FirebaseFirestore db;

    private final List<Date> daysInMonth = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private CalendarAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        tvMonthYear = findViewById(R.id.tvMonthYear);
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);

        db = FirebaseFirestore.getInstance();

        // 1. Generiši dane tekućeg meseca
        generateDaysForCurrentMonth();

        // 2. Podesi RecyclerView
        adapter = new CalendarAdapter(this, daysInMonth, tasks);
        calendarRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
        calendarRecyclerView.setAdapter(adapter);

        // 3. Učitaj zadatke iz Firestore
        loadTasksFromDb();
    }

    private void generateDaysForCurrentMonth() {
        daysInMonth.clear();
        Calendar calendar = Calendar.getInstance();

        // naslov meseca
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(calendar.getTime()));

        // postavi na prvi dan meseca
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // da bi offset bio 0-based
        if (firstDayOfWeek < 0) firstDayOfWeek = 6;

        // dodaj prazne dane pre prvog dana u mesecu
        for (int i = 0; i < firstDayOfWeek; i++) {
            daysInMonth.add(null); // null znači prazan slot
        }

        // dodaj sve dane u mesecu
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= maxDay; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            daysInMonth.add(calendar.getTime());
        }
    }

    private void loadTasksFromDb() {
        db.collection("tasks")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("FirestoreLoad", "Documents fetched: " + querySnapshot.size());
                    tasks.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Task t = doc.toObject(Task.class);
                            if (doc.contains("category")) {
                                Map<String, Object> categoryMap = (Map<String, Object>) doc.get("category");
                                if (categoryMap != null) {
                                    String name = (String) categoryMap.get("name");
                                    Long colorLong = (Long) categoryMap.get("color");
                                    int color = (colorLong != null) ? colorLong.intValue() : 0xFF9E9E9E;
                                    t.setCategory(new Category(doc.getId(), name, color));
                                }
                            }

                            tasks.add(t);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Greška pri parsiranju zadatka", Toast.LENGTH_SHORT).show();
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreLoad", "Failed to load tasks", e);
                    Toast.makeText(this, "Greška pri učitavanju zadataka: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

    }
}
