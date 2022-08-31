package de.hhu.bsinfo.ratisbenchmark.benchmark;

import de.hhu.bsinfo.ratisbenchmark.client.RatisClient;
import de.hhu.bsinfo.ratisbenchmark.benchmark.BenchmarkOptions.BenchmarkType;
import org.apache.commons.cli.ParseException;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroup;

import java.io.IOException;

// Der Bootstrapper startet parallel einen ConfigurationClient, um die Konfiguration des Benchmarks
// in die State Machine einzupflegen, sodass sie für andere Clients abrufbar wird.

public class ConfigurationClient extends RatisClient {

    private final BenchmarkOptions options;

    public ConfigurationClient(RaftGroup raftGroup, BenchmarkOptions options) {
        super(raftGroup);
        this.options = options;
    }

    public void start() throws IOException, ParseException {
        System.out.println("Transmit configuration...");
        super.send(Message.valueOf("CLIENT_COUNT:" + options.getClientCount()));
        super.send(Message.valueOf("THREAD_COUNT:" + options.getThreadCount()));
        if (options.getBenchmarkType().equals(BenchmarkType.LATENCY)){
            super.send(Message.valueOf("DURATION:" + options.getDuration()));
            super.send(Message.valueOf("MESSAGES_PER_SECOND:" + options.getMessagesPerSecond()));
        }
        else if (options.getBenchmarkType().equals(BenchmarkType.THROUGHPUT)){
            super.send(Message.valueOf("MESSAGE_SIZE:" + options.getMessageSize().getSize()));
            super.send(Message.valueOf("ROUNDS:" + options.getRounds()));
        }
        System.out.println("Configuration transmitted.");
        super.close();
    }


}
