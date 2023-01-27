
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

class Main {

    static void wasm(Context context, String code, int count) throws IOException {
        System.out.println("Loading WASM modules...");

        Source.Builder sourceBuilder = Source.newBuilder("js", code, "slugify.js")
                .mimeType("application/javascript+module");
        Source source = sourceBuilder.build();
        Value foo = context.eval(source).getMember("foo");

        System.out.println("Warming...");

        Value i = foo.execute(0);
        for (int j = 0; j < count; j++) {
            i = foo.execute(i);
        }

        System.out.println("Executing modules...");
        long startTime = System.nanoTime();

        i = foo.execute(1);
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
        Builder builder = Context.newBuilder().engine(engine)
                .allowAllAccess(true)
                .allowExperimentalOptions(true)
                .option("js.esm-eval-returns-exports", "true");
        // .option("wasm.Builtins", "wasi_snapshot_preview1");

        String code = Files.readString(Paths.get("./slugify.mjs"));

        for (int i = 0; i < 10; i++) {
            try (Context context = builder.build()) {
                // context.eval("js", "var a = 42;");
                // Value a = context.getBindings("js").getMember("a");
                // System.out.println(a);

                // Load the WASM contents into a byte array
                // byte[] binary = Files.readAllBytes(Paths.get("floyd.wasm"));
                // Source.Builder sourceBuilder = Source.newBuilder("wasm",
                // ByteSequence.create(binary), "floyd.wasm");
                wasm(context, code, 1_000_000);
            }
        }
    }
}
