package com.example.commoncoordinatelayout;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Created by 10732 on 2019/3/1.
 */

public class CommonCoordinateLayout extends LinearLayout {
    private boolean isFling;
    private boolean isChanged;
    private boolean isRecyclerViewMoving;
    private float startX;
    private float lastX;
    private RecyclerView recyclerView;
    private View moreLayout;
    private TextView more;
    private LinearLayout ll_icon;
    private boolean isSelfConsumer;
    private int SCROLL_DELAY_DISTANCE;
    private Scroller mScroller;
    private VelocityTracker velocityTracker;
    private int mVelocityX;

    public CommonCoordinateLayout(Context context) {
        super(context);
        init(context);
    }

    public CommonCoordinateLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CommonCoordinateLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        mScroller = new Scroller(context, null);
        SCROLL_DELAY_DISTANCE = 30;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        /*
         * HelpLayout的Clone事件
         * */
        MotionEvent eventMoreClone = MotionEvent.obtain(event);
        float moreX = event.getX() + getScrollX();
        eventMoreClone.setLocation(moreX, event.getRawY());


        /*
         * RecyclerView的Clone事件
         * */
        MotionEvent eventClone = MotionEvent.obtain(event);
        float cloneX = event.getX() - recyclerView.getRight() + getScrollX();
        eventClone.setLocation(cloneX, eventClone.getRawY());


        float currentX = event.getRawX();


        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                isChanged = false;

                recyclerView.dispatchTouchEvent(eventClone);
                if (inMoreLayout(event))
                    moreLayout.dispatchTouchEvent(eventMoreClone);

                lastX = startX = currentX;
                mScroller.forceFinished(true);
                isFling = false;
                isSelfConsumer = false;
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_MOVE:
                float dX = lastX - currentX;

                /*
                 * 如果没有在滚动就将事件传到 moreLayout 里面
                 * */
                if (!isSelfConsumer) {
                    if (inMoreLayout(event))
                        moreLayout.dispatchTouchEvent(eventMoreClone);
                }

                int scrollX = getScrollX();
                Log.e("TFF", "移动距离=" + Math.abs(currentX - startX));
                Log.e("TFF", "移动距离=getScrollX()=" + getScrollX());
                if (dX < 0) {
                    Log.e("TFF", "右滑");
                    /*
                     * 向右滑动时
                     * 当超过MaxScrollX之后, 交给RecyclerView处理
                     * 否则自己处理
                     * */

                    if (scrollX > 0) {
                        dealBySelf(dX, Math.abs(currentX - startX));
                        isRecyclerViewMoving = false;
                        dealMoreLayout(scrollX);
                    } else {
                        Log.e("TFF", "recyclerView处理");
                        recyclerView.dispatchTouchEvent(eventClone);
                        isRecyclerViewMoving = true;
                    }
                } else {
                    Log.e("TFF", "左滑");
                    /*
                     * 向左滑动时
                     * 当RecyclerView没有滑动到最右部时, 交给自己处理
                     * 否则RecyclerView处理
                     *
                     * */
                    if (isSlideToRight(recyclerView)) {
                        Log.e("TFF", "滑到尾部了，自己处理");
                        dealBySelf(dX, Math.abs(currentX - startX));

                        dealMoreLayout(scrollX);
                        isRecyclerViewMoving = false;
                    } else {
                        Log.e("TFF", "recyclerView处理");
                        recyclerView.dispatchTouchEvent(eventClone);
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
                if (isTouchedOnRecyclerView(currentX)) {
                    /*
                     * 在移动Parent的时候,手指移动小于10像素  或者  现在是RecyclerView接管事件的时候
                     * 将 UP 事件传递给 RecyclerView, 认为是点击
                     * 否则将事件改成 CANCEL,为了去掉 item_press_selector
                     * */
                    if ((Math.abs(startX - lastX) < 10 || isRecyclerViewMoving)) {
                        recyclerView.dispatchTouchEvent(eventClone);
                    } else {
                        eventClone.setAction(MotionEvent.ACTION_CANCEL);
                        recyclerView.dispatchTouchEvent(eventClone);
                    }

                    performClick();
                }

                /*
                 * 如果没有在滚动就将事件传到 HelpLayout 里面
                 * */
                if (!isSelfConsumer) {
                    if (inMoreLayout(event))
                        moreLayout.dispatchTouchEvent(eventMoreClone);
                }

                /*
                 * 惯性处理 (不是在操作Banner的时候才进行惯性滑动处理)
                 * */
                if (velocityTracker != null) {
                    velocityTracker.computeCurrentVelocity(2000);
                    float xVelocity = velocityTracker.getXVelocity();

                    velocityTracker.recycle();
                    velocityTracker = null;

                    /*
                     * 如果 <=0 并且不足够刷新, 回到 0 (下滑惯性)
                     * 如果 >=MaxScroll ,回到MaxScroll (上滑惯性)
                     * ADSORPTION_DISTANCE : 一个吸附功能, 在 maxScrollY 附近的距离被吸附
                     * */
                    Log.e("TFF", "移动距离getScrollX()=" + getScrollX());

                    if (getScrollX() < 0) {//左滑，回弹到80，显示更多
                        // mScroller.startScroll(getScrollX(), getScrollY(), -getScrollX() + 50, getScrollY());
                        Log.e("TFF", "悬停，显示更多");


                    } else if (getScrollX() > 250) {//显示松手加载
                        // mScroller.startScroll(getScrollX(), getScrollY(), -getScrollX(), getScrollY());

//                        mScroller.fling(getScrollX(), getScrollY(), (int) -xVelocity, 0, minScrollX-1000, maxScrollX+1000 , 0, 0);//此处不能用抛射
                        Log.e("TFF", "回滚，松开查看");

                    }

                    //回弹
                    if (!isRecyclerViewMoving) {
                        mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0);
                    }

                    //重置更多位置
                    moreLayout.setTranslationX(0);

                    postInvalidate();
                }
                break;
        }

        eventClone.recycle();
        eventMoreClone.recycle();

        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.computeScrollOffset()) {

            int nextX = mScroller.getCurrX();
            Log.e("TFF", "滚动中nextX=" + nextX);

            scrollTo(nextX, 0);
//            if (nextX > maxScrollX + OVER_SCROLL_LENGTH) {
//                //向左 Over Scroll
//                if (isFling) {
//                    mScroller.startScroll(getScrollX(), getScrollY(), -getScrollX(), getScrollY());
//                    isFling = false;
//                }
            if (nextX >= more.getMeasuredWidth()) { //抛射后，由于惯性，当滚动大于200后，自动弹回初始位置
                //向右 Over Scroll
                if (isFling) {
                    mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 1000);

                    isFling = false;
                }
            }


            postInvalidate();
        } else {
            Log.e("TFF", "滚动完成");

        }
    }


    private void dealMoreLayout(int scrollX) {
        if (scrollX > 350) { //固定不动
            moreLayout.setTranslationX(scrollX - 350);
            //todo 箭头开始转圈 (滚动距离从350转到450为止，在此之间箭头转了180度)
        }

        if (getScrollX() <= 450) {
            isFling = true;
            Log.e("TFF", "悬停，显示更多");
            more.setText("更多");


        } else {//转圈完成
            more.setText("松开查看");

            //todo 修改moreLayout大小;往左平移2个字符宽度
            if (true) {
//                                ViewGroup.LayoutParams layoutParams = moreLayout.getLayoutParams();
//                                layoutParams.width += 100;
//                                moreLayout.setLayoutParams(layoutParams);
//                                moreLayout.invalidate();
                                moreLayout.setTranslationX((scrollX - 450));
//                                ll_icon.setTranslationX(100-(scrollX - 450));
                isChanged = true;
            }

        }
    }

    private boolean inMoreLayout(MotionEvent event) {
        if (moreLayout.getTop() <= event.getY() && event.getY() <= moreLayout.getBottom()) {

            return true;
        }
        return false;
    }

    private void dealBySelf(float dx, float moveX) {
        Log.e("TFF", "自己处理");

        /*
         * 做一个小的迟钝处理, 当手指垂直发生一定的距离时再开始滑动
         * 如果 ViewPager 也就是Banner开始滚动了, 就不要在垂直滚动了
         * */
        if (!isSelfConsumer && moveX < SCROLL_DELAY_DISTANCE) return;

//        if (getScaleX() + dx > maxScrollX) {
//            scrollTo(maxScrollX, 0);
//        } else if (getScrollX() + dx < minScrollX) {
//            scrollBy((int) (dx / 1.5f), 0);
//        } else {
//            scrollBy((int) dx, 0);
//        }
        if (dx < 0 && getScrollX() + dx < 0) {//右滑，如果加dx后列表到了头部，则滚动到头部
            scrollTo(0, 0);

        } else {
            scrollBy((int) dx, 0);

        }

    }

    private boolean isTouchedOnRecyclerView(float currentX) {
        boolean b = currentX <= recyclerView.getMeasuredWidth() - getScrollX();
        Log.e("TFF", "isTouchedOnRecyclerView=" + b);

        return b;
    }


    public static boolean isSlideToRight(RecyclerView recyclerView) {

        if (((RecyclerView) recyclerView).getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) ((RecyclerView) recyclerView).getLayoutManager();
            int[] aa = staggeredGridLayoutManager.findLastCompletelyVisibleItemPositions(null);

            //当前RecyclerView的所有子项个数
            int totalItemCount = staggeredGridLayoutManager.getItemCount();
            if (aa[0] > totalItemCount - 1 - aa.length) {
                return true;
            }
        }


        return false;

    }


    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        /*
         * 监听滚动距离, 更新刷新区域内容
         * */
        isSelfConsumer = true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }


    public void setRecyclerView(final RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                isFling = true;
                mVelocityX = velocityX;
                Log.e("TFF", "velocityX=" + velocityX);

                if (velocityX > 0 && isSlideToRight(recyclerView)) {
                    Log.e("TFF", "velocityX到了尾部");

                    if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_SETTLING) {

                    }
                }

                return false;
            }
        });


        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                Log.e("TFF", "onScrolled#dx=" + dx);

                if (mVelocityX > 0 && isSlideToRight(recyclerView)) {
                    Log.e("TFF", "onScrolled#到底了");

                    mScroller.fling(getScrollX(), 0, (int) mVelocityX, 0, more.getMeasuredWidth(), more.getMeasuredWidth() + 1000, 0, 0);
                    CommonCoordinateLayout.this.invalidate();

                }
            }
        });
    }

    public void setMoreLayout(View moreLayout) {
        this.moreLayout = moreLayout;
    }

    public void setMore(TextView textView) {
        this.more = textView;
    }

    public void setLl_icon(LinearLayout ll_icon) {
        this.ll_icon = ll_icon;
    }
}
