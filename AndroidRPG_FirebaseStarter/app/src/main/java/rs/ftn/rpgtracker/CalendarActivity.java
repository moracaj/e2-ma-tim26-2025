package rs.ftn.rpgtracker;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
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
    private Button btnPrevMonth, btnNextMonth;

    private FirebaseFirestore db;
    private Calendar currentMonth;

    private final List<Date> daysInMonth = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private CalendarAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        tvMonthYear = findViewById(R.id.tvMonthYear);
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        db = FirebaseFirestore.getInstance();

        // start with current month
        currentMonth = Calendar.getInstance();

        adapter = new CalendarAdapter(this, daysInMonth, tasks);
        calendarRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
        calendarRecyclerView.setAdapter(adapter);

        // load first month
        generateDaysForMonth(currentMonth);
        loadTasksFromDb();

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            generateDaysForMonth(currentMonth);
            loadTasksFromDb();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            generateDaysForMonth(currentMonth);
            loadTasksFromDb();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 🔄 svaki put kad se vratiš na kalendar (npr. posle edit kategorije)
        loadTasksFromDb();
    }

    private void generateDaysForMonth(Calendar month) {
        daysInMonth.clear();

        // naslov meseca
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(month.getTime()));

        Calendar calendar = (Calendar) month.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (firstDayOfWeek < 0) firstDayOfWeek = 6;

        // dodaj prazne dane pre prvog dana meseca
        for (int i = 0; i < firstDayOfWeek; i++) {
            daysInMonth.add(null);
        }

        // dodaj sve dane meseca
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= maxDay; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            daysInMonth.add(calendar.getTime());
        }

        adapter.notifyDataSetChanged();
    }

    private void loadTasksFromDb() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("tasks")
                .whereEqualTo("userId", uid)   // 🔹 samo zadaci trenutnog korisnika
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("FirestoreLoad", "Documents fetched: " + querySnapshot.size());
                    tasks.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Task t = doc.toObject(Task.class);

                            // mapiraj kategoriju
                            if (doc.contains("category")) {
                                Map<String, Object> categoryMap = (Map<String, Object>) doc.get("category");
                                if (categoryMap != null) {
                                    String catId = (String) categoryMap.get("id");
                                    String name = (String) categoryMap.get("name");
                                    Long colorLong = (Long) categoryMap.get("color");
                                    int color = (colorLong != null) ? colorLong.intValue() : 0xFF9E9E9E;

                                    Category category = new Category(catId, name, color, uid);
                                    t.setCategory(category);
                                }
                            }

                            tasks.add(t);
                        } catch (Exception e) {
                            Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreLoad", "Failed to load tasks", e);
                    Toast.makeText(this, "Loading error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
