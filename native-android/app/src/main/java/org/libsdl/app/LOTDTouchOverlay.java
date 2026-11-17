package org.libsdl.app;

// LOTD_STAGE12_NATIVE_HUD
// Native counterpart to the earlier WebView V4/V5 touch controls. No WebView.
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public final class LOTDTouchOverlay extends FrameLayout {
    // Native JNI queues events on SDL's thread-safe queue. user32.c handles them
    // on the game thread, so no direct cross-thread modification of the game state.
    private static native void nativeSend(int operation, int arg1, int arg2);
    private final SDLActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FrameLayout gamePad;
    private final Button toggle;
    private final boolean[] held = new boolean[15];
    private final Runnable[] pendingRelease = new Runnable[15];
    private boolean controlsEnabled = false;

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }
    private static void event(int op, int a, int b) {
        try { nativeSend(op, a, b); }
        catch (UnsatisfiedLinkError ex) { android.util.Log.e("LOTD-CTRL", "Native control bridge missing", ex); }
    }
    public LOTDTouchOverlay(SDLActivity activity) {
        super(activity);
        this.activity = activity;
        setClickable(false);
        setFocusable(false);
        setMotionEventSplittingEnabled(true);

        gamePad = new FrameLayout(activity);
        gamePad.setClickable(false);
        gamePad.setMotionEventSplittingEnabled(true);
        addView(gamePad, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Touching the right half moves the game's virtual mouse (free aim).
        View look = new View(activity);
        look.setBackgroundColor(Color.TRANSPARENT);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams lookLp = new FrameLayout.LayoutParams(
                Math.max(dp(220), (int)(screenWidth * 0.53f)), LayoutParams.MATCH_PARENT, Gravity.RIGHT);
        gamePad.addView(look, lookLp);
        installLook(look);

        // Left stick: stable fixed position; converts to held WASD keys.
        Stick joystick = new Stick(activity);
        FrameLayout.LayoutParams moveLp = new FrameLayout.LayoutParams(dp(152), dp(152), Gravity.LEFT | Gravity.BOTTOM);
        moveLp.leftMargin = dp(16);
        moveLp.bottomMargin = dp(18);
        gamePad.addView(joystick, moveLp);

        addHold("FIRE", 1, 90, 90, Gravity.RIGHT | Gravity.BOTTOM, dp(12), dp(155), true);
        // LOTD_STAGE14_RIGHT_RUN_DUCK: RUN and DUCK directly under FIRE, ALT shifted left
        addHold("ALT", 2, 62, 46, Gravity.RIGHT | Gravity.BOTTOM, dp(110), dp(42), false);
        addHold("USE E", 3, 70, 44, Gravity.RIGHT | Gravity.BOTTOM, dp(113), dp(212), false);
        addHold("RELOAD", 4, 82, 44, Gravity.RIGHT | Gravity.BOTTOM, dp(111), dp(156), false);
        addHold("JUMP", 5, 73, 44, Gravity.RIGHT | Gravity.BOTTOM, dp(110), dp(100), false);
        addHold("RUN", 6, 67, 44, Gravity.RIGHT | Gravity.BOTTOM, dp(22), dp(96), false);
        addHold("DUCK", 11, 67, 44, Gravity.RIGHT | Gravity.BOTTOM, dp(22), dp(38), false);
        gamePad.setVisibility(View.GONE); // menus keep normal taps until Controls ON

        // Always accessible even with in-game controls hidden (profile/menu stage).
        LinearLayout top = new LinearLayout(activity);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams bar = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, dp(48), Gravity.TOP | Gravity.LEFT);
        bar.leftMargin = dp(7);
        bar.topMargin = dp(5);
        addView(top, bar);
        Button esc = button("ESC", false);
        top.addView(esc, toolbarParams(55));
        esc.setOnClickListener(v -> tap(12));
        Button keyboard = button("KEYBOARD", false);
        top.addView(keyboard, toolbarParams(94));
        keyboard.setOnClickListener(v -> showKeyboard());
        Button enter = button("ENTER", false);
        top.addView(enter, toolbarParams(66));
        enter.setOnClickListener(v -> tap(13));
        toggle = button("CONTROLS ON", false);
        top.addView(toggle, toolbarParams(114));
        toggle.setOnClickListener(v -> setControlsEnabled(!controlsEnabled));
    }
    private LinearLayout.LayoutParams toolbarParams(int widthDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(widthDp), dp(42));
        lp.rightMargin = dp(4);
        return lp;
    }
    private Button button(String text, boolean fire) {
        Button b = new Button(activity);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(fire ? 15f : 11f);
        b.setPadding(dp(4), dp(2), dp(4), dp(2));
        b.setMinHeight(0); b.setMinimumHeight(0);
        b.setMinWidth(0); b.setMinimumWidth(0);
        b.setElevation(dp(3));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fire ? 0xCC9A301F : 0xC8202D35);
        bg.setCornerRadius(dp(fire ? 44 : 12));
        bg.setStroke(dp(1), 0xAAFFFFFF);
        b.setBackground(bg);
        return b;
    }
    private void addHold(String label, int action, int w, int h, int gravity,
                         int marginSide, int bottom, boolean fire) {
        final Button b = button(label, fire);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(w), dp(h), gravity);
        if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) p.rightMargin = marginSide;
        else p.leftMargin = marginSide;
        p.bottomMargin = bottom;
        gamePad.addView(b, p);
        b.setOnTouchListener(new OnTouchListener() {
            int pointerId = -1;
            long started;
            @Override public boolean onTouch(View view, MotionEvent e) {
                int kind = e.getActionMasked();
                if (kind == MotionEvent.ACTION_DOWN) {
                    pointerId = e.getPointerId(0);
                    started = SystemClock.uptimeMillis();
                    b.setPressed(true);
                    setHeld(action, true);
                    return true;
                }
                if (kind == MotionEvent.ACTION_UP || kind == MotionEvent.ACTION_CANCEL) {
                    if (pointerId >= 0) {
                        b.setPressed(false);
                        pointerId = -1;
                        // Keep very short taps alive long enough for an engine frame.
                        long delay = Math.max(0, 110 - (SystemClock.uptimeMillis() - started));
                        releaseLater(action, delay);
                    }
                    return true;
                }
                return true;
            }
        });
    }
    private void setHeld(int code, boolean down) {
        if (code < 1 || code >= held.length) return;
        Runnable prior = pendingRelease[code];
        if (prior != null) { handler.removeCallbacks(prior); pendingRelease[code] = null; }
        if (held[code] != down) {
            held[code] = down;
            if (code == 1) android.util.Log.i("LOTD-FIRE", "HUD FIRE " + (down ? "DOWN" : "UP")); // LOTD_STAGE13_FIRE_SDL_MOUSE_PATH
            event(1, code, down ? 1 : 0);
        }
    }
    private void releaseLater(final int code, long delay) {
        if (delay <= 0) { setHeld(code, false); return; }
        Runnable old = pendingRelease[code];
        if (old != null) handler.removeCallbacks(old);
        Runnable task = () -> { pendingRelease[code] = null; setHeld(code, false); };
        pendingRelease[code] = task;
        handler.postDelayed(task, delay);
    }
    private void tap(int code) {
        setHeld(code, true);
        releaseLater(code, 130);
    }
    private void setControlsEnabled(boolean enabled) {
        controlsEnabled = enabled;
        gamePad.setVisibility(enabled ? View.VISIBLE : View.GONE);
        toggle.setText(enabled ? "CONTROLS OFF" : "CONTROLS ON");
        if (!enabled) {
            for (int i = 1; i < held.length; i++) setHeld(i, false);
        }
    }
    private void installLook(View view) {
        view.setOnTouchListener(new OnTouchListener() {
            int lookPointer = -1;
            float lastX, lastY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                int kind = e.getActionMasked();
                if (kind == MotionEvent.ACTION_DOWN) {
                    lookPointer = e.getPointerId(0);
                    lastX = e.getX(0); lastY = e.getY(0);
                    return true;
                }
                if (kind == MotionEvent.ACTION_MOVE && lookPointer >= 0) {
                    int idx = e.findPointerIndex(lookPointer);
                    if (idx >= 0) {
                        float x = e.getX(idx), y = e.getY(idx);
                        int dx = Math.max(-90, Math.min(90, Math.round((x - lastX) * 1.25f)));
                        int dy = Math.max(-90, Math.min(90, Math.round((y - lastY) * 1.25f)));
                        lastX = x; lastY = y;
                        if (dx != 0 || dy != 0) event(2, dx, dy);
                    }
                    return true;
                }
                if (kind == MotionEvent.ACTION_UP || kind == MotionEvent.ACTION_CANCEL) {
                    lookPointer = -1;
                    return true;
                }
                return true;
            }
        });
    }
    private void showKeyboard() {
        // Real Android EditText/IME, not SDL's hidden DummyEdit. The SEND action
        // forwards printable ASCII as the same WM_CHAR events used by V4 WebView.
        final EditText entry = new EditText(activity);
        entry.setSingleLine(true);
        entry.setHint("Profile name (English letters)");
        entry.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        entry.setImeOptions(EditorInfo.IME_ACTION_DONE);
        LinearLayout wrap = new LinearLayout(activity);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(18), dp(6), dp(18), dp(3));
        wrap.addView(entry, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(54)));
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("LOTD - Profile keyboard")
                .setMessage("Tap the name field in the game first. Then type here and press SEND.")
                .setView(wrap)
                .setPositiveButton("SEND", (d, which) -> {
                    String text = entry.getText().toString();
                    for (int i = 0; i < text.length(); i++) {
                        char c = text.charAt(i);
                        if (c >= 32 && c <= 126) event(3, c, 0);
                    }
                })
                .setNeutralButton("BACKSPACE", (d, which) -> tap(14))
                .setNegativeButton("CANCEL", (d, which) -> {})
                .create();
        dialog.getWindow();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        entry.requestFocus();
        entry.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(entry, InputMethodManager.SHOW_IMPLICIT);
        }, 130);
    }
    private final class Stick extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float bx = 0f, by = 0f;
        int activePointer = -1;
        Stick(Context ctx) { super(ctx); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float cx = getWidth()/2f, cy = getHeight()/2f;
            float radius = Math.min(cx,cy) - dp(3);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x66202935); c.drawCircle(cx,cy,radius,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2));
            p.setColor(0xAAFFFFFF); c.drawCircle(cx,cy,radius,p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xAA8BB5C4); c.drawCircle(cx+bx,cy+by,dp(32),p);
            p.setStyle(Paint.Style.STROKE); p.setColor(0xBBFFFFFF);
            c.drawCircle(cx+bx,cy+by,dp(32),p);
        }
        private void update(float x, float y) {
            float cx = getWidth()/2f, cy = getHeight()/2f;
            float mx = x - cx, my = y - cy;
            float max = Math.max(dp(12), Math.min(cx,cy)*0.68f);
            float len = (float)Math.hypot(mx,my);
            if (len > max) { mx *= max/len; my *= max/len; }
            bx = mx; by = my; invalidate();
            float dead = max * 0.26f;
            setHeld(7, my < -dead);  // W
            setHeld(8, mx < -dead);  // A
            setHeld(9, my > dead);   // S
            setHeld(10, mx > dead);  // D
        }
        private void reset() {
            bx=by=0f; invalidate(); activePointer = -1;
            for (int a=7; a<=10; a++) setHeld(a, false);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    activePointer=e.getPointerId(0);
                    update(e.getX(0),e.getY(0)); return true;
                case MotionEvent.ACTION_MOVE:
                    if (activePointer >= 0) {
                        int idx=e.findPointerIndex(activePointer);
                        if (idx>=0) update(e.getX(idx),e.getY(idx));
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    reset(); return true;
            }
            return true;
        }
    }
}
