package com.example.phototest.util;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;


public final class DrawMode {
    public final static PorterDuffXfermode CLEAR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    public final static PorterDuffXfermode SRC = new PorterDuffXfermode(PorterDuff.Mode.SRC);
    public final static PorterDuffXfermode DST_OUT = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
}
