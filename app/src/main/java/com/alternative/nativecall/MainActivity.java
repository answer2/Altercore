package com.alternative.nativecall;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dev.answer.altercore.NativeImpl;
import dev.answer.altercore.core.Altercore;

import java.lang.reflect.Field;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
//
//        try {
//            long TRUE = 1;
//            long FALSE = 0;
//
//            SymbolResolver art_module = SymbolResolver.getModule("libart.so");
//            SymbolResolver art_compiler_module = SymbolResolver.getModule("libart-compiler.so");
//            long jit_compile_method = art_compiler_module.getSymbolAddress("_ZN3art3jit11JitCompiler13CompileMethodEPNS_6ThreadEPNS0_15JitMemoryRegionEPNS_9ArtMethodEbb");
//            long jit_load = art_compiler_module.getSymbolAddress("jit_load");
//            long addWeakGloablReference = art_module.getSymbolAddress("_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadENS_6ObjPtrINS_6mirror6ObjectEEE");
//
//            System.out.println("This is addWeakGloablReference : " + addWeakGloablReference);
//            System.out.println("This is jit_compile_method : " + jit_compile_method);
//            System.out.println("This is jit_load :" + jit_load);
//
//            long jit_compiler_handle_ = NativeAccess.callPointerFunction(jit_load, FALSE);
//            System.out.println("This is jit_compiler_handle_ :" + jit_compiler_handle_);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        try {
            System.out.println(Arrays.toString( NativeImpl.exchangeType("aaaa")));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }



}