package dev.tmpfs.libcoresyscall.elfloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import dev.tmpfs.libcoresyscall.core.impl.ReflectHelper;

public class ProcessView {

    private ProcessView() {
        throw new AssertionError("no instance");
    }

    public static class Module {
        // the name of the module, e.g., libc.so
        public String name;
        // the path of the module, e.g., /system/lib/libc.so
        public String path;
        // the base address of the module, e.g., 0x0d0007210000
        public long base;
    }

    public static ArrayList<Module> getProcessModules(int pid) {
        if (pid <= 0) {
            throw new IllegalArgumentException("Invalid pid: " + pid);
        }
        File maps = new File("/proc/" + pid + "/maps");
        // read the content of the file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(maps)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw ReflectHelper.unsafeThrow(e);
        }
        String[] lines = new String(baos.toByteArray(), StandardCharsets.UTF_8).split("\n");
        ArrayList<Module> modules = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split(" ");
            if (parts.length < 6) {
                continue;
            }
            String prot = parts[1];
            if (prot.length() < 4 || prot.charAt(3) != 'p') {
                continue;
            }
            long address = Long.parseUnsignedLong(parts[0].split("-")[0], 16);
            long offset = Long.parseUnsignedLong(parts[2], 16);
            if (offset != 0) {
                continue;
            }
            // find the start of the path
            int pathStartIndex = parts[0].length() + parts[1].length() + parts[2].length() + parts[3].length() + parts[4].length() + 5;
            while (pathStartIndex < line.length() && Character.isWhitespace(line.charAt(pathStartIndex))) {
                pathStartIndex++;
            }
            String path = line.substring(pathStartIndex);
            if (path.startsWith("[")) {
                continue;
            }
            String soName = path.substring(path.lastIndexOf('/') + 1);
            if (soName.endsWith(" (deleted)")) {
                soName = soName.substring(0, soName.length() - " (deleted)".length());
            }
            Module module = new Module();
            module.name = soName;
            module.path = path;
            module.base = address;
            modules.add(module);
        }
        return modules;
    }

    public static ArrayList<Module> getCurrentProcessModules() {
        return getProcessModules(android.os.Process.myPid());
    }

}
