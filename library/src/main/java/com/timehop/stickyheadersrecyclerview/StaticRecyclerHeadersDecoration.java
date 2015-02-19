package com.timehop.stickyheadersrecyclerview;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class StaticRecyclerHeadersDecoration extends RecyclerView.ItemDecoration  {
    private final StickyRecyclerHeadersAdapter mAdapter;
    private final LongSparseArray<View> mHeaderViews = new LongSparseArray<>();

    public StaticRecyclerHeadersDecoration(StickyRecyclerHeadersAdapter adapter) {
        mAdapter = adapter;
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int orientation = getOrientation(parent);
        int itemPosition = parent.getChildPosition(view);
        if (hasNewHeader(itemPosition)) {
            View header = getHeaderView(parent, itemPosition);
            if (orientation == LinearLayoutManager.VERTICAL) {
                outRect.top = header.getHeight();
            } else {
                outRect.left = header.getWidth();
            }
        }
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);
        int orientation = getOrientation(parent);

        if (parent.getChildCount() > 0 && mAdapter.getItemCount() > 0) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                int position = parent.getChildPosition(parent.getChildAt(i));
                if (mAdapter.getHeaderId(position) >= 0 && hasNewHeader(position)) {
                    int translationX = 0;
                    int translationY = 0;
                    View header = getHeaderView(parent, position);
                    if (orientation == LinearLayoutManager.VERTICAL) {
                        translationY = parent.getChildAt(i).getTop() - header.getHeight();
                    } else {
                        translationX = parent.getChildAt(i).getLeft() - header.getWidth();
                    }
                    canvas.save();
                    canvas.translate(translationX, translationY);
                    header.draw(canvas);
                    canvas.restore();
                }
            }
        }
    }

    private int getOrientation(RecyclerView parent) {
        if (parent.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
            return layoutManager.getOrientation();
        } else {
            throw new IllegalStateException("StaticHeader can only be used with a " +
                    "LinearLayoutManager.");
        }
    }

    private boolean hasNewHeader(int position) {
        if (getFirstHeaderPosition() == position) {
            return true;
        } else if (mAdapter.getHeaderId(position) < 0) {
            return false;
        } else if (position > 0 && position < mAdapter.getItemCount()) {
            return mAdapter.getHeaderId(position) != mAdapter.getHeaderId(position - 1);
        } else {
            return false;
        }
    }

    private int getFirstHeaderPosition() {
        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            if (mAdapter.getHeaderId(i) >= 0) {
                return i;
            }
        }
        return -1;
    }

    public View getHeaderView(RecyclerView parent, int position) {
        long headerId = mAdapter.getHeaderId(position);

        View header = mHeaderViews.get(headerId);
        if (header == null) {
            //TODO - recycle views
            RecyclerView.ViewHolder viewHolder = mAdapter.onCreateHeaderViewHolder(parent, position);
            mAdapter.onBindHeaderViewHolder(viewHolder, position);
            header = viewHolder.itemView;
            if (header.getLayoutParams() == null) {
                header.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            int widthSpec;
            int heightSpec;

            if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
                widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
                heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);
            } else {
                widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.UNSPECIFIED);
                heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.EXACTLY);
            }

            int childWidth = ViewGroup.getChildMeasureSpec(widthSpec,
                    parent.getPaddingLeft() + parent.getPaddingRight(), header.getLayoutParams().width);
            int childHeight = ViewGroup.getChildMeasureSpec(heightSpec,
                    parent.getPaddingTop() + parent.getPaddingBottom(), header.getLayoutParams().height);
            header.measure(childWidth, childHeight);
            header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());
            mHeaderViews.put(headerId, header);
        }
        return header;
    }


}
