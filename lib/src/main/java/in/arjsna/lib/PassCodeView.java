package in.arjsna.lib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;

/**
 * Created by arjun on 8/2/16.
 */
public class PassCodeView extends View {
    private final int KEYS_COUNT = 12;
    private final String eraseChar = "\u232B";
    private int digits;
    private int filledCount;
    private Bitmap filledDrawable;
    private Bitmap emptyDrawable;
    private Paint paint;
    private int DEFAULT_DRAWABLE_DIM = 80;
    private int DEFAULT_VIEW_HEIGHT = 200;
    private int DRAWABLE_PADDING = 100;
    private int drawableWidth;
    private int drawableHeight;
    private int drawableStartX;
    private int drawableStartY;
    private static final int DIGIT_PADDING = 40;
    private int kpStartX;
    private int kpStartY;
    private ArrayList<KeyRect> keyRects = new ArrayList<>();
    private int keyWidth;
    private int keyHeight;
    private String passCodeText = "";
    private TextChangeListener textChangeListener;
    private boolean skipKepDraw = false;
    private boolean isPointerMoved = false;
    private int touchSlope;
    private int touchX = 0;
    private int touchY = 0;

    public PassCodeView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public PassCodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public PassCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public PassCodeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray values = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.PassCodeView, defStyleAttr, defStyleRes);
        try {
            digits = values.getInteger(R.styleable.PassCodeView_digits, 4);
            filledCount = values.getInteger(R.styleable.PassCodeView_filled_count, 0);
            filledDrawable = getBitmap(values.getResourceId(R.styleable.PassCodeView_filled_drawable, -1));
            emptyDrawable = getBitmap(values.getResourceId(R.styleable.PassCodeView_empty_drawable, -1));
            drawableWidth = DEFAULT_DRAWABLE_DIM;
            drawableHeight = DEFAULT_DRAWABLE_DIM;
        } catch (Exception e) {
            e.printStackTrace();
        }
        paint = new Paint(TextPaint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        values.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlope = configuration.getScaledTouchSlop();
                ;
    }

    private void computeDrawableStartXY() {
        int totalDrawableWidth = digits * drawableWidth;
        int totalPaddingWidth = DIGIT_PADDING * (digits - 1);
        int totalReqWidth = totalDrawableWidth + totalPaddingWidth;
        drawableStartX = getMeasuredWidth() / 2 - totalReqWidth / 2;
        drawableStartY = (drawableHeight + DRAWABLE_PADDING) / 2 - drawableHeight / 2;
        computeKeyboardStartXY();
    }

    private void computeKeyboardStartXY() {
        kpStartX = 0;
        kpStartY = drawableHeight + DRAWABLE_PADDING;
        keyWidth = getMeasuredWidth() / 3;
        keyHeight = (getMeasuredHeight() - (drawableHeight + 2 * DRAWABLE_PADDING)) / 4;
        initialiseKeyRects();
    }

    private void initialiseKeyRects() {
        keyRects.clear();
        int x = kpStartX, y = kpStartY;
        for (int i = 1 ; i <= KEYS_COUNT ; i ++) {
            keyRects.add(
                    new KeyRect(new Rect(x, y, x + keyWidth, y + keyHeight),
                    String.valueOf(i)));
            x = x + keyWidth;
            if (i % 3 == 0) {
                y = y + keyHeight;
                x = kpStartX;
            }
        }
        // TODO: 8/3/16 looks bad
        keyRects.get(9).setValue("");
        keyRects.get(10).setValue("0");
        keyRects.get(11).setValue(eraseChar);
    }

    private Bitmap getBitmap(int resId) {
        Drawable drawable = getResources().getDrawable(resId);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(DEFAULT_DRAWABLE_DIM, DEFAULT_DRAWABLE_DIM, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, DEFAULT_DRAWABLE_DIM, DEFAULT_DRAWABLE_DIM);
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.argb(255, 0, 0, 0));
        drawCodeText(canvas);
        drawKeyPad(canvas);
    }

    private void drawKeyPad(Canvas canvas) {
        paint.setTextSize(getResources().getDimension(R.dimen.key_text_size));
//        paint.setTypeface()
        Log.i("Keypad drawn", "Keypad drawn");
        for (KeyRect rect : keyRects) {
            canvas.drawText(rect.value, rect.rect.centerX(), rect.rect.centerY(), paint);
        }
    }

    private void drawCodeText(Canvas canvas) {
        int x = drawableStartX, y = drawableStartY;
        for (int i = 1 ; i <= filledCount ; i ++) {
            canvas.drawBitmap(filledDrawable, x, y, paint);
            x = x + (drawableWidth + DIGIT_PADDING);
        }
        for (int i = 1 ; i <= (digits - filledCount) ; i ++) {
            canvas.drawBitmap(emptyDrawable, x, y, paint);
            x = x + (drawableWidth + DIGIT_PADDING);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO: 8/3/16 mind the padding
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int measuredWidth = 0, measuredHeight = 0;
        if (widthMode == MeasureSpec.EXACTLY) {
            measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            double height = MeasureSpec.getSize(heightMeasureSpec) * 0.7;
            measuredHeight = (int)height;// + paddingTop + paddingBottom;
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
        computeDrawableStartXY();
    }

    private void setFilledCount(int count) {
        filledCount = count > digits ? digits : count;
        skipKepDraw = true;
        invalidate(drawableStartX,
                drawableStartX,
                drawableStartX + getMeasuredWidth(),
                drawableStartY + getMeasuredHeight());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
            return processTouch(event);
    }

    private boolean processTouch(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchX = (int) event.getX();
                touchY = (int) event.getY();
                break;

            case MotionEvent.ACTION_UP:
                if (isPointerMoved) {
                    isPointerMoved = false;
                    break;
                }
                int eventX = (int) event.getX();
                int eventY = (int) event.getY();
                for (KeyRect keyRect : keyRects) {
                    if (keyRect.rect.contains(eventX, eventY)) {
                        int length = passCodeText.length();
                        if (keyRect.value.equals(eraseChar)) {
                            if (length > 0) {
                                passCodeText = passCodeText.substring(0, passCodeText.length() - 1);
                                invalidateAndNotifyListener();
                            }
                        } else if (!keyRect.value.isEmpty() && length < digits) {
                            passCodeText = passCodeText + keyRect.value;
                            invalidateAndNotifyListener();
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int dx = (int) (touchX - event.getX());
                int dy = (int) (touchY - event.getY());
                touchX = (int) event.getX();
                touchY = (int) event.getY();
                if(Math.abs(dx) > touchSlope || Math.abs(dy) > touchSlope) {
                    Log.i("Moved", "Moved");
                    isPointerMoved = true;
                }
            case MotionEvent.ACTION_CANCEL:
                return false;
        }
        return true;
    }

    public interface TextChangeListener {
        void onTextChanged(String text);
    }

    private void invalidateAndNotifyListener() {
        setFilledCount(passCodeText.length());
        if (textChangeListener != null) {
            textChangeListener.onTextChanged(passCodeText);
        }
    }

    public void setOnTextChangeListener(TextChangeListener listener) {
        this.textChangeListener = listener;
    }

    public void removeOnTextChangeListener() {
        this.textChangeListener = null;
    }

}
