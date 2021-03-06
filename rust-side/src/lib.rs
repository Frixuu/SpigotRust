#![allow(clippy::missing_safety_doc)]

use std::ffi::*;
use std::os::raw::*;
use std::slice;

#[no_mangle]
pub unsafe extern "C" fn create_greeting(
    name: *const c_char,
    callback: unsafe extern "C" fn(),
    buffer_ptr: *mut c_char,
    buffer_length: i32,
) -> i32 {

    let their_name = CStr::from_ptr(name).to_str().unwrap();
    let mut greeting = format!("Hello from Rust, {}!", their_name);
    greeting.truncate(buffer_length as usize - 1);

    let greeting_bytes = CString::new(greeting).unwrap().into_bytes_with_nul();
    let buffer = slice::from_raw_parts_mut(buffer_ptr as *mut u8, buffer_length as usize);
    buffer[..greeting_bytes.len()].copy_from_slice(&greeting_bytes[..]);

    callback();
    greeting_bytes.len() as i32
}
