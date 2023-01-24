import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.ByteSequence;

class Main {

	static void wasm(Context context, byte[] binary, int moduleCount) throws IOException {
		System.out.println("Loading WASM modules...");
		for (int i = 0; i < moduleCount; i++) {
			Source.Builder sourceBuilder = Source.newBuilder("wasm", ByteSequence.create(binary), i + "noop.wasm");
			Source source = sourceBuilder.build();
			context.eval(source);
		}

		System.out.println("Warming...");
		Value i = context.getBindings("wasm").getMember("main").getMember("foo").execute(0);
		while (i.asInt() < moduleCount) {
			Value foo = context.getBindings("wasm").getMember(i.asInt() + "noop.wasm").getMember("foo");

			Value j = foo.execute(0);
			while (j.asInt() < 10) {
				j = foo.execute(j);
			}

			i = foo.execute(i);
		}

		System.out.println("Executing WASM modules...");

		long startTime = System.nanoTime();

		i = context.getBindings("wasm").getMember("main").getMember("foo").execute(0);
		while (i.asInt() < moduleCount) {
			i = context.getBindings("wasm").getMember(i.asInt() + "noop.wasm").getMember("foo").execute(i);
		}

		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000;

		System.out.println(i.asInt());
		System.out.println("Execution time: " + duration + "ms");
	}

	public static void main(String[] args) throws IOException {
		Engine engine = Engine.create();
		Builder builder = Context.newBuilder().engine(engine);
		// .option("wasm.Builtins", "wasi_snapshot_preview1");

		for (int i = 0; i < 10; i++) {
			try (Context context = builder.build()) {
				// context.eval("js", "var a = 42;");
				// Value a = context.getBindings("js").getMember("a");
				// System.out.println(a);

				// Load the WASM contents into a byte array
				// byte[] binary = Files.readAllBytes(Paths.get("floyd.wasm"));
				byte[] binary = Files.readAllBytes(Paths.get("../wasm-latency/noop.wasm"));

				// Source.Builder sourceBuilder = Source.newBuilder("wasm",
				// ByteSequence.create(binary), "floyd.wasm");

				wasm(context, binary, 1_000_000);
			}
		}
	}
}