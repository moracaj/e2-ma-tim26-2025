package rs.ftn.rpgtracker;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;

public class MessageAdapter extends BaseAdapter {
    private final Context ctx;
    private final List<Message> data = new ArrayList<>();
    private final String myUid;

    public MessageAdapter(Context ctx, String myUid){
        this.ctx = ctx; this.myUid = myUid;
    }

    public void setAll(List<Message> list){
        data.clear(); data.addAll(list); notifyDataSetChanged();
    }

    @Override public int getCount(){ return data.size(); }
    @Override public Message getItem(int position){ return data.get(position); }
    @Override public long getItemId(int position){ return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message m = getItem(position);
        boolean mine = myUid != null && myUid.equals(m.senderUid);
        int layout = mine ? R.layout.item_message_right : R.layout.item_message_left;
        View v = LayoutInflater.from(ctx).inflate(layout, parent, false);

        ((TextView)v.findViewById(R.id.tvUser)).setText(m.senderUsername);
        ((TextView)v.findViewById(R.id.tvText)).setText(m.text);
        String t = m.ts != null ? DateFormat.getDateTimeInstance().format(m.ts.toDate()) : "";
        ((TextView)v.findViewById(R.id.tvTime)).setText(t);
        return v;
    }
}
