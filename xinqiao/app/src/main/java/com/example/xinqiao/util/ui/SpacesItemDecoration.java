package com.example.xinqiao.util.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * RecyclerView 间距装饰，适配 StaggeredGridLayoutManager（瀑布流）。
 */
public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private final int spacingPx;
    private final boolean includeEdge;

    public SpacesItemDecoration(int spacingPx, boolean includeEdge) {
        this.spacingPx = spacingPx;
        this.includeEdge = includeEdge;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams sglp = (StaggeredGridLayoutManager.LayoutParams) lp;
            int spanIndex = sglp.getSpanIndex();
            // 左右间距：边缘更宽，中间统一为一半，保证列与边缘视觉一致
            if (spanIndex == 0) {
                outRect.left = includeEdge ? spacingPx : 0;
                outRect.right = spacingPx / 2;
            } else {
                outRect.left = spacingPx / 2;
                outRect.right = includeEdge ? spacingPx : 0;
            }
        } else {
            // 非瀑布流情况，给予对称半间距
            outRect.left = spacingPx / 2;
            outRect.right = spacingPx / 2;
        }
        // 顶部和底部间距
        outRect.top = spacingPx;
        outRect.bottom = spacingPx;
    }
}
