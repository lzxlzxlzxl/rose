package com.example.phototest.widget.listener;


import com.example.phototest.dealaction.TextAction;

/**
 * Created by 835127729qq.com on 16/9/12.
 * StickerView通知ActionImageView增加或者删除textAction
 */
public interface TextsControlListener{
    public void onAddText(TextAction textAction);
    public void onDeleteText(TextAction textAction);
}
