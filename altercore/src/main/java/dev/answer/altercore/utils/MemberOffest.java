/*
 * Copyright (C) 2025 AnswerDev. All rights reserved.
 *
 * Licensed under the GNU General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by AnswerDev
 */

package dev.answer.altercore.utils;

import static dev.answer.altercore.utils.UnsafeWrapper.getUnsafe;

import dev.answer.altercore.NativeImpl;
import dev.answer.altercore.core.NativeObject;

public class MemberOffest extends NativeObject {
    private long mOffest;

    public MemberOffest() {
        super(0);
    }
    public MemberOffest(long offest) {
        super(0);
        this.mOffest = offest;
    }

    long getAs(long instance) {
        if (!isValid()) {
            return 0;
        }
        return ( instance + mOffest);
    }

    public NativeObject get(long instance) {
        return new NativeObject(getAs(instance));
    }

    public NativeObject get(NativeObject instance) {
        return new NativeObject(getAs(instance.address()));
    }

    public void setAs(long instance, long value) {
        if (!isValid()) {
            return;
        }
        NativeImpl.memcpy(instance + mOffest, value, getUnsafe().addressSize());
    }

    public void set(NativeObject instance, NativeObject value) {
        setAs(instance.address(), value.address());
    }
    public void set(long instance, NativeObject value) {
        setAs(instance, value.address());
    }

    public void set(long instance, long value) {
        setAs(instance, value);
    }

    public void setOffset(long offset) {
        this.mOffest = offset;
    }

    public long getOffset() {
        return this.mOffest;
    }

    public boolean isValid() {
        return this.mOffest >= 0;
    }
}
