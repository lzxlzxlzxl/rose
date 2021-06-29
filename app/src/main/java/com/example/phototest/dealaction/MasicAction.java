package com.example.phototest.dealaction;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.example.phototest.util.DrawMode;



public class MasicAction implements Action{
    Path mPath;
    Paint mPaint;
    float paintWidth;

    public MasicAction(Path path,Paint paint,float width){
        mPaint = paint;
        mPath = path;
        paintWidth = width;
    }

    @Override
    public void execute(Canvas canvas) {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setXfermode(DrawMode.DST_OUT);
        mPaint.setStrokeWidth(paintWidth);
        canvas.drawPath(mPath, mPaint);
    }

    @Override
    public void start(Object... params) {
        mPath.moveTo((float)params[0],(float)params[1]);
    }

    @Override
    public void next(Object... params) {
        mPath.lineTo((float)params[0],(float)params[1]);
    }

    @Override
    public void stop(Object... params) {
        mPath.lineTo((float)params[0],(float)params[1]);
    }
}
