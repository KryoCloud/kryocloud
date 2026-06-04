# KryoSphere

KryoSphere is the KryoCloud service isolation layer.

It prepares fast, stateless launch plans for Minecraft services and keeps isolation logic outside the wrapper runtime.

Modes:

- `NONE` keeps the legacy process launch style.
- `BASIC` uses a locked-down workspace launch with private logs, PID tracking and shell limits.
- `STRICT` uses Bubblewrap on Linux when available and falls back to `BASIC` when it is missing.

Wrapper config:

```yaml
kryoSphereMode: "BASIC"
kryoSphereBubblewrap: true
kryoSpherePrivateTmp: true
kryoSphereProtectHome: true
kryoSphereRestrictProc: true
kryoSphereNoNewPrivileges: true
kryoSphereMemoryLimitMb: 0
kryoSphereCpuLimitPercent: 0
kryoSphereOpenFileLimit: 65536
kryoSphereProcessLimit: 0
kryoSphereServiceUser: ""
```
