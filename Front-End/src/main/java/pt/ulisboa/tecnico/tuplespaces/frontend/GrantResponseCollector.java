package pt.ulisboa.tecnico.tuplespaces.frontend;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class GrantResponseCollector {
    private final Map<Integer, ReplicaServerOuterClass.GrantResponse> responses = new ConcurrentHashMap<>();
    private final CountDownLatch latch;

    public GrantResponseCollector(int expectedResponses) {
        this.latch = new CountDownLatch(expectedResponses);
    }

    public void addResponse(int serverIndex, ReplicaServerOuterClass.GrantResponse response) {
        responses.put(serverIndex, response);
        latch.countDown();
    }

    public void waitUntilAllReceived() throws InterruptedException {
        latch.await();
    }

    public Map<Integer, ReplicaServerOuterClass.GrantResponse> getResponses() {
        return responses;
    }
}
