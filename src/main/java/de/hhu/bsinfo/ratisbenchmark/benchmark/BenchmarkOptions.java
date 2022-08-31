package de.hhu.bsinfo.ratisbenchmark.benchmark;

import de.hhu.bsinfo.ratisbenchmark.client.RatisClient;
import org.apache.commons.cli.*;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.util.SizeInBytes;

import java.io.IOException;

public class BenchmarkOptions {

    public enum BenchmarkType{LATENCY, THROUGHPUT};
    public enum Role{BOOTSTRAPPER, WORKER, CLIENT};

    private final CommandLine line;

    private static final SizeInBytes DEFAULT_LATENCY_BUFFER_SIZE = SizeInBytes.valueOf(256);
    private static final SizeInBytes DEFAULT_THROUGHPUT_BUFFER_SIZE = SizeInBytes.valueOf("4MB");
    private static final String DEFAULT_HADRONIO_SEND_BUFFER_LENGTH = "8388608";
    private static final String DEFAULT_HADRONIO_RECEIVE_BUFFER_LENGTH = "8388608";
    private static final String DEFAULT_HADRONIO_BUFFER_SLICE_LENGTH = "65536";
    private static final String DEFAULT_HADRONIO_FLUSH_INTERVAL_SIZE = "1024";


    public BenchmarkOptions(String[] args) throws ParseException{
        Options options = buildOptions();
        CommandLineParser parser = new DefaultParser();
        line = parser.parse(options, args);
    }

    public BenchmarkType getBenchmarkType() throws ParseException{
        String benchmarkType = line.getOptionValue("benchmark");
        if (benchmarkType.equals("latency"))
            return BenchmarkType.LATENCY;
        else if (benchmarkType.equals("throughput"))
            return BenchmarkType.THROUGHPUT;
        else
            throw new ParseException("Illegal argument for benchmark");
    }

    public Role getRole() throws ParseException{
        String benchmarkType = line.getOptionValue("role");
        if (benchmarkType.equals("bootstrapper"))
            return Role.BOOTSTRAPPER;
        else if (benchmarkType.equals("worker"))
            return Role.WORKER;
        else if (benchmarkType.equals("client"))
            return Role.CLIENT;
        else
            throw new ParseException("Illegal argument for role");
    }

    public boolean useHadroNIO() {
        return line.hasOption("-h");
    }

    public String[] getAddresses(){
        return line.getOptionValues("addresses");
    }

    public int getWorkerId() throws ParseException{
        return parseIntegerOptionArgument("wid");
    }

    public int getClientCount() throws ParseException{
        return parseIntegerOptionArgument("clientCount");
    }

    public int getDuration() throws ParseException{
        return parseIntegerOptionArgument("duration");
    }

    public int getRounds() throws ParseException{
        return parseIntegerOptionArgument("rounds");
    }

    public int getMessagesPerSecond() throws ParseException{
        return parseIntegerOptionArgument("messagesPerSecond");
    }

    public int getThreadCount() throws ParseException{
        return parseIntegerOptionArgument("threadCount");
    }

    public SizeInBytes getMessageSize() throws MissingOptionException{
        checkOptionIsMissing("messageSize");
        return SizeInBytes.valueOf(line.getOptionValue("messageSize"));
    }

    public SizeInBytes getMessageBufferSize() throws ParseException{
        if (line.hasOption("messageBufferSize"))
            return SizeInBytes.valueOf(line.getOptionValue("messageBufferSize"));
        if (getBenchmarkType().equals(BenchmarkType.LATENCY))
            return DEFAULT_LATENCY_BUFFER_SIZE;
        else if (getBenchmarkType().equals(BenchmarkType.THROUGHPUT))
            return DEFAULT_THROUGHPUT_BUFFER_SIZE;
        else
            throw new ParseException("Unknown benchmark type");
    }

    public String getHadronioSendBufferLength(){
        if (line.hasOption("hadronioSendBufferLength"))
            return line.getOptionValue("hadronioSendBufferLength");
        return DEFAULT_HADRONIO_SEND_BUFFER_LENGTH;
    }

    public String getHadronioReceiveBufferLength(){
        if (line.hasOption("hadronioReceiveBufferLength"))
            return line.getOptionValue("hadronioReceiveBufferLength");
        return DEFAULT_HADRONIO_RECEIVE_BUFFER_LENGTH;
    }

    public String getHadronioBufferSliceLength(){
        if (line.hasOption("hadronioBufferSliceLength"))
            return line.getOptionValue("hadronioBufferSliceLength");
        return DEFAULT_HADRONIO_BUFFER_SLICE_LENGTH;
    }

    public String getHadronioFlushInternalSize(){
        if (line.hasOption("hadronioFlushInternalSize"))
            return line.getOptionValue("hadronioFlushInternalSize");
        return DEFAULT_HADRONIO_FLUSH_INTERVAL_SIZE;
    }

    public String getOptionValue(String optionName) throws MissingOptionException{
        if (line.getOptionValue(optionName) != null)
            return line.getOptionValue(optionName);
        else
            throw new MissingOptionException("Missing option: " + optionName);
    }

    private void checkOptionIsMissing(String optionName) throws MissingOptionException{
        if (!line.hasOption(optionName))
            throw new MissingOptionException("Missing option: " + optionName);
    }

    private int parseIntegerOptionArgument(String optionName) throws ParseException{
        checkOptionIsMissing(optionName);
        try{
            return Integer.parseInt(line.getOptionValue(optionName));
        }
        catch (NumberFormatException e){
            throw new ParseException("Illegal argument for " + optionName);
        }
    }

    private static Options buildOptions(){
        Options options = new Options();

        options.addOption(Option.builder("r").longOpt("role").required().hasArg()
                .desc("Supported values: bootstrapper, worker, client").build());

        options.addOption(Option.builder("wid").longOpt("workerId").hasArg().type(Number.class).
                desc("Id of a worker (greater than 0)").build());

        options.addOption(Option.builder("a").longOpt("addresses").required().hasArgs().valueSeparator(';').
                desc("Server addresses divided by ;").build());

        options.addOption(Option.builder("cc").longOpt("clientCount").hasArg().build());

        options.addOption(Option.builder("tc").longOpt("threadCount").hasArg().build());

        options.addOption(Option.builder("b").longOpt("benchmark").required().hasArg()
                .desc("Supported values: latency, throughput").build());

        options.addOption(Option.builder("h").longOpt("hadroNIO").desc("use hadroNIO").build());

        options.addOption(Option.builder("d").longOpt("duration").hasArg().
                desc("Duration of latency benchmark in seconds").build());

        options.addOption(Option.builder("r").longOpt("rounds").hasArg().
                desc("Number of rounds for throughput benchmark").build());

        options.addOption(Option.builder("mps").longOpt("messagesPerSecond").hasArg().build());

        options.addOption(Option.builder("ms").longOpt("messageSize").hasArg().
                desc("Size of messages for throughput benchmark, e.g.: 4MB").build());

        options.addOption(Option.builder("mbs").longOpt("messageBufferSize").hasArg().
                desc("Buffer size of servers for messages, e.g.: 4MB").build());

        options.addOption(Option.builder("hsbl").longOpt("hadronioSendBufferLength").hasArg().build());

        options.addOption(Option.builder("hrbl").longOpt("hadronioReceiveBufferLength").hasArg().build());

        options.addOption(Option.builder("hbsl").longOpt("hadronioBufferSliceLength").hasArg().build());

        options.addOption(Option.builder("hfis").longOpt("hadronioFlushInternalSize").hasArg().build());

        return options;
    }

}
