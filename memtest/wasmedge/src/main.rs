use std::thread;

use anyhow::{Error, Ok};
use wasmedge_sdk::{params, wat2wasm, Instance, Module, Store, WasmVal};

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
    let mut executor = wasmedge_sdk::Executor::new(None, None).unwrap();

    thread::sleep(std::time::Duration::from_secs(2));

    let start = std::time::Instant::now();

    let module_count = 1;

    println!("Generating {} modules", module_count);
    let modules: Vec<Module> = (0..module_count).map(|i| module_gen(i).unwrap()).collect();

    println!("Registering modules");
    let instances: Vec<Instance> = modules
        .iter()
        .enumerate()
        .map(|(i, module)| {
            let noop = store
                .register_named_module(&mut executor, format!("noop{}", i), &module)
                .unwrap();
            noop
        })
        .collect();

    println!("Calling modules...");
    let funcs: Vec<_> = instances
        .iter()
        .enumerate()
        .map(|(i, instance)| {
            instance.func(format!("foo{}", i)).expect("foo not found")
            // instance
            //     .get_typed_func::<i32, i32>(&mut executor, "foo")
            //     .unwrap()
        })
        .collect();

    let mut i = 1;
    for foo in funcs.iter() {
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
