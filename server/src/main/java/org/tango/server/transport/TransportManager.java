package org.tango.server.transport;

import org.tango.server.network.NetworkInterfacesExtractor;
import org.tango.transport.TransportMeta;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.List;

/**
 * This class maintains ZMQ REQ/REP required data
 *
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.02.2020
 */
public class TransportManager {
    private final ZContext context = new ZContext();

    public ZMQ.Socket clientSocket;
    public ZMQ.Socket workerSocket;
    private String port;

    public ZContext bindZmqTransport() {
        this.clientSocket = createZMQSocket(ZMQ.ROUTER);
        port = String.valueOf(clientSocket.bindToRandomPort("tcp://*"));

        workerSocket = createZMQSocket(ZMQ.DEALER);
        workerSocket.bind("inproc://workers");


        return context;
    }

    public TransportMeta getTransportMeta() {
        List<String> connectionPoints = new NetworkInterfacesExtractor().getIp4Addresses();

        TransportMeta result = new TransportMeta();

        connectionPoints.stream()
                .map(s -> "tcp://" + s + ":" + port)
                .forEach(result::addEndpoint);

        return result;
    }

    public ZMQ.Socket createZMQSocket(int type) {
        final ZMQ.Socket socket = context.createSocket(type);
        return socket;
    }

}
