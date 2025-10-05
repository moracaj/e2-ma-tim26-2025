package rs.ftn.rpgtracker.model;

public class Boss {
    private int index;
    private int maxHp;

    public Boss(int index, int maxHp) {
        this.index = index;
        this.maxHp = maxHp;
    }
    public int getIndex() { return index; }
    public int getMaxHp() { return maxHp; }
}
