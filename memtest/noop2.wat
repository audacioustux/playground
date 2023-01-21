(module 
    (func (export "foo") (param i32) (result i32) 
      local.get 0
      i32.const 2
      i32.add
      return 
    )
)
