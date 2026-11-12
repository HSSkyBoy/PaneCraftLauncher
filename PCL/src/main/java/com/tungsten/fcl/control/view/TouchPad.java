package com.tungsten.fcl.control.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

import com.tungsten.fcl.control.FCLInput;
import com.tungsten.fcl.control.GameMenu;
import com.tungsten.fcl.control.GestureMode;
import com.tungsten.fcl.control.MouseMoveMode;
import com.mio.util.AndroidUtilKt;
import com.tungsten.fcl.setting.MenuSetting;
import com.tungsten.fclauncher.bridge.FCLBridge;


import java.util.Objects;

public class TouchPad extends View {

    private final int screenWidth;
    private final int screenHeight;

    private GameMenu gameMenu;


    public void init(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
    }

    public TouchPad(Context context) {
        this(context, null);
    }

    public TouchPad(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchPad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.screenWidth = AndroidUtilKt.getScreenWidth();
        this.screenHeight = AndroidUtilKt.getScreenHeight();
        float density = getResources().getDisplayMetrics().density;
        this.touchSlop = Math.max(ViewConfiguration.get(context).getScaledTouchSlop(), 12f * density);
        init();
    }

    private void init() {
        path = new Path();
        linePaint.setAntiAlias(true);
        linePaint.setColor(Color.GREEN);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    private final float touchSlop;

    // 手势死区与防抖状态
    private float startX;
    private float startY;
    private long startDownTime;
    private boolean isDragStarted = false;
    private boolean isLongPressTriggered = false;

    // 双击快速反向操作（如 BUILD 模式双击攻击打怪，FIGHT 模式双击副手使用）
    private long lastTapTime = 0;
    private float lastTapX = 0;
    private float lastTapY = 0;
    private static final long DOUBLE_TAP_TIMEOUT = 280;
    private static final long TAP_TIMEOUT = 250;
    private static final long LONG_PRESS_TIMEOUT = 320;

    // 触摸坐标全程 float：中途取整会让慢移时的小步长增量整体丢失，转视角顿挫
    private float downX;
    private float downY;
    private long downTime;
    private float initialX;
    private float initialY;
    private boolean cancelMouseLeft = false;
    private boolean cancelMouseRight = false;
    private int currentPointerID;
    private int lastPointerCount;
    private boolean shouldBeDown = false;
    private final Handler handler = new Handler();
    // 触控加速：本次按住的累计滑动距离与上一 MOVE 事件时间，用于距离加速与滑动速度计算
    private float acceleratedDistance;
    private long lastMoveTime;
    // 光标模式快照：按住期间游戏开关界面（如按 E 开关背包）会切换捕获/菜单态，
    // 跨模式继续拖动时旧锚点对应另一模式的轨迹，须重新锚定防视角/光标突跳
    private int cursorMode;

    private final Runnable runnable = () -> {
        if (!gameMenu.getMenuSetting().isDisableGesture()) {
            isLongPressTriggered = true;
            if (getGestureMode() == GestureMode.BUILD) {
                gameMenu.getInput().sendKeyEvent(FCLInput.MOUSE_LEFT, true);
                cancelMouseLeft = true;
                cancelMouseRight = false;
            } else {
                gameMenu.getInput().sendKeyEvent(FCLInput.MOUSE_RIGHT, true);
                cancelMouseRight = true;
                cancelMouseLeft = false;
            }
        }
    };

    private GestureMode getGestureMode() {
        return gameMenu.getMenuSetting().getGestureMode();
    }

    /**
     * 释放长按手势合成的鼠标键（按下超过阈值后由 runnable 按下的左/右键）。
     * 按住期间切换光标模式会让 UP/CANCEL 落到另一模式的分支，故在分支之前统一释放
     */
    private void releaseHoldKeys() {
        if (cancelMouseLeft) {
            gameMenu.getInput().sendKeyEvent(FCLInput.MOUSE_LEFT, false);
            cancelMouseLeft = false;
        }
        if (cancelMouseRight) {
            gameMenu.getInput().sendKeyEvent(FCLInput.MOUSE_RIGHT, false);
            cancelMouseRight = false;
        }
        isLongPressTriggered = false;
    }

    private Path path;
    private final Paint linePaint = new Paint();
    private int prefX;
    private int prefY;
    private int selfX;
    private int selfY;

    private boolean showLineHorizontal = false;
    private boolean showLineVertical = false;

    public void drawLine(int orientation, int pref, int self) {
        if (orientation == 0) {
            showLineHorizontal = true;
            prefX = pref;
            selfX = self;
        } else {
            showLineVertical = true;
            prefY = pref;
            selfY = self;
        }

        init();
        invalidate();
    }

    public void removeLine(int orientation) {
        if (orientation == 0)
            showLineHorizontal = false;
        else
            showLineVertical = false;

        init();
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (showLineHorizontal) {
            path.moveTo(prefX, 0);
            path.lineTo(prefX, getHeight());
            path.moveTo(selfX, 0);
            path.lineTo(selfX, getHeight());
            canvas.drawPath(path, linePaint);
        }
        if (showLineVertical) {
            path.moveTo(0, prefY);
            path.lineTo(getWidth(), prefY);
            path.moveTo(0, selfY);
            path.lineTo(getWidth(), selfY);
            canvas.drawPath(path, linePaint);
        }
    }

    private final static String POINTER_ID = "TouchPad";

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 编辑模式下触摸落到触摸板说明未命中任何控件：取消选中
        if (gameMenu.isEditMode() && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            gameMenu.getViewManager().clearSelection();
        }
        if (gameMenu.getTouchController() != null) {
            gameMenu.getTouchController().handleTouchEvent(event);
        }
        // 手势起点与取消时对齐模式快照；按住期间的切换由各拖动分支的 MOVE 检测重锚
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            cursorMode = gameMenu.getCursorMode();
        }
        // 松手统一释放长按手势合成的鼠标键并撤回未触发的长按：抬起事件可能落到另一光标模式的分支，
        // 只在捕获分支释放会把按键留在按下状态
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            releaseHoldKeys();
            handler.removeCallbacks(runnable);
        }
        if (gameMenu.getCursorMode() == FCLBridge.CursorEnabled) {
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    gameMenu.getInput().setPointer(event.getRawX(), event.getRawY());
                }
                // 指针捕获未生效时外接鼠标点击以普通 touch 事件到达，这里补齐按键投递
                gameMenu.getInput().handleExternalTouchButtons(event);
                //防止被外接鼠标触发
                return true;
            }
            if (gameMenu.getMenuSetting().getMouseMoveMode() == MouseMoveMode.CLICK) {
                gameMenu.getInput().setPointerId(POINTER_ID);
                gameMenu.getInput().setPointer(event.getX(), event.getY(), POINTER_ID);
                gameMenu.getInput().setPointerId(null);
                // 按住跨模式拖动时保持快照一致，回捕获态后锚点检测才能识别模式切换
                cursorMode = gameMenu.getCursorMode();
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        Choreographer.getInstance().postFrameCallbackDelayed(frameTimeNanos -> gameMenu.getInput().sendKeyEvent(FCLInput.MOUSE_LEFT, true), 33);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        Choreographer.getInstance().postFrameCallbackDelayed(frameTimeNanos -> gameMenu.getInput().sendKeyEvent(FCLInput.MOUSE_LEFT, false), 33);
                        break;
                    default:
                        break;
                }
            } else {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        startX = downX;
                        startY = downY;
                        startDownTime = System.currentTimeMillis();
                        downTime = startDownTime;
                        initialX = gameMenu.getCursorX();
                        initialY = gameMenu.getCursorY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (cursorMode != gameMenu.getCursorMode()) {
                            // 按住期间进出过捕获态：锚点对应另一模式的光标位置，从当前光标处重新锚定防跳变
                            cursorMode = gameMenu.getCursorMode();
                            downX = event.getX();
                            downY = event.getY();
                            startX = downX;
                            startY = downY;
                            initialX = gameMenu.getCursorX();
                            initialY = gameMenu.getCursorY();
                            break;
                        }
                        float deltaX = (event.getX() - downX) * (float) gameMenu.getMenuSetting().getMouseSensitivityCursor();
                        float deltaY = (event.getY() - downY) * (float) gameMenu.getMenuSetting().getMouseSensitivityCursor();
                        float targetX = Math.max(0, Math.min(screenWidth, initialX + deltaX));
                        float targetY = Math.max(0, Math.min(screenHeight, initialY + deltaY));
                        gameMenu.getInput().setPointerId(POINTER_ID);
                        gameMenu.getInput().setPointer(targetX, targetY, POINTER_ID);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - startDownTime;
                        float distSq = (event.getX() - startX) * (event.getX() - startX)
                                + (event.getY() - startY) * (event.getY() - startY);
                        if (duration <= TAP_TIMEOUT && distSq <= touchSlop * touchSlop) {
                            gameMenu.getInput().sendKeyEvent(FCLInput.MOUSE_LEFT, true);
                            Choreographer.getInstance().postFrameCallbackDelayed(frameTimeNanos -> gameMenu.getInput().sendKeyEvent(FCLInput.MOUSE_LEFT, false), 33);
                        }
                        if (Objects.equals(gameMenu.getInput().getPointerId(), POINTER_ID)) {
                            gameMenu.getInput().setPointerId(null);
                        }
                        break;
                    default:
                        break;
                }
            }
        } else {
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                // 游戏捕获视角期间外接鼠标的点击同样以 touch 事件到达，转发按键
                gameMenu.getInput().handleExternalTouchButtons(event);
                return true;
            }
            if (gameMenu.getMenuSetting().isDisableLeftTouch() && event.getX() <= (float) screenWidth / 2) {
                return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    currentPointerID = event.getPointerId(0);
                    downX = event.getX();
                    downY = event.getY();
                    startX = downX;
                    startY = downY;
                    startDownTime = System.currentTimeMillis();
                    downTime = startDownTime;
                    isDragStarted = false;
                    isLongPressTriggered = false;
                    acceleratedDistance = 0;
                    lastMoveTime = event.getEventTime();
                    handler.postDelayed(runnable, LONG_PRESS_TIMEOUT);
                    break;
                case MotionEvent.ACTION_MOVE:
                    int pointerCount = event.getPointerCount();
                    int pointerIndex = event.findPointerIndex(currentPointerID);
                    if (cursorMode != gameMenu.getCursorMode()) {
                        // 按住期间进出过菜单态（如按 E 开关背包界面）：锚点对应菜单期间的轨迹，
                        // 直接计算会把整段位移一次性下发，重新锚定并丢弃本帧
                        cursorMode = gameMenu.getCursorMode();
                        downX = pointerIndex == -1 ? event.getX() : event.getX(pointerIndex);
                        downY = pointerIndex == -1 ? event.getY() : event.getY(pointerIndex);
                        startX = downX;
                        startY = downY;
                        acceleratedDistance = 0;
                        lastMoveTime = event.getEventTime();
                        break;
                    }
                    if (pointerIndex == -1 || lastPointerCount != pointerCount || !shouldBeDown) {
                        shouldBeDown = true;
                        currentPointerID = event.getPointerId(0);
                        downX = event.getX();
                        downY = event.getY();
                        startX = downX;
                        startY = downY;
                        acceleratedDistance = 0;
                        lastMoveTime = event.getEventTime();
                        break;
                    }
                    float newDownX = event.getX(pointerIndex);
                    float newDownY = event.getY(pointerIndex);

                    // 手势死区防抖判定：未达到滑动阈值前不转动视角、不打断长按计时
                    if (!isDragStarted && !isLongPressTriggered) {
                        float totalDX = newDownX - startX;
                        float totalDY = newDownY - startY;
                        if (totalDX * totalDX + totalDY * totalDY > touchSlop * touchSlop) {
                            isDragStarted = true;
                            handler.removeCallbacks(runnable);
                            downX = newDownX;
                            downY = newDownY;
                        } else {
                            break;
                        }
                    }

                    float frameDX = newDownX - downX;
                    float frameDY = newDownY - downY;
                    float acceleration = viewAcceleration(event, frameDX, frameDY);
                    // 捕获态统一走相对增量流，与陀螺仪等来源的增量叠加互不干扰
                    double sensitivity = gameMenu.getMenuSetting().getMouseSensitivity();
                    float gameDX = (float) (frameDX * sensitivity * acceleration);
                    float gameDY = (float) (frameDY * sensitivity * acceleration);
                    downX = newDownX;
                    downY = newDownY;
                    // 非同时控制模式下先开始拖动的一方独占视角，被占用时丢弃增量；锚点已推进，不累积位移
                    if (gameMenu.getInput().acquireLookOwner(POINTER_ID)) {
                        FCLBridge.pushEventLookDelta(gameDX, gameDY);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    gameMenu.getInput().releaseLookOwner(POINTER_ID);
                    if (Objects.equals(gameMenu.getInput().getPointerId(), POINTER_ID)) {
                        gameMenu.getInput().setPointerId(null);
                    }
                    shouldBeDown = false;
                    currentPointerID = -1;
                    handler.removeCallbacks(runnable);

                    if (!isDragStarted && !isLongPressTriggered) {
                        long duration = System.currentTimeMillis() - startDownTime;
                        float totalDistSq = (event.getX() - startX) * (event.getX() - startX)
                                + (event.getY() - startY) * (event.getY() - startY);
                        if (duration <= TAP_TIMEOUT && totalDistSq <= touchSlop * touchSlop) {
                            if (!gameMenu.getMenuSetting().isDisableGesture()) {
                                long now = System.currentTimeMillis();
                                float distFromLastTapSq = (event.getX() - lastTapX) * (event.getX() - lastTapX)
                                        + (event.getY() - lastTapY) * (event.getY() - lastTapY);
                                if (now - lastTapTime <= DOUBLE_TAP_TIMEOUT && distFromLastTapSq <= touchSlop * touchSlop * 4) {
                                    // 双击手势：执行反向操作（BUILD 模式双击即为左键攻击，FIGHT 模式双击为右键使用）
                                    lastTapTime = 0;
                                    int key = (getGestureMode() == GestureMode.BUILD) ? FCLInput.MOUSE_LEFT : FCLInput.MOUSE_RIGHT;
                                    gameMenu.getInput().sendKeyEvent(key, true);
                                    Choreographer.getInstance().postFrameCallbackDelayed(frameTimeNanos -> gameMenu.getInput().sendKeyEvent(key, false), 33);
                                } else {
                                    // 单击手势：正常操作（BUILD 模式为右键放置/互动，FIGHT 模式为左键攻击）
                                    lastTapTime = now;
                                    lastTapX = event.getX();
                                    lastTapY = event.getY();
                                    int key = (getGestureMode() == GestureMode.BUILD) ? FCLInput.MOUSE_RIGHT : FCLInput.MOUSE_LEFT;
                                    gameMenu.getInput().sendKeyEvent(key, true);
                                    Choreographer.getInstance().postFrameCallbackDelayed(frameTimeNanos -> gameMenu.getInput().sendKeyEvent(key, false), 33);
                                }
                            }
                        }
                    }
                    releaseHoldKeys();
                    break;
                default:
                    break;
            }
            lastPointerCount = event.getPointerCount();
        }
        return true;
    }

    /**
     * 转视角的触控加速系数。
     * 滑动加速：滑动速度超过慢拖上限后线性放大，快速甩动时视角转动更远；
     * 距离加速：系数随本次按住的累计滑动距离增长，长距离连续拖动逐渐加快。
     * 两项加速均在松手或重新按下时归零。
     */
    private float viewAcceleration(MotionEvent event, float frameDX, float frameDY) {
        MenuSetting setting = gameMenu.getMenuSetting();
        if (!setting.isSlideAcceleration() && !setting.isDistanceAcceleration()) {
            return 1f;
        }
        float movement = Math.abs(frameDX) + Math.abs(frameDY);
        float speedMult = 1f;
        float distMult = 1f;
        if (setting.isSlideAcceleration()) {
            float dt = Math.max(1, event.getEventTime() - lastMoveTime);
            // 速度单位 px/ms，慢拖约低于 1；线性放大，约 7px/ms 快甩时达到 3 倍封顶
            float speed = movement / dt;
            speedMult = Math.min(1f + Math.max(0, speed - 1f) * 0.3f, 3f);
        }
        if (setting.isDistanceAcceleration()) {
            acceleratedDistance += movement;
            // 累计滑动每 1000px 增加 1 倍，2 倍封顶
            distMult = 1f + Math.min(acceleratedDistance, 1000f) / 1000f;
        }
        lastMoveTime = event.getEventTime();
        return speedMult * distMult;
    }
}
