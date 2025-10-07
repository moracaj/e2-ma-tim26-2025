package rs.ftn.rpgtracker;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SummaryView extends LinearLayout {
    private TextView tv;

    public SummaryView(Context c, AttributeSet a){ super(c,a); init(c); }
    public SummaryView(Context c){ super(c); init(c); }

    void init(Context c){
        inflate(c, R.layout.view_equipment_summary, this);
        tv = findViewById(R.id.tvSummary);
    }

    public void bind(double ppActive, double hitActive, int extraAttackChance, double ppPerm, double coinPerm){
        String s = ""
                + "Aktivni (sledeća borba):  PP +" + (int)ppActive + "%,  HIT +" + (int)hitActive + "%,  EXTRA ATTACK +" + extraAttackChance + "%\n"
                + "Trajno:  PP +" + (int)ppPerm + "%,  COINS +" + (int)coinPerm + "%";
        tv.setText(s);
    }
}
