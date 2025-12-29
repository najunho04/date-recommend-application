package com.najunho.datecourserecommendapplication.RecycerView;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.najunho.datecourserecommendapplication.DB.Location;
import com.najunho.datecourserecommendapplication.R;

import java.util.ArrayList;
import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>{

    public interface OnLocationActionListener {
        void onItemClick(int position, Location location);
    }

    private OnLocationActionListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION; // 선택된 아이템 position

    public void setOnLocationActionListener(OnLocationActionListener listener) {
        this.listener = listener;
    }

    private List<Location> placeList = new ArrayList<>();
    public void setPlaceList(List<Location> list){
        placeList.clear();
        placeList.addAll(list);
        notifyDataSetChanged(); // UI 강제 갱신
    }

    public PlaceAdapter(OnLocationActionListener listener) {
        super();
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        Log.d("PlaceViewHolder onCreateViewHolder", "success");
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        Location place = placeList.get(position);
        holder.bind(place);
        Log.d("PlaceViewHolder onBindViewHolder", "success");

    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }

    public class PlaceViewHolder extends RecyclerView.ViewHolder {

        TextView title, address;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_place_name);
            address = itemView.findViewById(R.id.tv_place_address);
        }

        private void bind(Location item){
            title.setText(item.getName());
            address.setText(item.getAddress());

            // 선택 표시
            if(selectedPosition == getLayoutPosition()){
                itemView.setBackgroundColor(Color.LTGRAY); // 선택된 배경
            } else {
                itemView.setBackgroundColor(Color.WHITE);  // 기본 배경
            }

            itemView.setOnClickListener(v -> {
                int previousPosition = selectedPosition;
                selectedPosition = getLayoutPosition();
                notifyItemChanged(previousPosition); // 이전 선택 표시 제거
                notifyItemChanged(selectedPosition); // 새로운 선택 표시
                if (listener != null) listener.onItemClick(getLayoutPosition(), item);
                itemView.isClickable();
            });
        }
    }
}
