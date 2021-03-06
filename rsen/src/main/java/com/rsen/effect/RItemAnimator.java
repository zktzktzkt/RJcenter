package com.rsen.effect;

/**
 * Created by angcyo on 16-02-25-025.
 */

import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import java.util.ArrayList;

/**
 * This implementation of {@link RecyclerView.ItemAnimator} provides basic
 * animations on remove, add, and move events that happen to the items in
 * a RecyclerView. RecyclerView uses a DefaultItemAnimator by default.
 *
 * @see RecyclerView#setItemAnimator(RecyclerView.ItemAnimator)
 */
public abstract class RItemAnimator extends SimpleItemAnimator {

    public ArrayList<RecyclerView.ViewHolder> mAddAnimations = new ArrayList<RecyclerView.ViewHolder>();
    public ArrayList<RecyclerView.ViewHolder> mMoveAnimations = new ArrayList<RecyclerView.ViewHolder>();
    public ArrayList<RecyclerView.ViewHolder> mRemoveAnimations = new ArrayList<RecyclerView.ViewHolder>();
    private ArrayList<RecyclerView.ViewHolder> mPendingRemovals = new ArrayList<RecyclerView.ViewHolder>();
    private ArrayList<RecyclerView.ViewHolder> mPendingAdditions = new ArrayList<RecyclerView.ViewHolder>();
    private ArrayList<MoveInfo> mPendingMoves = new ArrayList<MoveInfo>();
    private ArrayList<RecyclerView.ViewHolder> mAdditions = new ArrayList<RecyclerView.ViewHolder>();
    private ArrayList<MoveInfo> mMoves = new ArrayList<MoveInfo>();

    @Override
    public void runPendingAnimations() {
        boolean removalsPending = !mPendingRemovals.isEmpty();
        boolean movesPending = !mPendingMoves.isEmpty();
        boolean additionsPending = !mPendingAdditions.isEmpty();
        if (!removalsPending && !movesPending && !additionsPending) {
            // nothing to animate
            return;
        }

        for (RecyclerView.ViewHolder holder : mPendingRemovals) {
            prepareAnimateRemove(holder);
        }

        // First, remove stuff
        for (RecyclerView.ViewHolder holder : mPendingRemovals) {
            animateRemoveImpl(holder);
        }
        mPendingRemovals.clear();
        // Next, move stuff
        if (movesPending) {
            mMoves.addAll(mPendingMoves);
            mPendingMoves.clear();
            Runnable mover = new Runnable() {
                @Override
                public void run() {
                    for (MoveInfo moveInfo : mMoves) {
                        animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY, moveInfo.toX, moveInfo.toY);
                    }
                    mMoves.clear();
                }
            };
            if (removalsPending) {
                View view = mMoves.get(0).holder.itemView;
                ViewCompat.postOnAnimationDelayed(view, mover, getRemoveDuration());
            } else {
                mover.run();
            }
        }
        // Next, add stuff
        if (additionsPending) {
            mAdditions.addAll(mPendingAdditions);
            mPendingAdditions.clear();
            Runnable adder = new Runnable() {
                public void run() {
                    for (RecyclerView.ViewHolder holder : mAdditions) {
                        animateAddImpl(holder);
                    }
                    mAdditions.clear();
                }
            };
            if (removalsPending || movesPending) {
                View view = mAdditions.get(0).itemView;
                ViewCompat.postOnAnimationDelayed(view, adder, (removalsPending ? getRemoveDuration() : 0) + (movesPending ? getMoveDuration() : 0));
            } else {
                adder.run();
            }
        }
    }

    public boolean animateAdd(final RecyclerView.ViewHolder holder) {
        prepareAnimateAdd(holder);
        mPendingAdditions.add(holder);
        return true;
    }

    protected abstract void prepareAnimateAdd(final RecyclerView.ViewHolder holder);

    protected abstract void prepareAnimateRemove(final RecyclerView.ViewHolder holder);

    protected abstract void animateAddImpl(final RecyclerView.ViewHolder holder);

    public boolean animateRemove(final RecyclerView.ViewHolder holder) {
        mPendingRemovals.add(holder);
        return true;
    }

    protected abstract void animateRemoveImpl(final RecyclerView.ViewHolder holder);

    public boolean animateMove(final RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        final View view = holder.itemView;
        int deltaX = toX - fromX;
        int deltaY = toY - fromY;
        if (deltaX == 0 && deltaY == 0) {
            dispatchAnimationFinished(holder);
            return false;
        }
        if (deltaX != 0) {
            ViewCompat.setTranslationX(view, -deltaX);
        }
        if (deltaY != 0) {
            ViewCompat.setTranslationY(view, -deltaY);
        }
        mPendingMoves.add(new MoveInfo(holder, fromX, fromY, toX, toY));
        return true;
    }

    protected void animateMoveImpl(final RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        final View view = holder.itemView;
        final int deltaX = toX - fromX;
        final int deltaY = toY - fromY;
        ViewCompat.animate(view).cancel();
        if (deltaX != 0) {
            ViewCompat.animate(view).translationX(0);
        }
        if (deltaY != 0) {
            ViewCompat.animate(view).translationY(0);
        }

        /**
         * Dean Ding
         *
         * if you called setStartDelay in animateAddImpl or
         * animateRemoveImpl,you must reset it to 0 before move animation
         *
         */

        ViewCompat.animate(view).setDuration(getMoveDuration()).setStartDelay(0).setListener(new VpaListenerAdapter() {
            @Override
            public void onAnimationCancel(View view) {
                if (deltaX != 0) {
                    ViewCompat.setTranslationX(view, 0);
                }
                if (deltaY != 0) {
                    ViewCompat.setTranslationY(view, 0);
                }
            }

            @Override
            public void onAnimationEnd(View view) {
                dispatchAnimationFinished(holder);
                mMoveAnimations.remove(holder);
                dispatchFinishedWhenDone();
            }
        }).start();
        mMoveAnimations.add(holder);
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        final View view = item.itemView;
        ViewCompat.animate(view).cancel();
        if (mPendingMoves.contains(item)) {
            ViewCompat.setTranslationY(view, 0);
            ViewCompat.setTranslationX(view, 0);
            dispatchAnimationFinished(item);
            mPendingMoves.remove(item);
        }
        if (mPendingRemovals.contains(item)) {
            dispatchAnimationFinished(item);
            mPendingRemovals.remove(item);
        }
        if (mPendingAdditions.contains(item)) {
            ViewCompat.setAlpha(view, 1);
            dispatchAnimationFinished(item);
            mPendingAdditions.remove(item);
        }
        if (mMoveAnimations.contains(item)) {
            ViewCompat.setTranslationY(view, 0);
            ViewCompat.setTranslationX(view, 0);
            dispatchAnimationFinished(item);
            mMoveAnimations.remove(item);
        }
        if (mRemoveAnimations.contains(item)) {
            ViewCompat.setAlpha(view, 1);
            dispatchAnimationFinished(item);
            mRemoveAnimations.remove(item);
        }
        if (mAddAnimations.contains(item)) {
            ViewCompat.setAlpha(view, 1);
            dispatchAnimationFinished(item);
            mAddAnimations.remove(item);
        }
        dispatchFinishedWhenDone();
    }

    @Override
    public boolean isRunning() {
        return (!mMoveAnimations.isEmpty() || !mRemoveAnimations.isEmpty() || !mAddAnimations.isEmpty() || !mMoves.isEmpty() || !mAdditions.isEmpty());
    }

    /**
     * Check the state of currently pending and running animations. If there are
     * none pending/running, call {@link #dispatchAnimationsFinished()} to
     * notify any listeners.
     */
    public void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    @Override
    public void endAnimations() {
        int count = mPendingMoves.size();
        for (int i = count - 1; i >= 0; i--) {
            MoveInfo item = mPendingMoves.get(i);
            View view = item.holder.itemView;
            ViewCompat.animate(view).cancel();
            ViewCompat.setTranslationY(view, 0);
            ViewCompat.setTranslationX(view, 0);
            dispatchAnimationFinished(item.holder);
            mPendingMoves.remove(item);
        }
        count = mPendingRemovals.size();
        for (int i = count - 1; i >= 0; i--) {
            RecyclerView.ViewHolder item = mPendingRemovals.get(i);
            dispatchAnimationFinished(item);
            mPendingRemovals.remove(item);
        }
        count = mPendingAdditions.size();
        for (int i = count - 1; i >= 0; i--) {
            RecyclerView.ViewHolder item = mPendingAdditions.get(i);
            View view = item.itemView;
            ViewCompat.setAlpha(view, 1);
            dispatchAnimationFinished(item);
            mPendingAdditions.remove(item);
        }
        if (!isRunning()) {
            return;
        }
        count = mMoveAnimations.size();
        for (int i = count - 1; i >= 0; i--) {
            RecyclerView.ViewHolder item = mMoveAnimations.get(i);
            View view = item.itemView;
            ViewCompat.animate(view).cancel();
            ViewCompat.setTranslationY(view, 0);
            ViewCompat.setTranslationX(view, 0);
            dispatchAnimationFinished(item);
            mMoveAnimations.remove(item);
        }
        count = mRemoveAnimations.size();
        for (int i = count - 1; i >= 0; i--) {
            RecyclerView.ViewHolder item = mRemoveAnimations.get(i);
            View view = item.itemView;
            ViewCompat.animate(view).cancel();
            ViewCompat.setAlpha(view, 1);
            dispatchAnimationFinished(item);
            mRemoveAnimations.remove(item);
        }
        count = mAddAnimations.size();
        for (int i = count - 1; i >= 0; i--) {
            RecyclerView.ViewHolder item = mAddAnimations.get(i);
            View view = item.itemView;
            ViewCompat.animate(view).cancel();
            ViewCompat.setAlpha(view, 1);
            dispatchAnimationFinished(item);
            mAddAnimations.remove(item);
        }
        mMoves.clear();
        mAdditions.clear();
        dispatchAnimationsFinished();
    }

    private static class MoveInfo {
        public RecyclerView.ViewHolder holder;
        public int fromX, fromY, toX, toY;

        private MoveInfo(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
            this.holder = holder;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }
    }

    public static class VpaListenerAdapter implements ViewPropertyAnimatorListener {
        @Override
        public void onAnimationStart(View view) {
        }

        @Override
        public void onAnimationEnd(View view) {
        }

        @Override
        public void onAnimationCancel(View view) {
        }
    }
}
