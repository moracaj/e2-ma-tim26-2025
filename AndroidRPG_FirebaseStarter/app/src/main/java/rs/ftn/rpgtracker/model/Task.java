package rs.ftn.rpgtracker.model;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class Task {
    private String id;
    private String name;
    private String description;
    private Category category;
    private boolean isRecurring;
    private Date startDate;
    private Date endDate;
    private int repeatInterval;

    private String userId;
    public enum RepeatUnit{
        DAY,
        WEEK
    }
    private RepeatUnit repeatUnit;
    private String executionTime;
    public enum Difficulty {
        VERY_EASY(1),
        EASY(3),
        DIFFICULT(7),
        EXTREMLY_DIFFICULT(20);

        private int xpValue;
        Difficulty(int xpValue){
            this.xpValue = xpValue;
        }
        public int getXpValue(){
            return xpValue;
        }
        public String toString() {
            switch (this) {
                case VERY_EASY:
                    return "Very easy";
                case EASY:
                    return "Easy";
                case DIFFICULT:
                    return "Difficult";
                case EXTREMLY_DIFFICULT:
                    return "Extremly difficult";
                default:
                    return super.toString();
            }
        }
    }
    private Difficulty difficulty;
    public enum Importance {
        NORMAL(1),
        IMPORTANT(3),
        EXTREMLY_IMPORTANT(10),
        SPECIAL(100);

        private final int xpValue;

        Importance(int xpValue) {
            this.xpValue = xpValue;
        }

        public int getXpValue() {
            return xpValue;
        }

        @Override
        public String toString() {
            switch (this) {
                case NORMAL: return "Normal";
                case IMPORTANT: return "Important";
                case EXTREMLY_IMPORTANT: return "Extremly important";
                case SPECIAL: return "Special";
                default: return super.toString();
            }
        }
    }

    private Importance importance;
    private int totalXP;
    public enum Status {
        ACTIVE,
        COMPLETED,
        NOT_COMPLETED,
        PAUSED,
        CANCELLED
    }
    private Status status;
    private Date createdAt;

    public Task(){

    }

    public Task(String name, String description, Category category,
                boolean isRecurring, Date startDate, Date endDate,
                int repeatInterval, RepeatUnit repeatUnit, String executionTime,
                Difficulty difficulty, Importance importance, String userId) {

        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.category = category;
        this.isRecurring = isRecurring;
        this.startDate = startDate;
        this.endDate = endDate;
        this.repeatInterval = repeatInterval;
        this.repeatUnit = repeatUnit;
        this.executionTime = executionTime;
        this.difficulty = difficulty;
        this.importance = importance;
        this.totalXP = difficulty.getXpValue() + importance.getXpValue();
        this.status = Status.ACTIVE;
        this.createdAt = new Date();
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public RepeatUnit getRepeatUnit() {
        return repeatUnit;
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Importance getImportance() {
        return importance;
    }

    public int getTotalXP() {
        return totalXP;
    }

    public Status getStatus() {
        return status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setRepeatInterval(int repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public void setRepeatUnit(RepeatUnit repeatUnit) {
        this.repeatUnit = repeatUnit;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    public void setImportance(Importance importance) {
        this.importance = importance;
        updateTotalXP();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    private void updateTotalXP(){
        if(this.difficulty != null && this.importance != null){
            this.totalXP = this.difficulty.getXpValue() + this.importance.getXpValue();
        }
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        updateTotalXP();
    }

    public void setTotalXP(int totalXP) {
        this.totalXP = totalXP;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    public int getMaxQuota() {
        if (difficulty == Difficulty.VERY_EASY || importance == Importance.NORMAL) {
            return 5; // dnevno
        } else if (difficulty == Difficulty.EASY || importance == Importance.IMPORTANT) {
            return 5; // dnevno
        } else if (difficulty == Difficulty.DIFFICULT || importance == Importance.EXTREMLY_IMPORTANT) {
            return 2; // dnevno
        } else if (difficulty == Difficulty.EXTREMLY_DIFFICULT) {
            return 1; // nedeljno
        } else if (importance == Importance.SPECIAL) {
            return 1; // mesečno
        }
        return Integer.MAX_VALUE; // default: bez limita
    }

    /** Vremenska jedinica za kvotu (DAILY, WEEKLY, MONTHLY) */
    public String getQuotaUnit() {
        if (difficulty == Difficulty.VERY_EASY || importance == Importance.NORMAL) {
            return "DAILY";
        } else if (difficulty == Difficulty.EASY || importance == Importance.IMPORTANT) {
            return "DAILY";
        } else if (difficulty == Difficulty.DIFFICULT || importance == Importance.EXTREMLY_IMPORTANT) {
            return "DAILY";
        } else if (difficulty == Difficulty.EXTREMLY_DIFFICULT) {
            return "WEEKLY";
        } else if (importance == Importance.SPECIAL) {
            return "MONTHLY";
        }
        return "UNLIMITED";
    }
    public Date getQuotaStartDate() {
        Calendar calendar = Calendar.getInstance();

        switch (getQuotaUnit()) {
            case "DAILY":
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                return calendar.getTime();
            case "WEEKLY":
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                return calendar.getTime();
            case "MONTHLY":
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                return calendar.getTime();
            default:
                return null; // UNLIMITED
        }
    }
    public boolean canBeUpdated(Date currentDate) {
        if (status == Status.CANCELLED || status == Status.NOT_COMPLETED) return false;

        // ako je prošlo više od 3 dana od startDate → automatski NOT_COMPLETED
        if (startDate != null) {
            long diffDays = (currentDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
            if (diffDays > 3 && status == Status.ACTIVE) {
                this.status = Status.NOT_COMPLETED;
                return false;
            }
        }
        return true;
    }

    public int calculateXpReward() {
        if (status == Status.COMPLETED) {
            return totalXP;
        }
        // pauzirani i otkazani → 0
        return 0;
    }

}
