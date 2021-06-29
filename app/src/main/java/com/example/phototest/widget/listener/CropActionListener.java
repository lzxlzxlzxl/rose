package com.example.phototest.widget.listener;

import android.graphics.RectF;

/**
 * Created by 835127729qq.com on 16/9/21.
 * ActionImageView通知StickerView,发生裁剪操作
 */
public interface CropActionListener {
    void onCrop(float currentAngle, float currentNormalRectF2scaleRectF);
    void onCropBack(RectF rectF);
}
