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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_category);

        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();

        name = findViewById(R.id.categoryName);

        mPickColorButton = findViewById(R.id.pick_color_button);
        mSetColorButton = findViewById(R.id.set_color_button);
        mbtnAdd = findViewById(R.id.btnAdd);

        mColorPreview = findViewById(R.id.preview_selected_color);
        mDefaultColor = 0;

        mPickColorButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v){
                        openColorPickerDialogue();
                    }
                });
        mSetColorButton.setOnClickListener(
                new View.OnClickListener(){
                    public void onClick(View v){
                        name.setTextColor(mDefaultColor);
                    }
                });

        mbtnAdd.setOnClickListener(view -> Create());
    }
    public void openColorPickerDialogue() {

        final AmbilWarnaDialog colorPickerDialogue = new AmbilWarnaDialog(this, mDefaultColor,
                new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // leave this function body as
                        // blank, as the dialog
                        // automatically closes when
                        // clicked on cancel button
                    }

                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        // change the mDefaultColor to
                        // change the GFG text color as
                        // it is returned when the OK
                        // button is clicked from the
                        // color picker dialog
                        mDefaultColor = color;

                        // now change the picked color
                        // preview box to mDefaultColor
                        mColorPreview.setBackgroundColor(mDefaultColor);
                    }
                });
        colorPickerDialogue.show();
    }

    private void Create(){
        String e=name.getText().toString().trim();
        if(TextUtils.isEmpty(e)){
            Toast.makeText(this,"All fields are required",Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> category = new HashMap<>();
        category.put("name", e);
        category.put("color", mDefaultColor);

        db.collection("categories")
                .add(category)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Category created successfully!", Toast.LENGTH_SHORT).show();
                    name.setText("");
                    mColorPreview.setBackgroundColor(0);
                    mDefaultColor = 0;
                })
                .addOnFailureListener(e1 -> {
                    Toast.makeText(this, "Error: " + e1.getMessage(), Toast.LENGTH_SHORT).show();
                });
        Intent intent = new Intent(this, CategoryActivity.class);
        startActivity(intent);

    }

}
