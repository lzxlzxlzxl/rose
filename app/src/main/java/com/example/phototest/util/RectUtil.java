package com.example.phototest.util;

import android.graphics.Rect;
import android.graphics.RectF;


public class RectUtil {
    public static Rect changeRectF2Rect(RectF rectF){
        return new Rect((int)rectF.left,(int)rectF.top,(int)rectF.right,(int)rectF.bottom);
    }

    public static RectF changeRect2RectF(Rect rect){
        return new RectF(rect);
    }
}
