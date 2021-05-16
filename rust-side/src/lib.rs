#![allow(clippy::missing_safety_doc)]

use std::ffi::*;
use std::os::raw::*;
use std::slice;

#[no_mangle]
pub unsafe extern "C" fn create_greeting(
    name: *const c_char,
    output_buffer: *mut c_char,
    buffer_length: i32,
) -> i32 {
    let their_name = CStr::from_ptr(name).to_str().unwrap();
    let mut greeting = format!("Hello from Rust, {}!", their_name);
    greeting.truncate((buffer_length - 1) as usize);
    let greeting_bytes = CString::new(greeting).unwrap().into_bytes_with_nul();
    let buffer = slice::from_raw_parts_mut(output_buffer as *mut u8, buffer_length as usize);
    buffer[..greeting_bytes.len()].copy_from_slice(&greeting_bytes[..]);
    greeting_bytes.len() as i32
}
