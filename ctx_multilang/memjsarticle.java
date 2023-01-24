import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

class Main {
	
	static void waitforgc() throws InterruptedException {
		System.gc();
        Thread.sleep(Duration.ofSeconds(5).toMillis());
	}

	static void jcmdbaseline() throws IOException, InterruptedException {
		waitforgc();

		new ProcessBuilder("jcmd", Long.toString(ProcessHandle.current().pid()), "VM.native_memory", "baseline")
							.start()
							.waitFor();
	}

	static void jcmdsummary() throws IOException, InterruptedException {
		waitforgc();

		new ProcessBuilder("jcmd", Long.toString(ProcessHandle.current().pid()), "VM.native_memory", "summary.diff")
							.redirectOutput(ProcessBuilder.Redirect.INHERIT)
							.redirectError(ProcessBuilder.Redirect.INHERIT)
							.start()
							.waitFor();
	}

	static void js(Context context, String code, int moduleCount) throws IOException, InterruptedException {

		jcmdbaseline();

		System.out.println("Loading WASM modules...");

		List<Value> modules = new ArrayList<>();
		for (int i = 0; i < moduleCount; i++) {
			Source.Builder sourceBuilder = Source.newBuilder("js", code, i + "noop.js")
      			.mimeType("application/javascript+module");
			Source source = sourceBuilder.build();
			modules.add(context.eval(source));
		}

		System.out.println("after loading jcmd summary");
		jcmdsummary();

		System.out.println("Executing js modules...");
		
		Value i = modules.get(0).getMember("foo").execute(1);
		for (int j = 1; j < moduleCount; j++) {
			i = modules.get(j).getMember("foo").execute(i);
		}

		System.out.println("after execution jcmd summary");
		jcmdsummary();

		if (moduleCount > 1) {
			int randomNumber = new Random().nextInt(moduleCount - 1) + 1;
			i = modules.get(randomNumber).getMember("foo").execute(i);
		}

		System.out.println(i.asInt());
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Boolean dowait = args.length > 0 && args[0].equals("wait");
		if (dowait) Thread.sleep(Duration.ofSeconds(10).toMillis());

		Engine engine = Engine.create();
		Builder builder = Context.newBuilder()
			.allowAllAccess(true)
			.allowExperimentalOptions(true)
   	       	.option("js.esm-eval-returns-exports", "true")
		  	.engine(engine);
		// .option("wasm.Builtins", "wasi_snapshot_preview1");

		// byte[] binary = Files.readAllBytes(Paths.get("../wasm-latency/noop.wasm"));

		String code = Files.readString(Paths.get("./slugify.mjs"));

		try (Context context = builder.build()) {
			js(context, code, 5_000);
		}
	}
}