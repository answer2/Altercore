package dev.answer.altercore.core;

import android.os.Build;
import java.util.HashMap;
import java.util.Map;

public class SymbolResolver {

    private static final Map<Integer, Map<String, SymbolPair>> sdkSymbolMap = new HashMap<>();
    private static final Map<String, SymbolPair> defaultSymbols = new HashMap<>();

    static {

        defaultSymbols.put("NewLocalRef",
                new SymbolPair("_ZN3art12_GLOBAL__N_18CheckJNI11NewLocalRefEP7_JNIEnvP8_jobject",
                        ""));

        defaultSymbols.put("DeleteWeakGlobalRef",
                new SymbolPair("_ZN3art12_GLOBAL__N_18CheckJNI19DeleteWeakGlobalRefEP7_JNIEnvP8_jobject",
                        ""));

        defaultSymbols.put("jit_load", new SymbolPair("jit_load",
                "jit_load"));

        // Android 11
        registerSymbol(30,"jit_compile_method",
                "_ZN3art3jit11JitCompiler13CompileMethodEPNS_6ThreadEPNS0_15JitMemoryRegionEPNS_9ArtMethodEbb");

        registerSymbol(30,"addWeakGloablReference",
                "_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadENS_6ObjPtrINS_6mirror6ObjectEEE");

        registerSymbol(30, "FromReflectedMethod",
                "_ZN3art12_GLOBAL__N_18CheckJNI19FromReflectedMethodEP7_JNIEnvP8_jobject");

        // Android 13
        registerSymbol(33, "NewLocalRef", "_ZN3art3JNI11NewLocalRefEP7_JNIEnvP8_jobject");
        registerSymbol(33,"DeleteWeakGlobalRef", "_ZN3art3JNI19DeleteWeakGlobalRefEP7_JNIEnvP8_jobject");


    }

    /**
     * 符号对，包含32位和64位的符号名称
     */
    private static class SymbolPair {
        final String symbol32;
        final String symbol64;

        SymbolPair(String symbol32, String symbol64) {
            this.symbol32 = symbol32;
            this.symbol64 = symbol64;
        }

        String getSymbol(boolean is64Bit) {
            return is64Bit ? symbol64 : symbol32;
        }
    }

    /**
     * Gets the resolved symbol name for a given logical JNI function name.
     *
     * @param functionName e.g. "NewLocalRef", "DeleteWeakGlobalRef"
     * @param is64Bit      whether the target is 64-bit architecture
     * @return Real symbol string usable in dlsym or NativeAccess.getSymbolAddress
     */
    public static String getSymbol(String functionName, boolean is64Bit) {
        int sdk = Build.VERSION.SDK_INT;

        // 遍历匹配最接近的版本（从高到低）
        for (int ver = sdk; ver >= 21; ver--) {
            Map<String, SymbolPair> map = sdkSymbolMap.get(ver);
            if (map != null && map.containsKey(functionName)) {
                return map.get(functionName).getSymbol(is64Bit);
            }
        }

        // fallback 默认
        SymbolPair pair = defaultSymbols.get(functionName);
        return pair != null ? pair.getSymbol(is64Bit) : null;
    }

    /**
     * Manually register a symbol for a specific SDK version.
     */
    public static void registerSymbol(int sdkVersion, String functionName, String symbol32, String symbol64) {
        Map<String, SymbolPair> map = sdkSymbolMap.get(sdkVersion);
        if (map == null) {
            map = new HashMap<>();
            sdkSymbolMap.put(sdkVersion, map);
        }
        map.put(functionName, new SymbolPair(symbol32, symbol64));
    }

    /**
     * Register a symbol for a range of SDK versions (inclusive).
     * The symbol will apply to all SDK versions in [minSdkVersion, maxSdkVersion].
     *
     * @param minSdkVersion minimum SDK version (inclusive)
     * @param maxSdkVersion maximum SDK version (inclusive)
     * @param functionName  JNI function logical name (e.g. "NewLocalRef")
     * @param symbol32      Actual symbol string for 32-bit architecture
     * @param symbol64      Actual symbol string for 64-bit architecture
     */
    public static void registerSymbol(int minSdkVersion, int maxSdkVersion, String functionName,
                                      String symbol32, String symbol64) {
        if (minSdkVersion > maxSdkVersion) {
            throw new IllegalArgumentException("minSdkVersion must be <= maxSdkVersion");
        }

        for (int sdk = minSdkVersion; sdk <= maxSdkVersion; sdk++) {
            Map<String, SymbolPair> map = sdkSymbolMap.get(sdk);
            if (map == null) {
                map = new HashMap<>();
                sdkSymbolMap.put(sdk, map);
            }
            map.put(functionName, new SymbolPair(symbol32, symbol64));
        }
    }

    /**
     * 便捷方法：注册相同符号名（32位和64位相同的情况）
     */
    public static void registerSymbol(int sdkVersion, String functionName, String symbol) {
        registerSymbol(sdkVersion, functionName, symbol, symbol);
    }

    /**
     * 便捷方法：注册相同符号名（32位和64位相同的情况）
     */
    public static void registerSymbol(int minSdkVersion, int maxSdkVersion, String functionName, String symbol) {
        registerSymbol(minSdkVersion, maxSdkVersion, functionName, symbol, symbol);
    }

    /**
     * Optional: Dump current symbol mapping.
     */
    public static void dump() {
        System.out.println("=== JNI Symbol Resolver ===");
        for (Map.Entry<Integer, Map<String, SymbolPair>> entry : sdkSymbolMap.entrySet()) {
            int sdk = entry.getKey();
            for (Map.Entry<String, SymbolPair> fn : entry.getValue().entrySet()) {
                SymbolPair pair = fn.getValue();
                System.out.printf("SDK %d: %s => 32bit:%s, 64bit:%s\n",
                        sdk, fn.getKey(), pair.symbol32, pair.symbol64);
            }
        }
        System.out.println("Default:");
        for (Map.Entry<String, SymbolPair> fn : defaultSymbols.entrySet()) {
            SymbolPair pair = fn.getValue();
            System.out.printf("  %s => 32bit:%s, 64bit:%s\n",
                    fn.getKey(), pair.symbol32, pair.symbol64);
        }
    }
}