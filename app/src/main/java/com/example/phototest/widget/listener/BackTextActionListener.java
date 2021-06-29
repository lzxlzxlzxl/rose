package com.example.phototest.widget.listener;


import com.example.phototest.dealaction.TextAction;

/**
 *
 * ActionImageView通知StickerView,因为back操作,某个textAction被删除,
 * 对应的stickeritem也应该被删除
 */
public interface BackTextActionListener{
    void onBackTextAction(TextAction action);
}

