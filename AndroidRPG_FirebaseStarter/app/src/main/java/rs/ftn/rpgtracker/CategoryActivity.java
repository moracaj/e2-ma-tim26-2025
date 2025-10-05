package rs.ftn.rpgtracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import rs.ftn.rpgtracker.adapter.CategoryAdapter;
import rs.ftn.rpgtracker.model.Category;

public class CategoryActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayList<Category> categories;
    private ArrayAdapter<Category> adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Button btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        categories = new ArrayList<>();

        listView = findViewById(R.id.listCategory);
        adapter = new CategoryAdapter(this, categories);
        listView.setAdapter(adapter);
        btnAdd = findViewById(R.id.btnAdd);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Category c = categories.get(position);
            Intent i = new Intent(CategoryActivity.this, EditCategoryActivity.class);
            i.putExtra("id", c.getId());
            i.putExtra("name", c.getName());
            i.putExtra("color", c.getColor());
            startActivity(i);
        });

        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, NewCategoryActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        categories.clear();
        getCategories();
    }

    public void getCategories() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("categories")
                .whereEqualTo("userId", uid)  // 🔹 samo kategorije trenutnog korisnika
                .get()
                .addOnCompleteListener((@NonNull Task<QuerySnapshot> task) -> {
                    if(task.isSuccessful()){
                        for(QueryDocumentSnapshot d: task.getResult()) {
                            String id = d.getId();
                            String name = d.getString("name");
                            Long colorLong = d.getLong("color");
                            int color = (colorLong != null) ? colorLong.intValue() : 0;
                            String userId = d.getString("userId");

                            Category category = new Category(id, name, color, userId);
                            categories.add(category);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(CategoryActivity.this, "Error getting categories", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditDialog(Category category, int position) {
        EditText etName = new EditText(this);
        etName.setText(category.getName());

        EditText etColor = new EditText(this);
        etColor.setHint("Color (int or #RRGGBB)");
        etColor.setText(String.valueOf(category.getColor()));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        container.addView(etName);
        container.addView(etColor);

        new AlertDialog.Builder(this)
                .setTitle("Edit Category")
                .setView(container)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = etName.getText().toString().trim();
                    String colorText = etColor.getText().toString().trim();

                    int newColor = category.getColor();
                    try {
                        if (colorText.startsWith("#")) {
                            newColor = Color.parseColor(colorText);
                        } else if (!colorText.isEmpty()) {
                            newColor = Integer.parseInt(colorText);
                        }
                    } catch (Exception ignored) {}

                    updateCategory(category, position, newName, newColor);
                }).setNegativeButton("Cancel", null).show();
    }

    private void updateCategory(Category category, int position, String newName, int newColor) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("categories")
                .document(category.getId())
                .update(
                        "name", newName,
                        "color", newColor,
                        "userId", uid
                )
                .addOnSuccessListener(aVoid -> {
                    // 1) Lokalno osveži listu
                    category.setName(newName);
                    category.setColor(newColor);
                    category.setUserId(uid);
                    categories.set(position, category);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();

                    // 2) *** PROPAGIRAJ PROMENU U TASKOVE ***
                    db.collection("tasks")
                            .whereEqualTo("userId", uid)
                            .whereEqualTo("category.userId", uid)
                            .get()
                            .addOnSuccessListener(snap -> {
                                if (!snap.isEmpty()) {
                                    // pripremi novu mapu kategorije
                                    Map<String, Object> updatedCategory = new HashMap<>();
                                    updatedCategory.put("id", category.getId());
                                    updatedCategory.put("name", newName);
                                    updatedCategory.put("color", newColor);
                                    updatedCategory.put("userId", uid);

                                    // batch update
                                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                                    for (QueryDocumentSnapshot doc : snap) {
                                        batch.update(doc.getReference(), "category", updatedCategory);
                                    }

                                    batch.commit()
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(this, "Category propagated to tasks!", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Batch update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Greška pri čitanju taskova: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


}
