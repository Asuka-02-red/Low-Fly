package com.example.low_altitudereststop.feature.compliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.low_altitudereststop.R;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

/**
 * 禁飞区列表适配器。
 * <p>
 * 展示禁飞区的名称、类型、半径、坐标、生效时段和原因等信息，
 * 支持点击定位、编辑和删除操作回调，企业角色可显示管理按钮。
 * </p>
 */
public class ZoneAdapter extends RecyclerView.Adapter<ZoneAdapter.VH> {

    public interface OnZoneClickListener {
        void onZoneClick(FlightManagementModels.NoFlyZoneRecord zone);

        void onZoneEdit(FlightManagementModels.NoFlyZoneRecord zone);

        void onZoneDelete(FlightManagementModels.NoFlyZoneRecord zone);
    }

    private final List<FlightManagementModels.NoFlyZoneRecord> items = new ArrayList<>();
    private final OnZoneClickListener listener;
    private boolean managementEnabled;

    public ZoneAdapter(OnZoneClickListener listener) {
        this.listener = listener;
    }

    public void setManagementEnabled(boolean managementEnabled) {
        this.managementEnabled = managementEnabled;
    }

    public void submit(List<FlightManagementModels.NoFlyZoneRecord> data) {
        int previousSize = items.size();
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyChanges(previousSize, items.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_zone, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FlightManagementModels.NoFlyZoneRecord z = items.get(position);
        holder.tvTitle.setText(z.name == null ? "-" : z.name);
        holder.tvSubtitle.setText((z.zoneType == null ? "-" : z.zoneType)
                + " · 半径：" + z.radius + "m"
                + " · 坐标：" + safe(z.centerLat) + "," + safe(z.centerLng));
        holder.tvDesc.setText("时段：" + safe(z.effectiveStart) + " - " + safe(z.effectiveEnd)
                + "\n原因：" + safe(z.reason)
                + "\n说明：" + safe(z.description));
        holder.btnEdit.setVisibility(managementEnabled ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(managementEnabled && !z.builtIn ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onZoneClick(z);
            }
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onZoneEdit(z);
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onZoneDelete(z);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void notifyChanges(int previousSize, int newSize) {
        if (previousSize == 0 && newSize > 0) {
            notifyItemRangeInserted(0, newSize);
            return;
        }
        if (newSize == 0 && previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
            return;
        }
        int sharedCount = Math.min(previousSize, newSize);
        if (sharedCount > 0) {
            notifyItemRangeChanged(0, sharedCount);
        }
        if (newSize > previousSize) {
            notifyItemRangeInserted(previousSize, newSize - previousSize);
        } else if (previousSize > newSize) {
            notifyItemRangeRemoved(newSize, previousSize - newSize);
        }
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvSubtitle;
        final TextView tvDesc;
        final MaterialButton btnEdit;
        final MaterialButton btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvDesc = itemView.findViewById(R.id.tv_desc);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}

