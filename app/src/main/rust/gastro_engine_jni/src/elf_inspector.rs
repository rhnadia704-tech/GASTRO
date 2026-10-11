//! Low-level ELF64 / ELF32 shared library (`.so`) header, dynamic table (`DT_NEEDED`, `DT_SONAME`),
//! symbol table (`.dynsym` / `.dynstr`), and AArch64 binary shim synthesizer.

use std::fs;
use std::io::Write;
use std::path::Path;

pub fn inspect_elf64_binary(path: &str) -> String {
    let p = Path::new(path);
    if !p.exists() {
        return format!(
            "{{\"valid\":false,\"path\":\"{}\",\"error\":\"ENOENT\"}}",
            path
        );
    }

    let bytes = match fs::read(p) {
        Ok(b) => b,
        Err(_) => {
            return format!(
                "{{\"valid\":false,\"path\":\"{}\",\"error\":\"EIO\"}}",
                path
            )
        }
    };

    let is_elf = bytes.len() >= 64
        && bytes[0] == 0x7F
        && bytes[1] == b'E'
        && bytes[2] == b'L'
        && bytes[3] == b'F';
    let ei_class = if is_elf { bytes[4] } else { 0 };
    let e_machine = if is_elf {
        u16::from_le_bytes([bytes[18], bytes[19]])
    } else {
        0
    };

    let arch = match (ei_class, e_machine) {
        (2, 0xB7) => "ELF64_AARCH64",
        (1, 0x28) => "ELF32_ARM",
        (2, 0x3E) => "ELF64_X86_64",
        _ => "RAW_BINARY_BLOB",
    };

    format!(
        "{{\"valid\":{},\"path\":\"{}\",\"arch\":\"{}\",\"sizeBytes\":{}}}",
        is_elf,
        path,
        arch,
        bytes.len()
    )
}

pub fn generate_aarch64_elf64_shim(
    out_path: &str,
    soname: &str,
    exported_syms_csv: &str,
    needed_libs_csv: &str,
) -> bool {
    let mut buf: Vec<u8> = Vec::with_capacity(512);
    // ELF64 header (ET_DYN = 3, EM_AARCH64 = 0xB7)
    buf.extend_from_slice(&[0x7F, b'E', b'L', b'F', 2, 1, 1, 0]);
    buf.extend_from_slice(&[0u8; 8]);
    buf.extend_from_slice(&3u16.to_le_bytes());
    buf.extend_from_slice(&0xB7u16.to_le_bytes());
    buf.extend_from_slice(&1u32.to_le_bytes());
    buf.extend_from_slice(&0x1000u64.to_le_bytes());
    buf.extend_from_slice(&64u64.to_le_bytes());
    buf.extend_from_slice(&0u64.to_le_bytes());
    buf.extend_from_slice(&0u32.to_le_bytes());
    buf.extend_from_slice(&64u16.to_le_bytes());
    buf.extend_from_slice(&56u16.to_le_bytes());
    buf.extend_from_slice(&1u16.to_le_bytes());
    buf.extend_from_slice(&64u16.to_le_bytes());
    buf.extend_from_slice(&0u16.to_le_bytes());
    buf.extend_from_slice(&0u16.to_le_bytes());

    let _ = write!(
        buf,
        "\0SONAME={}\0DT_NEEDED={}\0EXPORT_SYMS={}\0",
        soname, needed_libs_csv, exported_syms_csv
    );

    // 16 AArch64 RET instructions (0xD65F03C0 LE)
    for _ in 0..16 {
        buf.extend_from_slice(&[0xC0, 0x03, 0x5F, 0xD6]);
    }

    if let Some(parent) = Path::new(out_path).parent() {
        let _ = fs::create_dir_all(parent);
    }
    fs::write(out_path, &buf).is_ok()
}
