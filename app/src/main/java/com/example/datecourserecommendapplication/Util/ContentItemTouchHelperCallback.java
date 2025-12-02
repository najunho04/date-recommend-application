package com.example.datecourserecommendapplication.Util;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ContentItemTouchHelperCallback extends ItemTouchHelper.Callback{
    private final ItemTouchHelperListener listener;

    public ContentItemTouchHelperCallback(ItemTouchHelperListener listener) {
        this.listener = listener;
    }
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN; // 수직 드래그
        int swipeFlags = 0; // 좌우 스와이프 없음

        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return listener.onItemMove(viewHolder.getBindingAdapterPosition(),
                target.getBindingAdapterPosition());
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        //스와이프 미사용
    }
    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }
}
