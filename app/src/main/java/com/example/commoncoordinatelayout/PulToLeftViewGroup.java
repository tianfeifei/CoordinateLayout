package com.example.commoncoordinatelayout;

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * 横滑时带有更多交互效果
 * 使用场景：双排横滑卡片，带有更多交互
 * Created by feifeitian on 2020/7/6.
 */

public class PulToLeftViewGroup extends LinearLayout {
    public static final String TAG = "CommonCoordinateLayout";
    private static final int SCROLL_DELAY_DISTANCE = 30;
    private boolean isChanged;
    private boolean isRecyclerViewMoving;
    private float startX;
    private float lastX;
    private RecyclerView recyclerView;
    private View moreLayout;
    private TextView more;
    private View icon;
    private boolean isSelfConsumer;
    private Scroller mScroller;
    private float mVelocityX;
    //更多按钮宽度+箭头宽度+pingding
    private int mFirstThreshold = -1;
    //此阈值为显示"松手查看"的临界值，阈值1到阈值2为箭头旋转的过程
    private int mSecondThreshold = -1;
    private float mCharacterWidth;
    private boolean enabledMore = true;

    private VelocityTracker velocityTracker;

    private CoordinateListener mCoordinateListener;

    public PulToLeftViewGroup(Context context) {
        super(context);
        init(context);
    }

    public PulToLeftViewGroup(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PulToLeftViewGroup(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    /**
     * 设置更多交互效果开关
     *
     * @param enabledMore
     */
    public void setEnabledMore(boolean enabledMore) {
        this.enabledMore = enabledMore;
    }

    /**
     * 是否使能更多交互效果
     */
    public boolean isEnabledMore() {
        return enabledMore;
    }


    /**
     * 设置触发阈值监听
     *
     * @param coordinateListener
     */
    public void setCoordinateListener(CoordinateListener coordinateListener) {
        mCoordinateListener = coordinateListener;
    }


    public void setRecyclerView(final RecyclerView recyclerView) {
        this.recyclerView = recyclerView;

        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (mVelocityX < 0 && isSlideToRight(recyclerView)) {
                    log("onScrolled# is scrolled end");
                    mScroller.fling(getScrollX(), 0, (int)- mVelocityX, 0, 0, getFirstThreshold(), 0, 0);
                    PulToLeftViewGroup.this.invalidate();

                }
            }
        });
    }

    private void init(final Context context) {
        post(new Runnable() {
            @Override
            public void run() {
                LayoutInflater.from(context).inflate(R.layout.view_coordinate_layout, PulToLeftViewGroup.this);
                more = findViewById(R.id.more);
                icon = findViewById(R.id.icon);
                moreLayout = findViewById(R.id.ll_more);

                initData();
            }
        });

        mScroller = new Scroller(context, null);
    }


    private void initData() {
        moreLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("onClick# moreLayout is clicked");
                if (mCoordinateListener != null) {
                    mCoordinateListener.onMore();
                }

            }
        });
    }

    private int getFirstThreshold() {
        if (mFirstThreshold != -1) {
            return mFirstThreshold;
        }
        return (int) (getCharacterWidth() + icon.getMeasuredWidth() + moreLayout.getPaddingLeft());

    }

    private float getCharacterWidth() {
        if ((mCharacterWidth == 0)) {
            mCharacterWidth = Util.getCharacterWidth(more, 2);
        }
        return mCharacterWidth;
    }

    private int getSecondThreshold() {
        if (mSecondThreshold != -1) {
            return mSecondThreshold;
        }
        return getFirstThreshold() + Util.dip2px(this.getContext(), 60);

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //MoreLayout的Clone事件
        MotionEvent eventMoreClone = MotionEvent.obtain(event);
        float moreX = event.getX() + getScrollX() - moreLayout.getLeft();
        float moreY = event.getY() - moreLayout.getTop();
        eventMoreClone.setLocation(moreX, moreY);

        //RecyclerView的Clone事件
        MotionEvent recyclerViewEvent = MotionEvent.obtain(event);
        float cloneX = event.getX() - recyclerView.getLeft() + getScrollX();
        recyclerViewEvent.setLocation(cloneX, recyclerViewEvent.getY());

        float currentX = event.getRawX();
        int scrollX = getScrollX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                recyclerView.dispatchTouchEvent(recyclerViewEvent);
                if (inMoreLayout(event)) {
                    moreLayout.dispatchTouchEvent(eventMoreClone);
                }

                //重置状态
                isChanged = false;
                moreLayout.setTranslationX(0);
                lastX = startX = currentX;
                mScroller.forceFinished(true);
                isSelfConsumer = false;
                mVelocityX=0;

                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(event);


                break;
            case MotionEvent.ACTION_MOVE:
                float dX = lastX - currentX;
                // 如果没有在滚动就将事件传到 moreLayout 里面
                if (!isSelfConsumer && inMoreLayout(event)) {
                    moreLayout.dispatchTouchEvent(eventMoreClone);
                }

                log("getScrollX()=" + scrollX);

                if (dX < 0) {
                    log("scroll to right");

                    if (scrollX > 0) {
                        dealBySelf(dX, Math.abs(currentX - startX));
                        isRecyclerViewMoving = false;
                    } else {
                        recyclerView.dispatchTouchEvent(recyclerViewEvent);
                        isRecyclerViewMoving = true;
                    }
                } else {
                    log("scroll to left");
                    if (isSlideToRight(recyclerView)) {
                        dealBySelf(dX, Math.abs(currentX - startX));
                        isRecyclerViewMoving = false;
                    } else {
                        recyclerView.dispatchTouchEvent(recyclerViewEvent);
                        isRecyclerViewMoving = true;
                    }

                }

                lastX = currentX;
                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                /*
                 * 在移动Parent的时候,手指移动小于10像素  或者  现在是RecyclerView接管事件的时候
                 * 将 UP 事件传递给 RecyclerView, 认为是点击
                 * 否则将事件改成 CANCEL,为了去掉 item_press_selector
                 * */
                if (isTouchedOnRecyclerView(currentX)) {
                    if (((Math.abs(startX - lastX) > 10) && !isRecyclerViewMoving)) {
                        recyclerViewEvent.setAction(MotionEvent.ACTION_CANCEL);
                    }
                    recyclerView.dispatchTouchEvent(recyclerViewEvent);
                    performClick();

                }

                if (!isSelfConsumer && inMoreLayout(event)) {
                    moreLayout.dispatchTouchEvent(eventMoreClone);
                }

                velocityTracker.computeCurrentVelocity(1000);
                 mVelocityX = velocityTracker.getXVelocity();
                velocityTracker.recycle();
                velocityTracker = null;

                log("xVelocity="+mVelocityX);

                //回弹,惯性处理
                if (isSelfConsumer) {
                    if (scrollX > getFirstThreshold() && scrollX <= getSecondThreshold()) {
                        mScroller.startScroll(getScrollX(), 0, -scrollX + getFirstThreshold(), 0);

                    } else if (scrollX > getSecondThreshold()) {
                        mScroller.startScroll(getScrollX(), 0, -getScrollX() + getFirstThreshold(), 0);
                        if (mCoordinateListener != null) {
                            mCoordinateListener.onMore();
                        }
                    }

                    if ((startX - currentX < 0) && scrollX > 0&&mVelocityX>0) {//右化惯性
                        mScroller.fling(getScrollX(), 0, (int) -mVelocityX, 0, 0, this.getMeasuredWidth(), 0, 0);
                    }
                }

                more.setText("更多");
                icon.setRotation(0);
                //重置更多平移位置
                moreLayout.setTranslationX(0);
                postInvalidate();
                break;
        }

        recyclerViewEvent.recycle();
        eventMoreClone.recycle();
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.computeScrollOffset() && enabledMore) {

            int nextX = mScroller.getCurrX();
            log("computeScroll# nextX=" + nextX);

            scrollTo(nextX, 0);
            postInvalidate();
        } else {
            log("computeScroll# scrolled end");
        }
    }

    private boolean inMoreLayout(MotionEvent event) {
        boolean y = moreLayout.getTop() <= event.getY() && event.getY() <= moreLayout.getBottom();
        float moreEventX = event.getX() + getScrollX();
        boolean x = moreLayout.getLeft() <= moreEventX && moreEventX <= moreLayout.getRight();
        return x && y;
    }


    private void dealBySelf(float dx, float moveX) {
        log("dealBySelf");
        if (!enabledMore) return;
        // 做一个小的迟钝处理, 当手指横向发生一定的距离时再开始滑动
        if (!isSelfConsumer && moveX < SCROLL_DELAY_DISTANCE) return;

        if (dx < 0 && getScrollX() + dx < 0) {//右滑，如果加dx后列表到了头部，则滚动到头部
            scrollTo(0, 0);

        } else {
            scrollBy((int) dx, 0);
        }

        dealMoreLayout(dx, getScrollX());
    }


    private void dealMoreLayout(float dx, int scrollX) {
        //平移更多布局
        if (scrollX > getFirstThreshold()) { //按0.5比例平移
            float v = (float) ((dx) * 0.6) + moreLayout.getTranslationX();
            log("dealMoreLayout dx=" + dx);
            log("dealMoreLayout v=" + v);
            float translationX = v;
            if (scrollX > getSecondThreshold()) {
                if (!isChanged) {
                    translationX = v - getCharacterWidth();
                    isChanged = true;
                    //震动
                    vibrate();
                }
            } else {
                if (isChanged) {
                    translationX = v + getCharacterWidth();
                    isChanged = false;
                }
            }

            moreLayout.setTranslationX(translationX);

            //箭头旋转 (滚动距离从阈值1转到阈值2为止，在此之间箭头转了180度)
            rotationIcon(scrollX);
        }

        changeMoreName(scrollX);
    }

    /**
     * @param scrollX 修改"更多"按钮的名字
     */
    private void changeMoreName(int scrollX) {
        if (scrollX < getSecondThreshold()) {
            more.setText("更多");
            log("dealMoreLayout，show more");
        } else {//转圈完成
            more.setText("松开查看");
            log("dealMoreLayout，show release to view");
        }
    }

    /**
     * 箭头旋转
     *
     * @param scrollX
     */
    private void rotationIcon(int scrollX) {
        int rotation = 180 * (scrollX - getFirstThreshold()) / (getSecondThreshold() - getFirstThreshold());

        if (rotation >= 180) {
            rotation = 180;
        } else if (rotation < -180) {
            rotation = -180;
        }
        log("rotation=" + rotation);
        icon.setRotation(rotation);
    }


    private boolean isTouchedOnRecyclerView(float currentX) {
        boolean b = currentX <= recyclerView.getMeasuredWidth() - getScrollX();
        log("isTouchedOnRecyclerView=" + b);
        return b;
    }


    private static boolean isSlideToRight(RecyclerView recyclerView) {

        if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) ((RecyclerView) recyclerView).getLayoutManager();
            if (staggeredGridLayoutManager != null) {
                int[] aa = staggeredGridLayoutManager.findLastCompletelyVisibleItemPositions(null);
                //当前RecyclerView的所有子项个数
                int totalItemCount = staggeredGridLayoutManager.getItemCount();
                return aa[0] > totalItemCount - 1 - aa.length;
            }

        } else {
            throw new RuntimeException("only supported to staggeredGridLayoutManager, Temporarily");
        }

        return false;

    }


    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        //监听滚动距离, 更新刷新区域内容
        isSelfConsumer = true;
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) this.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {100, 100};
        vibrator.vibrate(pattern, -1);
    }

    private void log(String log) {
        Log.e(TAG, log);
    }

    /**
     * 点击更多时以及左滑到阈值时回调
     */
    interface CoordinateListener {
        public void onMore();
    }

}
