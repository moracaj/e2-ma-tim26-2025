package rs.ftn.rpgtracker.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import rs.ftn.rpgtracker.R;
import rs.ftn.rpgtracker.model.Category;

public class CategoryAdapter extends ArrayAdapter<Category> {

    public CategoryAdapter(Context context, List<Category> data) {
        super(context, 0, data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Category item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_category, parent, false);
        }

        View viewColor = convertView.findViewById(R.id.viewColor);
        TextView tvName = convertView.findViewById(R.id.tvCategoryName);

        // Name
        tvName.setText(item != null ? item.getName() : "");

        // Color swatch as a nice rounded pill
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(999f);
        int color = (item != null) ? item.getColor() : 0xFF9E9E9E; // default gray
        bg.setColor(color);
        viewColor.setBackground(bg);

        return convertView;
    }
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }
    private View createView(int position, View convertView, ViewGroup parent) {
        Category item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_category, parent, false);
        }

        View viewColor = convertView.findViewById(R.id.viewColor);
        TextView tvName = convertView.findViewById(R.id.tvCategoryName);

        // Postavi ime
        tvName.setText(item != null ? item.getName() : "");

        // Postavi boju kao obojeni krug
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(999f); // da izgleda kao krug
        int color = (item != null) ? item.getColor() : 0xFF9E9E9E; // default gray
        bg.setColor(color);
        viewColor.setBackground(bg);

        return convertView;
    }

}