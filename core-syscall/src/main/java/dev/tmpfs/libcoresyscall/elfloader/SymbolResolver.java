package dev.tmpfs.libcoresyscall.elfloader;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import dev.tmpfs.libcoresyscall.core.NativeHelper;

public class SymbolResolver {

    // static cache, guarded by sCache
    private static final HashMap<String, SymbolResolver> sCache = new HashMap<>();

    public static class NoSuchModuleException extends ReflectiveOperationException {
        public NoSuchModuleException(String message) {
            super(message);
        }
    }

    @NonNull
    public static SymbolResolver getModule(String name) throws NoSuchModuleException, IOException {
        synchronized (sCache) {
            SymbolResolver m = sCache.get(name);
            if (m != null) {
                return m;
            }
        }
        // find the module
        ArrayList<ProcessView.Module> modules = ProcessView.getCurrentProcessModules();
        ProcessView.Module target = null;
        // perform path, then name match
        for (ProcessView.Module module : modules) {
            if (module.path.equals(name)) {
                target = module;
                break;
            }
        }
        if (target == null) {
            for (ProcessView.Module module : modules) {
                if (module.name.equals(name)) {
                    target = module;
                    break;
                }
            }
        }
        if (target == null) {
            throw new NoSuchModuleException("No such module: " + name);
        }
        File elfFile = new File(target.path);
        if (!elfFile.exists()) {
            throw new FileNotFoundException("File not found: " + elfFile);
        }
        // create a byte buffer, and load the elf file
        long size = elfFile.length();
        if (size > Integer.MAX_VALUE) {
            throw new IOException("File too large: " + size);
        }
        ByteBuffer buffer = ByteBuffer.allocate((int) size);
        // load the file into buffer
        try (FileInputStream fis = new FileInputStream(elfFile)) {
            while (buffer.hasRemaining()) {
                fis.getChannel().read(buffer);
            }
        }
        buffer.flip();
        // create an ElfView
        ElfView elfView = new ElfView(buffer);
        SymbolResolver resolver = new SymbolResolver(elfView, target.base);
        // add to cache, if and only if the ISA matches
        if (NativeHelper.getElfIsaFromFile(elfFile) == NativeHelper.getCurrentRuntimeIsa()) {
            synchronized (sCache) {
                sCache.put(name, resolver);
            }
        }
        return resolver;
    }

    // instance fields
    private ElfView mElfView;
    private long mBaseAddress;

    private SymbolResolver(ElfView elfView, long baseAddress) {
        mElfView = elfView;
        mBaseAddress = baseAddress;
    }

    public ElfView getElfView() {
        return mElfView;
    }

    public String getSoname() {
        return mElfView.getSoname();
    }

    public int getLibraryIsa() {
        return mElfView.getLibraryIsa();
    }

    public long getSymbolAddress(String symbol) {
        long offset = mElfView.getSymbolOffset(symbol);
        if (offset == 0) {
            return 0;
        }
        return mBaseAddress + offset;
    }

}
