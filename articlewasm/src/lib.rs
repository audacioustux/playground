use uuid::Uuid;
use uuid::v1::{Context, Timestamp};

#[no_mangle]
pub fn foo(n: i32) -> i32 {
    let context = Context::new(n.try_into().unwrap());
    let ts = Timestamp::from_unix(&context, 1497624119, 1234);
    let uuid = Uuid::new_v1(ts, &[1, 2, 3, 4, 5, 6]);

    uuid.to_string().into_bytes().iter().fold(0, |acc, x| acc + *x as i32)
}