package de.hhu.bsinfo.ratisbenchmark.benchmark.latency;

import de.hhu.bsinfo.ratisbenchmark.benchmark.BenchmarkStateMachine;
import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.statemachine.TransactionContext;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class LatencyStateMachine extends BenchmarkStateMachine {

    private final AtomicInteger counter = new AtomicInteger(0);
    private static final String GET_COMMAND = "GET";
    private static final String INCREMENT_COMMAND = "INCREMENT";

    @Override
    public CompletableFuture<Message> query(Message request) {
        String msg = request.getContent().toString(Charset.defaultCharset());
        // Alle Anfragen, die nicht den Wert von counter abfragen, werden von der Oberklasse bearbeitet.
        if (!msg.equals(GET_COMMAND)) {
            return super.query(request);
        }
        String reply = String.valueOf(counter.intValue());
        return CompletableFuture.completedFuture(
                Message.valueOf(reply));
    }

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        final RaftProtos.LogEntryProto entry = trx.getLogEntry();
        String logData = entry.getStateMachineLogEntry().getLogData()
                .toString(Charset.defaultCharset());

         if (!logData.equals(INCREMENT_COMMAND)) {
            final CompletableFuture<Message> f = super.applyTransaction(trx);
            if (super.resetNecessary())
                reset();
            return f;
        }

        final long index = entry.getIndex();
        updateLastAppliedTermIndex(entry.getTerm(), index);

        counter.incrementAndGet();

        return CompletableFuture.completedFuture(Message.valueOf(String.valueOf(counter.intValue())));
    }

    @Override
    public void reset() {
        super.reset();
        counter.set(0);
    }
}
