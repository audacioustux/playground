
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

    static void wasm(Context context, byte[] binary, int count) throws IOException {
        System.out.println("Loading WASM modules...");

        Source.Builder sourceBuilder = Source.newBuilder("wasm", ByteSequence.create(binary), "noop.wasm");
        Source source = sourceBuilder.build();
        context.eval(source);

        Value foo = context.getBindings("wasm").getMember("main").getMember("foo");
        System.out.println("Warming...");

        int nth = 10_000;

        Value i = foo.execute(nth);
        for (int j = 0; j < count; j++) {
            i = foo.execute(i);
        }

        System.out.println("Executing WASM modules...");
        long startTime = System.nanoTime();

        i = foo.execute(nth);
        for (int j = 0; j < count; j++) {
            i = foo.execute(i);
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);

        System.out.println(i.asInt());
        System.out.println("Execution time: " + duration + "ns");
        System.out.println("per iteration: " + duration / count + "ns");
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
                byte[] binary = Files.readAllBytes(Paths.get("../wasm-latency/nth_prime.wasm"));

                // Source.Builder sourceBuilder = Source.newBuilder("wasm",
                // ByteSequence.create(binary), "floyd.wasm");
                wasm(context, binary, 10_000);
            }
        }
    }
}
