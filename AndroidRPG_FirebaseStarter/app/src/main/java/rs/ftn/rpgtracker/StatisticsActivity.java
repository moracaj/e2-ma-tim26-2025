package rs.ftn.rpgtracker;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import rs.ftn.rpgtracker.model.Task;

public class StatisticsActivity extends AppCompatActivity {

    private PieChart statusDonut;
    private BarChart categoryBar;
    private LineChart avgDifficulty;
    private LineChart xpLast7;
    private TextView tvActiveDays, tvCurrentStreak, tvBestStreak, tvSpecialMissions;

    private FirebaseFirestore db;
    private String uid;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        statusDonut = findViewById(R.id.chartStatusDonut);
        categoryBar = findViewById(R.id.chartCategoryBar);
        avgDifficulty = findViewById(R.id.chartAvgDifficulty);
        xpLast7      = findViewById(R.id.chartXpLast7);
        tvActiveDays = findViewById(R.id.tvActiveDays);
        tvCurrentStreak = findViewById(R.id.tvCurrentStreak);
        tvBestStreak    = findViewById(R.id.tvBestStreak);
        tvSpecialMissions = findViewById(R.id.tvSpecialMissions);

        db = FirebaseFirestore.getInstance();
        uid = Prefs.getUid(this);

        loadStats();
    }

    private void loadStats() {
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "Nema ulogovanog korisnika.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db.collection("tasks")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(q -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot d : q) {
                        try {
                            Task t = d.toObject(Task.class);
                            tasks.add(t);
                        } catch (Exception ignored) {}
                    }
                    renderStats(tasks);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška pri učitavanju: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // prikaži makar hardkod
                    renderStats(Collections.emptyList());
                });
    }

    private void renderStats(List<Task> tasks) {
        if (tasks == null) tasks = Collections.emptyList();

        // ---------- 1) Brojevi po statusu (donut) ----------
        int total=0, completed=0, notCompleted=0, cancelled=0;
        Map<String,Integer> finishedPerCategory = new LinkedHashMap<>();

        // za avg teškoću i XP/7 dana
        Map<String, List<Integer>> difficultyByDay = new TreeMap<>();
        Map<String, Integer> xpByDay = new TreeMap<>();

        // pomoćno: dan -> info o „zakazano vs urađeno/neud.“
        Map<String, DayBucket> dayBuckets = new TreeMap<>();

        Calendar cal = Calendar.getInstance();
        String todayKey = dayKey(new Date());

        for (Task t : tasks) {
            total++;

            // statusi
            Task.Status s = getStatusSafe(t);
            if (s == Task.Status.COMPLETED) completed++;
            else if (s == Task.Status.NOT_COMPLETED) notCompleted++;
            else if (s == Task.Status.CANCELLED) cancelled++;

            // kategorije (samo završeni)
            if (s == Task.Status.COMPLETED) {
                String cat = (t.getCategory() != null && t.getCategory().getName()!=null)
                        ? t.getCategory().getName() : "Other";
                finishedPerCategory.put(cat, finishedPerCategory.getOrDefault(cat, 0)+1);
            }

            // mapiranje po danima u periodu taska (da ispoštujemo „ponavljajući”)
            List<Date> days = expandTaskDays(t);
            int diffValue = difficultyValue(t); // 1,3,7,20 (po tvojoj enum mapi)
            int xpValue   = xpValueSafe(t);     // totalXP ili izračunato

            for (Date d : days) {
                String key = dayKey(d);

                // pros., težina po danu (samo završen)
                if (s == Task.Status.COMPLETED) {
                    difficultyByDay.computeIfAbsent(key, kk -> new ArrayList<>()).add(diffValue);
                    xpByDay.put(key, xpByDay.getOrDefault(key, 0) + xpValue);
                }

                // za nizove: evidentiraj da je taj dan imao zakazan zadatak
                dayBuckets.computeIfAbsent(key, kk -> new DayBucket()).scheduled++;

                if (s == Task.Status.COMPLETED) {
                    dayBuckets.get(key).completed++;
                } else if (s == Task.Status.NOT_COMPLETED) {
                    dayBuckets.get(key).failed++;
                }
            }
        }

        // fallback / hardkod ako nema podataka
        if (total == 0) {
            total = 9; completed = 5; notCompleted = 3; cancelled = 1;
            finishedPerCategory.put("Zdravlje", 2);
            finishedPerCategory.put("Učenje", 3);
            finishedPerCategory.put("Zabava", 0);

            // 7 dana unazad „lažni“ XP i težina
            for (int i=6;i>=0;i--) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, -i);
                String k = dayKey(c.getTime());
                xpByDay.put(k, (i%2==0)? 20 : 8);
                difficultyByDay.put(k, Arrays.asList((i%3==0)?7:3));
                DayBucket b = new DayBucket();
                b.scheduled = 1; b.completed = (i%4==0) ? 0 : 1; b.failed = (i%5==0) ? 1 : 0;
                dayBuckets.put(k, b);
            }
        }

        // ---------- 2) Aktivni dani / nizovi ----------
        Streaks streaks = computeStreaks(dayBuckets);
        tvActiveDays.setText("Aktivni dani: " + streaks.activeDays);
        tvCurrentStreak.setText("Trenutni niz: " + streaks.currentStreak + " dana");
        tvBestStreak.setText("Najduži niz: " + streaks.bestStreak + " dana");

        // ---------- 3) Donut statusa ----------
        List<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(completed, "Urađeni"));
        pieEntries.add(new PieEntry(notCompleted, "Neurađeni"));
        pieEntries.add(new PieEntry(cancelled, "Otkazani"));
        pieEntries.add(new PieEntry(Math.max(0, total - (completed+notCompleted+cancelled)), "Aktivni"));
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setDrawValues(true);
        PieData pieData = new PieData(pieDataSet);
        statusDonut.setCenterText("Zadaci");
        statusDonut.setData(pieData);
        statusDonut.getDescription().setEnabled(false);
        statusDonut.setUsePercentValues(true);
        statusDonut.setEntryLabelTextSize(12f);
        statusDonut.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        statusDonut.invalidate();

        // ---------- 4) Bar: završeni po kategoriji ----------
        List<BarEntry> barEntries = new ArrayList<>();
        List<String> catLabels = new ArrayList<>();
        int idx=0;
        for (Map.Entry<String,Integer> e : finishedPerCategory.entrySet()){
            barEntries.add(new BarEntry(idx, e.getValue()));
            catLabels.add(e.getKey());
            idx++;
        }
        if (barEntries.isEmpty()){
            barEntries.add(new BarEntry(0, 0));
            catLabels.add("—");
        }
        BarDataSet barDataSet = new BarDataSet(barEntries, "Završeno");
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.7f);
        categoryBar.setData(barData);
        categoryBar.getXAxis().setValueFormatter(new IndexAxisValueFormatter(catLabels));
        categoryBar.getXAxis().setGranularity(1f);
        categoryBar.getXAxis().setGranularityEnabled(true);
        categoryBar.getDescription().setEnabled(false);
        categoryBar.getLegend().setEnabled(false);
        categoryBar.invalidate();

        // ---------- 5) Line: prosečna težina po danu ----------
        List<Entry> diffEntries = new ArrayList<>();
        List<String> diffLabels = new ArrayList<>();
        int i=0;
        for (Map.Entry<String, List<Integer>> e : difficultyByDay.entrySet()){
            double avg = e.getValue().stream().mapToInt(v->v).average().orElse(0);
            diffEntries.add(new Entry(i, (float)avg));
            diffLabels.add(shortLabel(e.getKey()));
            i++;
        }
        if (diffEntries.isEmpty()){
            diffEntries.add(new Entry(0, 0));
            diffLabels.add("—");
        }
        LineDataSet diffSet = new LineDataSet(diffEntries, "Prosečna težina");
        LineData diffData = new LineData(diffSet);
        avgDifficulty.setData(diffData);
        avgDifficulty.getXAxis().setValueFormatter(new IndexAxisValueFormatter(diffLabels));
        avgDifficulty.getDescription().setEnabled(false);
        avgDifficulty.invalidate();

        // ---------- 6) Line: Kumulativni XP u poslednjih 7 dana ----------
        Map<String,Integer> last7 = ensureLast7Days(xpByDay); // i dalje dnevni XP, ali ćemo ga pretvoriti u kumulativ
        List<Entry> xpEntries = new ArrayList<>();
        List<String> xpLabels = new ArrayList<>();

        int j = 0;
        int cum = 0; // kumulativni XP
        for (Map.Entry<String,Integer> e : last7.entrySet()){
            int daily = Math.max(0, e.getValue()); // za svaki slučaj, bez negativnih
            cum += daily;                           // saberi do tog dana
            xpEntries.add(new Entry(j, cum));       // crtamo kumulativ
            xpLabels.add(shortLabel(e.getKey()));
            j++;
        }

        LineDataSet xpSet = new LineDataSet(xpEntries, "Kumulativni XP"); // (opciono) promenjen label
        LineData xpData = new LineData(xpSet);
        xpLast7.setData(xpData);
        xpLast7.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xpLabels));
        xpLast7.getDescription().setEnabled(false);
        xpLast7.invalidate();


        // ---------- 7) Specijalne misije (placeholder ako nema kolekcije) ----------
        // Ako već imaš npr. users/{uid}/specialMissions sa poljima status, ovde izbroj.
        // U suprotnom – prikaži primerne vrednosti:
        int started = 1;
        int finished = 0;
        tvSpecialMissions.setText("Specijalne misije: započete " + started + " / završene " + finished);
    }

    // ============== POMOĆNE ==============

    private static class DayBucket {
        int scheduled = 0;  // koliko je zadataka planirano taj dan
        int completed = 0;  // koliko je završeno taj dan
        int failed    = 0;  // koliko je NOT_COMPLETED taj dan
    }
    private static class Streaks {
        int activeDays, currentStreak, bestStreak;
    }

    private Streaks computeStreaks(Map<String, DayBucket> days){
        Streaks s = new Streaks();
        s.activeDays = 0;
        s.currentStreak = 0;
        s.bestStreak = 0;

        // sortiraj po datumu
        List<String> keys = new ArrayList<>(days.keySet());
        Collections.sort(keys);

        int run = 0;
        for (String k : keys){
            DayBucket b = days.get(k);
            boolean hadTask = b.scheduled > 0;
            boolean broke = hadTask && b.failed > 0 && b.completed == 0;
            boolean okDay = b.completed > 0 || !hadTask; // niz se NE prekida ako nema zadataka

            if (b.completed > 0) s.activeDays++;

            if (okDay) {
                run++;
                s.bestStreak = Math.max(s.bestStreak, run);
            } else {
                run = 0;
            }
        }

        // trenutni niz: broj od danas unazad dok se ne prekine
        List<String> rev = new ArrayList<>(keys);
        Collections.reverse(rev);
        int cur=0;
        for (String k : rev){
            DayBucket b = days.get(k);
            boolean hadTask = b.scheduled > 0;
            boolean broke = hadTask && b.failed > 0 && b.completed == 0;
            boolean okDay = b.completed > 0 || !hadTask;
            if (okDay) cur++;
            else break;
        }
        s.currentStreak = cur;
        return s;
    }

    private Map<String,Integer> ensureLast7Days(Map<String,Integer> xp){
        // popuni praznine do 7 dana unazad
        LinkedHashMap<String,Integer> res = new LinkedHashMap<>();
        for (int i=6;i>=0;i--){
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -i);
            String k = dayKey(c.getTime());
            res.put(k, xp.getOrDefault(k, 0));
        }
        return res;
    }

    private String shortLabel(String yyyymmdd){
        // "2025-10-08" -> "08.10."
        String[] p = yyyymmdd.split("-");
        if (p.length<3) return yyyymmdd;
        return p[2]+"."+p[1]+".";
    }

    private Task.Status getStatusSafe(Task t){
        try { return (Task.Status) Task.class.getDeclaredField("status").get(t); }
        catch (Exception ignored) {}
        return Task.Status.ACTIVE;
    }

    private int difficultyValue(Task t){
        try {
            Task.Difficulty d = (Task.Difficulty) Task.class.getDeclaredField("difficulty").get(t);
            // prema tvojoj mapi: very_ea=1, easy=3, difficult=7, extremely=20
            switch (d){
                case VERY_EASY: return 1;
                case EASY: return 3;
                case DIFFICULT: return 7;
                case EXTREMLY_DIFFICULT: return 20;
            }
        } catch (Exception ignored) {}
        return 3;
    }

    private int xpValueSafe(Task t){
        try {
            int totalXP = (int) Task.class.getDeclaredField("totalXP").get(t);
            Task.Status s = getStatusSafe(t);
            return (s == Task.Status.COMPLETED) ? totalXP : 0;
        } catch (Exception ignored) {}
        return 0;
    }

    private String dayKey(Date d){
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
    }

    private List<Date> expandTaskDays(Task t){
        List<Date> out = new ArrayList<>();
        Date start = t.getStartDate();
        if (start == null) return out;

        if (!t.isRecurring()){
            out.add(trim(start));
            return out;
        }
        Date end = t.getEndDate();
        if (end == null) end = start;

        Task.RepeatUnit unit;
        try {
            unit = (Task.RepeatUnit) Task.class.getDeclaredField("repeatUnit").get(t);
        } catch (Exception e) {
            unit = Task.RepeatUnit.DAY;
        }
        int step = 1;
        try { step = (int) Task.class.getDeclaredField("repeatInterval").get(t); } catch (Exception ignored) {}

        Calendar c = Calendar.getInstance();
        c.setTime(trim(start));
        Date endTrim = trim(end);

        while (!c.getTime().after(endTrim)){
            out.add(c.getTime());
            if (unit == Task.RepeatUnit.DAY) c.add(Calendar.DAY_OF_YEAR, Math.max(1, step));
            else c.add(Calendar.WEEK_OF_YEAR, Math.max(1, step));
        }
        return out;
    }

    private Date trim(Date d){
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
