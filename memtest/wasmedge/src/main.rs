use std::thread;

use anyhow::{Error, Ok};
use wasmedge_sdk::{params, wat2wasm, Module, Store, WasmVal};

fn main() -> Result<(), Error> {
    let module_codegen = |i| -> String {
        format!(
            r#"
            (module 
                (func (export "foo{}") (param i32) (result i32) 
                  local.get 0
                  i32.const 1
                  i32.add
                  return 
                )
            )
        "#,
            i
        )
    };

    let module_gen = |i| -> Result<Module, Error> {
        let module_wat = module_codegen(i);
        let wasm_bytes = wat2wasm(module_wat.as_bytes())?;
        let module = wasmedge_sdk::Module::from_bytes(None, &wasm_bytes)?;

        Ok(module)
    };

    thread::sleep(std::time::Duration::from_secs(2));

    let module_count = 1_000_000;

    println!("Generating {} modules", module_count);
    let modules: Vec<Module> = (0..module_count).map(|i| module_gen(i).unwrap()).collect();

    let start = std::time::Instant::now();
    println!("Registering modules");
    let mut instances: Vec<_> = modules
        .iter()
        .enumerate()
        .map(|(i, module)| {
            let mut store = Store::new().unwrap();
            let mut executor = wasmedge_sdk::Executor::new(None, None).unwrap();
            let noop = store
                .register_named_module(&mut executor, format!("noop{}", i), &module)
                .unwrap();
            (noop, executor)
        })
        .collect();
    println!("Elapsed: {}ms", start.elapsed().as_millis());

    println!("Calling modules...");
    let start = std::time::Instant::now();
    let i = instances
        .iter_mut()
        .enumerate()
        .fold(1, |count, (i, (instance, executor))| {
            let foo = instance.func(format!("foo{}", i)).expect("foo not found");
            foo.call(executor, params!(count))
                .unwrap()
                .get(0)
                .unwrap()
                .to_i32()
        });
    println!("Elapsed: {}ms", start.elapsed().as_millis());

    println!("i = {}", i);

    thread::sleep(std::time::Duration::from_secs(10));

    Ok(())
}
