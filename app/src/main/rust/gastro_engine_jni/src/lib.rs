//! # GASTROengine JNI Rust Module (`libgastro_engine_jni.so` v2.5.0-ultra)
//!
//! Ultra-intelligent, zero-bootloop low-level ELF64 library (`.so`) and proprietary blob porting engine for GASTRO.
//! Exposes JNI C-ABI entrypoints to `com.example.core.gastro.GastroJniRustBlobBridge` for:
//! 1. Deep binary ELF64 header, `e_machine`, `PT_DYNAMIC`, `DT_NEEDED`, `DT_SONAME`, `DT_RUNPATH` & `.dynsym` analysis
//! 2. `.rodata` & `.text` AArch64 hardware telemetry scanner (extracting real `/sys/class/...`, `/dev/...`, `ioctl` magic constants, and `persist.vendor.*` properties directly from proprietary `.so` blobs)
//! 3. VNDK & Treble `linker64` namespace reconciler (`ld.config.vndk_bridge.txt` / `public.libraries.gastro.txt` generator)
//! 4. Fixed-width in-place `.dynstr` `DT_NEEDED` / `SONAME` binary slot patcher (zero section-header offset shift)
//! 5. Autonomous AArch64 (`EM_AARCH64 = 0xB7`, `0xD65F03C0` `RET`) ELF64 shared library shim synthesizer (`libshim_gastro_*.so`)
//! 6. Automated end-to-end low-level blob porting (`EXTRACT/` <-> `UNPACK/<selected_gsi>/`)

pub mod blob_manager;
pub mod elf_inspector;

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring, JNI_TRUE};
use jni::JNIEnv;

use crate::blob_manager::{
    audit_and_reconcile_blobs, generate_vndk_linker_namespace_config, patch_elf_dt_needed_inplace,
};
use crate::elf_inspector::{
    generate_aarch64_elf64_shim, inspect_elf64_binary, scan_elf_rodata_hardware_telemetry,
};

#[no_mangle]
pub extern "system" fn Java_com_example_core_gastro_GastroJniRustBlobBridge_nativeGetRustEngineVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = "gastro_engine_jni v2.5.0-ultra (ELF64 • AArch64 .rodata/IOCTL Scanner • VNDK Linker64 Bridge • Auto-Porting)";
    env.new_string(version)
        .expect("Failed to allocate JNI string")
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_gastro_GastroJniRustBlobBridge_nativeInspectElfLibraryJson(
    mut env: JNIEnv,
    _class: JClass,
    elf_path: JString,
) -> jstring {
    let path_str: String = env
        .get_string(&elf_path)
        .map(Into::into)
        .unwrap_or_default();
    let report_json = inspect_elf64_binary(&path_str);
    env.new_string(report_json)
        .expect("Failed to allocate JNI JSON string")
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_gastro_GastroJniRustBlobBridge_nativeAuditAndReconcileBlobsJson(
    mut env: JNIEnv,
    _class: JClass,
    unpacked_gsi_root: JString,
    extract_root: JString,
    auto_repair_blobs: jboolean,
) -> jstring {
    let gsi_dir: String = env
        .get_string(&unpacked_gsi_root)
        .map(Into::into)
        .unwrap_or_default();
    let ext_dir: String = env
        .get_string(&extract_root)
        .map(Into::into)
        .unwrap_or_default();
    let repair = auto_repair_blobs == JNI_TRUE;

    let result_json = audit_and_reconcile_blobs(&gsi_dir, &ext_dir, repair);
    env.new_string(result_json)
        .expect("Failed to allocate JNI audit JSON")
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_gastro_GastroJniRustBlobBridge_nativePatchElfNeededSlot(
    mut env: JNIEnv,
    _class: JClass,
    elf_path: JString,
    old_soname: JString,
    new_soname: JString,
) -> jint {
    let path_str: String = env.get_string(&elf_path).map(Into::into).unwrap_or_default();
    let old_s: String = env.get_string(&old_soname).map(Into::into).unwrap_or_default();
    let new_s: String = env.get_string(&new_soname).map(Into::into).unwrap_or_default();

    match patch_elf_dt_needed_inplace(&path_str, &old_s, &new_s) {
        Ok(patched_bytes) => patched_bytes as jint,
        Err(_) => -1,
    }
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_gastro_GastroJniRustBlobBridge_nativeGenerateElf64ShimLibrary(
    mut env: JNIEnv,
    _class: JClass,
    output_shim_path: JString,
    soname: JString,
    exported_symbols_csv: JString,
    needed_libs_csv: JString,
) -> jboolean {
    let out_path: String = env
        .get_string(&output_shim_path)
        .map(Into::into)
        .unwrap_or_default();
    let so_name: String = env.get_string(&soname).map(Into::into).unwrap_or_default();
    let syms_csv: String = env
        .get_string(&exported_symbols_csv)
        .map(Into::into)
        .unwrap_or_default();
    let libs_csv: String = env
        .get_string(&needed_libs_csv)
        .map(Into::into)
        .unwrap_or_default();

    let ok = generate_aarch64_elf64_shim(&out_path, &so_name, &syms_csv, &libs_csv);
    if ok { 1 } else { 0 }
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_gastro_GastroJniRustBlobBridge_nativeScanBlobRodataTelemetryJson(
    mut env: JNIEnv,
    _class: JClass,
    elf_path: JString,
) -> jstring {
    let path_str: String = env
        .get_string(&elf_path)
        .map(Into::into)
        .unwrap_or_default();
    let telemetry_json = scan_elf_rodata_hardware_telemetry(&path_str);
    env.new_string(telemetry_json)
        .expect("Failed to allocate JNI telemetry JSON")
        .into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_example_core_gastro_GastroJniRustBlobBridge_nativeGenerateVndkLinkerConfig(
    mut env: JNIEnv,
    _class: JClass,
    unpacked_gsi_root: JString,
    vndk_version: JString,
) -> jstring {
    let gsi_dir: String = env
        .get_string(&unpacked_gsi_root)
        .map(Into::into)
        .unwrap_or_default();
    let vndk_ver: String = env
        .get_string(&vndk_version)
        .map(Into::into)
        .unwrap_or_default();
    let cfg = generate_vndk_linker_namespace_config(&gsi_dir, &vndk_ver);
    env.new_string(cfg)
        .expect("Failed to allocate JNI linker config string")
        .into_raw()
}
