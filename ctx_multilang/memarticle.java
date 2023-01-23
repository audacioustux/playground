import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Random;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.ByteSequence;

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

	static void wasm(Context context, byte[] binary, int moduleCount) throws IOException, InterruptedException {

		jcmdbaseline();

		System.out.println("Loading WASM modules...");
		for (int i = 0; i < moduleCount; i++) {
			Source.Builder sourceBuilder = Source.newBuilder("wasm", ByteSequence.create(binary), i + "article.wasm");
			Source source = sourceBuilder.build();
			context.eval(source);
		}

		System.out.println("after loading jcmd summary");
		jcmdsummary();

		System.out.println("Executing WASM modules...");
		System.out.println(context.getBindings("wasm").getMember("main").getMemberKeys());

		context.getBindings("wasm").getMember("main").getMember("run").execute(0);
		for (int j = 1; j < moduleCount; j++) {
			context.getBindings("wasm").getMember(j + "article.wasm").getMember("_main").getMember("run").execute();
		}

		System.out.println("after execution jcmd summary");
		jcmdsummary();

		String randomModuleName = new Random().nextInt(moduleCount) + "article.wasm";
		context.getBindings("wasm").getMember(randomModuleName).getMember("run").execute();

	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Boolean dowait = args.length > 0 && args[0].equals("wait");
		if (dowait) Thread.sleep(Duration.ofSeconds(10).toMillis());

		Engine engine = Engine.create();
		Builder builder = Context
			.newBuilder()
			.allowAllAccess(true)
			.option("wasm.Builtins", "wasi_snapshot_preview1")
			.engine(engine);

		byte[] binary = Files.readAllBytes(Paths.get("../wasm-latency/article.wasm"));

		try (Context context = builder.build()) {
			wasm(context, binary, 1);
		}
	}
}