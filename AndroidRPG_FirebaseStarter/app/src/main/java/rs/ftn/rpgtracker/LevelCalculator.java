
package rs.ftn.rpgtracker;
public class LevelCalculator {
  public static int xpForLevel(int level){
    if(level<=1) return 200;
    int prev=xpForLevel(level-1);
    int raw=prev*2+prev/2;
    int rem=raw%100;
    if(rem!=0) raw+= (100-rem);
    return raw;
  }
  public static int ppForLevelReward(int level){
    if(level==1) return 40;
    int prev=ppForLevelReward(level-1);
    return (int)Math.round(prev+0.75*prev);
  }
}
