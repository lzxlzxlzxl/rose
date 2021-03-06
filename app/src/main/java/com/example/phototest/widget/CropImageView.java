package com.example.phototest.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.phototest.R;
import com.example.phototest.dealaction.RotateAction;
import com.example.phototest.util.DensityUtil;
import com.example.phototest.util.PaintUtil;
import com.example.phototest.widget.listener.CropActionListener;

/**
 * 剪切图片
 *
 */
public class CropImageView extends View implements RotateAction.RotateActionBackListener, CropActionListener {
	private static int STATUS_IDLE = 1;// 空闲状态
	private static int STATUS_MOVE = 2;// 移动状态
	private static int STATUS_SCALE = 3;// 缩放状态
	private final int delayMillis = 500;
	private final int hideTextSingal = 0;

	private int CIRCLE_WIDTH = 46;
	private Context mContext;
	private float oldx, oldy;
	private int status = STATUS_IDLE;
	private int selectedControllerCicle;
	private RectF backUpRect = new RectF();// 上
	private RectF backLeftRect = new RectF();// 左
	private RectF backRightRect = new RectF();// 右
	private RectF backDownRect = new RectF();// 下

	private RectF cropRect = new RectF();// 剪切矩形

	private Paint mBackgroundPaint;// 背景Paint
	private Paint mStyleBoxPaint;
	private Paint mTextPaint;
	private Paint clearPaint;

	private Bitmap circleBit;
	private Rect circleRect = new Rect();
	private RectF leftTopCircleRect;
	private RectF rightTopCircleRect;
	private RectF leftBottomRect;
	private RectF rightBottomRect;

	private RectF imageRect = new RectF();// 存贮图片位置信息
	private RectF tempRect = new RectF();// 临时存贮矩形数据

	private float ratio = -1;// 剪裁缩放比率
	//是否需要显示尺寸文字
	private volatile boolean isShowText = false;
	//文字距离框的距离
	private float textMargin = 50;
	//文字大小
	private float textSize = 10;
	//裁剪框是否被激活,也就是是否被触碰
	private boolean isActived = false;
	private CropActiveListener mCropActiveListener;

	private static Handler mHandler;

	public CropImageView(Context context) {
		super(context);
		init(context);
	}

	public CropImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		initHandler();

		mContext = context;
		textSize = DensityUtil.dip2px(context,13);
		textMargin = DensityUtil.dip2px(context,18);

		mBackgroundPaint = PaintUtil.newBackgroundPaint(context);
		circleBit = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.sticker_rotate);
		circleRect.set(0, 0, circleBit.getWidth(), circleBit.getHeight());
		leftTopCircleRect = new RectF(0, 0, CIRCLE_WIDTH, CIRCLE_WIDTH);
		rightTopCircleRect = new RectF(leftTopCircleRect);
		leftBottomRect = new RectF(leftTopCircleRect);
		rightBottomRect = new RectF(leftTopCircleRect);

		mStyleBoxPaint = new Paint();
		mStyleBoxPaint.setColor(Color.WHITE);
		mStyleBoxPaint.setStyle(Paint.Style.STROKE);

		mTextPaint = new Paint();
		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setStyle(Paint.Style.STROKE);
		mTextPaint.setTextAlign(Paint.Align.CENTER);
		mTextPaint.setTextSize(textSize);

		clearPaint = new Paint();
		clearPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		clearPaint.setColor(Color.TRANSPARENT);
	}

	private void initHandler() {
		if(mHandler==null){
			HandlerThread thread = new HandlerThread("CropImageView");
			thread.start();
			mHandler = new Handler(thread.getLooper()) {
				@Override
				public void handleMessage(Message msg) {
					if(msg.what==hideTextSingal && status == STATUS_IDLE){
						isShowText = false;
						postInvalidate();
					}
				}
			};
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		if(mHandler!=null){
			mHandler.getLooper().quit();
			mHandler = null;
		}
		super.onDetachedFromWindow();
	}

	/**
	 * 重置剪裁面
	 * 
	 * @param rect
	 */
	public void setCropRect(RectF rect) {
		imageRect.set(rect);
		cropRect.set(rect);
		scaleRect(cropRect, 1f);
		invalidate();
	}

	public void setRatioCropRect(RectF rect, float r) {
		this.ratio = r;
		if (r < 0) {
			setCropRect(rect);
			return;
		}

		imageRect.set(rect);
		cropRect.set(rect);
		// setCropRect(rect);
		// 调整Rect

		float h, w;
		if (cropRect.width() >= cropRect.height()) {// w>=h
			h = cropRect.height() / 2;
			w = this.ratio * h;
		} else {// w<h
			w = rect.width() / 2;
			h = w / this.ratio;
		}// end if
		float scaleX = w / cropRect.width();
		float scaleY = h / cropRect.height();
		scaleRect(cropRect, scaleX, scaleY);
		invalidate();
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		int w = getWidth();
		int h = getHeight();
		if (w <= 0 || h <= 0)
			return;
		//clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		//canvas.drawPaint(clearPaint);
		//clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

		// 绘制黑色背景
		backUpRect.set(0, 0, w, cropRect.top);
		backLeftRect.set(0, cropRect.top, cropRect.left, cropRect.bottom);
		backRightRect.set(cropRect.right, cropRect.top, w, cropRect.bottom);
		backDownRect.set(0, cropRect.bottom, w, h);

		canvas.drawRect(backUpRect, mBackgroundPaint);
		canvas.drawRect(backLeftRect, mBackgroundPaint);
		canvas.drawRect(backRightRect, mBackgroundPaint);
		canvas.drawRect(backDownRect, mBackgroundPaint);

		// 绘制四个控制点
		int radius = CIRCLE_WIDTH >> 1;
		leftTopCircleRect.set(cropRect.left - radius, cropRect.top - radius,
				cropRect.left + radius, cropRect.top + radius);
		rightTopCircleRect.set(cropRect.right - radius, cropRect.top - radius,
				cropRect.right + radius, cropRect.top + radius);
		leftBottomRect.set(cropRect.left - radius, cropRect.bottom - radius,
				cropRect.left + radius, cropRect.bottom + radius);
		rightBottomRect.set(cropRect.right - radius, cropRect.bottom - radius,
				cropRect.right + radius, cropRect.bottom + radius);
		drawLines(canvas);
		canvas.drawBitmap(circleBit, circleRect, leftTopCircleRect, null);
		canvas.drawBitmap(circleBit, circleRect, rightTopCircleRect, null);
		canvas.drawBitmap(circleBit, circleRect, leftBottomRect, null);
		canvas.drawBitmap(circleBit, circleRect, rightBottomRect, null);
	}

	/**
	 * 绘制九宫格
	 */
	private void drawLines(Canvas canvas){
		mStyleBoxPaint.setStyle(Paint.Style.STROKE);
		//canvas.drawRect(cropRect,mStyleBoxPaint);
		//横线
		float heightOfhorizontalLine = cropRect.height()/3;
		canvas.drawLine(cropRect.left,cropRect.top,
				cropRect.right,cropRect.top,mStyleBoxPaint);
		canvas.drawLine(cropRect.left,heightOfhorizontalLine+cropRect.top,
				cropRect.right,heightOfhorizontalLine+cropRect.top,mStyleBoxPaint);
		canvas.drawLine(cropRect.left,heightOfhorizontalLine*2+cropRect.top,
				cropRect.right,heightOfhorizontalLine*2+cropRect.top,mStyleBoxPaint);
		canvas.drawLine(cropRect.left,cropRect.bottom,
				cropRect.right,cropRect.bottom,mStyleBoxPaint);
		//纵线
		float widthOfVerticalLine = cropRect.width()/3;
		canvas.drawLine(cropRect.left,cropRect.top,
				cropRect.left,cropRect.bottom,mStyleBoxPaint);
		canvas.drawLine(cropRect.left+widthOfVerticalLine,cropRect.top,
				cropRect.left+widthOfVerticalLine,cropRect.bottom,mStyleBoxPaint);
		canvas.drawLine(cropRect.left+widthOfVerticalLine*2,cropRect.top,
				cropRect.left+widthOfVerticalLine*2,cropRect.bottom,mStyleBoxPaint);
		canvas.drawLine(cropRect.right,cropRect.top,
				cropRect.right,cropRect.bottom,mStyleBoxPaint);

		//绘制大小提示
		if(isShowText) {
			canvas.drawText(Math.round(cropRect.width()) + "x" + Math.round(cropRect.height()),
					cropRect.centerX(), cropRect.bottom + textMargin, mTextPaint);
		}
	}

	/**
	 * 触摸事件处理
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean ret = super.onTouchEvent(event);// 是否向下传递事件标志 true为消耗
		int action = event.getAction();
		float x = event.getX();
		float y = event.getY();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			isShowText = true;
			int selectCircle = isSeletedControllerCircle(x, y);
			if (selectCircle > 0) {// 选择控制点
				ret = true;
				selectedControllerCicle = selectCircle;// 记录选中控制点编号
				status = STATUS_SCALE;// 进入缩放状态
				innerActive();
			} else if (cropRect.contains(x, y)) {// 选择缩放框内部
				ret = true;
				status = STATUS_MOVE;// 进入移动状态
				innerActive();
			} else {// 没有选择

			}// end if
			break;
		case MotionEvent.ACTION_MOVE:
			if (status == STATUS_SCALE) {// 缩放控制
				// System.out.println("缩放控制");
				scaleCropController(x, y);
			} else if (status == STATUS_MOVE) {// 移动控制
				// System.out.println("移动控制");
				translateCrop(x - oldx, y - oldy);
			}
			break;
		case MotionEvent.ACTION_UP:
			status = STATUS_IDLE;// 回归空闲状态
			mHandler.sendEmptyMessageDelayed(hideTextSingal, delayMillis);
			break;
		}// end switch

		// 记录上一次动作点
		oldx = x;
		oldy = y;
		return ret;
	}

	private void innerActive(){
		if(!isActived){
			isActived = true;
			if(mCropActiveListener!=null) {
				mCropActiveListener.onCropActive();
			}
		}
	}

	/**
	 * 取消激活状态
	 */
	public void unActive(){
		isActived = false;
	}

	public boolean isActived(){
		return isActived;
	}


	/**
	 * 移动剪切框
	 * 
	 * @param dx
	 * @param dy
	 */
	private void translateCrop(float dx, float dy) {
		tempRect.set(cropRect);// 存贮原有数据，以便还原

		translateRect(cropRect, dx, dy);
		// 边界判定算法优化
		float mdLeft = imageRect.left - cropRect.left;
		if (mdLeft > 0) {
			translateRect(cropRect, mdLeft, 0);
		}
		float mdRight = imageRect.right - cropRect.right;
		if (mdRight < 0) {
			translateRect(cropRect, mdRight, 0);
		}
		float mdTop = imageRect.top - cropRect.top;
		if (mdTop > 0) {
			translateRect(cropRect, 0, mdTop);
		}
		float mdBottom = imageRect.bottom - cropRect.bottom;
		if (mdBottom < 0) {
			translateRect(cropRect, 0, mdBottom);
		}

		this.invalidate();
	}

	/**
	 * 移动矩形
	 * 
	 * @param rect
	 * @param dx
	 * @param dy
	 */
	private static final void translateRect(RectF rect, float dx, float dy) {
		rect.left += dx;
		rect.right += dx;
		rect.top += dy;
		rect.bottom += dy;
	}

	/**
	 * 操作控制点 控制缩放
	 * 
	 * @param x
	 * @param y
	 */
	private void scaleCropController(float x, float y) {
		tempRect.set(cropRect);// 存贮原有数据，以便还原
		switch (selectedControllerCicle) {
		case 1:// 左上角控制点
			cropRect.left = x;
			cropRect.top = y;
			break;
		case 2:// 右上角控制点
			cropRect.right = x;
			cropRect.top = y;
			break;
		case 3:// 左下角控制点
			cropRect.left = x;
			cropRect.bottom = y;
			break;
		case 4:// 右下角控制点
			cropRect.right = x;
			cropRect.bottom = y;
			break;
		}// end switch

		if (ratio < 0) {// 任意缩放比
			// 边界条件检测
			validateCropRect();
			invalidate();
		} else {
			// 更新剪切矩形长宽
			// 确定不变点
			switch (selectedControllerCicle) {
			case 1:// 左上角控制点
			case 2:// 右上角控制点
				cropRect.bottom = (cropRect.right - cropRect.left) / this.ratio
						+ cropRect.top;
				break;
			case 3:// 左下角控制点
			case 4:// 右下角控制点
				cropRect.top = cropRect.bottom
						- (cropRect.right - cropRect.left) / this.ratio;
				break;
			}// end switch

			// validateCropRect();
			if (cropRect.left < imageRect.left
					|| cropRect.right > imageRect.right
					|| cropRect.top < imageRect.top
					|| cropRect.bottom > imageRect.bottom
					|| cropRect.width() < CIRCLE_WIDTH
					|| cropRect.height() < CIRCLE_WIDTH) {
				cropRect.set(tempRect);
			}
			invalidate();
		}// end if
	}

	/**
	 * 边界条件检测
	 * 
	 */
	private void validateCropRect() {
		if (cropRect.width() < CIRCLE_WIDTH) {
			cropRect.left = tempRect.left;
			cropRect.right = tempRect.right;
		}
		if (cropRect.height() < CIRCLE_WIDTH) {
			cropRect.top = tempRect.top;
			cropRect.bottom = tempRect.bottom;
		}
		if (cropRect.left < imageRect.left) {
			cropRect.left = imageRect.left;
		}
		if (cropRect.right > imageRect.right) {
			cropRect.right = imageRect.right;
		}
		if (cropRect.top < imageRect.top) {
			cropRect.top = imageRect.top;
		}
		if (cropRect.bottom > imageRect.bottom) {
			cropRect.bottom = imageRect.bottom;
		}
	}

	/**
	 * 是否选中控制点
	 * 
	 * -1为没有
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private int isSeletedControllerCircle(float x, float y) {
		if (leftTopCircleRect.contains(x, y))// 选中左上角
			return 1;
		if (rightTopCircleRect.contains(x, y))// 选中右上角
			return 2;
		if (leftBottomRect.contains(x, y))// 选中左下角
			return 3;
		if (rightBottomRect.contains(x, y))// 选中右下角
			return 4;
		return -1;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	/**
	 * 返回剪切矩形
	 * 
	 * @return
	 */
	public RectF getCropRect() {
		return new RectF(this.cropRect);
	}

	/**
	 * 缩放指定矩形
	 * 
	 * @param rect
	 */
	private static void scaleRect(RectF rect, float scaleX, float scaleY) {
		float w = rect.width();
		float h = rect.height();

		float newW = scaleX * w;
		float newH = scaleY * h;

		float dx = (newW - w) / 2;
		float dy = (newH - h) / 2;

		rect.left -= dx;
		rect.top -= dy;
		rect.right += dx;
		rect.bottom += dy;
	}

	/**
	 * 缩放指定矩形
	 * 
	 * @param rect
	 * @param scale
	 */
	private static void scaleRect(RectF rect, float scale) {
		scaleRect(rect, scale, scale);
	}

	public float getRatio() {
		return ratio;
	}

	public void setRatio(float ratio) {
		this.ratio = ratio;
	}

	@Override
	public void onCropActionBack(RectF destRect) {
		setRatioCropRect(destRect,-1);
	}

	@Override
	public void onCrop(float currentAngle,float currentNormalRectF2scaleRectF) {
	}

	@Override
	public void onCropBack(RectF destRect) {
		setRatioCropRect(destRect,-1);
	}

	public interface CropActiveListener{
		void onCropActive();
	}

	public void setmCropActiveListener(CropActiveListener mCropActiveListener) {
		this.mCropActiveListener = mCropActiveListener;
	}
}
