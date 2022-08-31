package de.hhu.bsinfo.ratisbenchmark.benchmark.throughput;

import de.hhu.bsinfo.ratisbenchmark.benchmark.BenchmarkClient;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ThreadPoolExecutor;

public class ThroughputClient extends BenchmarkClient {

    private int threadCount;
    private int rounds;
    private int messageSize;
    private long[] durations;

    public ThroughputClient(RaftGroup raftGroup) {
        super(raftGroup);
    }

    @Override
    public void fetchConfiguration() throws IOException, NumberFormatException{
        super.fetchConfiguration();
        threadCount = super.getThreadCount();
        rounds = BenchmarkClient.parseIntegerReply(super.sendReadOnly(Message.valueOf("GET_ROUNDS")));
        messageSize = BenchmarkClient.parseIntegerReply(super.sendReadOnly(Message.valueOf("GET_MESSAGE_SIZE")));
        durations = new long[rounds];
    }

    @Override
    public void startBenchmark() throws IOException{
        System.out.println("message size: " + messageSize);
        ByteString messageContent = generateMessageContent();

        super.startBenchmark(); // Dieser Aufruf blockiert, solange nicht alle Clients bereit sind.

        System.out.println("Starting warm up.");
        super.send(Message.valueOf(messageContent));
        super.send(Message.valueOf("ROUND_FINISHED"));
        System.out.println("Warm up finished.");
        boolean otherClientsFinished = false;
        while (!otherClientsFinished) {
            otherClientsFinished = Boolean.parseBoolean(super.sendReadOnly(Message.valueOf("GET_ROUND_FINISHED")).
                                                    getMessage().getContent().toString(Charset.defaultCharset()));
        }

        for (int i = 0; i < rounds; i++){
            System.out.println((i+1) + ". round:");
            Instant start = Instant.now();
            ThreadPoolExecutor executor = getExecutor(threadCount);
            for (int j = 0; j < threadCount; j++){
                final int index = j+1;
                executor.submit(() -> send(messageContent, index));
            }
            terminateExecutor(executor, 600);
            Instant finish = Instant.now();
            durations[i] = Duration.between(start, finish).toMillis();
            super.send(Message.valueOf("ROUND_FINISHED"));
            System.out.println("Round finished.");

            otherClientsFinished = false;
            while (!otherClientsFinished) {
                otherClientsFinished = Boolean.parseBoolean(super.sendReadOnly(Message.valueOf("GET_ROUND_FINISHED")).
                        getMessage().getContent().toString(Charset.defaultCharset()));
            }
        }

        // Frage zur Kontrolle ab, wie viele Nachrichten insgesamt von allen Clients gesendet wurden.
        RaftClientReply reply = super.sendReadOnly(Message.valueOf("GET"));
        String response = reply.getMessage().getContent().toString(Charset.defaultCharset());
        System.out.println(response);
        for (int i = 0; i < durations.length; i++)
            System.out.print(durations[i] + (i < durations.length - 1 ? "," : "\n"));

        super.send(Message.valueOf("FINISHED"));
        super.close();
    }

    private void send(ByteString messageContent, int index){
        try{
            super.send(Message.valueOf(messageContent));
            System.out.println("Message " + index +" sent.");
        }catch (IOException e){
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }

    private ByteString generateMessageContent(){
        byte[] content = new byte[messageSize];
        new Random().nextBytes(content);
        content[messageSize - 1] = Byte.MAX_VALUE;
        return ByteString.copyFrom(content);
    }
}
