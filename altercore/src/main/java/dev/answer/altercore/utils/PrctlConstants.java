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

public class PrctlConstants {
    /**
     * 获取 MDWE (Memory-Deny-Write-Execute) 状态
     */
    public static final int PR_GET_MDWE = 66;

    /**
     * 设置虚拟内存区域操作
     */
    public static final int PR_SET_VMA = 0x53564d41;

    /**
     * 设置虚拟内存区域匿名名称
     */
    public static final int PR_SET_VMA_ANON_NAME = 0;

    /**
     * 获取辅助向量 (auxv)
     */
    public static final int PR_GET_AUXV = 0x41555856;

    /**
     * 设置内存合并 (KSM)
     */
    public static final int PR_SET_MEMORY_MERGE = 67;

    /**
     * 获取内存合并状态
     */
    public static final int PR_GET_MEMORY_MERGE = 68;

    /**
     * RISC-V 向量扩展：设置控制
     */
    public static final int PR_RISCV_V_SET_CONTROL = 69;

    /**
     * RISC-V 向量扩展：获取控制
     */
    public static final int PR_RISCV_V_GET_CONTROL = 70;

    /**
     * RISC-V 向量状态控制：默认
     */
    public static final int PR_RISCV_V_VSTATE_CTRL_DEFAULT = 0;

    /**
     * RISC-V 向量状态控制：关闭
     */
    public static final int PR_RISCV_V_VSTATE_CTRL_OFF = 1;

    /**
     * RISC-V 向量状态控制：开启
     */
    public static final int PR_RISCV_V_VSTATE_CTRL_ON = 2;

    /**
     * RISC-V 向量状态控制：继承
     */
    public static final int PR_RISCV_V_VSTATE_CTRL_INHERIT = (1 << 4);

    /**
     * RISC-V 向量状态控制：当前掩码
     */
    public static final int PR_RISCV_V_VSTATE_CTRL_CUR_MASK = 0x3;

    /**
     * RISC-V 向量状态控制：下一个掩码
     */
    public static final int PR_RISCV_V_VSTATE_CTRL_NEXT_MASK = 0xc;

    /**
     * RISC-V 向量状态控制：完整掩码
     */
    public static final int PR_RISCV_V_VSTATE_CTRL_MASK = 0x1f;

    // 私有构造函数，防止实例化
    private PrctlConstants() {
    }
}
