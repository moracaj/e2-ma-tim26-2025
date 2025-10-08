package rs.ftn.rpgtracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TaskActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Button btnAddTask;
    private Button btnCalendar;

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_task);
//
//        tabLayout = findViewById(R.id.tabLayout);
//        viewPager = findViewById(R.id.viewPager);
//
//        btnAddTask = findViewById(R.id.btnAddTask);
//        btnCalendar = findViewById(R.id.btnCalendar);
//
//        btnAddTask.setOnClickListener(v -> startActivity(new Intent(this, NewTaskActivity.class)));
//        btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
//
//        viewPager.setAdapter(new TaskPagerAdapter(this));
//
//        new TabLayoutMediator(tabLayout, viewPager,
//                (tab, position) -> {
//                    if (position == 0) {
//                        tab.setText("One time");
//                    } else {
//                        tab.setText("Recurring");
//                    }
//                }).attach();
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        btnAddTask = findViewById(R.id.btnAddTask);
        btnCalendar = findViewById(R.id.btnCalendar);

        btnAddTask.setOnClickListener(v -> startActivity(new Intent(this, NewTaskActivity.class)));
        btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));

        // >>> KLJUČNO: čekamo Auth pa tek onda kreiramo fragmente i tabove
        ensureAuthThen(() -> {
            viewPager.setAdapter(new TaskPagerAdapter(this));
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                tab.setText(position == 0 ? "One time" : "Recurring");
            }).attach();
        });
    }

    // Helper: osiguraj da smo ulogovani pre Firestore rada
    private void ensureAuthThen(Runnable then) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                    .addOnSuccessListener(r -> then.run())
                    .addOnFailureListener(e -> {
                        // ovde možeš prikazati poruku korisniku
                        e.printStackTrace();
                    });
        } else {
            then.run();
        }
    }


    //Metoda za dodavanje XP-a nakon uspešnog zavrsetka zadatka
//    public void addXpForTaskCompletion() {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
//            if (doc.exists()) {
//                long currentXp = doc.getLong("xp") != null ? doc.getLong("xp") : 0;
//                long nextLevelXp = doc.getLong("nextLevelXp") != null ? doc.getLong("nextLevelXp") : 100;
//                long level = doc.getLong("level") != null ? doc.getLong("level") : 1;
//
//                currentXp += 20; // npr. 20 XP po zadatku
//
//                if (currentXp >= nextLevelXp) {
//                    level++;
//                    currentXp = 0;
//                    db.collection("users").document(uid)
//                            .update("level", level,
//                                    "xp", currentXp,
//                                    "nextLevelXp", nextLevelXp * 1.5,
//                                    "canFightBoss", true);
//                } else {
//                    db.collection("users").document(uid)
//                            .update("xp", currentXp);
//                }
//            }
//        });
//    }

    public void addXpForTaskCompletion() {
        ensureAuthThen(() -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    long currentXp = doc.getLong("xp") != null ? doc.getLong("xp") : 0;
                    long nextLevelXp = doc.getLong("nextLevelXp") != null ? doc.getLong("nextLevelXp") : 100;
                    long level = doc.getLong("level") != null ? doc.getLong("level") : 1;

                    currentXp += 20; // primer

                    if (currentXp >= nextLevelXp) {
                        level++;
                        currentXp = 0;
                        db.collection("users").document(uid)
                                .update("level", level, "xp", currentXp, "nextLevelXp", (long)(nextLevelXp * 1.5), "canFightBoss", true);
                    } else {
                        db.collection("users").document(uid).update("xp", currentXp);
                    }
                }
            });
        });
    }


    private static class TaskPagerAdapter extends FragmentStateAdapter {
        public TaskPagerAdapter(@NonNull AppCompatActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return TaskListFragment.newInstance(false);
            } else {
                return TaskListFragment.newInstance(true);
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
