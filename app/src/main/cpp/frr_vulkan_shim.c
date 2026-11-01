// libvk_frr.so -- tiny Vulkan "loader" for ANGLE -> FristOneRR PanVK (Mali kbase).
// ANGLE (patched to dlopen "libvk_frr.so" instead of "libvulkan.so") lands here.
// We dlopen the Mesa PanVK ICD directly and forward vkGetInstanceProcAddr to
// vk_icdGetInstanceProcAddr. No Vulkan headers needed: everything is void*.
//
// Env (read here, set by FrrRuntimeEnv.java):
//   LIBVULKAN_PATH   absolute path of the driver .so   (default: <nativeLibDir>/libvulkan_panfrost.so)
//   FRR_DISABLE=1    skip PanVK, forward to the system Vulkan loader instead
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#ifdef __ANDROID__
#include <android/log.h>
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "frr_vk", __VA_ARGS__)
#else
#define LOG(...) (fprintf(stderr, __VA_ARGS__), fputc('\n', stderr))
#endif
#define EXPORT __attribute__((visibility("default")))

typedef void *(*PFN_gipa)(void *instance, const char *name);
typedef int   (*PFN_negotiate)(unsigned *version);

static pthread_once_t g_once = PTHREAD_ONCE_INIT;
static PFN_gipa g_gipa;

static void *try_open(const char *path) {
    void *h = dlopen(path, RTLD_NOW | RTLD_LOCAL);
    if (!h) LOG("dlopen(%s) failed: %s", path, dlerror());
    return h;
}

static void init_once(void) {
    const char *disable = getenv("FRR_DISABLE");
    const char *path    = getenv("LIBVULKAN_PATH");

    if (!(disable && disable[0] == '1') && path && path[0]) {
        void *h = try_open(path);
        if (h) {
            PFN_negotiate neg = (PFN_negotiate)dlsym(h, "vk_icdNegotiateLoaderICDInterfaceVersion");
            unsigned ver = 5;
            if (neg) neg(&ver);
            g_gipa = (PFN_gipa)dlsym(h, "vk_icdGetInstanceProcAddr");
            LOG("PanVK ICD loaded: %s (icd interface v%u)", path, ver);
        }
    }
    if (!g_gipa) {  // graceful fallback: stock vendor driver
        void *h = try_open("libvulkan.so");
        if (h) {
            g_gipa = (PFN_gipa)dlsym(h, "vkGetInstanceProcAddr");
            LOG("FALLBACK to system libvulkan.so");
        }
    }
    if (!g_gipa) LOG("no Vulkan implementation available");
}

EXPORT void *vkGetInstanceProcAddr(void *instance, const char *name) {
    pthread_once(&g_once, init_once);
    if (!g_gipa || !name) return NULL;
    if (!strcmp(name, "vkGetInstanceProcAddr")) return (void *)vkGetInstanceProcAddr;
    return g_gipa(instance, name);
}

EXPORT void *vkGetDeviceProcAddr(void *device, const char *name) {
    typedef void *(*F)(void *, const char *);
    F f = (F)vkGetInstanceProcAddr(NULL, "vkGetDeviceProcAddr");
    return f ? f(device, name) : NULL;
}

EXPORT int vkEnumerateInstanceVersion(unsigned *v) {
    typedef int (*F)(unsigned *);
    F f = (F)vkGetInstanceProcAddr(NULL, "vkEnumerateInstanceVersion");
    if (f) return f(v);
    *v = (1u << 22);  // 1.0
    return 0;
}

EXPORT int vkEnumerateInstanceExtensionProperties(const char *l, unsigned *n, void *p) {
    typedef int (*F)(const char *, unsigned *, void *);
    F f = (F)vkGetInstanceProcAddr(NULL, "vkEnumerateInstanceExtensionProperties");
    return f ? f(l, n, p) : -3;  // VK_ERROR_INITIALIZATION_FAILED
}

EXPORT int vkEnumerateInstanceLayerProperties(unsigned *n, void *p) {
    typedef int (*F)(unsigned *, void *);
    F f = (F)vkGetInstanceProcAddr(NULL, "vkEnumerateInstanceLayerProperties");
    if (f) return f(n, p);
    *n = 0;
    return 0;
}

EXPORT int vkCreateInstance(const void *ci, const void *alloc, void *out) {
    typedef int (*F)(const void *, const void *, void *);
    F f = (F)vkGetInstanceProcAddr(NULL, "vkCreateInstance");
    return f ? f(ci, alloc, out) : -3;
}
