package rs.ftn.rpgtracker.model;

public class Category {

    private String name;
    private String id;
    private int color;

    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Category(){

    }

    public Category(String id, String name, int color, String userId) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
