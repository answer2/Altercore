/*
 * Copyright (C) 2026 AnswerDev
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
package dev.answer.altercore.exception;

/**
 * Base exception for all AlterCore hook-related errors.
 * 
 * <p>This exception hierarchy provides structured error handling for hook operations,
 * enabling clients to distinguish between different failure modes and handle them
 * appropriately.
 */
public class HookException extends RuntimeException {
    
    /** Error code for hook operation failures. */
    private final ErrorCode errorCode;
    
    public enum ErrorCode {
        /** The target method or constructor is null. */
        NULL_TARGET(1000),
        
        /** The hook callback is null. */
        NULL_CALLBACK(1001),
        
        /** The target method is abstract and cannot be hooked. */
        ABSTRACT_METHOD(1002),
        
        /** Attempted to hook a class initializer (<clinit>). */
        CLASS_INITIALIZER(1003),
        
        /** The target is neither a Method nor a Constructor. */
        INVALID_TARGET_TYPE(1004),
        
        /** Failed to build the backup method. */
        BACKUP_BUILD_FAILED(2000),
        
        /** Failed to install the native hook. */
        NATIVE_HOOK_FAILED(2001),
        
        /** The target method is already hooked and reinstallation failed. */
        HOOK_REINSTALL_FAILED(2002),
        
        /** Internal error during hook operation. */
        INTERNAL_ERROR(9000);
        
        private final int code;
        
        ErrorCode(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
    }
    
    public HookException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public HookException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public int getErrorCodeValue() {
        return errorCode.getCode();
    }
    
    @Override
    public String getMessage() {
        return "[" + errorCode.name() + ":" + errorCode.getCode() + "] " + super.getMessage();
    }
}
