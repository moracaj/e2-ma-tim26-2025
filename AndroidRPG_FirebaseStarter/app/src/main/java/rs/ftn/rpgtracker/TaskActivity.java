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

public class TaskActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Button btnAddTask;
    private Button btnCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task); // koristi xml koji smo napravili

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        btnAddTask = findViewById(R.id.btnAddTask);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnAddTask.setOnClickListener(v -> startActivity(new Intent(this, NewTaskActivity.class)));
        btnCalendar.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));


        // Set adapter za ViewPager2
        viewPager.setAdapter(new TaskPagerAdapter(this));

        // Poveži TabLayout i ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Jednokratni");
                    } else {
                        tab.setText("Ponavljajući");
                    }
                }).attach();
    }

    // Adapter za ViewPager2
    private static class TaskPagerAdapter extends FragmentStateAdapter {

        public TaskPagerAdapter(@NonNull AppCompatActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return TaskListFragment.newInstance(false); // false = jednokratni
            } else {
                return TaskListFragment.newInstance(true);  // true = ponavljajući
            }
        }

        @Override
        public int getItemCount() {
            return 2; // dve stranice (jednokratni i ponavljajući)
        }
    }
}
