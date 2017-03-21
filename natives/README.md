This native method exposes the *man:setuid(2)* and *man:setgid(2)* system calls to Java.
Java code can call `club.wpia.gigi.natives.SetUID.setUid(uid, gid)` to set the user and group ID to the specified values if they’re currently different.

Gigi can use this to bind to Internet domain privileged ports (port numbers below 1024)
when started as root and then drop privileges by changing to a non-root user.

It should be noted that this is rarely necessary;
it is much safer to start Gigi as a regular user with `CAP_NET_BIND_SERVICE` (see *man:capabilities(7)*).
Gigi can also inherit its socket from the environment (file descriptor 0),
e. g. from systemd (see *man:systemd.socket(5)*) or (x)inetd.
