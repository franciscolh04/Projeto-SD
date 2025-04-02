package pt.ulisboa.tecnico.tuplespaces.frontend;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/*
 Why would we create a new class instead of reusing the existing one?
 The existing class is not enough to be used in the context of the grant operation.
 We use a map to store the responses in correspondence with the server index.
 And by using a CountDownLatch we can wait until all the responses are received.
 We have just ensured that the responses are in correspondence with the server index
 avoiding any possible confusion (sending the wrong release list to the wrong server).
/*

 */
public class GrantResponseCollector {
    // Map that will store all the responses in correspondence with the server index
    private final Map<Integer, ReplicaServerOuterClass.GrantResponse> responses = new ConcurrentHashMap<>();
    private final CountDownLatch latch; // CountDownLatch to wait until all responses are received

    // Constructor
    public GrantResponseCollector(int expectedResponses) {
        this.latch = new CountDownLatch(expectedResponses);
    }

    // Add a response to the map
    public void addResponse(int serverIndex, ReplicaServerOuterClass.GrantResponse response) {
        responses.put(serverIndex, response);
        latch.countDown();
    }

    // Wait until all responses are received
    public void waitUntilAllReceived() throws InterruptedException {
        latch.await();
    }

    // Get the responses map
    public Map<Integer, ReplicaServerOuterClass.GrantResponse> getResponses() {
        return responses;
    }
}
