use std::{thread};

use anyhow::{Error, Ok};
use wasmedge_sdk::{
    params, wat2wasm, Module, Store, WasmVal,
};

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
    let mut store = Store::new()?;

    thread::sleep(std::time::Duration::from_secs(2));

    let start = std::time::Instant::now();

    let module_count = 10_000;

    let mut i = 1;
    while i < module_count {
        let mut executor = wasmedge_sdk::Executor::new(None, None)?;
        let noop =
            store.register_named_module(&mut executor, format!("noop{}", i), &module_gen(i)?)?;
        let foo = noop.func(format!("foo{}", i)).expect("foo not found");
        i = foo
            .call(&mut executor, params!(i))?
            .get(0)
            .unwrap()
            .to_i32();
    }

    println!("i = {}", i);
    println!("Elapsed: {}ms", start.elapsed().as_millis());

    thread::sleep(std::time::Duration::from_secs(5));

    Ok(())
}
