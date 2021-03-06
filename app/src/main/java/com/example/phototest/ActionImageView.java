package com.example.phototest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.core.view.MotionEventCompat;

import com.example.phototest.dealaction.Action;
import com.example.phototest.dealaction.CropAction;
import com.example.phototest.dealaction.MarkAction;
import com.example.phototest.dealaction.MasicAction;
import com.example.phototest.dealaction.RotateAction;
import com.example.phototest.dealaction.TextAction;
import com.example.phototest.util.DrawMode;
import com.example.phototest.util.MasicUtil;
import com.example.phototest.util.SaveBitmap2File;
import com.example.phototest.widget.ColorPickBox;
import com.example.phototest.widget.MasicSizePickBox;
import com.example.phototest.widget.StickerView;
import com.example.phototest.widget.listener.BackTextActionListener;
import com.example.phototest.widget.listener.CropActionListener;
import com.example.phototest.widget.listener.RotateActionListener;
import com.example.phototest.widget.listener.TextsControlListener;
import com.example.phototest.widget.query.CurrentRotateRectQuery;
import com.example.phototest.widget.query.TextActionCacheQuery;

import java.util.ArrayList;
import java.util.List;


public abstract class ActionImageView extends androidx.appcompat.widget.AppCompatImageView implements TextsControlListener, CurrentRotateRectQuery, ColorPickBox.ColorPickListener,
		MasicSizePickBox.MasicSizePickListener{
	public static final int MODE_IDLE = 0;
	public static final int MODE_MARK = 1;
	public static final int MODE_MASIC = 2;
	public static final int MODE_TEXT = 3;
	public static final int MODE_CROP = 4;
	public static final int MODE_ROTATE = 5;
	/**
	 * ????????????
	 */
	private String picPath;
	/**
	 * ????????????
	 */Action mCurrentAction;
	/**
	 * ????????????
	 */
	private int mode = MODE_IDLE;

	/**
	 * ??????????????????Canvas
	 */
	private Canvas mForeCanvas,mCropCanvas,mCropMasicCanvas,mBehindCanvas;
	/**
	 * Actions??????
	 */
	private NotifyLinkedList<Action> actions = new NotifyLinkedList<Action>();
	/**
	 * ?????????????????????,????????????????????????
	 */
	private boolean isComplete = false;
	//Mark??????
	private Paint mMarkPaint = new Paint();
	//Masic??????
	private Paint mMasicPaint = new Paint();
	//????????????
	private Paint mClearPaint = new Paint();
	//????????????
	private Paint mTextPaint = new Paint();
	/**
	 * ????????????
	 */
	private Bitmap masicBitmap;
	private Bitmap mBehindBackground,cropMasicBitmap;
	/**
	 * ????????????
	 */
	Bitmap originBitmap;
	public Bitmap mForeBackground,cropBitmap;
	/**
	 * ????????????
	 */
	private float mWidth,mHeight;
	/**
	 * ??????????????????
	 */
	float mCurrentAngle = 0;
	/**
	 * ????????????
	 */
	private RectF originBitmapRectF;
	private RectF normalRectF;
	private Rect normalRect;
	private RectF rotateRectF;
	private RectF scaleRectF;
	//normalRectF???scaleRectF?????????
	private float normalRectF2scaleRectF = 1;
	/**
	 * ??????????????????
	 */
	private BackTextActionListener mBackTextActionListener;
	private RotateActionListener mRotateActionListener;
	private List<CropActionListener> mCropActionListeners = new ArrayList<CropActionListener>();
	private TextActionCacheQuery mTextActionCacheQuery;
	private int currentColor = Color.WHITE;
	private float currentStrokeWidth = 1;

	public ActionImageView(Context context) {
		this(context, null);
	}

	public ActionImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ActionImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initPaint();
	}

	private void initPaint() {
		setUpMarkPaint();
		setUpMasicPaint();
		setUpTextPaint();
	}

	private void setUpTextPaint() {
		mTextPaint.setStyle(Style.FILL_AND_STROKE);
		mTextPaint.setTextAlign(Paint.Align.CENTER);
		mTextPaint.setAntiAlias(true);
		mTextPaint.setDither(true);
		mTextPaint.setColor(Color.WHITE);
	}

	private void setUpMarkPaint(){
		mMarkPaint.setStyle(Style.STROKE);
		mMarkPaint.setColor(Color.RED);
		mMarkPaint.setStrokeWidth(6);
		mMarkPaint.setAntiAlias(true);
		mMarkPaint.setDither(true);
	}

	private void setUpMasicPaint(){
		mMasicPaint.setColor(Color.parseColor("#c0c0c0"));
		mMasicPaint.setAntiAlias(true);
		mMasicPaint.setDither(true);
		mMasicPaint.setStyle(Style.STROKE);
		mMasicPaint.setStrokeJoin(Paint.Join.ROUND); // ??????
		mMasicPaint.setStrokeCap(Paint.Cap.ROUND); // ??????
		// ??????????????????
		mMasicPaint.setStrokeWidth(18);
	}

	/**
	 * ???????????????????????????????????????,????????????Imageview??????????????????,???????????????onWindowFocusChanged()???????????????,??????hasFocus???true
	 * @param path
	 */
	public synchronized void init(String path){
		picPath = path;
		if(mWidth<=0 || mHeight<=0 || isComplete) return;
		isComplete = true;
		originBitmap = ((BitmapDrawable) getDrawable()).getBitmap();
		originBitmapRectF = decodeBounds(path);
		recaculateRects(originBitmapRectF);
		// ?????????bitmap
		mForeBackground = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Config.ARGB_8888);
		mForeCanvas = new Canvas(mForeBackground);
		//?????????
		cropBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Config.ARGB_8888);
		mCropCanvas = new Canvas(cropBitmap);

		//????????????
		Bitmap srcBitmap = originBitmap.copy(Config.ARGB_4444, true);
		//masicBitmap = PhotoProcessing.filterPhoto(srcBitmap, 12);
		//????????????????????????native???,???????????????????????????????????????
		masicBitmap = MasicUtil.getMosaicsBitmaps(srcBitmap,0.1);
		mBehindBackground = Bitmap.createScaledBitmap(masicBitmap,getMeasuredWidth(), getMeasuredHeight(), false);
		mBehindCanvas = new Canvas(mBehindBackground);
		//??????????????????
		cropMasicBitmap = Bitmap.createBitmap(getMeasuredWidth(),getMeasuredHeight(),Config.ARGB_4444);
		mCropMasicCanvas = new Canvas(cropMasicBitmap);
		//??????MasicUtil.getMosaicsBitmap()??????????????????,????????????srcBitmap,????????????
		srcBitmap.recycle();
	}

	private RectF decodeBounds(String path){
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		opts.inSampleSize = 1;
		BitmapFactory.decodeFile(path,opts);
		return new RectF(0,0,opts.outWidth,opts.outHeight);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		//??????masic??????
		if(masicBitmap!=null && isComplete) {
			drawBehindBackground(canvas);
			drawForeBackground(canvas);
		}else{
			super.onDraw(canvas);
		}
	}

	/**
	 * ????????????
	 * @param canvas
	 */
	private void drawBehindBackground(Canvas canvas){
		if(isComplete==false) return;
		if(mBehindCanvas==null) return;
		recaculateRects(originBitmapRectF);

		//??????
		mClearPaint.setXfermode(DrawMode.CLEAR);
		mBehindCanvas.drawPaint(mClearPaint);
		mClearPaint.setXfermode(DrawMode.SRC);

		mBehindCanvas.save();
		mBehindCanvas.drawBitmap(masicBitmap, null, normalRectF,null);
		mBehindCanvas.restore();

		if(cropSnapshot!=null && cropSnapshot.cropAction!=null && actions.contains(cropSnapshot.cropAction)){
			Rect lastNormalRect = new Rect(normalRect);
			RectF lastScaleRectf = getCurrentScaleRectF();
			recaculateRects(cropSnapshot.cropAction.mCropRect);
			cropSnapshot.cropAction.start(mCurrentAngle,rotateRectF,scaleRectF,lastNormalRect,lastScaleRectf,normalRectF);
			cropSnapshot.cropAction.drawCropMasicBitmapFromCache(mBehindCanvas);
			if(cropSnapshot.cropAction.angle/90%2==1){
				recaculateRects(new RectF(rotateRectF));
			}
		}else {
			for (Action action : actions) {
				if (action instanceof CropAction) {
					CropAction cropAction = (CropAction) action;
					Rect lastNormalRect = new Rect(normalRect);
					RectF lastScaleRectf = getCurrentScaleRectF();
					recaculateRects(cropAction.mCropRect);
					cropAction.start(mCurrentAngle,rotateRectF,scaleRectF,lastNormalRect,lastScaleRectf,normalRectF);
					cropAction.next(mBehindCanvas, mCurrentAngle);
					/*
					 * ????????????????????????,??????????????????,????????????????????????
					 * ??????????????????,????????????????????????,???????????????????????????,??????????????????
					 */
					if(cropAction.angle/90%2==1){
						recaculateRects(new RectF(rotateRectF));
					}
				}else if(action instanceof MasicAction){
					//action.execute(mBehindCanvas);
				}
			}
		}
		//????????????
		canvas.save();
		canvas.rotate(mCurrentAngle,mWidth/2,mHeight/2);
		canvas.drawBitmap(mBehindBackground,normalRect, getCurrentScaleRectF(),null);
		canvas.restore();
	}

	/**
	 * ????????????
	 * @param canvas
	 */
	private void drawForeBackground(Canvas canvas) {
		if(isComplete==false) return;
		if(mForeCanvas==null) return;
		recaculateRects(originBitmapRectF);
		drawActions(mForeCanvas);

//		try {
//			SaveBitmap2File.saveFile(mForeBackground,"/storage/emulated/0/ActionImage","sss.png");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		canvas.save();
		canvas.rotate(mCurrentAngle,mWidth/2.0f,mHeight/2.0f);
		canvas.drawBitmap(mForeBackground,normalRect, getCurrentScaleRectF(),null);
		canvas.restore();
	}

	private void drawActions(Canvas foreCanvas){
		//??????
		mClearPaint.setXfermode(DrawMode.CLEAR);
		foreCanvas.drawPaint(mClearPaint);
		mClearPaint.setXfermode(DrawMode.SRC);

		foreCanvas.save();
		//???png??????????????????,?????????????????????,??????png??????????????????????????????????????????
		mForeCanvas.drawRGB(0,0,0);
		mForeCanvas.drawBitmap(originBitmap, null, normalRectF,null);
		foreCanvas.restore();


		//??????????????????????????????,???????????????
		RotateAction lastRotateAction = null;
		for(int i=actions.size()-1;i>=0;i--){
			if(actions.get(i) instanceof RotateAction){
				lastRotateAction = (RotateAction)actions.get(i);
				break;
			}
		}

		float startAngle = 0;
		int actionIndex = 0;
		//??????????????????????????????crop??????
		if(cropSnapshot!=null && cropSnapshot.cropAction!=null && actions.contains(cropSnapshot.cropAction)){
			actionIndex = actions.indexOf(cropSnapshot.cropAction);
			for(int i=actionIndex-1;i>=0;i--){//????????????startAngles
				if(actions.get(i) instanceof RotateAction){
					startAngle = ((RotateAction) actions.get(i)).getmAngle();
					break;
				}
			}
		}

		//????????????
		for(;actionIndex<actions.size();actionIndex++){
			Action action = actions.get(actionIndex);
			if(action instanceof RotateAction){
				startAngle = ((RotateAction) action).getmAngle();
				continue;
			}
			//????????????????????????,???????????????
			if(action instanceof TextAction && mode==MODE_TEXT && mTextActionCacheQuery.query((TextAction) action)){
				continue;
			}
			if(action instanceof CropAction){
				CropAction cropAction = (CropAction) action;
				if(cropSnapshot.cropAction!=null && cropSnapshot.cropAction==action){
					Rect lastNormalRect = new Rect(normalRect);
					RectF lastScaleRectf = getCurrentScaleRectFBaseOnLastAngle(startAngle);
					recaculateRects(cropSnapshot.cropAction.mCropRect);
					cropSnapshot.cropAction.start(mCurrentAngle,rotateRectF,scaleRectF,lastNormalRect,lastScaleRectf,normalRectF);
					cropSnapshot.cropAction.drawCropBitmapFromCache(foreCanvas);
					if(cropAction.angle/90%2==1){
						recaculateRects(new RectF(rotateRectF));
					}
				}else {
					Rect lastNormalRect = new Rect(normalRect);
					RectF lastScaleRectf = getCurrentScaleRectFBaseOnLastAngle(startAngle);//getCurrentScaleRectF();
					recaculateRects(cropAction.mCropRect);
					action.start(mCurrentAngle,rotateRectF,scaleRectF,lastNormalRect,lastScaleRectf,normalRectF);
					action.execute(foreCanvas);
					/*
					 * ????????????????????????,??????????????????,????????????????????????
					 * ??????????????????,????????????????????????,???????????????????????????,??????????????????
					 */
					if(cropAction.angle/90%2==1){
						recaculateRects(new RectF(rotateRectF));
					}
				}
				action.stop(cropSnapshot);
			}else if(action instanceof TextAction){
				TextAction textAction = (TextAction) action;
				if(!mTextActionCacheQuery.query(textAction)) {
					action.start(-textAction.saveAngle, mWidth / 2.0f, mHeight / 2.0f, mTextPaint, textAction.saveNormalRectF2scaleRectF);
				}else{
					action.start(-mCurrentAngle, mWidth / 2.0f, mHeight / 2.0f, mTextPaint, normalRectF2scaleRectF);
				}
				action.execute(foreCanvas);
//				try {
//					SaveBitmap2File.saveFile(mForeBackground,"/storage/emulated/0/ActionImage",count+"aaa.png");
//					count++;
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
			}else {
				if (lastRotateAction != null) {//??????????????????
					foreCanvas.save();
					foreCanvas.rotate(-startAngle, mWidth / 2, mHeight / 2);
					action.execute(foreCanvas);
					foreCanvas.restore();
				} else {
					action.execute(foreCanvas);
				}
			}
		}
	}

	private CropSnapshot cropSnapshot = new CropSnapshot();

	@Override
	public RectF query() {
		return getCurrentRotateRectF();
	}

	@Override
	public void notifyColorChange(int color) {
		currentColor = color;
		mMarkPaint.setColor(color);
		mTextPaint.setColor(color);
	}

	@Override
	public void notify(float size) {
		currentStrokeWidth = size;
	}

	/**
	 * ??????????????????
	 */
	public class CropSnapshot{
		public boolean isCache = true;
		public void setCropAction(CropAction cropAction) {
			if(!isCache){
				this.cropAction = null;
				return;
			}
			this.cropAction = cropAction;
		}

		CropAction cropAction;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		mWidth = getMeasuredWidth();
		mHeight = getMeasuredHeight();
		if(mWidth>0 && mHeight>0 && !isComplete && !TextUtils.isEmpty(picPath)){
			init(picPath);
		}
		//Log.i("tag","w="+mWidth+",h="+mHeight);
	}

	//????????????
	private Matrix scalePointMatrix = new Matrix();
	//??????,????????????
	private float[] scalePoint = new float[2];
	/**
	 * ??????????????????,???????????????????????????
	 * @param event
	 */
	private void scalePoint(MotionEvent event) {
		scalePointMatrix.reset();
		scalePointMatrix.postScale(normalRectF2scaleRectF,normalRectF2scaleRectF,mWidth/2,mHeight/2);
		scalePoint[0] = event.getX();
		scalePoint[1] = event.getY();
		scalePointMatrix.mapPoints(scalePoint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(!isEnabled()) return false;
		int action = MotionEventCompat.getActionMasked(event);
		switch (action){
			case MotionEvent.ACTION_DOWN:
				//Log.i("tag","down");
				mCurrentAction = produceMarkActionOrMasicAction();
				if (mCurrentAction==null) return false;
				scalePoint(event);
				mCurrentAction.start(scalePoint[0], scalePoint[1]);
				actions.add(mCurrentAction);
				return true;
			case MotionEvent.ACTION_MOVE:
				//Log.i("tag","move");
				if (mCurrentAction==null) return false;
				scalePoint(event);
				mCurrentAction.next(scalePoint[0], scalePoint[1]);
				invalidate();
				return true;
			case MotionEvent.ACTION_UP:
				//Log.i("tag","up");
				if (mCurrentAction==null) return false;
				scalePoint(event);
				mCurrentAction.stop(scalePoint[0], scalePoint[1]);
				invalidate();
				return true;
		}
		return true;
	}

	private Action produceMarkActionOrMasicAction(){
		Action action = null;
		switch (mode){
			case MODE_MARK:
				action = new MarkAction(new Path(),mMarkPaint,currentColor);
				break;
			case MODE_MASIC:
				action = new MasicAction(new Path(),mMasicPaint,currentStrokeWidth);
				break;
		}
		return action;
	}

	/**
	 * ??????
	 */
	public boolean back(){
		if(actions.size()==0) return false;
		final Action action = actions.removeLast();
		post(new Runnable() {
			@Override
			public void run() {
				if(action instanceof RotateAction){
					//??????????????????????????????,???????????????
					int i = actions.size()-1;
					for(;i>=0;i--){
						if(actions.get(i) instanceof RotateAction){
							mCurrentAngle = ((RotateAction) actions.get(i)).getmAngle();
							break;
						}
					}
					if(i<0){
						mCurrentAngle = 0;
					}
					float nextNormalRectF2scaleRectF = 1.0f;
					if(mCurrentAngle/90%2==1){//???????????????,????????????
						nextNormalRectF2scaleRectF = scaleRectF.width()/normalRectF.width();
					}else{
						nextNormalRectF2scaleRectF = normalRectF.width()/scaleRectF.width();
					}
					mRotateActionListener.onRotateBack(mCurrentAngle-((RotateAction) action).getmAngle(),nextNormalRectF2scaleRectF);
					action.stop(getCurrentRotateRectF());
				}else if(action instanceof TextAction){
					if(mBackTextActionListener!=null) mBackTextActionListener.onBackTextAction((TextAction)action);
				}else if(action instanceof CropAction){
					CropAction lastCropAction = null;
					//???????????????rect
					for(int i=actions.size()-1;i>=0;i--){
						if(actions.get(i) instanceof CropAction){
							lastCropAction = (CropAction) actions.get(i);
							break;
						}
					}
					if(lastCropAction!=null){
						recaculateRects(lastCropAction.mCropRect);
						if(lastCropAction.angle/90%2==1){
							recaculateRects(new RectF(rotateRectF));
						}
					}else{
						recaculateRects(originBitmapRectF);
					}
					//???????????????textAction
					for(CropActionListener cropActionListener:mCropActionListeners) {
						cropActionListener.onCropBack(getCurrentRotateRectF());
					}
					cropSnapshot.setCropAction(null);
				}
				postInvalidate();
			}
		});
		return true;
	}

	/**
	 * ??????
	 * @param rectf
	 */
	public void crop(RectF rectf){
		mCurrentAction = new CropAction(mWidth/2,mHeight/2,rectf, cropBitmap,mForeBackground,mCropCanvas,
				cropMasicBitmap,mBehindBackground,mCropMasicCanvas,mCurrentAngle);
		actions.add(mCurrentAction);

		cropSnapshot.setCropAction(null);
		postInvalidate();
		for(CropActionListener cropActionListener:mCropActionListeners) {
			cropActionListener.onCrop(mCurrentAngle,normalRectF2scaleRectF);
		}
	}

	/**
	 * ??????
	 * @param angle
	 */
	public void rotate(float angle, RotateAction.RotateActionBackListener rotateActionBackListener, RotateActionListener rotateActionListener){
		mCurrentAction = new RotateAction(angle,rotateActionBackListener);
		mRotateActionListener = rotateActionListener;
		float nextNormalRectF2scaleRectF = 1.0f;
		if(angle/90%2==1){//???????????????,????????????
			nextNormalRectF2scaleRectF = scaleRectF.width()/normalRectF.width();
		}else{
			nextNormalRectF2scaleRectF = normalRectF.width()/scaleRectF.width();
		}
		mRotateActionListener.onRotate(angle-mCurrentAngle,nextNormalRectF2scaleRectF);
		mCurrentAngle = angle;
		actions.add(mCurrentAction);
		invalidate();
	}

	/**
	 * ??????????????????
	 */
	public String output(){
		Rect srcrect = new Rect((int)normalRectF.left,(int)normalRectF.top,(int)normalRectF.right,(int)normalRectF.bottom);
		RectF destrect;// = new RectF(0,0,getCurrentRotateRect().width(),getCurrentRotateRect().height());
		RectF rotateRect = getCurrentRotateRectF();
		if(originBitmapRectF.width()<mWidth&&originBitmapRectF.height()<mHeight) {
			float scale;
			if(originBitmapRectF.width()<originBitmapRectF.height()){
				scale = originBitmapRectF.width()/rotateRect.width();
			}else if(originBitmapRectF.width()==originBitmapRectF.height()){
				scale = rotateRect.width()<rotateRect.height()?originBitmapRectF.width()/rotateRect.width():originBitmapRectF.height()/rotateRect.height();
			}else{
				scale = originBitmapRectF.height()/rotateRect.height();
			}
			destrect = new RectF(0,0,rotateRect.width()*scale,rotateRect.height()*scale);
		}else{
			destrect = new RectF(0,0,rotateRect.width(),rotateRect.height());
		}
		RectF rect1 = new RectF();
		Matrix matrix = new Matrix();
		matrix.postRotate(mCurrentAngle,destrect.centerX(),destrect.centerY());
		matrix.mapRect(rect1,destrect);

		Bitmap bitmap = Bitmap.createBitmap((int)destrect.width(),(int)destrect.height(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.save();
		canvas.rotate(mCurrentAngle,destrect.centerX(),destrect.centerY());
		canvas.drawRect(rect1,mMarkPaint);
		canvas.drawBitmap(mBehindBackground,srcrect, rect1,null);
		canvas.drawBitmap(mForeBackground,srcrect, rect1,null);
		canvas.restore();
		return SaveBitmap2File.saveImageToGallery(ActionImageView.this.getContext(),bitmap).getAbsolutePath();
	}

	/**
	 * ???????????????????????????
	 * @param rectF
	 */
	private void recaculateRects(RectF rectF) {
		normalRectF = generateRectF(rectF);
		normalRect = new Rect((int)normalRectF.left,(int)normalRectF.top,(int)normalRectF.right,(int)normalRectF.bottom);
		rotateRectF = generateRotateRectF(rectF);
		scaleRectF = generateScaleRectF(rectF);
		normalRectF2scaleRectF = normalRectF.width()/getCurrentScaleRectF().width();
	}

	private void recaculateRects(Rect rect) {
		recaculateRects(new RectF(rect));
	}

	private Matrix recaculateRectMatrix = new Matrix();
	/**
	 * ?????????????????????????????????
	 * @param rectF
	 * @return
	 */
	private RectF generateRectF(RectF rectF){
		RectF rf = new RectF(rectF);
		float scaleW = mWidth/rectF.width();
		float scaleH = mHeight/rectF.height();
		float scale = scaleW<scaleH?scaleW:scaleH;

		recaculateRectMatrix.reset();
		recaculateRectMatrix.postTranslate(mWidth/2-rf.centerX(),mHeight/2-rf.centerY());
		recaculateRectMatrix.postScale(scale,scale,mWidth/2,mHeight/2);
		recaculateRectMatrix.mapRect(rf);

		if(scaleW<scaleH){
			//????????????
			float del = -rf.left;
			rf.left = 0;
			rf.right = rf.right+del;
		}else{
			//????????????
			float del = -rf.top;
			rf.top = 0;
			rf.bottom = rf.bottom+del;
		}
		return rf;
	}

	/**
	 * ?????????,?????????????????????????????????
	 * @param rectF
	 * @return
	 */
	private RectF generateRotateRectF(RectF rectF){
		RectF rf = new RectF(rectF);
		float scaleW = mWidth/rectF.height();
		float scaleH = mHeight/rectF.width();
		float scale = scaleW<scaleH?scaleW:scaleH;

		recaculateRectMatrix.reset();
		recaculateRectMatrix.postTranslate(mWidth/2-rf.centerX(),mHeight/2-rf.centerY());
		recaculateRectMatrix.postRotate(90,mWidth/2,mHeight/2);
		recaculateRectMatrix.postScale(scale,scale,mWidth/2,mHeight/2);
		recaculateRectMatrix.mapRect(rf);

		if(scaleW<scaleH){
			//????????????
			float del = -rf.left;
			rf.left = 0;
			rf.right = rf.right+del;
		}else{
			//????????????
			float del = -rf.top;
			rf.top = 0;
			rf.bottom = rf.bottom+del;
		}
		return rf;
	}

	/**
	 * ?????????????????????????????????,???????????????
	 * @param rectF
	 * @return
	 */
	private RectF generateScaleRectF(RectF rectF){
		//??????rotaterect????????????,??????scalerect????????????
		RectF rf = generateRotateRectF(rectF);
		recaculateRectMatrix.reset();
		recaculateRectMatrix.postTranslate(mWidth/2-rf.centerX(),mHeight/2-rf.centerY());
		recaculateRectMatrix.postRotate(-90,mWidth/2,mHeight/2);
		recaculateRectMatrix.mapRect(rf);
		return rf;
	}

	public RectF getCurrentRotateRectF(){
		if(mCurrentAngle/90%2==0){
			return new RectF(normalRectF);
		}else {
			return new RectF(rotateRectF);
		}
	}

	/**
	 * ????????????????????????
	 * @return
	 */
	public RectF getCurrentCropRotateRectF(RectF rectF){
		return generateRectF(rectF);
//		CropAction cropAction = null;
//		for(Action action:actions){
//			if(action instanceof CropAction){
//				cropAction = (CropAction) action;
//			}
//		}
//		if(cropAction==null) return null;
//		if(mCurrentAngle/90%2==0){
//			return generateRectF(cropAction.mCropRectF);
//		}else{
//			return generateRotateRectF(cropAction.mCropRectF);
//		}
	}

	private Rect getCurrentRotateRect(){
		if(mCurrentAngle/90%2==0){
			return new Rect((int)normalRectF.left,(int)normalRectF.top,(int)normalRectF.right,(int)normalRectF.bottom);
		}else {
			return new Rect((int)rotateRectF.left,(int)rotateRectF.top,(int)rotateRectF.right,(int)rotateRectF.bottom);
		}
	}

	private RectF getCurrentScaleRectF(){
		if(mCurrentAngle/90%2==0){
			return new RectF(normalRectF);
		}else {
			return new RectF(scaleRectF);
		}
	}

	private RectF getCurrentScaleRectFBaseOnLastAngle(float angle){
		if(angle/90%2==0){
			return new RectF(normalRectF);
		}else {
			return new RectF(scaleRectF);
		}
	}

	private Rect getCurrentScaleRect(){
		if(mCurrentAngle/90%2==0){
			return new Rect((int)normalRectF.left,(int)normalRectF.top,(int)normalRectF.right,(int)normalRectF.bottom);
		}else {
			return new Rect((int)scaleRectF.left,(int)scaleRectF.top,(int)scaleRectF.right,(int)scaleRectF.bottom);
		}
	}

	/**
	 * ????????????
	 * @return
	 */
	public RectF getmRectF(){
		return new RectF(getLeft(),getTop(),getRight(),getBottom());
	}

	public Rect getmRect(){
		return new Rect(getLeft(),getTop(),getRight(),getBottom());
	}

	public void setMode(int mode){
		this.mode = mode;
	}

	public int getMode(){
		return mode;
	}


	public void onAddText(TextAction textAction) {
		actions.add(textAction);
		postInvalidate();
	}


	public void onDeleteText(TextAction textAction) {
		actions.remove(textAction);
		postInvalidate();
	}

	public void setmBackTextActionListener(StickerView mBackTextActionListener) {
		this.mBackTextActionListener = (BackTextActionListener) mBackTextActionListener;
	}

	public void setmCropActionListener(StickerView mCropActionListener) {
		mCropActionListeners.add((CropActionListener) mCropActionListener);
	}

	public void setmTextActionCacheQuery(StickerView mTextActionCacheQuery) {
		this.mTextActionCacheQuery = (TextActionCacheQuery) mTextActionCacheQuery;
	}

	public void setLinkedListOperateListner(NotifyLinkedList.LinkedListOperateListner mLinkedListOperateListner) {
		actions.setmLinkedListOperateListner(mLinkedListOperateListner);
	}

	public boolean isComplete() {
		return isComplete;
	}

	public void setComplete(boolean complete) {
		isComplete = complete;
	}

	public int getActionsCount(){
		if(actions==null) return 0;
		return actions.size();
	}

	public synchronized void recycleResource(){
		isComplete = false;
		if(masicBitmap!=null){
			masicBitmap.recycle();
			masicBitmap = null;
		}

		if(mBehindBackground!=null){
			mBehindBackground.recycle();
			mBehindBackground = null;
		}

		if(cropMasicBitmap!=null){
			cropMasicBitmap.recycle();
			cropMasicBitmap = null;
		}

		if(mForeBackground!=null){
			mForeBackground.recycle();
			mForeBackground = null;
		}

		if(cropBitmap!=null){
			cropBitmap.recycle();
			cropBitmap = null;
		}
		System.gc();
	}
}
