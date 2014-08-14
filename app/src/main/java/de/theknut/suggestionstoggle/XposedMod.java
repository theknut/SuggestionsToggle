package de.theknut.suggestionstoggle;

import android.graphics.Rect;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;

public class XposedMod extends XC_MethodHook implements IXposedHookLoadPackage {

    private static GestureDetector gestureDetector;
    private static String ISHOOKED = "ISHOOKED";
    private static String INPUTTYPE = "INPUTTYPE";
    private static int NOT_INITIALIZED = -1;

    @Override
    public void handleLoadPackage(LoadPackageParam loadPackageParam) throws Throwable {

        XposedHelpers.findAndHookMethod(TextView.class, "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
            private int MOTION_EVENT = 0;

            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                if (!(param.thisObject instanceof EditText)) return;

                if (gestureDetector != null) {
                    // detect double tap
                    gestureDetector.onTouchEvent((MotionEvent) param.args[MOTION_EVENT]);
                }
            }
        });

        XposedHelpers.findAndHookMethod(TextView.class, "onFocusChanged", boolean.class, Integer.TYPE, Rect.class, new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                if (!(param.thisObject instanceof EditText)) return;
                boolean hasFocus = (Boolean) param.args[0];

                if (hasFocus) {
                    // check if we need to hook this EditText
                    if (getAdditionalInstanceField(param.thisObject, ISHOOKED) == null || !(Boolean) getAdditionalInstanceField(param.thisObject, ISHOOKED)) {

                        final EditText editText = (EditText) param.thisObject;
                        gestureDetector = new GestureDetector(editText.getContext(), new GestureDetector.SimpleOnGestureListener() {

                            @Override
                            public boolean onDoubleTap(MotionEvent e) {
                                int inputType = (Integer) getAdditionalInstanceField(editText, INPUTTYPE);
                                if (inputType == NOT_INITIALIZED) {
                                    // save original inputtype
                                    setAdditionalInstanceField(editText, INPUTTYPE, editText.getInputType());

                                    if (suggestionsDisabled(editText.getInputType())) {
                                        // enable suggestions if disabled
                                        editText.setInputType(editText.getInputType() & ~0x80090);
                                        // keep multiline
                                        editText.setSingleLine(false);
                                    } else {
                                        // disable suggestions if enabled
                                        editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                                        // keep multiline
                                        editText.setSingleLine(false);
                                    }
                                } else {
                                    // we are done with this EditText
                                    unhookView((EditText) param.thisObject);
                                }
                                return true;
                            }

                            private boolean suggestionsDisabled(int inputType) {
                                // TYPE_TEXT_FLAG_NO_SUGGESTIONS    = 0x80000
                                // TYPE_TEXT_PASSWORD               = 0x80
                                // TYPE_TEXT_VISIBLE_PASSWORD       = 0x90
                                return (inputType & 0x80090) > 0;
                            }
                        });

                        // set additional fields to identify the EditText
                        setAdditionalInstanceField(editText, ISHOOKED, true);
                        setAdditionalInstanceField(editText, INPUTTYPE, NOT_INITIALIZED);
                    }
                } else {
                    // remove our hook if we lost focus
                    if (getAdditionalInstanceField(param.thisObject, ISHOOKED) != null && (Boolean) getAdditionalInstanceField(param.thisObject, ISHOOKED)) {
                        unhookView((EditText) param.thisObject);
                        gestureDetector = null;
                    }
                }
            }
        });
    }

    private static void unhookView(EditText textview) {
        int inputType = (Integer) getAdditionalInstanceField(textview, INPUTTYPE);
        if (inputType != NOT_INITIALIZED) {
            // restore initial inputType
            textview.setInputType(inputType);
        }

        setAdditionalInstanceField(textview, INPUTTYPE, NOT_INITIALIZED);
        setAdditionalInstanceField(textview, ISHOOKED, false);
    }
}