package dev.tmpfs.libcoresyscall.elfloader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import dev.tmpfs.libcoresyscall.core.NativeHelper;

/**
 * The ELF symbol resolver.
 * This only works for ELF32 and ELF64 files, not support for the runtime process memory view.
 */
public class ElfView {

    /**
     * Parse the ELF file and get its symbol table.
     *
     * @param elfBuffer The ELF file buffer.
     */
    public ElfView(@NonNull ByteBuffer elfBuffer) {
        elfBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ElfInfo info = initElfInfo(elfBuffer);
        parseDynamicSymbolTable(elfBuffer, info);
        parseDebugSymbol(elfBuffer, info);
    }

    private int mLibraryIsa = 0;
    // load bias is typically 0
    private long mLoadBias = 0;
    // loaded image size
    private long mLoadedSize = 0;
    // symbols in .dynsym, already subtracted load bias
    private Map<String, Long> mDynamicSymbols = new HashMap<>();
    // symbols in .symtab, already subtracted load bias
    private Map<String, Long> mDebugSymbols = new HashMap<>();
    // soname, optional
    private String mSoname = null;
    private boolean mIs64Bit = false;

    private static class ElfInfo {
        public long dynstr;
        public long strtab;
        public long symtab;
        public int symtab_size;
        public long dynsym;
        public int dynsym_size;
    }

    // for LSP hint
    private static final boolean isLoaded = false;

    private long readPointer(@NonNull ByteBuffer elfBuffer, int offset) {
        if (mIs64Bit) {
            return elfBuffer.getLong(offset);
        } else {
            return Integer.toUnsignedLong(elfBuffer.getInt(offset));
        }
    }


    private int readPointerAsSignedInt32(@NonNull ByteBuffer elfBuffer, int offset) {
        long value = readPointer(elfBuffer, offset);
        // check overflow
        return Math.toIntExact(value);
    }

    private int readLe16(@NonNull ByteBuffer elfBuffer, int offset) {
        return Short.toUnsignedInt(elfBuffer.getShort(offset));
    }

    private long readLe32(@NonNull ByteBuffer elfBuffer, int offset) {
        return Integer.toUnsignedLong(elfBuffer.getInt(offset));
    }

    private int readLe32AsSignedInt32(@NonNull ByteBuffer elfBuffer, int offset) {
        return Math.toIntExact(readLe32(elfBuffer, offset));
    }

    private String readNullTerminatedString(@NonNull ByteBuffer elfBuffer, int offset) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            byte b = elfBuffer.get(offset++);
            if (b == 0) {
                break;
            }
            // currently only support ASCII, add UTF-8 support if really needed in the future
            sb.append((char) b);
        }
        return sb.toString();
    }

    private ElfInfo initElfInfo(@NonNull ByteBuffer elfBuffer) {
        byte[] e_ident = new byte[16];
        elfBuffer.get(e_ident);
        if (e_ident[4] == 1) {
            mIs64Bit = false;
        } else if (e_ident[4] == 2) {
            mIs64Bit = true;
        } else {
            throw new IllegalArgumentException("Invalid ELF class: " + e_ident[4]);
        }
        // check magic
        if (e_ident[0] != 0x7f || e_ident[1] != 'E' || e_ident[2] != 'L' || e_ident[3] != 'F') {
            throw new IllegalArgumentException("Invalid ELF magic");
        }
        ElfInfo info = new ElfInfo();
        int machine = readLe16(elfBuffer, 18);
        mLibraryIsa = NativeHelper.forElfClassAndMachine(e_ident[4], machine);
        int phoff = readPointerAsSignedInt32(elfBuffer, mIs64Bit ? 32 : 28);
        if (phoff != 0) {
            int phnum = readLe16(elfBuffer, mIs64Bit ? 56 : 44);
            int phentsize = readLe16(elfBuffer, mIs64Bit ? 54 : 42);
            long firstLoadedSegmentStart = Long.MAX_VALUE;
            long lastLoadedSegmentEnd = 0;
            int phdrSelfOffset = 0;
            int phdrDynamicOffset = 0;
            for (int i = 0; i < phnum; i++) {
                int phdr = (phoff + i * phentsize);
                final int PT_PHDR = 6;
                final int PT_DYNAMIC = 2;
                final int PT_LOAD = 1;
                int p_type = readLe32AsSignedInt32(elfBuffer, phdr);
                long p_vaddr = readPointer(elfBuffer, phdr + (mIs64Bit ? 16 : 8));
                long p_memsz = readPointer(elfBuffer, phdr + (mIs64Bit ? 40 : 20));
                if (p_type == PT_PHDR) {
                    phdrSelfOffset = phdr;
                } else if (p_type == PT_DYNAMIC) {
                    phdrDynamicOffset = phdr;
                } else if (p_type == PT_LOAD) {
                    if (p_vaddr < firstLoadedSegmentStart) {
                        firstLoadedSegmentStart = p_vaddr;
                    }
                    if (p_vaddr + p_memsz > lastLoadedSegmentEnd) {
                        lastLoadedSegmentEnd = p_vaddr + p_memsz;
                    }
                }
            }
            mLoadBias = firstLoadedSegmentStart;
            mLoadedSize = lastLoadedSegmentEnd - firstLoadedSegmentStart;
            if (phdrDynamicOffset != 0) {
                final int DT_NULL = 0;
                final int DT_SONAME = 14;
                final int DT_STRTAB = 5;
                // walk through dynamic section
                long sonameOffset = 0;
                long strtabOffset = 0;
                int phdrDynamic_p_memsz = readPointerAsSignedInt32(elfBuffer, phdrDynamicOffset + (mIs64Bit ? 40 : 20));
                int phdrDynamic_p_vaddr = readPointerAsSignedInt32(elfBuffer, phdrDynamicOffset + (mIs64Bit ? 16 : 8));
                long phdrDynamic_p_offset = readPointer(elfBuffer, phdrDynamicOffset + (mIs64Bit ? 8 : 4));
                int sizeof_Elf_Dyn = mIs64Bit ? 16 : 8;
                for (int i = 0; i < phdrDynamic_p_memsz / sizeof_Elf_Dyn; i++) {
                    long dynOffset = (phdrDynamic_p_offset) + (long) i * sizeof_Elf_Dyn;
                    int dyn_d_tag = readPointerAsSignedInt32(elfBuffer, (int) dynOffset);
                    switch (dyn_d_tag) {
                        case DT_NULL: {
                            break;
                        }
                        case DT_SONAME: {
                            sonameOffset = readPointer(elfBuffer, (int) (dynOffset + (mIs64Bit ? 8 : 4)));
                            break;
                        }
                        case DT_STRTAB: {
                            long val = readPointer(elfBuffer, (int) (dynOffset + (mIs64Bit ? 8 : 4)));
                            strtabOffset = val - mLoadBias;
                            break;
                        }
                        default: {
                            // ignore
                            break;
                        }
                    }
                }
                if (sonameOffset != 0 && strtabOffset != 0) {
                    mSoname = readNullTerminatedString(elfBuffer, (int) (strtabOffset + sonameOffset));
                }
            }
        }
        // walk through section header
        long shoff = readPointer(elfBuffer, mIs64Bit ? 40 : 32);
        if (shoff == 0) {
            throw new IllegalArgumentException("No section header");
        }
        int shnum = readLe16(elfBuffer, mIs64Bit ? 60 : 48);
        int shentsize = readLe16(elfBuffer, mIs64Bit ? 58 : 46);
        int e_shstrndx = readLe16(elfBuffer, mIs64Bit ? 62 : 50);
        int shstrtab = (int) (shoff + e_shstrndx * shentsize);
        long sectionHeaderStringTable = readPointerAsSignedInt32(elfBuffer, shstrtab + (mIs64Bit ? 24 : 16));
        for (int i = 0; i < shnum; i++) {
            final int SHT_STRTAB = 3;
            final int SHT_SYMTAB = 2;
            final int SHT_DYNSYM = 11;
            final int SHT_HASH = 5;
            final int SHT_GNU_HASH = 0x6ffffff6;
            final int SHT_PROGBITS = 1;
            long shdr = shoff + (long) i * shentsize;
            int shdr_sh_name = readLe32AsSignedInt32(elfBuffer, (int) (shdr));
            @Nullable
            String name = shdr_sh_name == 0 ? null
                    : readNullTerminatedString(elfBuffer, (int) (sectionHeaderStringTable + shdr_sh_name));
            int shdr_sh_type = readLe32AsSignedInt32(elfBuffer, (int) (shdr + 4));
            long shdr_sh_addr = readPointer(elfBuffer, (int) (shdr + (mIs64Bit ? 16 : 12)));
            long shdr_sh_offset = readPointer(elfBuffer, (int) (shdr + (mIs64Bit ? 24 : 16)));
            long shdr_sh_size = readPointer(elfBuffer, (int) (shdr + (mIs64Bit ? 32 : 20)));
            int sizeof_Elf_Sym = mIs64Bit ? 24 : 16;
//            Log.d("ElfView", "Section ["+i+"]: " + name + ", type: " + shdr_sh_type + ", addr: " + shdr_sh_addr + ", offset: " + shdr_sh_offset + ", size: " + shdr_sh_size);
            switch (shdr_sh_type) {
                case SHT_STRTAB: {
                    if (".dynstr".equals(name)) {
                        info.dynstr = isLoaded ? shdr_sh_addr : shdr_sh_offset;
                    } else if (".strtab".equals(name)) {
                        info.strtab = isLoaded ? shdr_sh_addr : shdr_sh_offset;
                    }
                    break;
                }
                case SHT_SYMTAB: {
                    if (".symtab".equals(name)) {
                        info.symtab = shdr_sh_offset;
                        info.symtab_size = (int) (shdr_sh_size / sizeof_Elf_Sym);
                    }
                    break;
                }
                case SHT_DYNSYM: {
                    info.dynsym = isLoaded ? shdr_sh_addr : shdr_sh_offset;
                    info.dynsym_size = (int) (shdr_sh_size / sizeof_Elf_Sym);
                    break;
                }
                case SHT_HASH:
                case SHT_GNU_HASH: {
                    // we no longer use this
                    break;
                }
                case SHT_PROGBITS: {
                    // .gnu_debugdata mini debug info support has not been implemented here
                    break;
                }
                default: {
                    // ignore
                    break;
                }
            }
        }
        return info;
    }

    private void parseDynamicSymbolTable(@NonNull ByteBuffer elfBuffer, @NonNull ElfInfo info) {
        long symtab = info.dynsym;
        long loadBias = mLoadBias;
        final int sizeof_Elf_Sym = mIs64Bit ? 24 : 16;
        // walk through the dynamic symbol table to get defined symbols
        for (int i = 0; i < info.dynsym_size; i++) {
            long sym = symtab + (long) i * sizeof_Elf_Sym;
            long sym_st_name = readLe32AsSignedInt32(elfBuffer, (int) sym);
            String symname = readNullTerminatedString(elfBuffer, (int) (info.dynstr + sym_st_name));
            long st_value = readPointer(elfBuffer, (int) (sym + (mIs64Bit ? 8 : 4)));
            if (st_value != 0) {
                mDynamicSymbols.put(symname, st_value - loadBias);
            }
        }
    }

    private void parseDebugSymbol(@NonNull ByteBuffer elfBuffer, @NonNull ElfInfo info) {
        long symtab = info.symtab;
        long loadBias = mLoadBias;
        final int sizeof_Elf_Sym = mIs64Bit ? 24 : 16;
        // walk through the symbol table to get defined symbols
        for (int i = 0; i < info.symtab_size; i++) {
            long sym = symtab + (long) i * sizeof_Elf_Sym;
            long sym_st_name = readLe32AsSignedInt32(elfBuffer, (int) sym);
            String symname = readNullTerminatedString(elfBuffer, (int) (info.strtab + sym_st_name));
            long st_value = readPointer(elfBuffer, (int) (sym + (mIs64Bit ? 8 : 4)));
            if (st_value != 0) {
                mDebugSymbols.put(symname, st_value - loadBias);
            }
        }
    }

    public long getSymbolOffset(@NonNull String symbol) {
        // search in .dynsym
        if (mDynamicSymbols != null) {
            Long address = mDynamicSymbols.get(symbol);
            if (address != null) {
                return address;
            }
        }
        // search in .symtab
        if (mDebugSymbols != null) {
            Long address = mDebugSymbols.get(symbol);
            if (address != null) {
                return address;
            }
        }
        return 0;
    }

    public HashMap<String, Long> getDynamicSymbols() {
        return new HashMap<>(mDynamicSymbols);
    }

    public HashMap<String, Long> getDebugSymbols() {
        return new HashMap<>(mDebugSymbols);
    }

    public HashMap<String, Long> getAllSymbols() {
        HashMap<String, Long> allSymbols = new HashMap<>(mDebugSymbols);
        allSymbols.putAll(mDynamicSymbols);
        return allSymbols;
    }

    public boolean is64Bit() {
        return mIs64Bit;
    }

    public long getLoadBias() {
        return mLoadBias;
    }

    public long getLoadedSize() {
        return mLoadedSize;
    }

    public int getLibraryIsa() {
        return mLibraryIsa;
    }

    @Nullable
    public String getSoname() {
        return mSoname;
    }

}
