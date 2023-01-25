#[no_mangle]
pub extern "C" fn foo(n: i32) -> i32 {
    let n = n + 1;
    let limit = sieve_upper_bound(n.try_into().unwrap());

    // Sieve of Eratosthenes: https://en.wikipedia.org/wiki/Sieve_of_Eratosthenes
    let mut sieve = vec![true; limit];

    sieve[0] = false;
    sieve[1] = false;

    let mut count = 0;

    for prime in 2..limit {
        // continue if not prime
        if !sieve[prime] {
            continue;
        }

        // terminate on nth prime
        count += 1;
        if count == n {
            // NOTE: mod 10000 to avoid overflow
            return (prime % 10000).try_into().unwrap();
        }

        // false-ify all multiples of prime
        for multiple in ((prime * prime)..limit).step_by(prime) {
            sieve[multiple] = false;
        }
    }

    2
}

// predicted nth-prime upper bound
fn sieve_upper_bound(n: u32) -> usize {
    let x = if n <= 10 { 10.0 } else { n as f64 };

    (x * (x * (x).ln()).ln()).ceil() as usize
}
