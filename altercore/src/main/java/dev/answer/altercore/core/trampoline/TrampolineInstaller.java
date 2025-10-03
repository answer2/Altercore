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

package dev.answer.altercore.core.trampoline;

import static dev.answer.altercore.utils.UnsafeWrapper.getUnsafe;

import android.system.ErrnoException;
import android.system.Os;

import dev.answer.altercore.NativeImpl;
import dev.answer.altercore.core.ArtMethodObject;
import dev.answer.altercore.core.NativeObject;
import dev.answer.altercore.core.trampoline.arch.ArchInfo;
import dev.answer.altercore.utils.AlterLog;
import dev.answer.altercore.utils.MemoryUtils;

public class TrampolineInstaller {
    public TrampolineInstaller getOrInitDefault() {
//        if (default_ == nullptr) {
//#ifdef __aarch64__
//            default_ = new Arm64TrampolineInstaller;
//#elif defined(__arm__)
//                    default_ = new Thumb2TrampolineInstaller;
//#elif defined(__i386__)
//                    default_ = new X86TrampolineInstaller;
//#endif
//            default_->Init();
//        }
//        return default_;
        return null;
    }


    public static NativeObject CreateDirectJumpTrampoline(ArchInfo info, NativeObject to) {
        NativeObject mem = MemoryUtils.allocUnprotected(info.kDirectJumpTrampolineSize);
        if (mem.isNull()) {
            AlterLog.e("Failed to allocate direct jump trampoline!");
            return NativeObject.EMPTY_OBJECT;
        }
        WriteDirectJumpTrampolineTo(info, mem, to);
        return mem;
    }

    public static void WriteDirectJumpTrampolineTo(ArchInfo info, NativeObject mem, NativeObject jump_to) {
        NativeImpl.memcpy(mem.address(), info.kDirectJumpTrampoline, info.kDirectJumpTrampolineSize);
        NativeObject to_out = new NativeObject(mem.address() + info.kDirectJumpTrampolineEntryOffset);
        NativeImpl.memcpy(to_out.address(), jump_to.peekPointer().address(), getUnsafe().addressSize());
        try {
            Os.munmap(mem.address(), info.kDirectJumpTrampolineSize);
        } catch (ErrnoException e) {
            e.printStackTrace();
            AlterLog.e("Clear this catch failure!");
        }
    }


    public static NativeObject CreateBridgeJumpTrampoline(ArchInfo info, ArtMethodObject target, ArtMethodObject bridge,
                                                          NativeObject origin_code_entry) throws ErrnoException {
        NativeObject mem = MemoryUtils.allocUnprotected(info.kBridgeJumpTrampolineSize);
        if (mem.isNull()) {
            AlterLog.e("Failed to allocate bridge jump trampoline!");
            return NativeObject.EMPTY_OBJECT;
        }
        NativeImpl.memcpy(mem.address(), info.kBridgeJumpTrampoline, (int) info.kBridgeJumpTrampolineSize);
        NativeObject addr = mem.peekPointer();

        NativeObject target_out =// reinterpret_cast<art::ArtMethod**>
                new NativeObject(addr.address() + info.kBridgeJumpTrampolineTargetMethodOffset);

        target_out.pokePointer(0, target);//    *target_out = target;

        NativeObject extras_out = //reinterpret_cast<Extras**>
                new NativeObject(addr.address() + info.kBridgeJumpTrampolineExtrasOffset);
        extras_out.pokePointer(0, NativeObject.EMPTY_OBJECT);//暂时不知道怎么写
        //*extras_out = new Extras;

        NativeObject bridge_out = //reinterpret_cast<art::ArtMethod**>
                new NativeObject(addr.address() + info.kBridgeJumpTrampolineBridgeMethodOffset);
        //*bridge_out = bridge;
        bridge_out.pokePointer(0, bridge);

        NativeObject bridge_entry_out = //reinterpret_cast<void**>
                new NativeObject(addr.address() + info.kBridgeJumpTrampolineBridgeEntryOffset);

        //*bridge_entry_out = bridge->GetEntryPointFromCompiledCode();
        bridge_entry_out.pokePointer(0, bridge.getEntryPointFromCompiledCode());

        NativeObject origin_entry_out = //reinterpret_cast<void**>(
                new NativeObject(addr.address() + info.kBridgeJumpTrampolineOriginCodeEntryOffset);
        //*origin_entry_out = origin_code_entry;
        origin_entry_out.pokePointer(0, origin_code_entry);

        Os.munmap(mem.address(), info.kBridgeJumpTrampolineSize);

        return mem;
    }


}
