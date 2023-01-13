function benchmark(instance, times, iterations) {
  let totalTime = 0;
  let result = 0;

  for (let i = 0; i < iterations; i++) {
    const start = performance.now();

    for (let i = 0; i < times; i++) {
      result = instance.exports.foo(result);
    }

    const end = performance.now();
    totalTime += end - start;
  }

  return {
    totalTime: totalTime / iterations,
    result,
  };
}

(async () => {
  const response = await fetch("http://192.168.31.220:5500/wasm-latency/noop.wasm", { cache: "force-cache" }).then(
    (response) => response.arrayBuffer()
  );
  const module = new WebAssembly.Module(response);
  const instance = new WebAssembly.Instance(module);

  benchmark(instance, 10_000_000, 10);
  const { totalTime, result } = benchmark(instance, 1_000_000, 10);

  alert("WASM Latency: " + totalTime + "ms" + "\n" + "WASM Result: " + result);
})();
