package de.hhu.bsinfo.ratisbenchmark.benchmark;

import de.hhu.bsinfo.ratisbenchmark.client.RatisClient;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.RaftGroup;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*
 * In dieser Klasse fragt ein Client die Konfiguration des Benchmarks und die Zustände der anderen Clients ab.
 * Erst wenn alle anderen Clients bereit sind, wird der Start des Benchmarks ermöglicht.
 */

public abstract class BenchmarkClient extends RatisClient {

    private int threadCount;

    public BenchmarkClient(RaftGroup raftGroup)
    {
        super(raftGroup);
    }

    public void start() throws IOException, NumberFormatException{
        System.out.println("Fetch configuration...");
        fetchConfiguration();
        System.out.println("Configuration fetched.");
        startBenchmark();
    }

    public void fetchConfiguration() throws IOException, NumberFormatException{
        threadCount = parseIntegerReply(super.sendReadOnly(Message.valueOf("GET_THREAD_COUNT")));
    }

    public void startBenchmark() throws IOException, NumberFormatException{
        super.send(Message.valueOf("READY"));
        System.out.println("Checking for other clients...");
        int missingClients;
        int oldMissingClients = 0;
        do{
            RaftClientReply reply = super.sendReadOnly(Message.valueOf("GET_MISSING_CLIENTS"));
            missingClients = parseIntegerReply(reply);
            if (missingClients != oldMissingClients)
                System.out.println("Waiting for " + missingClients + " client" + (missingClients != 1 ? "s." : "."));
            oldMissingClients = missingClients;
        }
        while(missingClients > 0);
        System.out.println("Starting benchmark...");
    }

    public ThreadPoolExecutor getExecutor(int threadCount){
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
    }

    public void terminateExecutor(ThreadPoolExecutor executor, int timeout){
        executor.shutdown();
        try {
            executor.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }

    public int getThreadCount(){
        return threadCount;
    }

    public static int parseIntegerReply(RaftClientReply reply) throws NumberFormatException{
        return Integer.parseInt(reply.getMessage().getContent().toString(Charset.defaultCharset()));
    }
}
