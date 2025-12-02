package com.example.android.notepad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Custom EditText that draws lines between each line of text
 */
public class LinedEditText extends EditText {
    private Rect mRect;
    private Paint mPaint;

    public LinedEditText(Context context) {
        super(context);
        init();
    }

    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LinedEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mRect = new Rect();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(0xFFE0E0E0); // Light gray lines
        mPaint.setStrokeWidth(1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int lineCount = getLineCount();
        int lineHeight = getLineHeight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int viewHeight = getHeight();

        // Get the baseline for the first line
        getLineBounds(0, mRect);
        int baseline = mRect.top;

        // Draw horizontal lines for each line of text
        for (int i = 0; i < lineCount; i++) {
            int lineY = paddingTop + baseline + (i * lineHeight);
            // Stop drawing if we're beyond the visible area
            if (lineY > viewHeight - paddingBottom) {
                break;
            }
            canvas.drawLine(
                    getPaddingLeft(),
                    lineY + lineHeight - 4,
                    getWidth() - getPaddingRight(),
                    lineY + lineHeight - 4,
                    mPaint
            );
        }

        super.onDraw(canvas);
    }
}