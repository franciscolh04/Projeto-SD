package pt.ulisboa.tecnico.tuplespaces.frontend;

import java.util.ArrayList;
import java.util.List;

public class TakeResponseCollector<T> {
    // List that will store all the responses
    public List<T> collectedResponses;

    public TakeResponseCollector() {
        collectedResponses = new ArrayList<>();
    }

    // Add a response to the list
    synchronized public void addResponse(T response) {
        collectedResponses.add(response);
        notifyAll();
    }

    // Wait until the list has n responses
    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (collectedResponses.size() < n) {
            wait();
        }
    }
}
