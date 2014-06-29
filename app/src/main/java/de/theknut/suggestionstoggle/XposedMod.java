package de.theknut.suggestionstoggle;

import android.text.InputType;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.removeAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;

public class XposedMod extends XC_MethodHook implements IXposedHookLoadPackage {

    private static String ISHOOKED = "ISHOOKED";
    private static int NOT_INITIALIZED = -1;
    private static int MOTION_EVENT = 0;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        XposedHelpers.findAndHookMethod(TextView.class, "onTouchEvent", MotionEvent.class, new XC_MethodHook() {

            GestureDetector gestureDetector;
            EditText editText;
            int inputType = NOT_INITIALIZED;

            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                if (!(param.thisObject instanceof EditText)) return;

                if (getAdditionalInstanceField(param.thisObject, ISHOOKED) == null) {
                    editText = (EditText) param.thisObject;
                    gestureDetector = new GestureDetector(editText.getContext(), new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            if (inputType == NOT_INITIALIZED) {
                                inputType = editText.getInputType();
                                editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                                editText.setSingleLine(false);
                            } else {
                                editText.setInputType(inputType);
                                inputType = NOT_INITIALIZED;
                                removeAdditionalInstanceField(editText, ISHOOKED);
                            }
                            return true;
                        }
                    });
                    setAdditionalInstanceField(editText, ISHOOKED, true);
                }

                gestureDetector.onTouchEvent((MotionEvent) param.args[MOTION_EVENT]);
            }
        });
    }
}