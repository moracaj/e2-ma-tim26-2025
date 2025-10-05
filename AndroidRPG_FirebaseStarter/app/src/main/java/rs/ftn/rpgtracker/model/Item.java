package rs.ftn.rpgtracker.model;

public class Item {
    int id;
    String type, name, bonusType;
    double bonusValue;
    int price, durationBattles;
    boolean permanent, upgradeable;

    public Item(int id, String type, String name, String bonusType, double bonusValue,
         int price, int durationBattles, boolean permanent, boolean upgradeable) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.bonusType = bonusType;
        this.bonusValue = bonusValue;
        this.price = price;
        this.durationBattles = durationBattles;
        this.permanent = permanent;
        this.upgradeable = upgradeable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBonusType() {
        return bonusType;
    }

    public void setBonusType(String bonusType) {
        this.bonusType = bonusType;
    }

    public double getBonusValue() {
        return bonusValue;
    }

    public void setBonusValue(double bonusValue) {
        this.bonusValue = bonusValue;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getDurationBattles() {
        return durationBattles;
    }

    public void setDurationBattles(int durationBattles) {
        this.durationBattles = durationBattles;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    public boolean isUpgradeable() {
        return upgradeable;
    }

    public void setUpgradeable(boolean upgradeable) {
        this.upgradeable = upgradeable;
    }

    @Override
    public String toString() {
        return name + " (" + type + ") - " + price + "c";
    }
}
