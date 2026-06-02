# KryoCloud Build

## Launcher bauen

```bash
mvn -pl kryocloud-launcher -am clean package
```

Output:

```text
kryocloud-launcher/target/kryocloud-launcher.jar
```

## Starten

```bash
java -jar kryocloud-launcher/target/kryocloud-launcher.jar
java -jar kryocloud-launcher/target/kryocloud-launcher.jar node
java -jar kryocloud-launcher/target/kryocloud-launcher.jar wrapper
java -jar kryocloud-launcher/target/kryocloud-launcher.jar all
```

Default:

```text
node
```

## Fix für `Invalid signature file digest`

Wenn beim Starten der shaded Launcher-JAR dieser Fehler kommt:

```text
java.lang.SecurityException: Invalid signature file digest for Manifest main attributes
```

dann lagen signierte Dependency-Metadaten in der Fat-JAR. Der Shade-Build filtert jetzt automatisch folgende Dateien raus:

```text
META-INF/*.SF
META-INF/*.DSA
META-INF/*.RSA
META-INF/*.EC
META-INF/SIG-*
```

Danach immer sauber neu bauen:

```bash
mvn -pl kryocloud-launcher -am clean package
java -jar kryocloud-launcher/target/kryocloud-launcher.jar
```

