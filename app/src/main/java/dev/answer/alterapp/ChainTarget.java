package dev.answer.alterapp;

import java.util.Locale;

/**
 * Targets for the libxposed interceptor-chain tests. Every method is deliberately trivial and
 * side-effect-bearing where a test needs to observe whether the original ran.
 */
public class ChainTarget {

    /** How many times an original body actually executed. */
    public static int originCalls;

    /** Written by both constructors, so skipping one is observable. */
    public String label;

    public ChainTarget() {
    }

    public ChainTarget(String label) {
        originCalls++;
        this.label = label;
    }

    public ChainTarget(int marker) {
        originCalls++;
        this.label = "int:" + marker;
    }

    public static String shout(String message) {
        originCalls++;
        return message.toUpperCase(Locale.ROOT);
    }

    public int add(int left, int right) {
        originCalls++;
        return left + right;
    }

    public String blocked() {
        originCalls++;
        return "original";
    }

    public void mutate(StringBuilder builder) {
        originCalls++;
        builder.append("-body");
    }

    public static String boom() {
        originCalls++;
        throw new IllegalStateException("boom");
    }

    public static String protective(String value) {
        originCalls++;
        return "orig:" + value;
    }

    public static String passthrough(String value) {
        originCalls++;
        return "orig:" + value;
    }

    /** Target of the hook-id test: two builders share one id, so only the second stays live. */
    public static String identified(String value) {
        originCalls++;
        return "orig:" + value;
    }

    /** Target of the replaceHook test. */
    public static String replaced(String value) {
        originCalls++;
        return "orig:" + value;
    }
}
