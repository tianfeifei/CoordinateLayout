package com.example.commoncoordinatelayout;


import android.content.Context;
import android.graphics.Paint;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;



import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import static androidx.customview.widget.ViewDragHelper.INVALID_POINTER;


/**
 * 横滑时带有更多交互效果
 * 使用场景：双排横滑卡片，带有更多交互
 *
 * @author feifeitian
 * @date 2020/7/6
 */

public class HorizontalLoadMoreLayout extends LinearLayout {
    public static final String TAG = "HorizontalLoadMoreLayout";
    private static final int SCROLL_DELAY_DISTANCE = 30;
    private boolean isChanged;
    private boolean isRecyclerViewMoving;
    private float startX;
    private float lastX;
    private RecyclerView mRecyclerView;
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

    private VelocityTracker mVelocityTracker;

    private PullToLeftListener mPullToLeftListener;

    private int mInitialTouchX, mInitialTouchY;
    private int mTouchSlop;
    private int mScrollPointerId = INVALID_POINTER;
    private boolean isLanJie;


    public HorizontalLoadMoreLayout(Context context) {
        super(context);
        init(context);
    }

    public HorizontalLoadMoreLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HorizontalLoadMoreLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
     * @param pullToLeftListener
     */
    public void setPullToLeftListener(PullToLeftListener pullToLeftListener) {
        mPullToLeftListener = pullToLeftListener;
    }


    public void setRecyclerView(final RecyclerView recyclerView) {
        this.mRecyclerView = recyclerView;

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (mVelocityX < 0 && isSlideToRight(recyclerView)) {
                    log("onScrolled# is scrolled end");
                    mScroller.fling(getScrollX(), 0, (int) -mVelocityX, 0, 0, getFirstThreshold(), 0, 0);
                    HorizontalLoadMoreLayout.this.invalidate();

                }
            }
        });
    }

    private void init(final Context context) {
        //此处使用post:为了保证加载时序问题，保证view_coordinate_layout后加载
        post(new Runnable() {
            @Override
            public void run() {
                LayoutInflater.from(context).inflate(R.layout.view_coordinate_layout, HorizontalLoadMoreLayout.this);
                more = findViewById(R.id.more);
                icon = findViewById(R.id.icon);
                moreLayout = findViewById(R.id.ll_more);

                initData();
            }
        });

        mScroller = new Scroller(context, null);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }


    private void initData() {
        moreLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                log("onClick# moreLayout is clicked");
                if (mPullToLeftListener != null) {
                    mPullToLeftListener.onMore();
                }

            }
        });
    }

    private int getFirstThreshold() {
        if (mFirstThreshold != -1) {
            return mFirstThreshold;
        }
        return (int) (getCharacterWidth() + icon.getMeasuredWidth() + moreLayout.getPaddingLeft()) + Util.dip2px(this.getContext(),5);

    }

    private float getCharacterWidth() {
        if ((mCharacterWidth == 0)) {
            mCharacterWidth = getCharacterWidth(more, 2);
        }
        return mCharacterWidth;
    }

    private int getSecondThreshold() {
        if (mSecondThreshold != -1) {
            return mSecondThreshold;
        }
        return getFirstThreshold() + Util.dip2px(this.getContext(),60);

    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        log("onTouchEvent#getAction" + event.getAction());

        //MoreLayout的Clone事件
        MotionEvent eventMoreClone = MotionEvent.obtain(event);
        float moreX = event.getX() + getScrollX() - moreLayout.getLeft();
        float moreY = event.getY() - moreLayout.getTop();
        eventMoreClone.setLocation(moreX, moreY);

        //RecyclerView的Clone事件
        MotionEvent recyclerViewEvent = MotionEvent.obtain(event);
        if (mRecyclerView == null) {
            log("mRecyclerView == null");
            return false;
        } else {
            log("mRecyclerView != null");

        }

        float cloneX = event.getX() - mRecyclerView.getLeft() + getScrollX();
        recyclerViewEvent.setLocation(cloneX, recyclerViewEvent.getY());

        float currentX = event.getRawX();
        int scrollX = getScrollX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mRecyclerView.dispatchTouchEvent(recyclerViewEvent);
                if (inMoreLayout(event)) {
                    moreLayout.dispatchTouchEvent(eventMoreClone);
                }
                mScrollPointerId = event.getPointerId(0);
                mInitialTouchX = (int) (event.getX() + 0.5f);
                mInitialTouchY = (int) (event.getY() + 0.5f);

                //重置状态
                isChanged = false;
                moreLayout.setTranslationX(0);
                lastX = startX = currentX;
                mScroller.forceFinished(true);
                isSelfConsumer = false;
                mVelocityX = 0;


                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    mVelocityTracker.clear();
                }
                mVelocityTracker.addMovement(event);


                break;
            case MotionEvent.ACTION_MOVE:
                final int index = event.findPointerIndex(mScrollPointerId);
                if (index < 0) {
                    return false;
                }
                final int x = (int) (event.getX(index) + 0.5f);
                final int y = (int) (event.getY(index) + 0.5f);
                final int dx = x - mInitialTouchX;
                final int dy = y - mInitialTouchY;
                if (Math.abs(dy) > mTouchSlop && Math.abs(dx) < Math.abs(dy)) {
                    log("####e.Math.abs(dx) < Math.abs(dy)" + true);
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(true);

                }

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
                        mRecyclerView.dispatchTouchEvent(recyclerViewEvent);
                        isRecyclerViewMoving = true;
                    }
                } else {
                    log("scroll to left");
                    if (isSlideToRight(mRecyclerView)) {
                        dealBySelf(dX, Math.abs(currentX - startX));
                        isRecyclerViewMoving = false;
                    } else {
                        mRecyclerView.dispatchTouchEvent(recyclerViewEvent);
                        isRecyclerViewMoving = true;
                    }

                }

                lastX = currentX;
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(event);
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
                    mRecyclerView.dispatchTouchEvent(recyclerViewEvent);
                    performClick();

                }

                if (!isSelfConsumer && inMoreLayout(event)) {
                    moreLayout.dispatchTouchEvent(eventMoreClone);
                }
                if (mVelocityTracker != null) {

                    mVelocityTracker.computeCurrentVelocity(1000);
                    mVelocityX = mVelocityTracker.getXVelocity();
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }


                log("xVelocity=" + mVelocityX);

                //回弹,惯性处理
                performInertia(currentX, scrollX);

                more.setText(R.string.more);
                icon.setRotation(0);
                //重置更多平移位置
                moreLayout.setTranslationX(0);
                postInvalidate();
                break;
            default:
                break;
        }

        recyclerViewEvent.recycle();
        eventMoreClone.recycle();


        return true;
    }

    private void performInertia(float currentX, int scrollX) {
        if (isSelfConsumer) {
            if (scrollX > getFirstThreshold() && scrollX <= getSecondThreshold()) {
                mScroller.startScroll(getScrollX(), 0, -scrollX + getFirstThreshold(), 0);

            } else if (scrollX > getSecondThreshold()) {
                mScroller.startScroll(getScrollX(), 0, -getScrollX() + getFirstThreshold(), 0);
                if (mPullToLeftListener != null) {
                    mPullToLeftListener.onMore();
                }
            }

            if ((startX - currentX < 0) && scrollX > 0 && mVelocityX > 0) {//右滑惯性
                //参数说明：往右滑，scrollx是由大到小变化；
                mScroller.fling(getScrollX(), 0, (int) -mVelocityX, 0,
                        0/*最小scrollx坐标,0为未发生滚动的位置；不能设置小与0，因为等于0时，应该recleryView滑动*/,
                        this.getMeasuredWidth(),
                        0, 0);
            }
        }
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
        if (!enabledMore) {
            return;
        }
        // 做一个小的迟钝处理, 当手指横向发生一定的距离时再开始滑动
        if (!isSelfConsumer && moveX < SCROLL_DELAY_DISTANCE) {
            return;
        }


        if (dx < 0 && getScrollX() + dx < 0) {//右滑，如果加dx后列表到了头部，则滚动到头部
            scrollTo(0, 0);

        } else {
            scrollBy((int) dx, 0);
        }

        dealMoreLayout(dx, getScrollX());
    }


    private void dealMoreLayout(float dx, int scrollX) {
        //平移更多布局
        if (scrollX > getFirstThreshold()) {
            //按0.6比例平移
            float nextTransX = (float) ((dx) * 0.6) + moreLayout.getTranslationX();
            log("dealMoreLayout dx=" + dx);
            log("dealMoreLayout nextTransX=" + nextTransX);
            float translationX = nextTransX;
            if (scrollX > getSecondThreshold()) {
                if (!isChanged) {
                    translationX = nextTransX - getCharacterWidth();
                    isChanged = true;
                    //震动
                    vibrate();
                }
            } else {
                if (isChanged) {
                    translationX = nextTransX + getCharacterWidth();
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
            more.setText(R.string.more);
            log("dealMoreLayout，show more");
        } else {//转圈完成
            more.setText(R.string.release_look);
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
        boolean b = currentX <= mRecyclerView.getMeasuredWidth() - getScrollX();
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
        Log.d(TAG, log);
    }


    /**
     * 获取控件内部字体的宽度
     *
     * @param tv
     * @param size
     * @return
     */
    public static float getCharacterWidth(TextView tv, int size) {
        if (null == tv) {
            return 0f;
        }
        float v = getCharacterWidth(tv.getText().toString(), tv.getTextSize()) * tv.getScaleX();
        return v * size;
    }


    private static float getCharacterWidth(String text, float size) {
        if (null == text || "".equals(text)) {
            return 0;
        }
        float width = 0;
        Paint paint = new Paint();
        paint.setTextSize(size);
        float textWidth = paint.measureText(text);//得到总体长度
        width = textWidth / text.length();//每一个字符的长度
        return width;
    }

    /**
     * 点击更多时以及左滑到阈值时回调
     */
    public interface PullToLeftListener {
        public void onMore();
    }

}


