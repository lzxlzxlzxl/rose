package com.example.phototest.dealaction;

import android.graphics.Canvas;


public interface Action {
    public void execute(Canvas canvas);

    public void start(Object... params);

    public void next(Object... params);

    public void stop(Object... params);
}
