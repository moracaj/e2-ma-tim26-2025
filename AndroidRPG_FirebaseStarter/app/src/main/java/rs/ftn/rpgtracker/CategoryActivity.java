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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import rs.ftn.rpgtracker.adapter.CategoryAdapter;
import rs.ftn.rpgtracker.model.Category;

public class CategoryActivity extends AppCompatActivity{
    private ListView listView;
    private ArrayList<Category> categories;
    private ArrayAdapter<Category> adapter;
    private FirebaseFirestore db;
    private Button btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        db = FirebaseFirestore.getInstance();
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
        db.collection("categories")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for(QueryDocumentSnapshot d: task.getResult()) {
                                String id = d.getId();
                                String name = d.getString("name");
                                Long colorLong = d.getLong("color");
                                int color = (colorLong != null) ? colorLong.intValue() : 0;
                                Category category = new Category(id, name, color);
                                categories.add(category);
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(CategoryActivity.this, "Error getting categories", Toast.LENGTH_SHORT).show();
                        }
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
        db.collection("categories")
                .document(category.getId())
                .update("name", newName, "color", newColor)
                .addOnSuccessListener(aVoid -> {
                    // Update local list so UI reflects changes
                    category.setName(newName);
                    category.setColor(newColor);
                    categories.set(position, category);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                                Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}
