package rs.ftn.rpgtracker;
public class LevelCalculator {
    public static int xpForLevel(int level) {
        if (level <= 1) return 200;
        int prev = xpForLevel(level - 1);
        int raw = prev * 2 + prev / 2;
        int remainder = raw % 100;
        if (remainder != 0) raw += (100 - remainder);
        return raw;
    }
    public static int ppForLevelReward(int level) {
        if (level == 1) return 40;
        int prev = ppForLevelReward(level - 1);
        double reward = prev + 0.75 * prev;
        return (int)Math.round(reward);
    }
}