package de.hhu.bsinfo.ratisbenchmark;

import de.hhu.bsinfo.ratisbenchmark.benchmark.BenchmarkClient;
import de.hhu.bsinfo.ratisbenchmark.benchmark.BenchmarkOptions;
import de.hhu.bsinfo.ratisbenchmark.benchmark.BenchmarkOptions.BenchmarkType;
import de.hhu.bsinfo.ratisbenchmark.benchmark.BenchmarkOptions.Role;
import de.hhu.bsinfo.ratisbenchmark.benchmark.BenchmarkStateMachine;
import de.hhu.bsinfo.ratisbenchmark.benchmark.ConfigurationClient;
import de.hhu.bsinfo.ratisbenchmark.benchmark.latency.LatencyClient;
import de.hhu.bsinfo.ratisbenchmark.benchmark.latency.LatencyStateMachine;
import de.hhu.bsinfo.ratisbenchmark.benchmark.throughput.ThroughputClient;
import de.hhu.bsinfo.ratisbenchmark.benchmark.throughput.ThroughputStateMachine;
import de.hhu.bsinfo.ratisbenchmark.server.RatisServer;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.util.SizeInBytes;

import java.io.File;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/*
 * Diese Klasse dient als Einstiegspunkt. Abhängig von den übergebenen Argumenten wird hier entweder der
 * Bootstrapper, ein Worker oder ein Client gestartet. Dafür sind einige Ratis-spezifische Objekte wie
 * z.B. eine RaftGroup notwendig, welche ebenfalls hier instanziiert werden.
 */

public class Main {

    public static void main(String[] args) {

        try {
            BenchmarkOptions options = new BenchmarkOptions(args);

            if (options.useHadroNIO()){
                System.setProperty("de.hhu.bsinfo.hadronio.Configuration.SEND_BUFFER_LENGTH", options.getHadronioSendBufferLength());
                System.setProperty("de.hhu.bsinfo.hadronio.Configuration.RECEIVE_BUFFER_LENGTH", options.getHadronioReceiveBufferLength());
                System.setProperty("de.hhu.bsinfo.hadronio.Configuration.BUFFER_SLICE_LENGTH", options.getHadronioBufferSliceLength());
                System.setProperty("de.hhu.bsinfo.hadronio.Configuration.FLUSH_INTERVAL_SIZE", options.getHadronioFlushInternalSize());
                System.setProperty("java.nio.channels.spi.SelectorProvider", "de.hhu.bsinfo.hadronio.HadronioProvider");
            }

            final List<RaftPeer> peers = createRaftPeers(options.getAddresses());
            final UUID CLUSTER_GROUP_ID = UUID.fromString("02511d47-d67c-49a3-9011-abb3109a44c1");
            final RaftGroup raftGroup = RaftGroup.valueOf(RaftGroupId.valueOf(CLUSTER_GROUP_ID), peers);

            if (options.getRole().equals(Role.BOOTSTRAPPER) || options.getRole().equals(Role.WORKER) ){
                final BenchmarkStateMachine stateMachine = createStateMachine(options);
                final SizeInBytes messageBufferSize = options.getMessageBufferSize();
                final int peerId = options.getRole().equals(Role.BOOTSTRAPPER) ? 0 : options.getWorkerId();
                final RaftPeer peer = peers.get(peerId);
                final RatisServer ratisServer = createServer(stateMachine, peer, raftGroup, messageBufferSize);
                ratisServer.start();
                // Starte als Bootstrapper parallel einen ConfigurationClient, um die Konfiguration des Benchmarks
                // in die State Machine einzupflegen, sodass sie für andere Clients abrufbar wird.
                if (options.getRole().equals(Role.BOOTSTRAPPER))
                    new ConfigurationClient(raftGroup, options).start();
                handleInput(ratisServer);
            }
             else {
                final BenchmarkClient client = createBenchmarkClient(options, raftGroup);
                client.start();
                exitOnInput();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<RaftPeer> createRaftPeers(String[] addresses){
        List<RaftPeer> peers = new ArrayList<>(addresses.length);
        for (int i = 0; i < addresses.length; i++) {
            peers.add(RaftPeer.newBuilder().setId("n" + i).setAddress(addresses[i]).build());
        }
        return Collections.unmodifiableList(peers);
    }

    private static BenchmarkStateMachine createStateMachine(BenchmarkOptions options) throws Exception {
        BenchmarkType benchmarkType = options.getBenchmarkType();
        if (benchmarkType.equals(BenchmarkType.LATENCY))
            return new LatencyStateMachine();
        else if (benchmarkType.equals(BenchmarkType.THROUGHPUT))
            return new ThroughputStateMachine();
        else throw new Exception("Unsupported benchmark type");
    }

    private static RatisServer createServer(BenchmarkStateMachine stateMachine, RaftPeer peer,
                                            RaftGroup raftGroup, SizeInBytes messageBufferSize) throws Exception {
        final File storageDir = new File("/tmp/ratis/" + peer.getId());
        deleteDirectory(storageDir);
        return new RatisServer(peer, storageDir, raftGroup, stateMachine, messageBufferSize);
    }


    private static BenchmarkClient createBenchmarkClient(BenchmarkOptions options, RaftGroup raftGroup) throws Exception {
        BenchmarkType benchmarkType = options.getBenchmarkType();
        if (benchmarkType.equals(BenchmarkType.LATENCY)){
            String resultsFileName = "/tmp/ratis/latencies.txt";
            return new LatencyClient(raftGroup, resultsFileName);
        }
        else if (benchmarkType.equals(BenchmarkType.THROUGHPUT))
            return new ThroughputClient(raftGroup);
        else throw new Exception("Unsupported benchmark type");
    }

    private static void deleteDirectory(File file){
        File[] allContents = file.listFiles();
        if (allContents != null) {
            for (File f : allContents) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }

    private static void handleInput(RatisServer server) throws Exception{
        System.out.println("Press c to close server.");
        Scanner scanner = new Scanner(System.in, UTF_8.name());
        boolean closed = false;
        while (!closed){
            String input = scanner.nextLine();
            if (input.equals("c")){
                server.close();
                closed = true;
            }
        }
        scanner.nextLine();
        deleteDirectory(server.getStorageDir());
        System.exit(0);
    }

    private static void exitOnInput(){
        Scanner scanner = new Scanner(System.in, UTF_8.name());
        scanner.nextLine();
        System.exit(0);
    }
}
