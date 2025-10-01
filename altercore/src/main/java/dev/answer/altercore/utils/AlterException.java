package dev.answer.altercore.utils;

/**
 * @Author Answer.Dev
 * @Date 2024/02/03 12:08
 */
public class AlterException extends Exception {

    public static final String TAG = AlterException.class.getName();

    public AlterException(String message) {
        super(message);
    }

    public AlterException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlterException(Throwable cause) {
        super(TAG, cause);
    }

}

