package rs.ftn.rpgtracker;

import android.content.Intent;
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

public class NewCategoryActivity extends AppCompatActivity {
    private EditText name;
    private Button mSetColorButton, mPickColorButton, mbtnAdd;
    private View mColorPreview;
    private int mDefaultColor;
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_category);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        name = findViewById(R.id.categoryName);

        mPickColorButton = findViewById(R.id.pick_color_button);
        mSetColorButton = findViewById(R.id.set_color_button);
        mbtnAdd = findViewById(R.id.btnAdd);

        mColorPreview = findViewById(R.id.preview_selected_color);
        mDefaultColor = 0;

        mPickColorButton.setOnClickListener(v -> openColorPickerDialogue());
        mSetColorButton.setOnClickListener(v -> name.setTextColor(mDefaultColor));

        mbtnAdd.setOnClickListener(view -> Create());
    }

    public void openColorPickerDialogue() {
        final AmbilWarnaDialog colorPickerDialogue = new AmbilWarnaDialog(this, mDefaultColor,
                new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // zatvara se automatski
                    }

                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mDefaultColor = color;
                        mColorPreview.setBackgroundColor(mDefaultColor);
                    }
                });
        colorPickerDialogue.show();
    }

    private void Create() {
        String e = name.getText().toString().trim();
        if (TextUtils.isEmpty(e)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // prvo proveri da li postoji kategorija sa istom bojom za istog korisnika
        db.collection("categories")
                .whereEqualTo("color", mDefaultColor)
                .whereEqualTo("userId", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            Toast.makeText(this, "Color already used in another category!", Toast.LENGTH_SHORT).show();
                        } else {
                            // boja je slobodna, možeš dodati novu kategoriju
                            Map<String, Object> category = new HashMap<>();
                            category.put("name", e);
                            category.put("color", mDefaultColor);
                            category.put("userId", uid); // 🔹 obavezno dodaj userId

                            db.collection("categories")
                                    .add(category)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "Category created successfully!", Toast.LENGTH_SHORT).show();
                                        name.setText("");
                                        mColorPreview.setBackgroundColor(0);
                                        mDefaultColor = 0;

                                        // vrati korisnika na listu kategorija
                                        Intent intent = new Intent(this, CategoryActivity.class);
                                        startActivity(intent);
                                    })
                                    .addOnFailureListener(e1 -> {
                                        Toast.makeText(this, "Error: " + e1.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Error checking color uniqueness", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
