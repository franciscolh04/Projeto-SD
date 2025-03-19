package pt.ulisboa.tecnico.tuplespaces.frontend;

import java.util.ArrayList;
import java.util.List;

public class TakeResponseCollector<T> {

    public List<T> collectedResponses;

    public TakeResponseCollector() {
        collectedResponses = new ArrayList<>();
    }

    synchronized public void addResponse(T response) {
        collectedResponses.add(response);
        notifyAll();
    }

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (collectedResponses.size() < n) {
            wait();
        }
    }
}
