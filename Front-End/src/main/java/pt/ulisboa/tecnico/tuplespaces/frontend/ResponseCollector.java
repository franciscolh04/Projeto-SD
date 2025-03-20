package pt.ulisboa.tecnico.tuplespaces.frontend;

import java.util.ArrayList;

public class ResponseCollector<T> {
    // List that will store all the responses
    ArrayList<String> collectedResponses;

    public ResponseCollector() {
        collectedResponses = new ArrayList<>();
    }

    // Add a response to the list
    synchronized public void addString(String s) {
        collectedResponses.add(s);
        notifyAll();
    }

    // Wait until the list has n responses
    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (collectedResponses.size() < n) {
            wait();
        }
    }
}
