package rs.ftn.rpgtracker.model;

public class User {
    private String uid;
    private int ppBase;     // osnovna snaga
    private int coins;      // novčići
    private int currentLevel; // na kom bosu je korisnik (1 = prvi, itd.)

    public User() {}
    public String getUid() { return uid; }
    public int getPpBase() { return ppBase; }
    public int getCoins() { return coins; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCoins(int coins) { this.coins = coins; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
}
