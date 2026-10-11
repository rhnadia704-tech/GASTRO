//! # GASTROengine JNI Rust Module (`libgastro_engine_jni.so`)
//!
//! Low-level library (`.so` ELF64/ELF32) and proprietary blob management subsystem for GASTRO.
//! Exposes JNI entrypoints to `com.example.core.gastro.GastroJniRustBlobBridge` for:
//! - Binary ELF64 header, `e_machine`, `PT_DYNAMIC`, `DT_NEEDED`, `DT_SONAME`, `DT_RUNPATH` & `.dynsym` parsing
//! - Transitive shared library dependency resolution across `/system/lib64`, `/system_ext/lib64`, `/product/lib64`, `/vendor/lib64`, and VNDK APEX namespaces
//! - Proprietary vendor blob integrity verification (SHA-256, CRC32, POSIX mode `0644`/`0755`, SELinux `security.selinux` xattr labels)
//! - In-place binary `DT_NEEDED` / `SONAME` fixed-width C-string patching without corrupting `.dynstr` offsets
//! - Autonomous AArch64 (`EM_AARCH64 = 0xB7`) ELF64 shared library shim generation (`libshim_gastro_*.so`)

pub mod blob_manager;
pub mod elf_inspector;

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring, JNI_TRUE};
use jni::JNIEnv;

use crate::blob_manager::{audit_and_reconcile_blobs, patch_elf_dt_needed_inplace};
use crate::elf_inspector::{generate_aarch64_elf64_shim, inspect_elf64_binary};

#[no_mangle]
pub extern "system" fn Java_com_example_core_gastro_GastroJniRustBlobBridge_nativeGetRustEngineVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = "gastro_engine_jni v2.4.0-rust (ELF64/VNDK/BlobManager • AArch64 JNI ABI)";
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
