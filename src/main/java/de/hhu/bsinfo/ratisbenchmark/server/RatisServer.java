package de.hhu.bsinfo.ratisbenchmark.server;

import de.hhu.bsinfo.ratisbenchmark.client.RatisClient;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.RaftConfigKeys;
import org.apache.ratis.datastream.SupportedDataStreamType;
import org.apache.ratis.netty.NettyConfigKeys;
import org.apache.ratis.netty.NettyFactory;
import org.apache.ratis.netty.server.NettyRpcService;
import org.apache.ratis.proto.netty.NettyProtos;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.rpc.SupportedRpcType;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.server.protocol.RaftServerProtocol;
import org.apache.ratis.statemachine.StateMachine;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.apache.ratis.thirdparty.io.netty.util.NetUtil;
import org.apache.ratis.thirdparty.io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.apache.ratis.util.LogUtils;
import org.apache.ratis.util.NetUtils;
import org.apache.ratis.util.SizeInBytes;
import org.apache.ratis.util.TimeDuration;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

public final class RatisServer implements Closeable {
    private final RaftServer server;
    private final File storageDir;

    public RatisServer(RaftPeer peer, File storageDir, RaftGroup raftGroup, BaseStateMachine stateMachine, SizeInBytes bufferSize) throws IOException, InterruptedException {

        RaftProperties properties = new RaftProperties();

        RaftConfigKeys.Rpc.setType(properties, SupportedRpcType.NETTY);

        this.storageDir = storageDir;
        RaftServerConfigKeys.setStorageDir(properties, Collections.singletonList(this.storageDir));

        final int port = NetUtils.createSocketAddr(peer.getAddress()).getPort();
        NettyConfigKeys.Server.setPort(properties, port);

        RaftServerConfigKeys.Log.Appender.setBufferByteLimit(properties, bufferSize);
        RaftServerConfigKeys.Log.setUseMemory(properties,true);

        this.server = RaftServer.newBuilder()
                .setGroup(raftGroup)
                .setProperties(properties)
                .setServerId(peer.getId())
                .setStateMachine(stateMachine)
                .build();
    }

    public void start() throws IOException, InterruptedException {
        System.out.println("Start server...");
        server.start();
        System.out.println("Server started.");
    }

    public File getStorageDir() throws IOException{
        return storageDir;
    }

    @Override
    public void close() throws IOException {
        System.out.println("Close server...");
        server.close();
        System.out.println("Server closed.");
    }

}
