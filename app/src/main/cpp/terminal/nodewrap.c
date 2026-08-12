/*
 * nodewrap — a tiny seccomp wrapper around the `node` binary.
 *
 * Why this exists:
 *   The terminal runs inside a proot jail. The shipped proot (NDK r20 build)
 *   does NOT know about the `statx()` syscall (it is absent from proot's
 *   syscall table). Node.js v20+/v22 (via libuv) uses `statx()` to implement
 *   fs.stat / Module._resolveFilename. Because proot does not intercept
 *   statx, the raw syscall is forwarded to the kernel *with untranslated
 *   guest paths*, so every statx() on a rootfs path returns ENOENT even
 *   though the file exists. Result: Node cannot load modules and prints
 *
 *       node:internal/modules/cjs/loader:1433
 *         throw err;
 *         Error: Cannot find module '<...>'
 *
 *   Classic stat()/newfstatat() IS handled by proot and works fine.
 *
 * Fix:
 *   Install a seccomp-BPF filter that returns ENOSYS for the statx syscall.
 *   libuv probes statx() once at startup; on ENOSYS it falls back to
 *   stat()/newfstatat(), which proot translates correctly. Modules then load
 *   and npm/pnpm work.
 *
 * Usage:
 *   Install this binary on the PATH *before* the real `node` (e.g. as
 *   /usr/local/bin/node). It locates the real node by trying a list of
 *   candidate paths (excluding itself), installs the filter, then execve()'s
 *   the real node with the same argv.
 */

#define _GNU_SOURCE
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <sys/prctl.h>
#include <linux/seccomp.h>
#include <linux/filter.h>
#include <linux/audit.h>
#include <linux/bpf.h>
#include <sys/syscall.h>

/* syscall numbers: aarch64=291, x86_64=332, arm=397 (oabi) — cover all. */
static long do_statx_syscall(long a, long b, long c, long d, long e) {
#ifdef __NR_statx
    return syscall(__NR_statx, a, b, c, d, e);
#else
    (void)a; (void)b; (void)c; (void)d; (void)e;
    errno = ENOSYS;
    return -1;
#endif
}

static int install_seccomp_block_statx(void) {
    if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0) {
        return -1;
    }

#ifdef __NR_statx
    struct sock_filter filter[] = {
        /* load syscall number */
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS,
                 offsetof(struct seccomp_data, nr)),
        /* if nr == __NR_statx -> return ENOSYS */
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, __NR_statx, 0, 1),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | (ENOSYS & SECCOMP_RET_DATA)),
        /* otherwise allow */
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
    };
    struct sock_fprog prog = {
        .len = (unsigned short)(sizeof(filter) / sizeof(filter[0])),
        .filter = filter,
    };

    /* Prefer seccomp(2); fall back to prctl(PR_SET_SECCOMP) */
    if (syscall(__NR_seccomp, SECCOMP_SET_MODE_FILTER, 0, &prog) != 0) {
        if (prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog) != 0) {
            return -1;
        }
    }
#endif
    return 0;
}

/* Try to locate the *real* node, skipping our own binary. */
static const char *find_real_node(char *self_path, size_t self_len) {
    static char buf[4096];
    ssize_t n = readlink("/proc/self/exe", buf, sizeof(buf) - 1);
    if (n > 0) {
        buf[n] = '\0';
        if (self_path && self_len) {
            strncpy(self_path, buf, self_len - 1);
            self_path[self_len - 1] = '\0';
        }
    }

    /* Candidate real-node locations, in priority order. */
    static const char *candidates[] = {
        "/usr/bin/node",
        "/usr/local/bin/node",
        "/opt/node/bin/node",
        "/usr/bin/node-current",
        NULL,
    };

    char self[4096] = {0};
    if (n > 0) {
        strncpy(self, buf, sizeof(self) - 1);
    }

    for (int i = 0; candidates[i] != NULL; i++) {
        const char *c = candidates[i];
        if (self[0] != '\0' && strcmp(c, self) == 0) {
            continue;
        }
        if (access(c, X_OK) == 0) {
            /* Make sure we are not resolving to ourselves via a symlink. */
            char r[4096] = {0};
            ssize_t rn = readlink(c, r, sizeof(r) - 1);
            if (rn > 0) {
                r[rn] = '\0';
                if (strcmp(r, self) == 0) {
                    continue;
                }
            }
            return c;
        }
    }

    /* Last resort: look up "node" on PATH, skipping ourselves. */
    const char *path_env = getenv("PATH");
    if (!path_env) {
        return NULL;
    }
    char *pcopy = strdup(path_env);
    if (!pcopy) {
        return NULL;
    }
    const char *result = NULL;
    char *save = NULL;
    for (char *dir = strtok_r(pcopy, ":", &save);
         dir != NULL;
         dir = strtok_r(NULL, ":", &save)) {
        char cand[4096];
        int w = snprintf(cand, sizeof(cand), "%s/node", dir);
        if (w <= 0 || (size_t)w >= sizeof(cand)) {
            continue;
        }
        if (self[0] != '\0' && strcmp(cand, self) == 0) {
            continue;
        }
        if (access(cand, X_OK) == 0) {
            char r[4096] = {0};
            ssize_t rn = readlink(cand, r, sizeof(r) - 1);
            if (rn > 0) {
                r[rn] = '\0';
                if (strcmp(r, self) == 0) {
                    continue;
                }
            }
            result = strdup(cand);
            break;
        }
    }
    free(pcopy);
    return result;
}

int main(int argc, char **argv) {
    char self[4096] = {0};
    const char *real_node = find_real_node(self, sizeof(self));
    if (!real_node) {
        fprintf(stderr, "nodewrap: cannot locate the real `node` binary\n");
        return 127;
    }

    if (install_seccomp_block_statx() != 0) {
        /* Filter unavailable (e.g. kernel without seccomp). Fall through and
         * exec the real node directly — Node will hit the statx problem, but
         * at least we do not make things worse. */
        fprintf(stderr,
                "nodewrap: WARNING: seccomp filter install failed (%s); "
                "node module loading may be broken in this proot jail.\n",
                strerror(errno));
    }

    /* Build argv for the real node: argv[0] = "node", rest unchanged. */
    char **newargv = (char **)malloc(sizeof(char *) * (argc + 1));
    if (!newargv) {
        fprintf(stderr, "nodewrap: out of memory\n");
        return 127;
    }
    newargv[0] = (char *)"node";
    for (int i = 1; i < argc; i++) {
        newargv[i] = argv[i];
    }
    newargv[argc] = NULL;

    execv(real_node, newargv);
    perror("nodewrap: execv");
    free(newargv);
    return 127;
}
