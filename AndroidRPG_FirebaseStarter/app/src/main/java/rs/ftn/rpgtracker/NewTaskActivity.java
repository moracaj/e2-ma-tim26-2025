package rs.ftn.rpgtracker;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rs.ftn.rpgtracker.adapter.CategoryAdapter;
import rs.ftn.rpgtracker.model.Category;
import rs.ftn.rpgtracker.model.Task;

public class NewTaskActivity extends AppCompatActivity {

    private Spinner spinnerCategory, spinnerDifficulty, spinnerImportance, spinnerRepeatUnit;
    private EditText taskName, description, etRepeatInterval;
    private Button btnExecutionTime, btnDateRange, btnSaveTask;
    private RadioButton rbRecurring;
    private RadioGroup rgFrequency;
    private LinearLayout layoutRecurring;

    private FirebaseFirestore db;
    private List<Category> categories = new ArrayList<>();

    private String startDateStr, endDateStr, taskId;
    private Task currentTask;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        spinnerImportance = findViewById(R.id.spinnerImportance);
        taskName = findViewById(R.id.taskName);
        description = findViewById(R.id.description);
        btnExecutionTime = findViewById(R.id.btnExecutionTime);
        btnDateRange = findViewById(R.id.btnDateRange);
        rbRecurring = findViewById(R.id.rbRecurring);
        rgFrequency = findViewById(R.id.rgFrequency);
        layoutRecurring = findViewById(R.id.layoutRecurring);
        etRepeatInterval = findViewById(R.id.etRepeatInterval);
        spinnerRepeatUnit = findViewById(R.id.spinnerRepeatUnit);
        btnSaveTask = findViewById(R.id.btnSaveTask);

        db = FirebaseFirestore.getInstance();

        // Adapteri
        ArrayAdapter<Task.Difficulty> diffAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Task.Difficulty.values());
        diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(diffAdapter);

        ArrayAdapter<Task.Importance> impAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Task.Importance.values());
        impAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImportance.setAdapter(impAdapter);

        ArrayAdapter<CharSequence> repeatUnitAdapter =
                ArrayAdapter.createFromResource(this, R.array.repeat_units, android.R.layout.simple_spinner_item);
        repeatUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeatUnit.setAdapter(repeatUnitAdapter);

        loadCategoriesFromDb();
        setExecutionTime();
        setFrequencyListener();
        setDateRange();

        taskId = getIntent().getStringExtra("taskId");
        if (taskId != null) {
            loadTaskDataForEdit();
        }

        btnSaveTask.setOnClickListener(v -> saveOrUpdateTask());
    }

    private void setDateRange() {
        btnDateRange.setOnClickListener(v -> {
            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            constraintsBuilder.setValidator(DateValidatorPointForward.now());

            MaterialDatePicker.Builder<Pair<Long, Long>> builder =
                    MaterialDatePicker.Builder.dateRangePicker()
                            .setTitleText("Select start and end date")
                            .setCalendarConstraints(constraintsBuilder.build());

            MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
            picker.show(getSupportFragmentManager(), picker.toString());

            picker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    Long startMillis = selection.first;
                    Long endMillis = selection.second;
                    startDateStr = sdf.format(new Date(startMillis));
                    endDateStr = sdf.format(new Date(endMillis));
                    btnDateRange.setText(startDateStr + " - " + endDateStr);
                }
            });
        });
    }

    private void setFrequencyListener() {
        rgFrequency.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRecurring) {
                layoutRecurring.setVisibility(View.VISIBLE);
            } else {
                layoutRecurring.setVisibility(View.GONE);
            }
        });
    }

    private void setExecutionTime() {
        btnExecutionTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            new TimePickerDialog(
                    NewTaskActivity.this,
                    (view, h, m) -> {
                        String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                        btnExecutionTime.setText(time);
                    },
                    hour, minute, true
            ).show();
        });
    }

    private void loadCategoriesFromDb() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("categories")
                .whereEqualTo("userId", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        categories.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String id = doc.getId();
                            String name = doc.getString("name");
                            Long colorLong = doc.getLong("color");
                            int color = (colorLong != null) ? colorLong.intValue() : 0;
                            String ownerId = doc.getString("userId");
                            categories.add(new Category(id, name, color, ownerId));
                        }
                        spinnerCategory.setAdapter(new CategoryAdapter(this, categories));
                    }
                });
    }

    private void loadTaskDataForEdit() {
        db.collection("tasks").document(taskId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentTask = doc.toObject(Task.class);
                        if (currentTask != null) {
                            taskName.setText(currentTask.getName());
                            description.setText(currentTask.getDescription());
                            btnExecutionTime.setText(currentTask.getExecutionTime());
                            spinnerDifficulty.setSelection(currentTask.getDifficulty().ordinal());
                            spinnerImportance.setSelection(currentTask.getImportance().ordinal());

                            if (currentTask.isRecurring()) {
                                rbRecurring.setChecked(true);
                                layoutRecurring.setVisibility(View.VISIBLE);
                                etRepeatInterval.setText(String.valueOf(currentTask.getRepeatInterval()));
                                spinnerRepeatUnit.setSelection(
                                        currentTask.getRepeatUnit() == Task.RepeatUnit.DAY ? 0 : 1
                                );
                                startDateStr = sdf.format(currentTask.getStartDate());
                                endDateStr = sdf.format(currentTask.getEndDate());
                                btnDateRange.setText(startDateStr + " - " + endDateStr);
                            }

                            if (currentTask.getStatus() == Task.Status.COMPLETED ||
                                    !currentTask.canBeUpdated(new Date())) {
                                disableEditing();
                            }
                        }
                    }
                });
    }

    private void disableEditing() {
        taskName.setEnabled(false);
        description.setEnabled(false);
        btnExecutionTime.setEnabled(false);
        spinnerDifficulty.setEnabled(false);
        spinnerImportance.setEnabled(false);
        btnDateRange.setEnabled(false);
        btnSaveTask.setEnabled(false);
        etRepeatInterval.setEnabled(false);
        spinnerRepeatUnit.setEnabled(false);
    }

    private void saveOrUpdateTask() {
        String name = taskName.getText().toString().trim();
        String desc = description.getText().toString().trim();
        Category selectedCategory = getSelectedCategory();
        Task.Difficulty selectedDifficulty = (Task.Difficulty) spinnerDifficulty.getSelectedItem();
        Task.Importance selectedImportance = (Task.Importance) spinnerImportance.getSelectedItem();
        String executionTime = btnExecutionTime.getText().toString();
        boolean isRecurring = rbRecurring.isChecked();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Date startDate = null, endDate = null;
        int repeatInterval = 0;
        Task.RepeatUnit repeatUnit = null;

        if (isRecurring) {
            try {
                startDate = sdf.parse(startDateStr);
                endDate = sdf.parse(endDateStr);
            } catch (Exception e) {
                Toast.makeText(this, "Select valid start and end dates", Toast.LENGTH_SHORT).show();
                return;
            }

            String intervalStr = etRepeatInterval.getText().toString();
            repeatInterval = TextUtils.isEmpty(intervalStr) ? 1 : Integer.parseInt(intervalStr);
            String unitStr = spinnerRepeatUnit.getSelectedItem().toString();
            repeatUnit = unitStr.equals("Day") ? Task.RepeatUnit.DAY : Task.RepeatUnit.WEEK;
        } else {
            // danasnji datum za jednokratne
            Date today = new Date();
            startDate = today;
            endDate = today;
        }

        if (taskId == null) {
            Task task = new Task(
                    name, desc, selectedCategory, isRecurring,
                    startDate, endDate, repeatInterval, repeatUnit,
                    executionTime, selectedDifficulty, selectedImportance, uid
            );

            db.collection("tasks").document(task.getId())
                    .set(task)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Task created!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, TaskActivity.class));
                        finish();
                    });
        } else {
            db.collection("tasks").document(taskId)
                    .update(
                            "name", name,
                            "description", desc,
                            "executionTime", executionTime,
                            "difficulty", selectedDifficulty,
                            "importance", selectedImportance,
                            "isRecurring", isRecurring,
                            "startDate", startDate,
                            "endDate", endDate,
                            "repeatInterval", repeatInterval,
                            "repeatUnit", repeatUnit != null ? repeatUnit.toString() : null
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }
    }

    private Category getSelectedCategory() {
        int pos = spinnerCategory.getSelectedItemPosition();
        return (pos >= 0 && pos < categories.size()) ? categories.get(pos) : null;
    }
}
