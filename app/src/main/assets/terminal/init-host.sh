# Patched: added /tmp, /linkerconfig binds; seccomp hint
prootArgs="-r $ALPINE -0 -b /dev/ -b /sys/ -b /proc/ -b /sdcard -b /storage -b $PREFIX -b /tmp -b /linkerconfig -w /home --kill-on-exit --link2symlink"

exec $PROOT $prootArgs /bin/sh $PREFIX/bin/init "$@"
