/*
 * Copyright (c) 2025 canyie
 * Modify 2026 AnswerDev
 * "Anti 996" License Version 1.0 (Draft)
 *
 * Permission is hereby granted to any individual or legal entity
 * obtaining a copy of this licensed work (including the source code,
 * documentation and/or related items, hereinafter collectively referred
 * to as the "licensed work"), free of charge, to deal with the licensed
 * work for any purpose, including without limitation, the rights to use,
 * reproduce, modify, prepare derivative works of, distribute, publish
 * and sublicense the licensed work, subject to the following conditions:
 *
 * 1. The individual or the legal entity must conspicuously display,
 * without modification, this License and the notice on each redistributed
 * or derivative copy of the Licensed Work.
 *
 * 2. The individual or the legal entity must strictly comply with all
 * applicable laws, regulations, rules and standards of the jurisdiction
 * relating to labor and employment where the individual is physically
 * located or where the individual was born or naturalized; or where the
 * legal entity is registered or is operating (whichever is stricter). In
 * case that the jurisdiction has no such laws, regulations, rules and
 * standards or its laws, regulations, rules and standards are
 * unenforceable, the individual or the legal entity are required to
 * comply with Core International Labor Standards.
 *
 * 3. The individual or the legal entity shall not induce, suggest or force
 * its employee(s), whether full-time or part-time, or its independent
 * contractor(s), in any methods, to agree in oral or written form, to
 * directly or indirectly restrict, weaken or relinquish his or her
 * rights or remedies under such laws, regulations, rules and standards
 * relating to labor and employment as mentioned above, no matter whether
 * such written or oral agreements are enforceable under the laws of the
 * said jurisdiction, nor shall such individual or the legal entity
 * limit, in any methods, the rights of its employee(s) or independent
 * contractor(s) from reporting or complaining to the copyright holder or
 * relevant authorities monitoring the compliance of the license about
 * its violation(s) of the said license.
 *
 * THE LICENSED WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN ANY WAY CONNECTION WITH THE
 * LICENSED WORK OR THE USE OR OTHER DEALINGS IN THE LICENSED WORK.
 */

package dev.answer.altercore.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;

import dev.answer.altercore.AlterCore;

public class HookParams {
    /**
     * The calling method.
     */
    public final Member method;

    /**
     * The "this" object of this call, {@code null} if executing a static method.
     * Change it in {@code beforeCall} to set new object as "this" when calling original method.
     */
    public Object thisObject;

    /**
     * The arguments passed to the method in this call. Will never be null.
     * Change it or its value in {@code beforeCall} to change arguments when calling original method.
     */
    public Object[] args;
    private Object result;
    private Throwable throwable;
    /* package */ public boolean returnEarly;
    private HookRecord hookRecord;

    public HookParams(HookRecord hookRecord, Object thisObject, Object[] args) {
        this.hookRecord = hookRecord;
        this.method = hookRecord.target;
        this.thisObject = thisObject;
        this.args = args;
    }

    /**
     * Get the result that will be returned in this method call.
     * @return The result that will be returned in this method call
     */
    public Object getResult() {
        return result;
    }

    /**
     * Set a result that will be returned in this method call.
     * If you call it {@code beforeCall}, the original method call will be prevented, and next
     * hooks will not be called.
     * @param result The return value you want to set.
     */
    public void setResult(Object result) {
        this.result = result;
        this.throwable = null;
        this.returnEarly = true;
    }

    /**
     * Like {@link HookParams#setResult(Object)} but only set the return value if no exception will be thrown.
     * @param result The return value you want to set.
     */
    public void setResultIfNoException(Object result) {
        if (this.throwable == null) {
            this.result = result;
            this.returnEarly = true;
        }
    }

    /**
     * Get the exception that will be thrown in this method call.
     * @return The exception that will be thrown in this method call.
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Return whether an exception will be thrown as the result of this method call.
     * @return {@code true} If there is an exception will be thrown, {@code false} otherwise.
     */
    public boolean hasThrowable() {
        return throwable != null;
    }

    /**
     * Set the exception that will be thrown in this method call.
     * If you call it {@code beforeCall}, the original method call will be prevented, and next
     * hooks will not be called.
     * @param throwable The exception you want to throw.
     */
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
        this.result = null;
        this.returnEarly = true;
    }

    /**
     * Like {@link HookParams#getResult()} but throwing an exception if there is an exception set.
     * @return The result of this method call
     * @throws Throwable The exception happened in this method call
     */
    public Object getResultOrThrowable() throws Throwable {
        if (throwable != null)
            throw throwable;
        return result;
    }

    /**
     * Reset any previous result or exception, and allows the original method to be executed.
     */
    public void resetResult() {
        this.result = null;
        this.throwable = null;
        this.returnEarly = false;
    }

    /**
     * Invokes the original (un-hooked) implementation of the target with the current
     * {@code thisObject} and {@code args} already captured by this callback invocation.
     */
    public Object invokeOriginalMethod() throws IllegalAccessException, InvocationTargetException {
        return invokeOriginalMethod(thisObject, args);
    }

    /**
     * Invokes the original (un-hooked) implementation of the target with an explicit
     * receiver and arguments, instead of the ones captured by this callback invocation.
     */
    public Object invokeOriginalMethod(Object thisObject, Object... args)
            throws IllegalAccessException, InvocationTargetException {
        return AlterCore.invokeOriginalMethod(hookRecord.target, thisObject, args);
    }

}