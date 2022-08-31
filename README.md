# Ein Benchmark für die Beschleunigung von Apache Ratis durch hadroNIO

Diese Anwendung soll einen Benchmark für die Beschleunigung von [Apache Ratis](http://ratis.incubator.apache.org/) durch [hadroNIO](https://github.com/hhu-bsinfo/hadroNIO)
ermöglichen, indem Latenz und Durchsatz in einem konfigurierbaren Testverfahren gemessen werden können.

## Verwendung

Zum Starten eines Benchmarks können die vorhandenen Shell-Skripte verwendet werden. Es wird genau ein Server benötigt, der das Bootstrapping übernimmt. Die Anzahl der Worker-Server und Clients ist beliebig. Jedem Skript müssen die Adressen der Server übergeben werden. Die Worker-Skripte erwarten zusätzlich eine ID, wobei die ID 0 dem Bootstrapper vorbehalten ist. Sowohl die Anzahl der Clients als auch die restliche Konfiguration des Benchmarks können in den Bootstrapper-Skripten geändert werden. Zur Verwendung von hadroNIO muss [UCX](https://openucx.org/) installiert sein. Ein Start eines Benchmarks mit 3 Servern und 2 Clients könnte zum Beispiel wie folgt aussehen (Möglicherweise müssen zunächst Container mit UCX gestartet verwenden):

```shell
node68: ./latencyBootstrapper.sh node68:6000 node69:6000 node70:6000
```

```shell
node69: ./latencyWorker.sh node68:6000 node69:6000 node70:6000 1
```

```shell
node70: ./latencyWorker.sh node68:6000 node69:6000 node70:6000 2
```

```shell
node71: ./latencyClient.sh node68:6000 node69:6000 node70:6000
```

```shell
node72: ./latencyClient.sh node68:6000 node69:6000 node70:6000
```

Die Server schließen sich bei Eingabe der Taste "c". Durch Eingabe einer weiteren beliebigen Taste wird die Anwendung geschlossen. Die Clients lassen sich ebenfalls durch Eingabe einer beliebigen Taste beenden.

## Bekannte Probleme

Nach Beenden eines Servers kann es eine gewisse Zeit dauern, bis die verwendeten Ports wieder freigegeben werden. Beim Start eines Servers dürfen die Ports jedoch nicht belegt sein. Es wird daher empfohlen bei jedem Neustart andere Ports zu verwenden.
