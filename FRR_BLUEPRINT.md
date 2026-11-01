# dnbootstrap + FristOneRR PanVK on Mali-G57 MC2 — Implementation Blueprint

Everything below was derived from the three archives you uploaded (ELF inspection, `strings`, source reading).
**Nothing here has been run on a device** — I had no NDK or phone. Items are tagged
**[verified]** (seen in the binaries/sources), **[reasoned]** (engineering inference) or **[test]** (needs an A/B on your phone).

## 0. What the archives actually show (read this first)

| Finding | Consequence |
|---|---|
| dnbootstrap renders with **OpenGL ES through ANGLE** (`LIBGL_EGL=libEGL_angle.so`, GLFW fork). It never creates a Vulkan instance itself. [verified] | "Hooking dnbootstrap into PanVK" really means **ANGLE's Vulkan backend → PanVK**. You cannot tune command buffers/descriptors directly; you tune ANGLE and the driver. |
| ANGLE hard-codes `dlopen("libvulkan.so")` and has no path override. [verified] | Env vars alone cannot redirect it. Fix: same-length binary patch `libvulkan.so → libvk_frr.so` + a tiny shim (`frr_vulkan_shim.c`). |
| `libvulkan_panfrost.so` exports only `vk_icd*` (no hwvulkan `HAL_MODULE_INFO_SYM`) and contains a **kbase backend** (`Found kbase device`, `/dev/mali%d`, `panvk_android.c`). [verified] | It is a Mesa ICD that talks to the stock Mali kernel driver — viable on Android. Load it directly, not via the HAL. |
| `libvulkan_wrapper.so` is the Mesa *wrapper* ICD built for **Winlator/Termux**: NEEDED `libxcb*`, `libdrm`, `libX11-xcb`, `libandroid-shmem`, `libadrenotools`, `libc++_shared`, `libz.so.1`; RUNPATH/paths point at `com.winlator.cmod`. Its `install.bat` targets `Z:\usr\lib` (Wine). [verified] | **It cannot load inside a plain APK** (none of those libs ship). It also exists to emulate BCn/spoof devices for DXVK — ANGLE needs none of that. Bundled but **not in the default chain**. |
| Env vars you listed: `LIBVULKAN_PATH`, `PAN_MESA_DEBUG`, `VK_ICD_FILENAMES` are **not read by either binary**; `MESA_VK_VERSION_OVERRIDE` **is**. [verified] | `LIBVULKAN_PATH` is now defined by *our* shim. `PAN_MESA_DEBUG` → use `PANVK_DEBUG` / `PANVK_*`. `VK_ICD_FILENAMES` is meaningless without a Khronos loader. |
| Runtime is **.NET 8.0.22 CoreCLR** via hostfxr (Mono lines in `dnbootstrap.c` are commented out). [verified] | Tune `DOTNET_*`, not `MONO_*`. |
| Driver ELF segments are 16 KB aligned (`0x4000`). [verified] | OK for 16 KB-page devices/Android 15+. |

## 1. Library injection & driver architecture

### 1.1 Chain
```
Vintage Story (.NET) → GLFW fork → LTW/ANGLE (libGLESv2_angle.so, patched)
   → dlopen("libvk_frr.so")           [our shim, CMake target vk_frr]
   → dlopen($LIBVULKAN_PATH)          [libvulkan_panfrost.so, PanVK ICD]
   → /dev/mali0 (kbase)
```
If the PanVK load fails the shim logs `FALLBACK to system libvulkan.so` and the game still runs on the stock driver.

### 1.2 Layout (`app/src/main/jniLibs/arm64-v8a/`)
```
libvulkan_panfrost.so    18 MB  PanVK 3.5 (loaded by absolute path)
libvulkan_wrapper.so     11 MB  optional / experimental, unused by default
libGLESv2_angle.so       patched copy (overrides the one in libs/angle.aar)
(libvk_frr.so, libdnbootstrap.so, libglfw.so are built by CMake)
```
`tools/apply.sh <root> <PanVK.zip> <Wrapper.zip>` does all of this, including the ANGLE patch (2 occurrences, file size unchanged).

### 1.3 Environment — set before any native init
`FrrApp` (declared in the manifest) calls `FrrRuntimeEnv.apply()` in `Application.onCreate`, i.e. before `System.loadLibrary("glfw")` and before hostfxr. It uses `Os.setenv`, which is the same process environment `getenv()` sees in C. Edit values live without rebuilding via
`/sdcard/Android/data/git.artdeell.dnbootstrap/files/frr_env.txt` (`KEY=VALUE`, empty value unsets).

| Variable | Set to | Read by |
|---|---|---|
| `LIBVULKAN_PATH` | `<nativeLibraryDir>/libvulkan_panfrost.so` | our shim |
| `FRR_DISABLE` | `0` (`1` = stock driver) | our shim |
| `ANGLE_DEFAULT_PLATFORM` | `vulkan` | ANGLE [verified string] |
| `MESA_VK_VERSION_OVERRIDE` | unset; try `1.1` only if ANGLE rejects the device | Mesa [verified] |
| `PANVK_LOG_ERRORS` | `1` (debug builds) | PanVK [verified] |
| `MESA_SHADER_CACHE_DIR` / `_MAX_SIZE` | app cache / `512M` | Mesa [verified] — avoids shader recompiles |

### 1.4 Using the wrapper anyway (experimental)
Requires building/shipping its missing deps (`libadrenotools`, `libandroid-shmem`, libxcb family, libdrm, `libz.so.1`, `libzstd.so.1` — note the app's `libzstd.so` lacks the `.so.1` name) and a Khronos loader to read an ICD JSON — which is where `VK_ICD_FILENAMES` would finally matter. Its own knobs (`WRAPPER_*`, `NO_PRESENT_WAIT`, `ADRENOTOOLS_DRIVER_PATH/NAME`) are real **[verified]**. I do not recommend this for ANGLE.

### 1.5 Biggest unverified risks **[test]**
1. **`libhardware.so` / `libsync.so` in PanVK's NEEDED list.** `libhardware` is not in Android's public-library list, so `dlopen` from the app namespace may fail on some ROMs. Check `adb logcat -s frr_vk`. If you see `dlopen(...) failed: library "libhardware.so" not found`, add `libadrenotools`' `linkernsbypass` and open the driver through it (fallback keeps the game running meanwhile).
2. **Surface creation without a loader.** `vkCreateAndroidSurfaceKHR` is normally brokered by the loader; calling the Mesa ICD directly depends on the PanVK build exposing it. Logcat/PanVK errors will show this immediately.
3. ANGLE's Vulkan backend needs Vulkan ≥ 1.1; check the reported version in logcat before trying `MESA_VK_VERSION_OVERRIDE` (forcing a higher version than the driver implements is unsafe).

## 2. Mali-G57 (Valhall) TBDR & memory tuning

Vintage Story submits GL; ANGLE records Vulkan. What you can actually influence:

**Render passes (biggest TBDR lever) [reasoned]**
- Every pass break forces a tile store+reload to DRAM. Avoid `glReadPixels`, `glFinish`, and mid-frame FBO ping-pong; the VS shadow/SSAO/bloom chain is where this hurts. Disabling SSAO/bloom/shadow quality in-game is worth more than any driver flag.
- Make sure attachments are cleared/invalidated at pass start (`glClear` / `glInvalidateFramebuffer`) so ANGLE emits `LOAD_OP_CLEAR/DONT_CARE` instead of `LOAD`; same for depth/stencil at pass end (`STORE_OP_DONT_CARE`).
- Render at a lower scale (e.g. 0.75) and upscale: fill-rate is the usual G57 MC2 limit.

**Surface/image formats**
- `R8G8B8A8_UNORM` swapchain, 3 images; skip 10-bit/FP16 surfaces. Depth: prefer `D24S8`-class over `D32F`. Avoid `STORAGE` usage on color targets — it generally disables AFBC compression. PanVK has AFBC support (`PANVK_AFBC`, `wsi_afbc` strings) **[verified names, semantics test]**.

**Descriptors**
- ANGLE owns descriptor pools. With ANGLE's default (one pool, per-program sets) nothing to tune manually; don't enable `useMultipleDescriptorsForExternalFormats`. Fewer, larger UBOs beat many small ones on Valhall; `forceMaxUniformBufferSize16KB` exists in this ANGLE build if you hit UBO size errors **[verified name]**.

**Coherent vs non-coherent memory (voxel chunk uploads)**
- On a non-IO-coherent Mali, `HOST_VISIBLE|HOST_COHERENT` memory is normally CPU **uncached, write-combined**; `HOST_CACHED` memory needs explicit flush/invalidate. Chunk meshes are **write-once streaming data** → WC/coherent is the right choice: no cache maintenance, fast sequential writes. Cached memory only pays when the CPU reads back or read-modify-writes.
- Hence the defaults: `ANGLE_FEATURE_OVERRIDES_DISABLED=preferHostCachedForNonStaticBufferUsage`, `…ENABLED=persistentlyMappedBuffers` (both names **[verified]** in this ANGLE). Never read from mapped GL buffers on the CPU side (WC reads are very slow).
- In VS-side behaviour: batch chunk uploads (fewer, larger `glBufferSubData` ranges) rather than many tiny ones; flush ranges must be aligned to `nonCoherentAtomSize` if cached memory is ever selected.
- PanVK exposes explicit knobs whose **names** suggest control over this: `PANVK_KBASE_USER_CACHE_SYNC`, `PANVK_HOSTIMP_CACHED`, `PANVK_HOSTIMP_*`. **[test]** one at a time through `frr_env.txt`; keep whichever lowers frame-time spikes during chunk loading.
- Tiler heap: dense voxel scenes can overflow the tile-list heap and stall. `PANVK_TILER_HEAP_MB`, `PANVK_POLY_HEAP_MB`, `PANVK_STALL_LOG` exist **[verified names]** — enable `PANVK_STALL_LOG=1`, and raise the heap only if stalls are logged.

## 3. NDK / build settings (already applied in `frr-mali.patch`)

- `-O3 -flto=thin -fvisibility=hidden -mcpu=cortex-a75+nocrypto` on **our targets only** (`dnbootstrap`, `vk_frr`), not GLFW. `-mcpu=cortex-a75` already implies armv8.2-a; adding `-march=armv8-a` is contradictory, so I dropped it. `+nocrypto` because some MediaTek SKUs lack the crypto extension. Infinix Hot 50 variants use A75/A76 + A55 — A75 is a safe floor. Honest note: our native code is tiny; these flags won't change game performance. The hot paths are the .NET JIT and prebuilt libs.
- **`-fvisibility=hidden` hazard:** the GLFW fork looks up `dnb_main` by symbol. I marked it `visibility("default")`; without that the app would start and fail to attach the JVM. JNI functions are safe (`JNIEXPORT` is default-visibility).
- Packaging: `useLegacyPackaging = true` (already in your project) + `android:extractNativeLibs="true"` so drivers land in `nativeLibraryDir` as real files; `keepDebugSymbols` for the two driver libs (no strip, byte-identical); `pickFirsts` so the patched ANGLE beats the AAR copy; `-Wl,-z,max-page-size=16384` for 16 KB-page devices.

## 4. .NET 8 CoreCLR tuning for voxel GC pressure

Set by `FrrRuntimeEnv` (hex values are bytes):

| Variable | Value | Why |
|---|---|---|
| `DOTNET_gcServer` | `0` | workstation GC; server GC is wrong for 8 cores of mixed A76/A55 |
| `DOTNET_gcConcurrent` | `1` | background gen2 keeps long pauses off the render thread |
| `DOTNET_GCgen0size` | `0x2000000` (32 MB) | gen0 budget is derived from cache size, which is often undetectable on ARM Android → tiny budget → constant gen0 GCs during meshing. A fixed 32 MB cuts GC frequency. **[reasoned/test]** |
| `DOTNET_GCRetainVM` | `1` | keep freed memory instead of returning it to the OS and re-faulting |
| `DOTNET_TieredPGO` | `0` | removes instrumentation overhead in tier-0 code |
| `DOTNET_TC_QuickJitForLoops` / `TieredCompilation` | `1` / `1` | default; hot loops get promoted via OSR |

Try `0x4000000` (64 MB) if the device has ≥6 GB RAM, and drop to `0x1000000` if the OOM killer shows up. Don't also set `GCgen0MaxBudget`; it fights `GCgen0size`. A standalone GC (`libclrgc.so`) is not in the shipped runtime, so skip `DOTNET_GCName`.

## 5. Install & verify

```bash
tools/apply.sh <dnbootstrap-root> PanVK-FristOneRR-V_3_5.zip FristOneRR-Wrapper-3_0.zip
./gradlew assembleDebug
adb logcat -s frr_vk FrrRuntimeEnv
```
Success = `PanVK ICD loaded: …/libvulkan_panfrost.so`. `FALLBACK to system libvulkan.so` = driver rejected (see 1.5). Note: `apply.sh` copies the already-patched files, so **don't also `git apply`** the diff — the `.patch` is provided for review only.

Legal: both archives include Mesa (MIT) licensing; keep `LICENSE-Mesa.txt` with any redistributed APK.
