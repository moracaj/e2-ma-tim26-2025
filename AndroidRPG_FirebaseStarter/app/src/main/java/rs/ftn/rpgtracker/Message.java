package rs.ftn.rpgtracker;

import com.google.firebase.Timestamp;

public class Message {
    public String id;
    public String text;
    public String senderUid;
    public String senderUsername;
    public Timestamp ts;

    public Message() {}
    public Message(String text, String senderUid, String senderUsername, Timestamp ts){
        this.text = text; this.senderUid = senderUid; this.senderUsername = senderUsername; this.ts = ts;
    }
}
