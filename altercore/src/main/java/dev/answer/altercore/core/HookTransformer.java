/*
 * MIT License
 *
 * Copyright (c) 2026 vova7878
 * Modify 2026 AnswerDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.answer.altercore.core;

import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.Transformers.AbstractTransformer;

import java.lang.invoke.MethodHandle;

public interface HookTransformer {
    void transform(MethodHandle original, EmulatedStackFrame frame) throws Throwable;
}

final class HookTransformerImpl extends AbstractTransformer {
    private final MethodHandle original;
    private final HookTransformer transformer;

    HookTransformerImpl(MethodHandle original, HookTransformer transformer) {
        this.original = original;
        this.transformer = transformer;
    }

    @Override
    protected void transform(MethodHandle thiz, EmulatedStackFrame frame) throws Throwable {
        frame.type(original.type());
        transformer.transform(original, frame);
    }
}
