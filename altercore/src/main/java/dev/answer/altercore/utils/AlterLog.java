package dev.answer.altercore.utils;

import android.util.Log;
import static dev.answer.altercore.AlterConfig.debug;

public class AlterLog {
        public static final String TAG = "AlterCore-Framework";

        public static int v(String s) {
            return debug ? Log.v(TAG, s) : 0;
        }

        public static int i(String s) {
            return debug ? Log.i(TAG, s) : 0;
        }

        public static int d(String s) {
            return debug ? Log.d(TAG, s) : 0;
        }

        public static int w(String s) {
            return Log.w(TAG, s);
        }

        public static int e(String s) {
            return Log.e(TAG, s);
        }

        public static int e(String s, Throwable t) {
            return Log.e(TAG, s, t);
        }

}
