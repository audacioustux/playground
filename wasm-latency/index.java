import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.io.ByteSequence;
import java.nio.file.Files;
import java.nio.file.Path;

class Main {
	public static void main(String[] args) throws Exception {
		var engine = Engine.newBuilder().build();

		var noop = Files.readAllBytes(Path.of("./noop.wasm"));
		var source = Source.newBuilder("wasm", ByteSequence.create(noop), "noop.wasm").build();

		for (int i = 0; i < 1000000; i++) {
			try (var context = Context.newBuilder().engine(engine).build()) {
				var module = context.parse(source);
				module.execute();
			}
		}

		Thread.sleep(5000);
		System.gc();

		var start = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			try (var context = Context.newBuilder().engine(engine).build()) {
				var module = context.parse(source);
				module.execute();
			}
		}
		var end = System.nanoTime();
		System.out.println("Time: " + (end - start) / 1000000 + "ms");

		Thread.sleep(5000);
		System.gc();

		start = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			var context = Context.newBuilder().engine(engine).build();
			var module = context.parse(source);
			module.execute();
		}
		end = System.nanoTime();
		System.out.println("Time (without closing): " + (end - start) / 1000000 + "ms");
	}
}