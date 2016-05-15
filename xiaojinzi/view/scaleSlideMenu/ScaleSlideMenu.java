package xiaojinzi.view.scaleSlideMenu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

import com.example.cxj.fragmentscalemenu.util.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

import xiaojinzi.view.common.RectEntity;


/**
 * Created by cxj on 2016/5/15.
 *
 * @author 小金子
 *         这个是一个自定义的层叠式的侧滑菜单
 *         请遵循一下使用原则:
 *         1.如果是两个孩子的情况
 *         第一个孩子是菜单,默认是左边
 *         第二个是主界面
 *         2.如果是三个孩子的
 *         第一个孩子是左边的菜单
 *         第二个孩子是右边的菜单
 *         第三个孩子是主界面
 *         3.超过三个孩子或者小于两个孩子都会报错
 */
public class ScaleSlideMenu extends ViewGroup {

    //=================================常量 start =====================================

    /**
     * 最大的距离生效的百分比
     */
    public static final float MAX_SLIDEPERCENT = 0.4f;

    /**
     * 最小的距离生效的百分比
     */
    public static final float MIN_SLIDEPERCENT = 0.2f;

    /**
     * 侧滑的时候,当触摸的x距离屏幕小于屏幕宽度的10%的时候会生效
     */
    public static final float DEFALUT_SLIDEPERCENT = MIN_SLIDEPERCENT;

    /**
     * 普通的模式
     */
    public static final int NORMAL_SLIDE_MODE = 0;

    //=================================常量 end =====================================

    public ScaleSlideMenu(Context context) {
        this(context, null);
    }

    public ScaleSlideMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleSlideMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化
     *
     * @param context
     */
    private void init(Context context) {
        this.context = context;
        scroller = new Scroller(context);
        vt = VelocityTracker.obtain();
        screenWidth = ScreenUtils.getScreenWidth(context);
    }

    //======================================成员变量 start===========================================

    /**
     * 屏幕的宽度
     */
    private int screenWidth;

    /**
     * 上下文对象
     */
    private Context context;

    /**
     * 触摸移动的时候需要平滑的移动的数据计算工具类
     */
    private Scroller scroller;

    /**
     * 用于计算触摸的时候的速度
     */
    private VelocityTracker vt = null;

    /**
     * 平滑移动动画的时长
     */
    private int defalutDuring = 800;

    /**
     * 菜单是不是打开状态
     */
    private boolean isMenuOpen;

    /**
     * 菜单状态状态改变之前的状态
     */
    private boolean preIsMenuOpen;

    /**
     * 自身的宽
     */
    private int mWidth;

    /**
     * 自身的高
     */
    private int mHeight;

    //======================================成员变量 end===========================================

    //======================================重写方法 start===========================================

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        mWidth = getWidth();
        mHeight = getHeight();
    }

    @Override
    public void computeScroll() { //当View完成滚动的时候调用
        if (scroller.computeScrollOffset()) {
            removeRepeatData();//消除重复数据
            //如果完成了滑动
            if (getScrollX() == scroller.getFinalX() && scroller.getCurrX() == scroller.getFinalX()) {
                //标识停止滚动了
                isScrolling = false;
                if (onMenuStateListener != null && preIsMenuOpen != isMenuOpen) {
                    preIsMenuOpen = isMenuOpen;
                    onMenuStateListener.onMenuState(isMenuOpen);
                }
            }
            scrollTo(scroller.getCurrX(), 0);
            scaleMainView();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN: //按下

                //获取到按下的时候的相对于屏幕的横坐标x
                int x = (int) ev.getX();

                //拦截菜单打开的情况下主界面的事件
                if (isMenuOpen && x > Math.abs(rectEntities.get(0).leftX)) {
                    return true;
                }
        }

        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 安排孩子的位置
     *
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //检查孩子个数
        checkChildCount();

        //计算所有孩子的位置信息
        computePosition();

        //获取孩子的个数
        int childCount = getChildCount();
        int size = rectEntities.size();

        for (int i = 0; i < size && i < childCount; i++) {

            View v = getChildAt(i);
            RectEntity rectEntity = rectEntities.get(i);

            v.layout(rectEntity.leftX, rectEntity.leftY, rectEntity.rightX, rectEntity.rightY);
        }

    }

    private int currentX;
    private int currentY;
    private int finalX;
    private int finalY;

    /**
     * 是否移动了
     */
    private boolean isMove;

    /**
     * 是不是侧滑的事件
     */
    private boolean isMyEvent = false;

    /**
     * 是否正在滚动
     */
    private boolean isScrolling;

    /**
     * 事件在经过孩子的事件分发之后才到这里的
     *
     * @param e
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {

        int action = e.getAction(); // screenWidth

        //如果按下的,进行一些判断
        if ((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            float x = e.getX();
            if (x / ((Number) screenWidth).floatValue() < slidePercent) { //如果距离边界的百分比小于设置的百分比
                isMyEvent = true;
            }
        }

        //如果不需要侧滑出菜单,并且菜单是关闭状态
        if (!isMyEvent && !isMenuOpen) {
            return false;
        }

        vt.addMovement(e);

        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN: //按下

                isMove = false;
                currentX = (int) e.getX();
                currentY = (int) e.getY();

                scroller.setFinalX(currentX);
                scroller.abortAnimation();

                break;

            case MotionEvent.ACTION_MOVE: //移动

                finalX = (int) e.getX();
                finalY = (int) e.getY();

                if (finalX != currentX || finalY != currentY) {
                    int dx = finalX - currentX;
                    int dy = finalY - currentY;

                    scrollBy(-dx, 0);

                    scaleMainView();

                    currentX = (int) e.getX();
                    currentY = (int) e.getY();
                    isMove = true;
                }

                break;
            case MotionEvent.ACTION_UP://抬起

                if (isMove) {
                    //计算速度
                    vt.computeCurrentVelocity(1000, Integer.MAX_VALUE);
                    //水平方向的速度
                    float xVelocity = vt.getXVelocity();

                    if (xVelocity > 200) {
                        openMenu();
                    } else if (xVelocity < -200) {
                        closeMenu();
                    } else {
                        judgeShouldSmoothToLeftOrRight();
                    }
                } else {
                    closeMenu();
                }

                vt.clear();
                isMove = false;
                isMyEvent = false;
                break;
        }

        return true;
    }

    //======================================重写方法 end===========================================

    //======================================私有方法 start===========================================


    /**
     * 存储孩子的位置信息,由方法{@link ScaleSlideMenu#computePosition()}计算而来
     */
    private List<RectEntity> rectEntities = new ArrayList<RectEntity>();

    /**
     * 计算所有孩子的位置信息
     *
     * @return
     */
    private void computePosition() {
        rectEntities.clear();

        //获取孩子的个数
        int childCount = getChildCount();

        if (childCount == 2) { //如果是两个孩子的情况
            //第一个孩子是一个菜单
            View menuView = getChildAt(0);

            //闯进第一个孩子的位置参数
            RectEntity menuRect = new RectEntity();
            menuRect.rightX = 0;
            menuRect.leftX = menuRect.rightX - menuView.getMeasuredWidth();
            menuRect.leftY = 0;
            menuRect.rightY = menuRect.leftY + menuView.getMeasuredHeight();

            //第二个孩子是一个主界面
            View mainView = getChildAt(1);

            //闯进第一个孩子的位置参数
            RectEntity mainRect = new RectEntity();
            mainRect.leftX = 0;
            mainRect.rightX = mainRect.leftX + mainView.getMeasuredWidth();
            mainRect.leftY = 0;
            mainRect.rightY = mainRect.leftY + mainView.getMeasuredHeight();

            //添加位置信息到集合
            rectEntities.add(menuRect);
            rectEntities.add(mainRect);

        } else { //如果是三个孩子的情况

        }

    }


    /**
     * 检查孩子个数
     */
    private void checkChildCount() {
        int childCount = getChildCount();
        if (childCount > 3 || childCount < 2) {
            throw new RuntimeException("the childCount must be 2 or 3");
        }
    }

    /**
     * 用于消除滑动的时候出现的重复数据
     * 因为平滑的滑动的时候产生的数据很多都是重复的
     * 都是所以这里如果遇到事重复的就拿下一个,直到不重复为止
     * 1.当前的值{@link View#getScrollX()}不等于{@link Scroller#getCurrX()}
     * 2.
     */
    private void removeRepeatData() {
        //获取当前的滚动的值
        int scrollX = getScrollX();
        //如果当前的值不是最后一个值,并且当前的值等于scroller中的当前值,那么获取下一个值
        while (scrollX != scroller.getFinalX() && scrollX == scroller.getCurrX()) {
            scroller.computeScrollOffset();
        }
    }

    /**
     * 平滑的移动到指定位置
     */
    private void smoothTo(int finalX) {
        isScrolling = true;
//        scroller.abortAnimation();
        scroller.startScroll(getScrollX(), 0, finalX - getScrollX(), 0, defalutDuring);
        removeRepeatData();//消除重复数据
        scrollTo(scroller.getCurrX(), 0);
    }

    /**
     * 缩放主界面
     */
    private void scaleMainView() {
        float percent = ((Number) Math.abs(getScrollX())).floatValue() / ((Number) Math.abs(rectEntities.get(0).leftX)).floatValue();

        getChildAt(1).setScaleX(1f - 0.4f * (percent));
        getChildAt(1).setScaleY(1f - 0.4f * (percent));
    }

    /**
     * 判断当前的位置应该是滑出菜单还是关闭菜单
     */
    private void judgeShouldSmoothToLeftOrRight() {
        //菜单的宽度
        int menuLeftX = rectEntities.get(0).leftX;
        //获取到当前的位置
        int scrollX = getScrollX();
        if (menuLeftX / 2 > scrollX) {
            openMenu();
        } else {
            closeMenu();
        }
    }

    //======================================私有方法 end===========================================


    //============================================用户方法区域 start============================================

    /**
     * 侧滑菜单生效的距离百分比
     */
    private float slidePercent = DEFALUT_SLIDEPERCENT;

    /**
     * 设置侧滑的百分比,0-1
     *
     * @param slidePercent 0-1
     */
    public void setSlidePercent(float slidePercent) {
        if (slidePercent > MAX_SLIDEPERCENT) {
            slidePercent = MAX_SLIDEPERCENT;
        }
        if (slidePercent > MIN_SLIDEPERCENT) {
            slidePercent = MIN_SLIDEPERCENT;
        }
        this.slidePercent = slidePercent;
    }

    /**
     * 打开菜单
     */
    public void openMenu() {
        //拿到菜单的位置参数
        RectEntity menuRect = rectEntities.get(0);
        smoothTo(menuRect.leftX);
        preIsMenuOpen = isMenuOpen;
        isMenuOpen = true;
    }

    /**
     * 关闭菜单
     */
    public void closeMenu() {
        smoothTo(0);
        preIsMenuOpen = isMenuOpen;
        isMenuOpen = false;
    }

    //============================================用户方法区域 end============================================
    //============================================用户接口区域 start==========================================

    /**
     * 菜单状态的监听接口
     */
    public interface OnMenuStateListener {
        /**
         * 回调的方法
         *
         * @param state
         */
        public void onMenuState(boolean state);
    }

    /**
     * 菜单状态的监听接口
     */
    private OnMenuStateListener onMenuStateListener = null;

    /**
     * 设置菜单的状态监听
     *
     * @param onMenuStateListener
     */
    public void setOnMenuStateListener(OnMenuStateListener onMenuStateListener) {
        this.onMenuStateListener = onMenuStateListener;
    }

    //============================================用户接口区域 end============================================

}
