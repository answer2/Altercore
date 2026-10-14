package dev.answer.alterapp;

/** Target for the legacy-API compatibility checks; kept separate from the chain targets. */
public class LegacyTarget {

    public static int originCalls;

    public static String echo(String value) {
        originCalls++;
        return "orig:" + value;
    }
}
