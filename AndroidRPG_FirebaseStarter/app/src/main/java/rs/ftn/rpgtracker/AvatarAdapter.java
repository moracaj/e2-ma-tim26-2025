package rs.ftn.rpgtracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.VH> {

    public static class AvatarItem {
        public final String key;   // npr. "avatar_blue"
        public final int resId;    // R.drawable.avatar_blue
        public AvatarItem(String key, int resId) { this.key = key; this.resId = resId; }
    }

    public interface OnAvatarSelected {
        void onSelected(String key);
    }

    private final List<AvatarItem> data;
    private int selected = 0;
    private final OnAvatarSelected callback;

    public AvatarAdapter(List<AvatarItem> data, OnAvatarSelected cb) {
        this.data = data;
        this.callback = cb;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        AvatarItem it = data.get(pos);
        ImageView img = h.itemView.findViewById(R.id.imgAvatar);
        View ring = h.itemView.findViewById(R.id.selectionRing);
        img.setImageResource(it.resId);
        ring.setVisibility(pos == selected ? View.VISIBLE : View.GONE);
        h.itemView.setOnClickListener(v -> {
            int old = selected;
            selected = h.getAdapterPosition();
            notifyItemChanged(old);
            notifyItemChanged(selected);
            if (callback != null) callback.onSelected(data.get(selected).key);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    public String getSelectedKey() { return data.get(selected).key; }

    static class VH extends RecyclerView.ViewHolder { public VH(@NonNull View itemView){ super(itemView); } }
}
