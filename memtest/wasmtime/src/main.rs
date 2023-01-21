use std::thread;

use anyhow::Error;
use wasmtime::{Config, Engine, Instance, Module, Store};

fn main() -> Result<(), Error> {
    let config = Config::new();

    let engine = Engine::new(&config)?;
    let mut store = Store::new(&engine, ());

    let curr_dir = std::env::current_dir()?;
    let wasm_file = curr_dir.join("noop.wasm");

    thread::sleep(std::time::Duration::from_secs(2));

    let start = std::time::Instant::now();

    let module_count = 10_000;

    let mut i = 1;
    while i < module_count {
        let module = Module::from_file(&engine, wasm_file.clone())?;
        let instance = Instance::new(&mut store, &module, &vec![])?;
        i = instance
            .get_typed_func::<i32, i32>(&mut store, "foo")?
            .call(&mut store, i)?;
    }

    println!("i = {}", i);
    print!("Elapsed: {}ms", start.elapsed().as_millis());

    thread::sleep(std::time::Duration::from_secs(5));

    Ok(())
}
