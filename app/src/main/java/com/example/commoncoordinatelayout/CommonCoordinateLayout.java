package com.example.commoncoordinatelayout;

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * Created by feifeitian on 2020/7/6.
 */

public class CommonCoordinateLayout extends LinearLayout {
    public static final String TAG = "TFF";
    private static final int SCROLL_DELAY_DISTANCE = 30;

    private boolean isFling;
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
    private int mVelocityX;
    private int mFirstThreshold = -1;
    private int mSecondThreshold = -1;
    private float mCharacterWidth;

    private CoordinateListener mCoordinateListener;


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


    public void setCoordinateListener(CoordinateListener coordinateListener) {
        mCoordinateListener = coordinateListener;
    }

    public int getFirstThreshold() {
        if (mFirstThreshold != -1) {
            return mFirstThreshold;
        } else {
            //更多按钮宽度+箭头宽度
            float measuredWidth = getCharacterWidth();
            return (int) (measuredWidth + icon.getMeasuredWidth() + moreLayout.getPaddingLeft());
        }
    }

    private float getCharacterWidth() {
        if (mCharacterWidth > 0) {
            return mCharacterWidth;
        } else {
            mCharacterWidth = Util.getCharacterWidth(more, 2);
            return mCharacterWidth;
        }

    }

    public int getSecondThreshold() {
        if (mSecondThreshold != -1) {
            return mSecondThreshold;
        } else {
            //此阈值为显示"松手查看"的临界值，阈值1到阈值2为箭头旋转的过程，
            return getFirstThreshold() + Util.dip2px(this.getContext(), 60);
        }
    }

    private void init(Context context) {
        post(new Runnable() {
            @Override
            public void run() {
                LayoutInflater.from(CommonCoordinateLayout.this.getContext()).inflate(R.layout.view_coordinate_layout, CommonCoordinateLayout.this);
                more = findViewById(R.id.more);
                icon = findViewById(R.id.icon);
                moreLayout = findViewById(R.id.ll_more);

                initData();
            }
        });

        mScroller = new Scroller(CommonCoordinateLayout.this.getContext(), null);

    }


    private void initData() {
        moreLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, " on Click moreLayout");
                if (mCoordinateListener != null) {
                    mCoordinateListener.onMore();
                }

            }
        });


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
        Log.e(TAG, "[onTouchEvent] event.getX()" + event.getX());
        Log.e(TAG, "[onTouchEvent] moreX" + moreX);
        Log.e(TAG, "[onTouchEvent] getRawX" + event.getRawX());

        //RecyclerView的Clone事件
        MotionEvent eventClone = MotionEvent.obtain(event);
        float cloneX = event.getX() - recyclerView.getLeft() + getScrollX();
        eventClone.setLocation(cloneX, eventClone.getY());

        float currentX = event.getRawX();
        int scrollX = getScrollX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                isChanged = false;
                //重置更多平移位置
                moreLayout.setTranslationX(0);
                lastX = startX = currentX;
                mScroller.forceFinished(true);
                isFling = false;
                isSelfConsumer = false;

                recyclerView.dispatchTouchEvent(eventClone);
                if (inMoreLayout(event)) {
                    moreLayout.dispatchTouchEvent(eventMoreClone);
                    Log.e(TAG, "moreLayout down");

                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dX = lastX - currentX;


                // 如果没有在滚动就将事件传到 moreLayout 里面
                if (!isSelfConsumer && inMoreLayout(event)) {
                    moreLayout.dispatchTouchEvent(eventMoreClone);
                    Log.e(TAG, "moreLayout move");

                }

                Log.e(TAG, "Math.abs(currentX - startX)=" + Math.abs(currentX - startX));
                Log.e(TAG, "getScrollX()=" + scrollX);

                if (dX < 0) {
                    Log.e(TAG, "右滑");
                    /*
                     * 向右滑动时
                     * 当超过MaxScrollX之后, 交给RecyclerView处理
                     * 否则自己处理
                     * */

                    if (scrollX > 0) {
                        dealBySelf(dX, Math.abs(currentX - startX));
                        isRecyclerViewMoving = false;
                    } else {
                        Log.e(TAG, "recyclerView处理");
                        recyclerView.dispatchTouchEvent(eventClone);
                        isRecyclerViewMoving = true;
                    }
                } else {
                    Log.e(TAG, "左滑");
                    /*
                     * 向左滑动时
                     * 当RecyclerView没有滑动到最右部时, 交给自己处理
                     * 否则RecyclerView处理
                     *
                     * */
                    if (isSlideToRight(recyclerView)) {
                        Log.e(TAG, "滑到尾部了，自己处理");
                        dealBySelf(dX, Math.abs(currentX - startX));

                        isRecyclerViewMoving = false;
                    } else {
                        Log.e(TAG, "recyclerView处理");
                        recyclerView.dispatchTouchEvent(eventClone);
                        isRecyclerViewMoving = true;
                    }

                }

                lastX = currentX;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                /*
                 * 在移动Parent的时候,手指移动小于10像素  或者  现在是RecyclerView接管事件的时候
                 * 将 UP 事件传递给 RecyclerView, 认为是点击
                 * 否则将事件改成 CANCEL,为了去掉 item_press_selector
                 * */
                if (isTouchedOnRecyclerView(currentX)) {
                    if ((Math.abs(startX - lastX) < 10 || isRecyclerViewMoving)) {
                        recyclerView.dispatchTouchEvent(eventClone);
                    } else {
                        eventClone.setAction(MotionEvent.ACTION_CANCEL);
                        recyclerView.dispatchTouchEvent(eventClone);
                    }

                    performClick();

                }

                if (!isSelfConsumer && inMoreLayout(event)) {
                    moreLayout.dispatchTouchEvent(eventMoreClone);
                    Log.e(TAG, "moreLayout up");

                }


                //回弹,惯性处理
                if (isSelfConsumer) {
                    if (scrollX > getFirstThreshold() && scrollX <= getSecondThreshold()) {//左滑，回弹到阈值1，显示更多
                        mScroller.startScroll(getScrollX(), 0, -scrollX + getFirstThreshold(), 0);
                        Log.e(TAG, "悬停，显示更多");


                    } else if (scrollX > getSecondThreshold()) {//显示松手加载
                        mScroller.startScroll(getScrollX(), 0, -getScrollX() + getFirstThreshold(), 0);
//                        mScroller.fling(getScrollX(), getScrollY(), (int) -xVelocity, 0, minScrollX-1000, maxScrollX+1000 , 0, 0);//此处不能用抛射
                        Log.e(TAG, "回滚，松开查看");

                    }


                }

                more.setText("更多");
                icon.setRotation(0);
                //重置更多平移位置
                moreLayout.setTranslationX(0);
                postInvalidate();

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
            Log.e(TAG, "滚动中nextX=" + nextX);

            scrollTo(nextX, 0);
// TODO 不能删除 和todo2号强相关
//            if (nextX >= more.getMeasuredWidth()) { //抛射后，由于惯性，当滚动大于200后，自动弹回初始位置
//                //向右 Over Scroll
//                if (isFling) {
//                    mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 1000);
//
//                    isFling = false;
//                }
//            }


            postInvalidate();
        } else {
            Log.e(TAG, "滚动完成");


        }
    }

    private boolean inMoreLayout(MotionEvent event) {
        boolean y = moreLayout.getTop() <= event.getY() && event.getY() <= moreLayout.getBottom();
        Log.e(TAG, "inMoreLayout y=" + y);
        float moreEventX = event.getX() + getScrollX();
        boolean x = moreLayout.getLeft() <= moreEventX && moreEventX <= moreLayout.getRight();
        Log.e(TAG, "inMoreLayout x=" + x);

        return x && y;
    }


    private void dealBySelf(float dx, float moveX) {
        Log.e(TAG, "dealBySelf");

        // 做一个小的迟钝处理, 当手指垂直发生一定的距离时再开始滑动
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
        if (scrollX > getFirstThreshold()) { //按0.2比例平移
            float v = (float) ((dx) * 0.5) + moreLayout.getTranslationX();
            Log.e(TAG, "dealMoreLayout v=" + v);
            float translationX = v;
            if (scrollX > getSecondThreshold()) {
                if (!isChanged) {
                    Log.e(TAG, "dealMoreLayout more=" + getCharacterWidth());

                    translationX = v - getCharacterWidth();
                    isChanged = true;

                    vibrate();
                }
            } else {
                if (isChanged) {
                    Log.e(TAG, "dealMoreLayout more=" + getCharacterWidth());

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
            Log.e(TAG, "dealMoreLayout，显示更多");
            more.setText("更多");

        } else {//转圈完成
            more.setText("松开查看");
            Log.e(TAG, "dealMoreLayout，松开查看");
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
        Log.e(TAG, "rotation=" + rotation);
        icon.setRotation(rotation);
    }


    private boolean isTouchedOnRecyclerView(float currentX) {
        boolean b = currentX <= recyclerView.getMeasuredWidth() - getScrollX();
        Log.e(TAG, "isTouchedOnRecyclerView=" + b);
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
        //监听滚动距离, 更新刷新区域内容
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
                Log.e(TAG, "velocityX=" + velocityX);

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
                Log.e(TAG, "onScrolled#dx=" + dx);

                if (mVelocityX > 0 && isSlideToRight(recyclerView)) {
                    Log.e(TAG, "onScrolled#到底了");
                    //TODO （2号）惯性走位 滑动recyclerVeiw到尾部后，由于惯性要继续前进，最大前进距离为"更多+箭头宽度"
                    mScroller.fling(getScrollX(), 0, (int) mVelocityX, 0, 0, getFirstThreshold(), 0, 0);
                    CommonCoordinateLayout.this.invalidate();

                }
            }
        });
    }


    private void vibrate() {
        Vibrator vibrator = (Vibrator) this.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {100, 100};
        vibrator.vibrate(pattern, -1);

    }


    interface CoordinateListener {
        public void onMore();
    }

}
