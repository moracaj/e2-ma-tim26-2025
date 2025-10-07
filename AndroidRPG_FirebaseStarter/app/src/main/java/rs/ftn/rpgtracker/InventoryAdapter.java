package rs.ftn.rpgtracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.VH> {

    interface ActionListener {
        void onActivatePotion(Row r);                // jednokratni
        void onConsumePermanentPotion(Row r);        // trajni
        void onToggleArmor(Row r, boolean activate);
        void onUpgradeWeapon(Row r);
    }

    static class Row {
        int id;
        String type;           // potion/armor/weapon
        String name;
        String bonusType;      // pp_percent, pp_perm_percent, hit_percent, extra_attack_chance, coin_perm_percent
        double bonusValue;
        int durationBattles;
        boolean permanent;
        int level;
        boolean equipped;
        Integer remainingBattles;
    }

    private final LayoutInflater inf;
    private final List<Row> data = new ArrayList<>();
    private final ActionListener listener;

    public InventoryAdapter(Context ctx, ActionListener l){
        this.inf = LayoutInflater.from(ctx);
        this.listener = l;
    }

    public void submit(List<Row> rows){
        data.clear(); data.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt){
        return new VH(inf.inflate(R.layout.item_inventory_equipment, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i){
        Row r = data.get(i);
        h.title.setText(r.name);
        h.subtitle.setText(desc(r));
        h.btn1.setVisibility(View.GONE);
        h.btn2.setVisibility(View.GONE);

        switch (r.type) {
            case "potion":
                if (r.permanent) {
                    h.btn1.setText("Consume");
                    h.btn1.setVisibility(View.VISIBLE);
                    h.btn1.setOnClickListener(v -> listener.onConsumePermanentPotion(r));
                } else {
                    h.btn1.setText(r.equipped ? "Equipped" : "Activate");
                    h.btn1.setEnabled(!r.equipped);
                    h.btn1.setVisibility(View.VISIBLE);
                    h.btn1.setOnClickListener(v -> listener.onActivatePotion(r));
                }
                break;

            case "armor":
                h.btn1.setText(r.equipped ? "Deactivate" : "Activate");
                h.btn1.setVisibility(View.VISIBLE);
                h.btn1.setOnClickListener(v -> listener.onToggleArmor(r, !r.equipped));
                break;

            case "weapon":
                h.btn1.setText("Upgrade");
                h.btn1.setVisibility(View.VISIBLE);
                h.btn1.setOnClickListener(v -> listener.onUpgradeWeapon(r));
                h.btn2.setText("Active");
                h.btn2.setEnabled(false);
                h.btn2.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override public int getItemCount(){ return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        Button btn1, btn2;
        VH(@NonNull View v){
            super(v);
            title = v.findViewById(R.id.title);
            subtitle = v.findViewById(R.id.subtitle);
            btn1 = v.findViewById(R.id.btn1);
            btn2 = v.findViewById(R.id.btn2);
        }
    }

    private String desc(Row r){
        StringBuilder sb = new StringBuilder();
        switch (r.bonusType) {
            case "pp_percent": sb.append(String.format("+%.0f%% PP (aktivno)", r.bonusValue)); break;
            case "pp_perm_percent": sb.append(String.format("+%.0f%% PP (trajno)", r.bonusValue)); break;
            case "hit_percent": sb.append(String.format("+%.0f%% HIT", r.bonusValue)); break;
            case "extra_attack_chance": sb.append(String.format("+%d%% EXTRA ATTACK", (int)r.bonusValue)); break;
            case "coin_perm_percent": sb.append(String.format("+%.0f%% COINS (trajno)", r.bonusValue)); break;
        }
        if ("armor".equals(r.type)) {
            sb.append(r.equipped ? String.format(" • %d borbi preostalo", r.remainingBattles==null?0:r.remainingBattles) : " • neaktivno");
        }
        if ("weapon".equals(r.type)) {
            sb.append(String.format(" • Lvl %d", r.level));
        }
        if ("potion".equals(r.type) && !r.permanent) {
            sb.append(r.equipped ? " • spreman za narednu borbu" : " • neaktivan");
        }
        return sb.toString();
    }
}
