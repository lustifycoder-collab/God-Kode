#
# This file is part of GodKode.
#
# GodKode is free software: you can redistribute it and/or modify it under the terms of
# the GNU General Public License as published by the Free Software Foundation, either version 3 of
# the License, or (at your option) any later version.
#
# GodKode is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
# without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with GodKode.
# If not, see <https://www.gnu.org/licenses/>.
#

# https://github.com/Xed-Editor/Xed-Editor/blob/main/core/main/src/main/assets/terminal/init.sh
# + Patched by OpenCode: glibc-compat, coreutils, nodejs, python, nsswitch, locales, ld fix

set -e

export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export HOME=/home
export PROMPT_DIRTRIM=2
export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@godkode \[\033[39m\]\w \[\033[0m\]\\$ "
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

START_SHELL="/bin/bash"

# === [1] Базовый набор пакетов ===
required_packages="bash nano sudo file build-base"
missing_packages=""
for pkg in $required_packages; do
    if ! apk info -e "$pkg" >/dev/null 2>&1; then
        missing_packages="$missing_packages $pkg"
    fi
done
if [ -n "$missing_packages" ]; then
    echo -e "\e[34;1m[*] \e[37mInstalling base packages...\e[0m"
    apk update >/dev/null 2>&1
    apk add $missing_packages
    if [ $? -eq 0 ]; then
        echo -e "\e[32;1m[+] \e[37mBase packages OK\e[0m"
    fi
fi

# === [2] Glibc-совместимость ===
compat_packages="gcompat libc6-compat libstdc++"
compat_missing=""
for pkg in $compat_packages; do
    if ! apk info -e "$pkg" >/dev/null 2>&1; then
        compat_missing="$compat_missing $pkg"
    fi
done
if [ -n "$compat_missing" ]; then
    echo -e "\e[34;1m[*] \e[37mInstalling glibc-compatible layer...\e[0m"
    apk add $compat_missing
    if [ $? -eq 0 ]; then
        echo -e "\e[32;1m[+] \e[37mglibc-compat OK\e[0m"
    fi
fi

# === [3] NSSwitch.conf (фикс DNS для проприетарных бинарников) ===
if [ ! -f /etc/nsswitch.conf ]; then
    echo -e "\e[34;1m[*] \e[37mCreating /etc/nsswitch.conf...\e[0m"
    echo "hosts: files dns" > /etc/nsswitch.conf
fi

# === [4] Симлинки для stubborn бинарей (ld-linux interpreter) ===
ARCH=$(uname -m)
if [ "$ARCH" = "x86_64" ] || [ "$ARCH" = "amd64" ]; then
    if [ ! -f /lib64/ld-linux-x86-64.so.2 ]; then
        mkdir -p /lib64
        ln -sf /lib/ld-musl-x86_64.so.1 /lib64/ld-linux-x86-64.so.2 2>/dev/null || true
        ln -sf /lib/libc.so /lib64/ld-linux-x86-64.so.2 2>/dev/null || true
    fi
elif [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then
    if [ ! -f /lib/ld-linux-aarch64.so.1 ]; then
        ln -sf /lib/ld-musl-aarch64.so.1 /lib/ld-linux-aarch64.so.1 2>/dev/null || true
    fi
fi

# === [5] Node.js / npm (musl-native! не качать с nodejs.org) ===
if ! command -v node >/dev/null 2>&1; then
    echo -e "\e[34;1m[*] \e[37mInstalling Node.js (Alpine native)...\e[0m"
    apk add nodejs npm 2>/dev/null || apk add nodejs-current npm 2>/dev/null || true
    if command -v node >/dev/null 2>&1; then
        echo -e "\e[32;1m[+] \e[37mNode $(node -v) OK\e[0m"
    else
        echo -e "\e[33;1m[!] \e[37mNode.js not available in repos\e[0m"
    fi
fi

# === [5b] Node module-loading fix (proot/statx workaround) ===
# The proot jail does NOT implement the statx() syscall that Node.js v20+
# uses (via libuv) for fs.stat / Module._resolveFilename. As a result every
# statx() on a rootfs path returns ENOENT and Node dies with:
#     node:internal/modules/cjs/loader:1433  Error: Cannot find module ...
# nodewrap is a seccomp wrapper that returns ENOSYS for statx(), forcing libuv
# to fall back to stat()/newfstatat() — which proot DOES translate — so module
# loading works. We install it as /usr/local/bin/node so it shadows the real
# /usr/bin/node on PATH (/usr/local/bin precedes /usr/bin).
if [ -n "$NODEWRAP" ] && [ -x "$NODEWRAP" ] && command -v node >/dev/null 2>&1; then
    mkdir -p /usr/local/bin
    # Only shadow if not already pointing at our wrapper.
    if [ ! -e /usr/local/bin/node ] || [ "$(readlink -f /usr/local/bin/node 2>/dev/null)" != "$NODEWRAP" ]; then
        rm -f /usr/local/bin/node
        ln -sf "$NODEWRAP" /usr/local/bin/node
    fi
    if node -e "require('path')" >/dev/null 2>&1; then
        echo -e "\e[32;1m[+] \e[37mNode module-loader fix (statx->stat) active\e[0m"
    else
        echo -e "\e[33;1m[!] \e[37mNode statx workaround installed but module load failed; check nodewrap seccomp support\e[0m"
    fi
fi

# === [6] Python + pip + dev-headers ===
if ! command -v python3 >/dev/null 2>&1; then
    echo -e "\e[34;1m[*] \e[37mInstalling Python3...\e[0m"
    apk add python3 py3-pip python3-dev linux-headers 2>/dev/null || apk add python3 py3-pip 2>/dev/null || true
    if command -v python3 >/dev/null 2>&1; then
        echo -e "\e[32;1m[+] \e[37mPython $(python3 -V 2>&1) OK\e[0m"
    fi
fi

# === [7] Локали ===
if ! command -v locale >/dev/null 2>&1; then
    apk add musl-locales 2>/dev/null || true
fi

# === [8] Coreutils + сетевые утилиты ===
tools="coreutils procps curl wget git openssl ca-certificates"
tools_missing=""
for pkg in $tools; do
    if ! apk info -e "$pkg" >/dev/null 2>&1; then
        tools_missing="$tools_missing $pkg"
    fi
done
if [ -n "$tools_missing" ]; then
    echo -e "\e[34;1m[*] \e[37mInstalling core utils + net tools...\e[0m"
    apk add $tools_missing
    if [ $? -eq 0 ]; then
        echo -e "\e[32;1m[+] \e[37mUtils OK\e[0m"
    fi
fi

# === [9] Fix linker warning ===
if [ ! -f /linkerconfig/ld.config.txt ]; then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

echo -e "\e[32;1m[+] \e[37mAlpine init complete. Type 'apk' to install more packages.\e[0m"

# === [10] Запуск shell ===
# Builder mode: GodKode asks to run a command (e.g. ./gradlew assembleDebug)
# instead of an interactive bash. Passed through env (not positional args) so
# that spaces/&&/quotes in the command survive — `eval` re-parses it as shell.
if [ -n "$GODKODE_RUN_CMD" ]; then
    echo -e "\e[34;1m[▸] \e[37m\$ $GODKODE_RUN_CMD\e[0m"
    eval "$GODKODE_RUN_CMD"
    exit $?
fi

if [ "$#" -eq 0 ]; then
    $START_SHELL
else
    # shellcheck disable=SC2068
    $@
fi
