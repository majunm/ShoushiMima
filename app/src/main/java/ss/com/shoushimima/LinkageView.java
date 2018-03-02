package ss.com.shoushimima;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

/**
 * 时间：2018/3/1 下午5:10
 * 联动: view
 */

public class LinkageView extends View {
    private Paint mpaint;
    private Paint mOutPaint;
    public int defColor = Lock9View.DEF_COLOR;
    public int linkageColor = Color.BLUE;

    public void init(Context context) {
        mpaint = new Paint(Paint.DITHER_FLAG);
        mpaint.setStyle(Paint.Style.FILL);
        mpaint.setAntiAlias(true); // 抗锯齿
        mpaint.setColor(defColor);
        mOutPaint = new Paint(Paint.DITHER_FLAG);
        mOutPaint.setStyle(Paint.Style.STROKE);
        mOutPaint.setStrokeWidth(1); // 1
        mOutPaint.setAntiAlias(true); // 抗锯齿
        mOutPaint.setColor(linkageColor);
    }

    public LinkageView(Context context) {
        super(context);
        init(context);
    }

    public LinkageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LinkageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LinkageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isClearLinkageStatus) {
            mOutPaint.setStyle(Paint.Style.FILL);
            mOutPaint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, getWidth(), getHeight(), mOutPaint);
            // view 的一半
            mpaint.setColor(defColor);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 4, mpaint);
        } else {
            mpaint.setColor(linkageColor);
            mOutPaint.setColor(linkageColor);
            mOutPaint.setStyle(Paint.Style.STROKE); // 空心
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 4, mpaint);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2 - 1, mOutPaint); // -1 让圆小一点
        }
    }

    public boolean isClearLinkageStatus = true;

    public void autoLinkage(int linkageColor) {
        if (linkageColor != 0 && this.linkageColor != linkageColor) {
            this.linkageColor = linkageColor;
        }
        isClearLinkageStatus = false;
        invalidate();
    }

    public void clearLinkage() {
        isClearLinkageStatus = true;
        invalidate();
    }
}
