package de.hhu.bsinfo.ratisbenchmark.benchmark;

import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Diese Klasse ist für die Verwaltung der Clients zuständig. Clients rufen hier die Konfiguration des Benchmarks ab
 * und die Zustände anderer Clients. Damit wird ermöglicht, dass alle Clients denselben Benchmark zum selben Zeitpunkt
 * beginnen können.
 */

public abstract class BenchmarkStateMachine extends BaseStateMachine {

    private final AtomicInteger clientCount = new AtomicInteger(0);
    private final AtomicInteger threadCount = new AtomicInteger(0);
    private final AtomicInteger duration = new AtomicInteger(0);
    private final AtomicInteger rounds = new AtomicInteger(0);
    private final AtomicInteger messagesPerSecond = new AtomicInteger(0);
    private final AtomicInteger messageSize = new AtomicInteger(0);

    private final AtomicInteger clientCounter = new AtomicInteger(0);
    private final AtomicInteger finishedCounter = new AtomicInteger(0);
    private final AtomicInteger roundFinishedCounter = new AtomicInteger(0);
    private final AtomicInteger roundFinishedSentCounter = new AtomicInteger(0);

    private static final String GET_MISSING_CLIENTS_COMMAND = "GET_MISSING_CLIENTS";
    private static final String GET_THREAD_COUNT_COMMAND = "GET_THREAD_COUNT";
    private static final String GET_DURATION_COMMAND = "GET_DURATION";
    private static final String GET_ROUNDS_COMMAND = "GET_ROUNDS";
    private static final String GET_MESSAGES_PER_SECOND_COMMAND = "GET_MESSAGES_PER_SECOND";
    private static final String GET_MESSAGE_SIZE_COMMAND = "GET_MESSAGE_SIZE";
    private static final String GET_ROUND_FINISHED_COMMAND = "GET_ROUND_FINISHED";

    private static final String READY_COMMAND = "READY";
    private static final String FINISHED_COMMAND = "FINISHED";
    private static final String ROUND_FINISHED_COMMAND = "ROUND_FINISHED";

    private static final String CLIENT_COUNT_PREFIX = "CLIENT_COUNT";
    private static final String THREAD_COUNT_PREFIX = "THREAD_COUNT";
    private static final String DURATION_PREFIX = "DURATION";
    private static final String ROUNDS_PREFIX = "ROUNDS";
    private static final String MESSAGES_PER_SECOND_PREFIX = "MESSAGES_PER_SECOND";
    private static final String MESSAGE_SIZE_PREFIX = "MESSAGE_SIZE";

    private static final List<String> TRANSACTION_COMMANDS = Arrays.asList(READY_COMMAND, FINISHED_COMMAND, ROUND_FINISHED_COMMAND);
    private static final List<String> TRANSACTION_PREFIX = Arrays.asList(CLIENT_COUNT_PREFIX, THREAD_COUNT_PREFIX, DURATION_PREFIX,
                                                                    ROUNDS_PREFIX, MESSAGES_PER_SECOND_PREFIX, MESSAGE_SIZE_PREFIX);

    @Override
    public CompletableFuture<Message> query(Message request) {
        String msg = request.getContent().toString(Charset.defaultCharset());
        switch (msg){
            case GET_MISSING_CLIENTS_COMMAND:
                return CompletableFuture.completedFuture(
                        Message.valueOf(String.valueOf(clientCount.intValue() - clientCounter.intValue())));
            case GET_ROUND_FINISHED_COMMAND:
                final boolean response = roundFinishedCounter.intValue() >= clientCounter.intValue();
                if (response){
                    if (roundFinishedSentCounter.incrementAndGet() >= clientCounter.intValue()){
                        roundFinishedCounter.set(0);
                        roundFinishedSentCounter.set(0);
                    }
                }
                return CompletableFuture.completedFuture(
                        Message.valueOf(String.valueOf(response)));
            case GET_THREAD_COUNT_COMMAND:
                return CompletableFuture.completedFuture(Message.valueOf(String.valueOf(threadCount.intValue())));
            case GET_DURATION_COMMAND:
                return CompletableFuture.completedFuture(Message.valueOf(String.valueOf(duration.intValue())));
            case GET_ROUNDS_COMMAND:
                return CompletableFuture.completedFuture(Message.valueOf(String.valueOf(rounds.intValue())));
            case GET_MESSAGES_PER_SECOND_COMMAND:
                return CompletableFuture.completedFuture(Message.valueOf(String.valueOf(messagesPerSecond.intValue())));
            case GET_MESSAGE_SIZE_COMMAND:
                return CompletableFuture.completedFuture(Message.valueOf(String.valueOf(messageSize.intValue())));
            default:
                return CompletableFuture.completedFuture(Message.valueOf("Invalid Command"));
        }
    }

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        final RaftProtos.LogEntryProto entry = trx.getLogEntry();

        String logData = entry.getStateMachineLogEntry().getLogData()
                .toString(Charset.defaultCharset());

        //check if the command is valid
        if (!TRANSACTION_COMMANDS.contains(logData)  && TRANSACTION_PREFIX.stream().noneMatch(logData::startsWith)) {
            return CompletableFuture.completedFuture(
                    Message.valueOf("Invalid Command"));
        }
        //update the last applied term and index
        final long index = entry.getIndex();
        updateLastAppliedTermIndex(entry.getTerm(), index);

        if (logData.equals(READY_COMMAND))
            clientCounter.incrementAndGet();
        else if (logData.equals(FINISHED_COMMAND))
            finishedCounter.incrementAndGet();
        else if (logData.equals(ROUND_FINISHED_COMMAND))
            roundFinishedCounter.incrementAndGet();
        else if (logData.startsWith(CLIENT_COUNT_PREFIX))
            clientCount.set(parseConfigurationMessageContent(logData));
        else if (logData.startsWith(THREAD_COUNT_PREFIX))
            threadCount.set(parseConfigurationMessageContent(logData));
        else if (logData.startsWith(DURATION_PREFIX))
            duration.set(parseConfigurationMessageContent(logData));
        else if (logData.startsWith(ROUNDS_PREFIX))
            rounds.set(parseConfigurationMessageContent(logData));
        else if (logData.startsWith(MESSAGES_PER_SECOND_PREFIX))
            messagesPerSecond.set(parseConfigurationMessageContent(logData));
        else if (logData.startsWith(MESSAGE_SIZE_PREFIX))
            messageSize.set(parseConfigurationMessageContent(logData));

        return CompletableFuture.completedFuture(Message.valueOf("Accepted."));
    }

    public boolean resetNecessary(){
        return finishedCounter.intValue() >= clientCounter.intValue() && clientCounter.intValue() > 0;
    }

    public void reset(){
        clientCounter.set(0);
        finishedCounter.set(0);
        roundFinishedCounter.set(0);
        setLastAppliedTermIndex(null);
    }

    private static int parseConfigurationMessageContent(String content){
        return Integer.parseInt(content.split(":")[1]);
    }
}