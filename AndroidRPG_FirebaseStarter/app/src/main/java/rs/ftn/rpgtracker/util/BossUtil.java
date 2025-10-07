// rs/ftn/rpgtracker/util/BossUtil.java
package rs.ftn.rpgtracker.util;

import rs.ftn.rpgtracker.model.Boss;

public class BossUtil {
    // Prvi boss: 200 HP
    // Svaki sledeći: HP_prev * 2 + HP_prev / 2  (tj. * 2.5)
    public static Boss getBossForLevel(int level) {
        int hp = 200;
        for (int i = 2; i <= level; i++) {
            hp = hp * 5 / 2; // *2.5 bez float
        }
        return new Boss(level, hp);
    }

    // Nagrada u novčićima:
    // level 1: 200, level n: 200 * (1.2)^(n-1)
    public static int coinsForLevel(int level) {
        double base = 200.0;
        double coins = base * Math.pow(1.2, (level - 1));
        return (int)Math.round(coins);
    }
}
