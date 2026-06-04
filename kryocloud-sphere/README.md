# KryoSphere

KryoSphere is the KryoCloud service isolation layer.

It prepares fast, stateless launch plans for Minecraft services and keeps isolation logic outside the wrapper runtime.

Modes:

- `NONE` keeps the legacy process launch style.
- `BASIC` uses a locked-down Linux workspace launch with private logs, PID tracking and shell limits.
- `STRICT` uses Bubblewrap on Linux and requires `bwrap` to be installed.

On non-Linux platforms KryoSphere is disabled automatically at runtime. The effective mode becomes `NONE`, KryoCloud keeps starting, and the configured mode is not persisted as changed.

Wrapper config:

```yaml
kryoSphereMode: "BASIC"
kryoSphereBubblewrap: true
kryoSpherePrivateTmp: true
kryoSphereProtectHome: true
kryoSphereRestrictProc: true
kryoSphereNoNewPrivileges: true
kryoSphereClearEnvironment: true
kryoSphereAllowNetwork: true
kryoSphereMemoryLimitMb: 0
kryoSphereCpuLimitPercent: 0
kryoSphereOpenFileLimit: 65536
kryoSphereProcessLimit: 0
kryoSphereTmpSizeMb: 256
kryoSphereServiceUser: ""
kryoSphereReadOnlyPaths: ""
kryoSphereWritablePaths: ""
```
