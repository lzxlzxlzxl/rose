package com.example.phototest.widget.listener;

import android.view.View;

/**
 * Created by 835127729qq.com on 16/9/19.
 * ActionImageView进行旋转/撤销旋转,通知StickerView选择自己的stickeritem
 */
public interface RotateActionListener {
    void onRotate(float angle, float nextNormalRectF2scaleRectF);
    void onRotateBack(float angle, float nextNormalRectF2scaleRectF);
}
