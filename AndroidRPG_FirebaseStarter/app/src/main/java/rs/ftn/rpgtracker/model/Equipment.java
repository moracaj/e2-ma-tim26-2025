// rs/ftn/rpgtracker/model/Equipment.java
package rs.ftn.rpgtracker.model;

public class Equipment {
    public enum Type { CLOTHES, WEAPON }

    private String id;
    private String name;
    private Type type;
    private int bonusPP;        // koliko PP dodaje
    private boolean equipped;   // da li je aktivirana za borbu

    public Equipment() {}
    public Equipment(String id, String name, Type type, int bonusPP, boolean equipped) {
        this.id = id; this.name = name; this.type = type; this.bonusPP = bonusPP; this.equipped = equipped;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Type getType() { return type; }
    public int getBonusPP() { return bonusPP; }
    public boolean isEquipped() { return equipped; }
    public void setEquipped(boolean equipped) { this.equipped = equipped; }
}
