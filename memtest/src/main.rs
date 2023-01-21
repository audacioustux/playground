use anyhow::Error;
use wasmtime::{Config, Engine, Instance, Module, Store};

fn main() -> Result<(), Error> {
    let config = Config::new();

    let engine = Engine::new(&config)?;

    let curr_dir = std::env::current_dir()?;
    let wasm_file = curr_dir.join("noop.wasm");
    let module = Module::from_file(&engine, wasm_file).unwrap();

    let mut store = Store::new(&engine, ());
    let imports = vec![];
    let instance = Instance::new(&mut store, &module, &imports)?;

    let result = instance
        .get_typed_func::<i32, i32>(&mut store, "foo")?
        .call(&mut store, 0)?;

    println!("{}", result);
    Ok(())
}
