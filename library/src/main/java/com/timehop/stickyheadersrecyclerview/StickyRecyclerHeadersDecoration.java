package com.timehop.stickyheadersrecyclerview;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

public class StickyRecyclerHeadersDecoration extends RecyclerView.ItemDecoration {


    /**
     * The two states used to describe the current state of the sticky header.
     * State.Stacked implies that the header is being drawn on top of other views.
     * State.Inline implies that it appears like a normal header, i.e. no views are scrolling "under" it yet.
     */
    public enum State {
        Stacked,
        Inline
    }

    private final StickyRecyclerHeadersAdapter mAdapter;
    private final LongSparseArray<View> mHeaderViews = new LongSparseArray<>();
    private final SparseArray<Rect> mHeaderRects = new SparseArray<>();
    private final LongSparseArray<State> mHeaderViewStates = new LongSparseArray<>();
    private final LongSparseArray<RecyclerView.ViewHolder> mHeaderViewHolders = new LongSparseArray<>();

    public StickyRecyclerHeadersDecoration(StickyRecyclerHeadersAdapter adapter) {
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
        mHeaderRects.clear();

        if (parent.getChildCount() > 0 && mAdapter.getItemCount() > 0) {
            // draw the first visible child's header at the top of the view
            View firstView = parent.getChildAt(0);
            int firstPosition = parent.getChildPosition(firstView);
            long firstHeaderId = mAdapter.getHeaderId(firstPosition);
            if (firstHeaderId >= 0) {
                View firstHeader = getHeaderView(parent, firstPosition);
                View nextView = getNextView(parent);
                int leftOffset = firstView.getLeft() - firstHeader.getWidth();
                int topOffset = firstView.getTop() - firstHeader.getHeight();


                if (orientation == LinearLayoutManager.VERTICAL) {
                    if (topOffset == 0) {
                        // The view is directly below the header but not under
                        // make sure the header knows it's in inline mode
                        setHeaderStateAndNotify(firstHeaderId, firstPosition, State.Inline);
                    } else if (topOffset < 0) {
                        // < 0  implies the view is being tucked under the header
                        // make sure the header knows it's in stacked mode
                        setHeaderStateAndNotify(firstHeaderId, firstPosition, State.Stacked);
                    }
                } else if (orientation == LinearLayoutManager.HORIZONTAL) {
                    if (leftOffset == 0) {
                        // The view is directly below the header but not under
                        // make sure the header knows it's in inline mode
                        setHeaderStateAndNotify(firstHeaderId, firstPosition, State.Inline);
                    } else if (leftOffset < 0) {
                        // < 0  implies the view is being tucked under the header
                        // make sure the header knows it's in stacked mode
                        setHeaderStateAndNotify(firstHeaderId, firstPosition, State.Stacked);
                    }
                }

                int translationX = Math.max(leftOffset, 0);
                int translationY = Math.max(topOffset, 0);

                int nextPosition = parent.getChildPosition(nextView);
                if (nextPosition > 0 && hasNewHeader(nextPosition)) {
                    View secondHeader = getHeaderView(parent, nextPosition);
                    long secondHeaderId = mAdapter.getHeaderId(nextPosition);
                    //Translate the topmost header so the next header takes its place, if applicable
                    if (orientation == LinearLayoutManager.VERTICAL &&
                            nextView.getTop() - secondHeader.getHeight() - firstHeader.getHeight() < 0) {
                            translationY += nextView.getTop() - secondHeader.getHeight() - firstHeader.getHeight();
                            //Set both headers to inline mode while one is pushing off the other as no views are under them
                            setHeaderStateAndNotify(firstHeaderId, firstPosition, State.Inline);
                            setHeaderStateAndNotify(secondHeaderId, nextPosition, State.Inline);

                    } else if (orientation == LinearLayoutManager.HORIZONTAL &&
                            nextView.getLeft() - secondHeader.getWidth() - firstHeader.getWidth() < 0) {
                            translationX += nextView.getLeft() - secondHeader.getWidth() - firstHeader.getWidth();
                            //Set both headers to inline mode while one is pushing off the other as no views are under them
                            setHeaderStateAndNotify(firstHeaderId, firstPosition, State.Inline);
                            setHeaderStateAndNotify(secondHeaderId, nextPosition, State.Inline);
                    }
                }
                canvas.save();

                canvas.translate(translationX, translationY);
                firstHeader.draw(canvas);
                canvas.restore();
                mHeaderRects.put(firstPosition, new Rect(translationX, translationY,
                        translationX + firstHeader.getWidth(), translationY + firstHeader.getHeight()));
            }
            for (int i = 1; i < parent.getChildCount(); i++) {
                int position = parent.getChildPosition(parent.getChildAt(i));
                if (hasNewHeader(position)) {
                    // this header is different than the previous, it must be drawn in the correct place
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
                    mHeaderRects.put(position, new Rect(translationX, translationY,
                            translationX + header.getWidth(), translationY + header.getHeight()));
                }
            }
        }
    }

    private void setHeaderStateAndNotify(long headerId, int position, State state) {
        if (mHeaderViewStates.get(headerId) != state) {
            mHeaderViewStates.put(headerId, state);
            mAdapter.onStickyHeaderStateChange(mHeaderViewHolders.get(headerId), state, position);
        }
    }


    /**
     * Returns the first item currently in the recyclerview that's not obscured by a header.
     *
     * @param parent
     * @return
     */
    private View getNextView(RecyclerView parent) {
        View firstView = parent.getChildAt(0);
        // draw the first visible child's header at the top of the view
        int firstPosition = parent.getChildPosition(firstView);
        View firstHeader = getHeaderView(parent, firstPosition);
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
            if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
                if (child.getTop() - layoutParams.topMargin > firstHeader.getHeight()) {
                    return child;
                }
            } else {
                if (child.getLeft() - layoutParams.leftMargin > firstHeader.getWidth()) {
                    return child;
                }
            }
        }
        return null;
    }

    private int getOrientation(RecyclerView parent) {
        if (parent.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
            return layoutManager.getOrientation();
        } else {
            throw new IllegalStateException("StickyListHeadersDecoration can only be used with a " +
                    "LinearLayoutManager.");
        }
    }

    /**
     * Gets the position of the header under the specified (x, y) coordinates.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return position of header, or -1 if not found
     */
    public int findHeaderPositionUnder(int x, int y) {
        for (int i = 0; i < mHeaderRects.size(); i++) {
            Rect rect = mHeaderRects.get(mHeaderRects.keyAt(i));
            if (rect.contains(x, y)) {
                return mHeaderRects.keyAt(i);
            }
        }
        return -1;
    }

    /**
     * Gets the header view for the associated position.  If it doesn't exist yet, it will be
     * created, measured, and laid out.
     *
     * @param parent
     * @param position
     * @return Header view
     */
    public View getHeaderView(RecyclerView parent, int position) {
        long headerId = mAdapter.getHeaderId(position);

        View header = mHeaderViews.get(headerId);
        if (header == null) {
            //TODO - recycle views
            RecyclerView.ViewHolder viewHolder = mAdapter.onCreateHeaderViewHolder(parent, position);
            mHeaderViewHolders.put(headerId, viewHolder);
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
            mHeaderViewStates.put(headerId, State.Inline);
        }
        return header;
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

    /**
     * Invalidates cached headers.  This does not invalidate the recyclerview, you should do that manually after
     * calling this method.
     */
    public void invalidateHeaders() {
        mHeaderViews.clear();
    }
}
