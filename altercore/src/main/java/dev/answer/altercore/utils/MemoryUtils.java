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

import static dev.answer.altercore.utils.PrctlConstants.PR_SET_VMA;
import static dev.answer.altercore.utils.PrctlConstants.PR_SET_VMA_ANON_NAME;
import static dev.answer.altercore.utils.UnsafeWrapper.getUnsafe;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import dev.answer.altercore.AlterConfig;
import dev.answer.altercore.core.NativeObject;
import dev.tmpfs.libcoresyscall.core.MemoryAccess;
import dev.tmpfs.libcoresyscall.core.MemoryAllocator;

public class MemoryUtils {

    private static int page_size = getUnsafe().pageSize();
    private static long address = 0;
    private static long offset = 0;
    public static NativeObject allocUnprotected(long size){
        if (size > page_size) {
            AlterLog.eA("Attempting to allocate too much memory space (%zx bytes)", size);
            return null;
        }
        if (address != 0) {
            long next_offset = offset + size;
            if (next_offset <= page_size) {
                NativeObject ptr = new NativeObject(address + offset);
                offset = next_offset;
                return ptr;
            }
        }


        try {
            long mapped = Os.mmap(0, page_size, OsConstants.PROT_READ | OsConstants.PROT_WRITE,
                    OsConstants.MAP_PRIVATE, null, 0);
            if (AlterConfig.debug)
                AlterLog.d(String.format("Mapped new memory %p (size %u)", mapped, page_size));

            if (!AlterConfig.anti_checks)
                Os.prctl(PR_SET_VMA, PR_SET_VMA_ANON_NAME, mapped, size, MemoryAllocator.copyCString("pine codes").getAddress());

            MemoryAccess.memset(mapped, 0, page_size);
            address = (mapped);
            offset = size;

            return new NativeObject(mapped);
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
    }
}
