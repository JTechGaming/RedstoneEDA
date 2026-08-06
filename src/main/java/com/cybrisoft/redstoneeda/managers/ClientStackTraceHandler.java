package com.cybrisoft.redstoneeda.managers;

import java.util.Stack;

public class ClientStackTraceHandler {
    private static final Stack<Trace> stackTrace = new Stack<>();

    public static void push(Trace trace) {
        stackTrace.push(trace);
    }

    public static Trace pop() {
        return stackTrace.pop();
    }

    public static Trace peek() {
        return stackTrace.peek();
    }

    public static Stack<Trace> getAll() {
        return stackTrace;
    }

    public static void clear() {
        stackTrace.clear();
    }
}
