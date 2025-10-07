package rs.ftn.rpgtracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import yuku.ambilwarna.AmbilWarnaDialog;

public class EditCategoryActivity extends AppCompatActivity {

    private EditText name;
    private Button btnPickColor, btnSetColor, btnSave;
    private View colorPreview;

    private String docId;
    private int selectedColor;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_category); // 👈 reuse the SAME layout

        db = FirebaseFirestore.getInstance();

        // Get data from intent
        docId = getIntent().getStringExtra("id");
        String startName = getIntent().getStringExtra("name");
        int startColor = getIntent().getIntExtra("color", 0);

        // Bind views (same ids as NewCategoryActivity)
        name          = findViewById(R.id.categoryName);
        btnPickColor  = findViewById(R.id.pick_color_button);
        btnSetColor   = findViewById(R.id.set_color_button);
        btnSave       = findViewById(R.id.btnAdd);                 // reuse "Create" button as "Save"
        colorPreview  = findViewById(R.id.preview_selected_color);

        // Prefill values
        name.setText(startName);
        selectedColor = startColor;
        colorPreview.setBackgroundColor(selectedColor);
        btnSave.setText("Save");                                   // change label

        // Color picker (AmbilWarna) — same behavior as Add
        btnPickColor.setOnClickListener(v -> openColorPicker());

        // Optional cosmetic action (keep same as your Add): set text color to chosen one
        btnSetColor.setOnClickListener(v -> name.setTextColor(selectedColor));

        // Save → update Firestore doc
        btnSave.setOnClickListener(v -> saveEdit());
    }

    private void openColorPicker() {
        new AmbilWarnaDialog(this, selectedColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override public void onCancel(AmbilWarnaDialog dialog) {}

            @Override public void onOk(AmbilWarnaDialog dialog, int color) {
                selectedColor = color;
                colorPreview.setBackgroundColor(selectedColor); // live preview
            }
        }).show();
    }

    private void saveEdit() {
        String newName = name.getText().toString().trim();
        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (docId == null || docId.isEmpty()) {
            Toast.makeText(this, "Missing document id", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) Update kategorije
        db.collection("categories")
                .document(docId)
                .update("name", newName, "color", selectedColor)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show();

                    // 2) Pronađi sve taskove koji koriste ovu kategoriju
                    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                            : null;

                    if (uid != null) {
                        db.collection("tasks")
                                .whereEqualTo("userId", uid)
                                .whereEqualTo("category.id", docId) // filtriraj taskove vezane za kategoriju
                                .get()
                                .addOnSuccessListener(snap -> {
                                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap) {
                                        // 3) Kreiraj novu mapu kategorije
                                        Map<String, Object> updatedCategory = new HashMap<>();
                                        updatedCategory.put("id", docId);
                                        updatedCategory.put("name", newName);
                                        updatedCategory.put("color", selectedColor);
                                        updatedCategory.put("userId", uid);

                                        // 4) Pregazi staru kategoriju u tasku
                                        doc.getReference().update("category", updatedCategory);
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Task update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }

                    finish(); // vrati se nazad, CategoryActivity.onStart() će ponovo učitati liste
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}