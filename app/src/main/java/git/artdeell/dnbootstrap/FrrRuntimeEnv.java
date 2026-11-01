package git.artdeell.dnbootstrap;

import android.content.Context;
import android.system.Os;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sets every environment variable the GPU/.NET stack reads. Must run before
 * System.loadLibrary("glfw") and before hostfxr starts (see FrrApp).
 *
 * Override file (no rebuild needed for A/B tests):
 *   /sdcard/Android/data/git.artdeell.dnbootstrap/files/frr_env.txt
 * One KEY=VALUE per line, '#' comments. A line "KEY=" (empty) UNSETS the key.
 */
public final class FrrRuntimeEnv {
    private static final String TAG = "FrrRuntimeEnv";
    private FrrRuntimeEnv() {}

    public static void apply(Context ctx) {
        String libDir = ctx.getApplicationInfo().nativeLibraryDir;
        Map<String, String> env = new LinkedHashMap<>();

        // ---- 1. Driver routing (read by libvk_frr.so, see frr_vulkan_shim.c) ----
        env.put("LIBVULKAN_PATH", libDir + "/libvulkan_panfrost.so");
        env.put("FRR_DISABLE", "0");                 // "1" = fall back to the stock vendor driver
        env.put("ANGLE_DEFAULT_PLATFORM", "vulkan"); // string verified inside libGLESv2_angle.so

        // ---- 2. Mesa/PanVK (names verified with `strings` on libvulkan_panfrost.so) ----
        // env.put("MESA_VK_VERSION_OVERRIDE", "1.1"); // honored by Mesa; leave unset unless ANGLE rejects the device
        env.put("PANVK_LOG_ERRORS", "1");              // cheap; drop for release builds
        env.put("MESA_SHADER_CACHE_DIR", ctx.getCacheDir().getAbsolutePath() + "/mesa_shaders");
        env.put("MESA_SHADER_CACHE_MAX_SIZE", "512M");
        // A/B knobs (semantics inferred from names only - test one at a time via frr_env.txt):
        //   PANVK_KBASE_USER_CACHE_SYNC=1  PANVK_HOSTIMP_CACHED=1  PANVK_AFBC=1
        //   PANVK_TILER_HEAP_MB=<n>  PANVK_POLY_HEAP_MB=<n>  PANVK_STALL_LOG=1  PANVK_STATS=1
        // NOT read by these binaries (checked): PAN_MESA_DEBUG, VK_ICD_FILENAMES, VK_DRIVER_FILES.

        // ---- 3. ANGLE feature flags (names verified in the binary) ----
        // Streaming voxel meshes are write-once: keep them in uncached write-combined memory.
        env.put("ANGLE_FEATURE_OVERRIDES_DISABLED", "preferHostCachedForNonStaticBufferUsage");
        env.put("ANGLE_FEATURE_OVERRIDES_ENABLED", "persistentlyMappedBuffers");

        // ---- 4. .NET 8 CoreCLR (runtime in dotnet-runtime.tgz is 8.0.22) ----
        env.put("DOTNET_gcServer", "0");
        env.put("DOTNET_gcConcurrent", "1");        // background gen2 -> short pauses
        env.put("DOTNET_GCgen0size", "0x2000000");  // 32 MB gen0 budget (hex bytes)
        env.put("DOTNET_GCRetainVM", "1");          // keep freed segments, avoid madvise churn
        env.put("DOTNET_TieredPGO", "0");           // no instrumentation overhead in tier-0
        env.put("DOTNET_TC_QuickJitForLoops", "1");
        env.put("DOTNET_TieredCompilation", "1");

        loadOverrides(ctx, env);

        for (Map.Entry<String, String> e : env.entrySet()) {
            try {
                if (e.getValue().isEmpty()) Os.unsetenv(e.getKey());
                else Os.setenv(e.getKey(), e.getValue(), true);
            } catch (Exception ex) {
                Log.e(TAG, "setenv " + e.getKey(), ex);
            }
        }
        Log.i(TAG, "applied " + env.size() + " env vars; driver=" + env.get("LIBVULKAN_PATH"));
    }

    private static void loadOverrides(Context ctx, Map<String, String> env) {
        File dir = ctx.getExternalFilesDir(null);
        if (dir == null) return;
        File f = new File(dir, "frr_env.txt");
        if (!f.isFile()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                int eq = line.indexOf('=');
                if (line.isEmpty() || line.startsWith("#") || eq <= 0) continue;
                env.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        } catch (Exception ex) {
            Log.w(TAG, "cannot read " + f, ex);
        }
    }
}
