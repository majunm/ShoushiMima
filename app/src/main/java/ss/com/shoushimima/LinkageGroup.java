package ss.com.shoushimima;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间：2018/3/1 下午5:10
 * 邮箱：747673016@qq.com
 * 联动布局出现的原因 大小可控
 */

public class LinkageGroup extends LinearLayout {
    public List<LinkageView> mLinkageViews = new ArrayList<>(); // 数值集合

    public void init(Context context) {
        setOrientation(VERTICAL);
        setBackgroundColor(Color.WHITE);
        View.inflate(context, R.layout.linkage_group_layout, this);
        for (int x = 0; x < getChildCount(); x++) {
            View view = getChildAt(x);
            if (view instanceof LinearLayout) {
                LinearLayout childs = (LinearLayout) view;
                for (int n = 0; n < childs.getChildCount(); n++) {
                    LinkageView linkage = (LinkageView) childs.getChildAt(n);
                    mLinkageViews.add(linkage);
                }
            }
            System.out.println(x);
        }
        System.out.println(mLinkageViews.size());
    }

    public LinkageGroup(Context context) {
        super(context);
        init(context);
    }

    public LinkageGroup(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LinkageGroup(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LinkageGroup(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // canvas.drawColor(Color.RED);
    }

    /**
     * 联动
     * 联动颜色<br/>
     */
    public void autoLinkage(int[] numbers, int linkageColor) {
        try {
            for (int n = 0; n < numbers.length; n++) {
                LinkageView linkageView = mLinkageViews.get(numbers[n] - 1);
                linkageView.autoLinkage(linkageColor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearLinkage() {
        for (LinkageView linkageView : mLinkageViews) {
            linkageView.clearLinkage();
        }
    }
}
