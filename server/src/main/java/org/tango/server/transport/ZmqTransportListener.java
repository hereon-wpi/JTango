package org.tango.server.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.server.admin.AdminDevice;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 19.02.2020
 */
public class ZmqTransportListener implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(ZmqTransportListener.class);

    private final ZContext ctx;
    private final AdminDevice admin;

    public ZmqTransportListener(ZContext ctx, AdminDevice admin) {
        this.ctx = ctx;
        this.admin = admin;
    }

    @Override
    public void run() {
        logger.debug("Starting ZmqTransportListener");
        ZMQ.Socket socket = ctx.createSocket(ZMQ.REP);
        socket.connect("inproc://workers");

        logger.debug("Listening for incoming messages ZmqTransportListener");
        while (!Thread.currentThread().isInterrupted()) {
            byte[] data = socket.recv();
            String msg = new String(data, StandardCharsets.UTF_8);

            //TODO non blocking
            //TODO thread pool
            socket.send(
                    new ZmqMessageProcessorImpl(msg, admin)
                            .process()
                            .toString()
                            .getBytes(StandardCharsets.UTF_8));

        }
    }
}
