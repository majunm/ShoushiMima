package ss.com.shoushimima;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Lock9View extends ViewGroup {
    private static int ERROR_ANIM_DURATION = 340;
    // 灰色小圆点,联动用
    public static int DEF_COLOR = Color.parseColor("#e5e5e5");
    public int errorLineColor;

    /**
     * 输入模式和设置模式,界面张的不一样
     *
     * @param settingMod
     */
    public void setSettingMode(boolean settingMod) {
        isSettingMode = settingMod;
        for (int n = 0; n < getChildCount(); n++) {
            NodeView node = (NodeView) getChildAt(n);
            node.error = false;
            node.setBackgroundDrawable(nodeSrc);
        }
        invalidate();
    }

    /**
     * isSettingMode 默认是设置模式
     */
    public boolean isSettingMode = true; // 设置模式|输入模式
    /**
     * 节点相关定义
     */
    private final List<NodeView> nodeList = new ArrayList<>(); // 已经连线的节点链表

    private float x; // 当前手指坐标x
    private float y; // 当前手指坐标y

    /**
     * 布局和节点样式
     */
    private Drawable nodeSrc;
    private Drawable nodeOnSrc;
    private Drawable nodeErrorOnSrc;
    private float nodeSize; // 节点大小，如果不为0，则忽略内边距和间距属性
    private float nodeAreaExpand; // 对节点的触摸区域进行扩展
    private int nodeOnAnim; // 节点点亮时的动画
    private boolean runAnim = false; // 执行动画,不了
    public int lineColor;
    private float lineWidth;
    private float padding; // 内边距
    private float spacing; // 节点间隔距离

    /**
     * 自动连接中间节点
     */
    private boolean autoLink;

    /**
     * 震动管理器
     */
    private Vibrator vibrator;
    private boolean enableVibrate = false;
    private int vibrateTime;

    /**
     * 画线用的画笔
     */
    private Paint paint;

    /**
     * 结果回调监听器接口
     */
    private GestureCallback callback;

    public interface GestureCallback {

        void onNodeConnected(@NonNull int[] numbers);

        /**
         * 手势完成,输入错误时返回true
         */
        boolean onGestureFinished(@NonNull int[] numbers);

    }

    public void setGestureCallback(@Nullable GestureCallback callback) {
        this.callback = callback;
    }

    /**
     * 构造函数
     */

    public Lock9View(@NonNull Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public Lock9View(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public Lock9View(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Lock9View(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * 初始化
     */
    private void init(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        // 获取定义的属性
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Lock9View, defStyleAttr, defStyleRes);

        nodeSrc = a.getDrawable(R.styleable.Lock9View_lock9_nodeSrc);
        linkage = a.getBoolean(R.styleable.Lock9View_lock9_linkage, false);
        nodeOnSrc = a.getDrawable(R.styleable.Lock9View_lock9_nodeOnSrc);
        nodeErrorOnSrc = a.getDrawable(R.styleable.Lock9View_lock9_node_error_OnSrc); // 错误时
        nodeSize = a.getDimension(R.styleable.Lock9View_lock9_nodeSize, 0);
        nodeAreaExpand = a.getDimension(R.styleable.Lock9View_lock9_nodeAreaExpand, 0);
        nodeOnAnim = a.getResourceId(R.styleable.Lock9View_lock9_nodeOnAnim, 0);
        lineColor = a.getColor(R.styleable.Lock9View_lock9_lineColor, Color.argb(0, 0, 0, 0));
        errorLineColor = a.getColor(R.styleable.Lock9View_lock9_error_lineColor, Color.argb(0, 0, 0, 0));
        lineWidth = a.getDimension(R.styleable.Lock9View_lock9_lineWidth, 0);
        padding = a.getDimension(R.styleable.Lock9View_lock9_padding, 0);
        spacing = a.getDimension(R.styleable.Lock9View_lock9_spacing, 0);

        autoLink = a.getBoolean(R.styleable.Lock9View_lock9_autoLink, false);

        enableVibrate = a.getBoolean(R.styleable.Lock9View_lock9_enableVibrate, false);
        vibrateTime = a.getInt(R.styleable.Lock9View_lock9_vibrateTime, 20);

        a.recycle();
        if (nodeSize > 0) {
            isSettingMode = true;
        }
        // 初始化振动器
        if (enableVibrate && !isInEditMode()) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        // 初始化画笔
        paint = new Paint(Paint.DITHER_FLAG);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(lineWidth);
        paint.setColor(lineColor);
        paint.setAntiAlias(true); // 抗锯齿

        // 构建node
        for (int n = 0; n < 9; n++) {
            NodeView node = new NodeView(getContext(), n + 1);
            addView(node);
        }

        // 清除FLAG，否则 onDraw() 不会调用，原因是 ViewGroup 默认透明背景不需要调用 onDraw()
        setWillNotDraw(false);
    }

    /**
     * 我们让高度等于宽度 - 方法有待验证
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = measureSize(widthMeasureSpec); // 测量宽度
        setMeasuredDimension(size, size);
    }

    /**
     * 测量长度
     */
    private int measureSize(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec); // 得到模式
        int specSize = MeasureSpec.getSize(measureSpec); // 得到尺寸
        switch (specMode) {
            case MeasureSpec.EXACTLY:
            case MeasureSpec.AT_MOST:
                return specSize;
            case MeasureSpec.UNSPECIFIED:
            default:
                return 0;
        }
    }

    /**
     * 在这里进行node的布局
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            if (nodeSize > 0) { // 如果设置nodeSize值，则将节点绘制在九等分区域中心
                float areaWidth = (right - left) / 3;
                for (int n = 0; n < 9; n++) {
                    NodeView node = (NodeView) getChildAt(n);
                    // 获取3*3宫格内坐标
                    int row = n / 3;
                    int col = n % 3;
                    // 计算实际的坐标
                    int l = (int) (col * areaWidth + (areaWidth - nodeSize) / 2);
                    int t = (int) (row * areaWidth + (areaWidth - nodeSize) / 2);
                    int r = (int) (l + nodeSize);
                    int b = (int) (t + nodeSize);
                    node.layout(l, t, r, b);
                }
            } else { // 否则按照分割边距布局，手动计算节点大小
                float nodeSize = (right - left - padding * 2 - spacing * 2) / 3;
                for (int n = 0; n < 9; n++) {
                    NodeView node = (NodeView) getChildAt(n);
                    // 获取3*3宫格内坐标
                    int row = n / 3;
                    int col = n % 3;
                    // 计算实际的坐标，要包括内边距和分割边距
                    int l = (int) (padding + col * (nodeSize + spacing));
                    int t = (int) (padding + row * (nodeSize + spacing));
                    int r = (int) (l + nodeSize);
                    int b = (int) (t + nodeSize);
                    node.layout(l, t, r, b);
                }
            }
        }
    }

    /**
     * 在这里处理手势
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (linkage) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                x = event.getX(); // 这里要实时记录手指的坐标
                y = event.getY();
                NodeView currentNode = getNodeAt(x, y);
                if (currentNode != null && !currentNode.isHighLighted()) { // 碰触了新的未点亮节点
                    if (nodeList.size() > 0) { // 之前有点亮的节点
                        if (autoLink) { // 开启了中间节点自动连接
                            NodeView lastNode = nodeList.get(nodeList.size() - 1);
                            NodeView middleNode = getNodeBetween(lastNode, currentNode);
                            if (middleNode != null && !middleNode.isHighLighted()) { // 存在中间节点没点亮
                                // 点亮中间节点
                                middleNode.setHighLighted(true, true);
                                nodeList.add(middleNode);
                                handleOnNodeConnectedCallback();
                            }
                        }
                    }
                    // 点亮当前触摸节点
                    currentNode.setHighLighted(true, false);
                    nodeList.add(currentNode);
                    handleOnNodeConnectedCallback();
                }
                // 有点亮的节点才重绘
                if (nodeList.size() > 0) {
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (nodeList.size() > 0) { // 有点亮的节点
                    // 手势完成
                    boolean error = handleOnGestureFinishedCallback();
                    if (error) {
                        Toast.makeText(getContext(), "错误", Toast.LENGTH_SHORT).show();
                        error();
                    } else {
                        clear();
                    }
                }
                break;
        }
        return true;
    }

    private void clear() {
        // 清除状态
        nodeList.clear();
        for (int n = 0; n < getChildCount(); n++) {
            NodeView node = (NodeView) getChildAt(n);
            if (node.error) {
                node.error = false;
            }
            node.setHighLighted(false, false);
        }
        // 通知重绘
        invalidate();
    }

    /**
     * 生成当前数字列表
     */
    @NonNull
    private int[] generateCurrentNumbers() {
        int[] numbers = new int[nodeList.size()];
        for (int i = 0; i < nodeList.size(); i++) {
            NodeView node = nodeList.get(i);
            numbers[i] = node.getNumber();
        }
        return numbers;
    }

    /**
     * 每次连接一个点
     */
    private void handleOnNodeConnectedCallback() {
        if (callback != null) {
            callback.onNodeConnected(generateCurrentNumbers());
        }
    }

    /**
     * 手势完成,连词输入错误时返回true
     */
    private boolean handleOnGestureFinishedCallback() {
        if (callback != null) {
            return callback.onGestureFinished(generateCurrentNumbers());
        }
        return false;
    }

    public void error() {
        paint.setColor(errorLineColor);
        for (NodeView view : nodeList) {
            view.error = true;
            view.setHighLighted(view.isHighLighted(), view.isMid);
            invalidate();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                clear();
                paint.setColor(lineColor);
            }
        }, ERROR_ANIM_DURATION);
    }

    /**
     * 系统绘制回调-主要绘制连线
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // 先绘制已有的连线
        for (int n = 1; n < nodeList.size(); n++) {
            NodeView firstNode = nodeList.get(n - 1);
            NodeView secondNode = nodeList.get(n);
            canvas.drawLine(firstNode.getCenterX(), firstNode.getCenterY(), secondNode.getCenterX(), secondNode.getCenterY(), paint);
        }
        // 如果已经有点亮的点，则在点亮点和手指位置之间绘制连线
        if (nodeList.size() > 0) {
            NodeView lastNode = nodeList.get(nodeList.size() - 1);
            canvas.drawLine(lastNode.getCenterX(), lastNode.getCenterY(), x, y, paint);
        }

    }

    /**
     * 获取给定坐标点的Node，返回null表示当前手指在两个Node之间
     */
    private NodeView getNodeAt(float x, float y) {
        for (int n = 0; n < getChildCount(); n++) {
            NodeView node = (NodeView) getChildAt(n);
            if (!(x >= node.getLeft() - nodeAreaExpand && x < node.getRight() + nodeAreaExpand)) {
                continue;
            }
            if (!(y >= node.getTop() - nodeAreaExpand && y < node.getBottom() + nodeAreaExpand)) {
                continue;
            }
            return node;
        }
        return null;
    }

    /**
     * 获取两个Node中间的Node，返回null表示没有中间node
     */
    @Nullable
    private NodeView getNodeBetween(@NonNull NodeView na, @NonNull NodeView nb) {
        if (na.getNumber() > nb.getNumber()) { // 保证 na 小于 nb
            NodeView nc = na;
            na = nb;
            nb = nc;
        }
        if (na.getNumber() % 3 == 1 && nb.getNumber() - na.getNumber() == 2) { // 水平的情况
            return (NodeView) getChildAt(na.getNumber());
        } else if (na.getNumber() <= 3 && nb.getNumber() - na.getNumber() == 6) { // 垂直的情况
            return (NodeView) getChildAt(na.getNumber() + 2);
        } else if ((na.getNumber() == 1 && nb.getNumber() == 9) || (na.getNumber() == 3 && nb.getNumber() == 7)) { // 倾斜的情况
            return (NodeView) getChildAt(4);
        } else {
            return null;
        }
    }

    /**
     * 节点描述类
     */
    private final class NodeView extends View {

        private int number;

        public void setHighLighted(boolean highLighted) {
            this.highLighted = highLighted;
        }

        private boolean highLighted = false;
        public boolean error;
        public boolean isMid;
        private Paint mpaint;
        private Paint mOutPaint;
        private int defColor = DEF_COLOR;
//        public boolean linkages = false; // 妈的,不知道为什么状态不一致,自己持有一个吧

        NodeView(Context context, int number) {
            super(context);
            this.number = number;
            //noinspection deprecation
            inits();
        }

        private void inits() {
            if (isSettingMode) {
                mpaint = new Paint(Paint.DITHER_FLAG);
                mpaint.setStyle(Style.FILL);
                mpaint.setStrokeWidth(lineWidth);
                mpaint.setColor(defColor);
                mpaint.setAntiAlias(true); // 抗锯齿
                mOutPaint = new Paint(Paint.DITHER_FLAG);
                mOutPaint.setStyle(Style.STROKE);
                mOutPaint.setStrokeWidth(1); // 1
                mOutPaint.setColor(lineColor);
                mOutPaint.setAntiAlias(true); // 抗锯齿
            } else {
                setBackgroundDrawable(nodeSrc);
            }
        }

        boolean isHighLighted() {
            return highLighted;
        }

        void setHighLighted(boolean highLighted, boolean isMid) {
            this.isMid = isMid;

            if (this.highLighted != highLighted || error) {
                this.highLighted = highLighted;
                if (error) {
                    enableVibrate = true; // 打开震动
                    if (nodeErrorOnSrc != null) { // 没有设置高亮图片则不变化
                        if (isSettingMode) {
                            setBackgroundDrawable(highLighted ? nodeErrorOnSrc : null);
                        } else {
                            setBackgroundDrawable(highLighted ? nodeErrorOnSrc : nodeSrc);
                        }
                    }
                } else {
                    enableVibrate = false; // 关闭震动
                    if (nodeOnSrc != null) { // 没有设置高亮图片则不变化
                        if (isSettingMode) {
                            setBackgroundDrawable(highLighted ? nodeOnSrc : null);
                        } else {
                            setBackgroundDrawable(highLighted ? nodeOnSrc : nodeSrc);
                        }
                    }
                }

                if (nodeOnAnim != 0) { // 播放动画
                    if (runAnim) {
                        if (highLighted) {
                            startAnimation(AnimationUtils.loadAnimation(getContext(), nodeOnAnim));
                        } else {
                            clearAnimation();
                        }
                    } else {

                    }
                }
                if (enableVibrate && !isMid) { // 震动
                    if (highLighted) {
                        // vibrator.vibrate(vibrateTime); // 需要权限
                    }
                }
            }
        }

        int getCenterX() {
            return (getLeft() + getRight()) / 2;
        }

        int getCenterY() {
            return (getTop() + getBottom()) / 2;
        }

        int getNumber() {
            return number;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // canvas.drawColor(Color.RED); // 用来辅助
            System.out.println("====设置模式====" + isSettingMode);
            System.out.println("====联动状态====" + linkage + "|" + linkage);
            System.out.println("====高亮状态====" + highLighted);
            if (isSettingMode) {
                if (linkage) {
                    if (highLighted) {
                        mpaint.setColor(lineColor);
                        mOutPaint.setColor(lineColor);
                        mOutPaint.setStyle(Style.STROKE);
                        canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 4, mpaint);
                        canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2 - 2, mOutPaint); // -2 随便弄个数,不要在意
                    } else {
                        mOutPaint.setStyle(Style.FILL);
                        mOutPaint.setColor(Color.WHITE);
                        canvas.drawRect(0, 0, getWidth(), getHeight(), mOutPaint);
                        mpaint.setColor(defColor);
                        canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 4, mpaint);
                    }
                    //canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2, mOutPaint); // 外环先画
                    setBackgroundDrawable(null); // 清空背景
                } else {
                    if (highLighted) {

                    } else {
                        mpaint.setColor(defColor);
                        float radius = getContext().getResources().getDimension(R.dimen.small_icon_radius);
                        canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, mpaint);
                    }
                }
            }
        }
    }

    public boolean linkage = false;

    /**
     * 联动
     */
    public void autoLinkage(int[] numbers) {
        linkage = true;
        System.out.println("====开始联动====" + getChildCount() + "||||====" + linkage);
        isSettingMode = true; // 联动时 = isSettingMode
        for (int n = 0; n < numbers.length; n++) {
            NodeView node = (NodeView) getChildAt(numbers[n] - 1);
            node.setHighLighted(true, false); // 高亮
        }
        invalidate();
    }

    /**
     * 清空联动
     * 日志如下:
     * ====联动状态============true
     * 03-01 19:48:15.050 31770 31770 I System.out: ====绘制了====true
     * 03-01 19:48:15.051 31770 31770 I System.out: ====联动状态====false
     * 03-01 19:48:15.051 31770 31770 I System.out: ====高亮状态====false
     */
    public void clearLikage() {
        linkage = true; // 设置了true,node对象不同步
        System.out.println("====联动状态============" + linkage);
        for (int n = 0; n < getChildCount(); n++) {
            NodeView node = (NodeView) getChildAt(n);
            System.out.println("=>>>>>>>>" + node);
            node.setHighLighted(false);
            node.invalidate();
        }

    }
}

