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

import static com.v7878.unsafe.ArtVersion.A13;
import static com.v7878.unsafe.ArtVersion.A14;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;
import static com.v7878.unsafe.foreign.LibArt.ART;
import static dev.answer.altercore.core.Runtime.DebugState.kNonJavaDebuggable;

import com.v7878.foreign.Arena;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.JNIUtils;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.Conditions;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

import java.util.Objects;

public class Runtime {
    public enum DebugState {
        // This doesn't support any debug features / method tracing. This is the expected state usually.
        kNonJavaDebuggable,
        // This supports method tracing and a restricted set of debug features (for ex: redefinition
        // isn't supported). We transition to this state when method tracing has started or when the
        // debugger was attached and transition back to NonDebuggable once the tracing has stopped /
        // the debugger agent has detached.
        kJavaDebuggable,
        // The runtime was started as a debuggable runtime. This allows us to support the extended set
        // of debug features (for ex: redefinition). We never transition out of this state.
        kJavaDebuggableAtInit
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(conditions = @Conditions(min_art = A14),
                name = "_ZN3art7Runtime20SetRuntimeDebugStateENS0_17RuntimeDebugStateE")
        @LibrarySymbol(conditions = @Conditions(max_art = A13),
                name = "_ZN3art7Runtime17SetJavaDebuggableEb")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, INT})
        abstract void SetRuntimeDebugState(long runtime, int state);

        @LibrarySymbol(name = "_ZN3art7Runtime19DeoptimizeBootImageEv")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void DeoptimizeBootImage(long runtime);

        static final Native INSTANCE = BulkLinker.generateImpl(SCOPE, Native.class, ART);
    }

    public static void setRuntimeDebugState(DebugState state) {
        Objects.requireNonNull(state);
        int value;
        if (ART_INDEX <= A13) {
            value = state == kNonJavaDebuggable ? 0 : 1;
        } else {
            value = state.ordinal();
        }
        Native.INSTANCE.SetRuntimeDebugState(JNIUtils.getRuntimePtr(), value);
    }

    public static void DeoptimizeBootImage() {
        var instance = Native.INSTANCE;
        var runtime = JNIUtils.getRuntimePtr();
        try (var ignored = new ScopedSuspendAll(false)) {
            instance.DeoptimizeBootImage(runtime);
        }
    }
}
