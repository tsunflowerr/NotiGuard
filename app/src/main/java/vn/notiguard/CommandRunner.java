package vn.notiguard;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Executes fixed argv under the Shizuku UserService identity. Never invokes a shell parser. */
public final class CommandRunner implements Shell {
    private final AtomicLong executions = new AtomicLong();
    public long executionCount() { return executions.get(); }
    @Override public String run(String... args) throws Exception {
        if (args.length == 0 || !args[0].startsWith("/system/bin/")) throw new SecurityException("Invalid executable");
        executions.incrementAndGet();
        Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
        process.getOutputStream().close();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> {
            try (InputStream input = process.getInputStream()) {
                byte[] buffer = new byte[2048]; int count;
                while ((count = input.read(buffer)) != -1) {
                    synchronized (bytes) { if (bytes.size() < 64_000) bytes.write(buffer, 0, Math.min(count, 64_000 - bytes.size())); }
                }
            } catch (Exception ignored) { /* Process termination closes the pipe. */ }
        }, "command-output");
        reader.setDaemon(true); reader.start();
        try {
            if (!process.waitFor(8, TimeUnit.SECONDS)) throw new IllegalStateException("Lệnh hệ thống quá hạn (8 giây)");
            reader.join(1000);
            if (reader.isAlive()) throw new IllegalStateException("Không đọc hết kết quả hệ thống");
            String output;
            synchronized (bytes) { output = new String(bytes.toByteArray(), StandardCharsets.UTF_8); }
            if (process.exitValue() != 0 || output.contains("Permission Denial") || output.contains("SecurityException")
                    || output.contains("Exception occurred") || output.contains("Error:") || output.contains("Unknown command")) {
                throw new IllegalStateException(output.trim().isEmpty() ? "Mã lỗi " + process.exitValue() : output.trim());
            }
            return output.replaceFirst("\\r?\\n$", "");
        } finally { process.destroyForcibly(); }
    }
}
