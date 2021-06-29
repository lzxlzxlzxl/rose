package com.example.phototest;

import android.app.ActivityManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.phototest.util.PhotoDealImageLoader;
import com.example.phototest.widget.ColorPickBox;
import com.example.phototest.widget.CropImageView;
import com.example.phototest.widget.EditTextActionLayout;
import com.example.phototest.widget.MasicSizePickBox;
import com.example.phototest.widget.StickerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    CropImageView cropImageView;
    ActionImageView actionImageView;
    StickerView stickerView;
    EditTextActionLayout editView;
    ColorPickBox mColorPickBox;
    MasicSizePickBox mMasicSizePickBox;
    private FrameLayout workSpace;
    String path;
    private boolean needToResize = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        path = getIntent().getStringExtra("path");
        Log.e("路径路径", path);
        if(TextUtils.isEmpty(path)){
            finish();
        }
        allFindViewById();
        setupColorPickBox();
        setupMasicSizePickBox();
        addAllListener();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus && !actionImageView.isComplete() && needToResize) {
            //确定workplace高度
            resizeLayout();
            initImage();
        }
    }

    /**
     * 动态计算布局,将底部工具栏显示出来
     * 并且给actionImageView设置一个确定的高度,防止软键盘弹出的时候被挤压
     */
    private void resizeLayout() {
        needToResize = false;
        ViewGroup.LayoutParams layoutParams = workSpace.getLayoutParams();
        int newHeight = workSpace.getMeasuredHeight() - getResources().getDimensionPixelSize(R.dimen.photo_no_pic_part_layout_height);
        newHeight = newHeight/2*2;//强制偶数
        layoutParams.height = newHeight;
        workSpace.setLayoutParams(layoutParams);
        ViewGroup.LayoutParams layoutParams2 = actionImageView.getLayoutParams();
        layoutParams2.height = newHeight;
        actionImageView.setLayoutParams(layoutParams2);
    }

    private void setupMasicSizePickBox() {
        mMasicSizePickBox.init(15,25,35,45,55);
    }

    private void initImage() {
        PhotoDealImageLoader.getInstance().loadBitmap(path, actionImageView, new PhotoDealImageLoader.LoadListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onEnd() {
                new Thread(){
                    @Override
                    public void run() {
                        actionImageView.init(path);
                        actionImageView.postInvalidate();
                    }
                }.start();
            }
        });
    }

    private void addAllListener() {
        actionImageView.setmBackTextActionListener(stickerView);
        stickerView.setmTextsControlListener(actionImageView);
        stickerView.setmBeginAddTextListener(editView);
        editView.setmStopAddTextListener(stickerView);
        actionImageView.setmCropActionListener(stickerView);
        actionImageView.setmTextActionCacheQuery(stickerView);
        stickerView.setmCurrentRotateRectQuery(actionImageView);
        mColorPickBox.addColorPickListener((ColorPickBox.ColorPickListener) actionImageView);
        mColorPickBox.addColorPickListener(stickerView);
        mColorPickBox.addColorPickListener(editView);
        mMasicSizePickBox.addMasicSizePickListener((MasicSizePickBox.MasicSizePickListener) actionImageView);
    }

    private void allFindViewById() {
        workSpace = (FrameLayout) findViewById(R.id.work_space);
        cropImageView = (CropImageView) findViewById(R.id.crop);
        actionImageView = (ActionImageView) findViewById(R.id.main_image);
        editView = (EditTextActionLayout) findViewById(R.id.edit);
        stickerView = (StickerView) findViewById(R.id.stick);
        mColorPickBox = (ColorPickBox) findViewById(R.id.color_pick_box);
        mMasicSizePickBox = (MasicSizePickBox) findViewById(R.id.masic_size_pick_box);
    }

    private void setupColorPickBox() {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        arr.add(Color.rgb(255,255,255));
        arr.add(Color.rgb(0,0,0));
        arr.add(Color.rgb(196,200,25));
        arr.add(Color.rgb(219,134,0));
        arr.add(Color.rgb(219,0,120));
        arr.add(Color.rgb(152,0,198));
        arr.add(Color.rgb(68,12,203));
        arr.add(Color.rgb(0,173,202));
        arr.add(Color.rgb(0,212,67));
        mColorPickBox.initByInteger(arr);
    }

    private void preHide(){
        stickerView.setVisibility(View.GONE);
        cropImageView.setVisibility(View.GONE);
        actionImageView.setEnabled(true);
        actionImageView.setMode(ActionImageView.MODE_IDLE);
        mColorPickBox.setVisibility(View.GONE);
        mMasicSizePickBox.setVisibility(View.GONE);
        stickerView.clearState();
    }

    public void mark(View view){
        Log.i("tag","mark");
        if(!actionImageView.isComplete()) return;
        preHide();
        mColorPickBox.setVisibility(View.VISIBLE);
        actionImageView.setMode(ActionImageView.MODE_MARK);
    }

    public void masic(View view){
        Log.i("tag","masic");
        if(!actionImageView.isComplete()) return;
        preHide();
        mMasicSizePickBox.setVisibility(View.VISIBLE);
        actionImageView.setMode(ActionImageView.MODE_MASIC);
    }

    public void text(View view){
        Log.i("tag","text");
        if(!actionImageView.isComplete()) return;
        preHide();
        actionImageView.setMode(ActionImageView.MODE_TEXT);
        stickerView.setVisibility(View.VISIBLE);
        mColorPickBox.setVisibility(View.VISIBLE);
        actionImageView.setEnabled(false);
    }

    public void crop(View view){
        Log.i("tag","crop");
        if(!actionImageView.isComplete()) return;
        preHide();
        actionImageView.setMode(ActionImageView.MODE_CROP);
        cropImageView.setRatioCropRect(actionImageView.getCurrentRotateRectF(),-1);
        cropImageView.setVisibility(View.VISIBLE);
    }

    public void rotate(final View view){
        Log.i("tag","rotate");
        if(!actionImageView.isComplete()) return;
        preHide();
        actionImageView.setMode(ActionImageView.MODE_ROTATE);
        //actionImageView.rotate(actionImageView.mCurrentAngle+90,cropImageView,stickerView);
    }

    public void back(View view){
        preHide();
        Log.i("tag","back");
        if(!actionImageView.isComplete()) return;
        actionImageView.back();
    }

    public void finish(View view){
        //preHide();
        Log.i("tag","finish");
        if(!actionImageView.isComplete()) return;
        if(actionImageView.getMode()!=ActionImageView.MODE_CROP) return;
        actionImageView.crop(cropImageView.getCropRect());
        actionImageView.setMode(ActionImageView.MODE_IDLE);
        cropImageView.setVisibility(View.GONE);
    }

    public void output(View view){
        Log.i("tag","output");
        if(!actionImageView.isComplete()) return;
        //以下操作,是为了保证最后绘制的内容,都被绘制完整
        actionImageView.setMode(ActionImageView.MODE_IDLE);
        actionImageView.postInvalidate();
        actionImageView.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread() {
                    @Override
                    public void run() {
                        actionImageView.output();
                    }
                }.start();
            }
        },300);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityManager activityMgr= (ActivityManager) this.getSystemService(ACTIVITY_SERVICE );
        activityMgr.killBackgroundProcesses(getPackageName());
    }
}
