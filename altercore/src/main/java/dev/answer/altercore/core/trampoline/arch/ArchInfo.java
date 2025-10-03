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

package dev.answer.altercore.core.trampoline.arch;

public class ArchInfo {
    public int kDirectJumpTrampoline;
    public int freeDirectPageMemory;
    public int kDirectJumpTrampolineSize;
    public long kDirectJumpTrampolineEntryOffset;
    public long kBridgeJumpTrampoline;
    public long kBridgeJumpTrampolineSize;
    public long kBridgeJumpTrampolineTargetMethodOffset;
    public long kBridgeJumpTrampolineExtrasOffset;
    public long kBridgeJumpTrampolineBridgeMethodOffset;
    public long kBridgeJumpTrampolineBridgeEntryOffset;
    public long kBridgeJumpTrampolineOriginCodeEntryOffset;

    /*
    *
    *   kTrampolinesEnd = AS_VOID_PTR(AS_PTR_NUM(bridge_jump_trampoline) + Memory::AlignUp<uintptr_t>(sizeof(bridge_jump_trampoline), 4)); // For calculate size only

    kCallOriginTrampoline = kTrampolinesEnd; // For calculate size only

    kDirectJumpTrampoline = kBackupTrampoline; // For calculate size only
    kDirectJumpTrampolineSize = 0;
    kCallOriginTrampolineOriginMethodOffset = 0;
    kCallOriginTrampolineOriginalEntryOffset = 0;

    kBackupTrampoline = kCallOriginTrampoline; // For calculate size only
    kBackupTrampolineOriginMethodOffset = 0;
    kBackupTrampolineOverrideSpaceOffset = 0;
    kBackupTrampolineRemainingCodeEntryOffset = 0 ;
    * */
}
