package rs.ftn.rpgtracker;

public final class LevelCalculator {
  private LevelCalculator(){}

  // Ako želiš striktno "zaokruži na PRVU NAREDNU stotinu", stavi na true:
  private static final boolean CEIL_TO_NEXT_100 = true;

  // XP potreban da sa TEKUĆEG nivoa pređeš na SLEDEĆI.
  // Primeri (kao u specifikaciji/primeru): 1→200, 2→500, 3→1250 ...
  public static int xpForLevel(int currentLevel) {
    if (currentLevel <= 1) return 200;
    int t = 200; // prag za prelazak sa 1 na 2
    for (int l = 1; l < currentLevel; l++) {
      float raw = t * 2.5f; // XP_prev * 2 + XP_prev/2
      if (CEIL_TO_NEXT_100) {
        t = ceilToNextHundred(raw);
      } else {
        t = Math.round(raw); // drži se primera (500, 1250, 3125…)
      }
    }
    return t;
  }

  private static int ceilToNextHundred(float v) {
    int val = (int)Math.ceil(v / 100f) * 100;
    return val;
  }

  // PP nagrada za PREĐENI nivo (nivo koji si upravo završio).
  // Primer: lvl1→40, lvl2→70, lvl3→123 ...
  public static int ppForLevelReward(int levelFinished) {
    if (levelFinished <= 0) return 0;
    int pp = 40; // posle prvog pređenog nivoa
    for (int l = 2; l <= levelFinished; l++) {
      pp = Math.round(pp * 1.75f); // PP_prev + 3/4 * PP_prev
    }
    return pp;
  }

  // Skalar za porast XP-a težine/bitnosti po nivou (svaki pređeni nivo +50%, zaokruženo).
  // level=1 ⇒ baza; level=2 ⇒ round(base*1.5); level=3 ⇒ round(prev*1.5) …
  public static int scaledByLevel(int base, int level) {
    int v = base;
    for (int i = 2; i <= level; i++) {
      v = Math.round(v * 1.5f);
    }
    return v;
  }

  // Pomoćno: bazne vrednosti iz specifikacije (možeš da koristiš gde dodeljuješ XP zadacima)
  public static final class BaseXP {
    // Težina
    public static final int VERY_EASY = 1;
    public static final int EASY      = 3;
    public static final int HARD      = 7;
    public static final int EXTREME   = 20;
    // Bitnost
    public static final int NORMAL    = 1;
    public static final int IMPORTANT = 3;
    public static final int X_IMPORT  = 10;
    public static final int SPECIAL   = 100;
  }

  // Titule – proizvoljno za prva 3 nivoa; posle generički
  public static String titleForLevel(int level) {
    switch (level) {
      case 0:  return "Unranked";
      case 1:  return "Beginner";
      case 2:  return "Apprentice";
      case 3:  return "Adventurer";
      default: return "Hero L" + level;
    }
  }

  // (Opcionalno, korisno za sekciju 5) Nagrada u novčićima za poraženog bosa tog nivoa
  // lvl1→200, posle *1.2 (240, 288, …)
  public static int coinsForBoss(int level) {
    if (level <= 1) return 200;
    double c = 200.0;
    for (int i = 2; i <= level; i++) c *= 1.2;
    return (int)Math.round(c);
  }
}
