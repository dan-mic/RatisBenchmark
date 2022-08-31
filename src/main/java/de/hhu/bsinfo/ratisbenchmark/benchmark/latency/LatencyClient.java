package de.hhu.bsinfo.ratisbenchmark.benchmark.latency;

import de.hhu.bsinfo.ratisbenchmark.benchmark.BenchmarkClient;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.RaftGroup;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadPoolExecutor;

public class LatencyClient extends BenchmarkClient{

    private int duration;
    private int messagesPerSecond;
    private int threadCount;
    private long[] latencies;
    private final String resultsFileName;

    public LatencyClient(RaftGroup raftGroup, String resultsFileName)
    {
        super(raftGroup);
        this.resultsFileName = resultsFileName;
    }

    @Override
    public void fetchConfiguration() throws IOException, NumberFormatException{
        super.fetchConfiguration();
        threadCount = super.getThreadCount();
        duration = BenchmarkClient.parseIntegerReply(super.sendReadOnly(Message.valueOf("GET_DURATION")));
        messagesPerSecond = BenchmarkClient.parseIntegerReply(super.sendReadOnly(Message.valueOf("GET_MESSAGES_PER_SECOND")));
        latencies = new long[duration * messagesPerSecond];
    }

    @Override
    public void startBenchmark() throws IOException {
        super.startBenchmark(); // Dieser Aufruf blockiert, solange nicht alle Clients bereit sind.

        System.out.println("Messages per second: " + messagesPerSecond);
        System.out.println("Duration: " + duration);
        ThreadPoolExecutor executor = getExecutor(threadCount);
        final int messageCount = duration * messagesPerSecond;
        final double frequency = 1000.0 / messagesPerSecond;
        final double warmUpFrequency = frequency * 2;
        int benchmarkMessagesSent = 0;
        int warmUpMessagesSent = 0;
        int tempMessagesSent = 0;
        int secondCounter = 1;
        long warmUpDuration = 0;

        Instant warmUpBegin = Instant.now();
        System.out.println("Starting warm up.");
        while (warmUpDuration < 2000L){
            warmUpDuration = Duration.between(warmUpBegin, Instant.now()).toMillis();
            if (warmUpDuration > warmUpFrequency * warmUpMessagesSent && executor.getActiveCount() < threadCount){
                executor.submit(() -> super.send(Message.valueOf("INCREMENT")));
                warmUpMessagesSent++;
            }
        }
        System.out.println(warmUpMessagesSent + " warm up messages sent.");

        Instant benchmarkBegin = Instant.now();
        while (benchmarkMessagesSent < messageCount)
        {
            long benchmarkDuration = Duration.between(benchmarkBegin, Instant.now()).toMillis();
            if (benchmarkDuration > frequency * benchmarkMessagesSent && executor.getActiveCount() < threadCount){
                final int messageId = benchmarkMessagesSent;
                executor.submit(() -> measureLatency(messageId));
                benchmarkMessagesSent++;
            }
            // Die Ausgabe der Anzahl der gesendeten Nachrichten nach jeder Sekunde dient zur Kontrolle, ob die Nachrichten
            // rechtzeitig abgeschickt wurden.
            if (benchmarkDuration > secondCounter * 1000L){
                System.out.println("Second " + secondCounter + ": " + (benchmarkMessagesSent - tempMessagesSent) + " messages sent.");
                tempMessagesSent = benchmarkMessagesSent;
                secondCounter++;
            }
        }
        long d = Duration.between(benchmarkBegin, Instant.now()).toMillis();
        System.out.println("Sending messages finished after " + d/1000.0 + " seconds.");
        terminateExecutor(executor, 600);

        // Frag ab, wie viele Nachrichten insgesamt von allen Clients versendet wurden.
        // Der Wert kann für verschiedene Durchläufe auch für den als letztes terminierenden Client variieren,
        // da der warm up für jeden Client nach 2 Sekunden abgebrochen wird, egal ob alle
        // warm up Nachrichten versendet wurden oder nicht.
        RaftClientReply count = super.sendReadOnly(Message.valueOf("GET"));
        String response = count.getMessage().getContent().toString(Charset.defaultCharset());

        super.send(Message.valueOf("FINISHED"));
        super.close();

        //Write results to file
        FileWriter writer = new FileWriter(resultsFileName);
        for (int i = 0; i < latencies.length; i++){
            String s = latencies[i] + (i < latencies.length-1 ? "," : "\n");
            writer.write(s);
        }
        writer.close();

        System.out.println(response);
        printAverageLatencies();
    }

    private void measureLatency(int messageId){
        Instant start = Instant.now();
        try {
            super.send(Message.valueOf("INCREMENT"));
        } catch (IOException e){
            e.printStackTrace();
        }
        Instant finish = Instant.now();
        latencies[messageId] = Duration.between(start, finish).toNanos();
    }

    private void printAverageLatencies(){
        System.out.println("Average Latencies: ");
        int counter = 0;
        for (int i = 0; i < duration; i++){
            long sum = 0;
            for (int j = 0; j < messagesPerSecond; j++){
                sum += latencies[counter];
                counter++;
            }
            float average = (float) (sum) / messagesPerSecond;
            System.out.println((i+1) + ". second: " + average);
        }
    }
}
