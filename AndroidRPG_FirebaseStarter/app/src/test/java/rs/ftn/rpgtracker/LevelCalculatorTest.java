package rs.ftn.rpgtracker;

import org.junit.Test;
import static org.junit.Assert.*;

public class LevelCalculatorTest {

    @Test public void xpThresholds() {
        assertEquals(200,  LevelCalculator.xpForLevel(1));
        assertEquals(500,  LevelCalculator.xpForLevel(2));
        assertEquals(1300, LevelCalculator.xpForLevel(3));
    }

    @Test public void ppRewards() {
        assertEquals(40,  LevelCalculator.ppForLevelReward(1));
        assertEquals(70,  LevelCalculator.ppForLevelReward(2));
        assertEquals(123, LevelCalculator.ppForLevelReward(3));
    }

    @Test public void scalingNormal() {
        assertEquals(1, LevelCalculator.scaledByLevel(LevelCalculator.BaseXP.NORMAL, 1));
        assertEquals(2, LevelCalculator.scaledByLevel(LevelCalculator.BaseXP.NORMAL, 2));
        assertEquals(3, LevelCalculator.scaledByLevel(LevelCalculator.BaseXP.NORMAL, 3));
    }

    @Test public void scalingXImportant() {
        assertEquals(10, LevelCalculator.scaledByLevel(LevelCalculator.BaseXP.X_IMPORT, 1));
        assertEquals(15, LevelCalculator.scaledByLevel(LevelCalculator.BaseXP.X_IMPORT, 2));
        assertEquals(23, LevelCalculator.scaledByLevel(LevelCalculator.BaseXP.X_IMPORT, 3));
    }
}
