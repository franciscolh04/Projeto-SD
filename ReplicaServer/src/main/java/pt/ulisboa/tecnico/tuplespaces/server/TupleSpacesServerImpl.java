package pt.ulisboa.tecnico.tuplespaces.server;


import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;



public class TupleSpacesServerImpl extends ReplicaServerGrpc.ReplicaServerImplBase {

    private ServerState state;
    private ConcurrentHashMap<Integer, Integer> clientTickets = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Object> clientLocks = new ConcurrentHashMap<>();

    public TupleSpacesServerImpl() {
        state = new ServerState();
    }

    // Put a tuple in the tuple space
    @Override
    public void putServer(ReplicaServerOuterClass.PutRequestServer request, StreamObserver<ReplicaServerOuterClass.PutResponseServer> responseObserver) {
        String tuple = request.getNewTuple();
        String tuple2 = request.getNewTuple2();
        final int client_id = request.getClientId();
        final int requestTicketNumber = request.getTicketNumber();

        // Get the client lock for this client
        clientLocks.putIfAbsent(client_id, new Object());
        Object clientLock = clientLocks.get(client_id);

        synchronized (clientLock) {
            while (clientTickets.getOrDefault(client_id, 1) != requestTicketNumber) {
                try {
                    clientLock.wait(); // Wait for the ticket to be available
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Keep the interrupt status
                    return;
                }
            }

            // Put the tuple in the tuple space and increment the ticket number
            state.put(tuple, client_id);
            state.put(tuple2, client_id);
            // Notify all waiting threads for this client
            // Increment the ticket number and notify all waiting threads for this client
            clientTickets.put(client_id, requestTicketNumber + 1);
            clientLock.notifyAll();
        }

        // Send an empty response
        ReplicaServerOuterClass.PutResponseServer response = ReplicaServerOuterClass.PutResponseServer.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Read a tuple from the tuple space
    @Override
    public void readServer(ReplicaServerOuterClass.ReadRequestServer request, StreamObserver<ReplicaServerOuterClass.ReadResponseServer> responseObserver) {
        String pattern = request.getSearchPattern();
        final int client_id = request.getClientId();
        final int requestTicketNumber = request.getTicketNumber();

        // Get the client lock for this client
        clientLocks.putIfAbsent(client_id, new Object());
        Object clientLock = clientLocks.get(client_id);

        String tuple = "";

        synchronized (clientLock) {
            while (clientTickets.getOrDefault(client_id, 1) != requestTicketNumber) {
                try {
                    clientLock.wait(); // Wait for the ticket to be available
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Keep the interrupt status
                    return;
                }
            }

            // Put the tuple in the tuple space and increment the ticket number
            tuple = state.read(pattern, client_id);
            clientTickets.put(client_id, requestTicketNumber + 1);

            // Notify all waiting threads for this client
            clientLock.notifyAll();
        }

        // Send the response with the read tuple
        ReplicaServerOuterClass.ReadResponseServer response = ReplicaServerOuterClass.ReadResponseServer.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Take a tuple from the tuple space
    @Override
    public void takeServer(ReplicaServerOuterClass.TakeRequestServer request, StreamObserver<ReplicaServerOuterClass.TakeResponseServer> responseObserver) {
        String tupleToTake = request.getTuple();
        final int client_id = request.getClientId();
        final int requestTicketNumber = request.getTicketNumber();
        String tuple = "";
        List<String> releaseList = null;

        // Get the client lock for this client
        clientLocks.putIfAbsent(client_id, new Object());
        Object clientLock = clientLocks.get(client_id);

        synchronized (clientLock) {
            while (clientTickets.getOrDefault(client_id, 1) != requestTicketNumber) {
                try {
                    clientLock.wait(); // Wait for the ticket to be available
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Keep the interrupt status
                    return;
                }
            }

            // Put the tuple in the tuple space and increment the ticket number
            // It only takes the tuple if it is not empty (no tuple to take / no intersection)
            if (!tupleToTake.isEmpty()) {
                tuple = state.takeServer(tupleToTake, client_id);
            }

            // Now we release the remaining tuples that were locked
            // We have avoided extra messages by sending all the tuples to release in a single message
            // And in the same request in which we take the tuple
            releaseList = request.getReleaseTuplesList();
            if (!releaseList.isEmpty()) {
                state.release(releaseList, client_id);
            }

            // Increment the ticket number and notify all waiting threads for this client
            if(tuple != null && tuple != "") {
                clientTickets.put(client_id, requestTicketNumber + 1);
                clientLock.notifyAll();
            }


        }

        // Send the response with the taken tuple
        ReplicaServerOuterClass.TakeResponseServer response = ReplicaServerOuterClass.TakeResponseServer.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Get the state of the tuple spaces
    @Override
    public void getTupleSpacesStateServer(ReplicaServerOuterClass.getTupleSpacesStateRequestServer request, StreamObserver<ReplicaServerOuterClass.getTupleSpacesStateResponseServer> responseObserver) {
        final int client_id = request.getClientId();
        final int requestTicketNumber = request.getTicketNumber();

        // Get the client lock for this client
        clientLocks.putIfAbsent(client_id, new Object());
        Object clientLock = clientLocks.get(client_id);

        List<String> allTuples = null;

        synchronized (clientLock) {
            while (clientTickets.getOrDefault(client_id, 1) != requestTicketNumber) {
                try {
                    clientLock.wait(); // Wait for the ticket to be available
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Keep the interrupt status
                    return;
                }
            }

            // Put the tuple in the tuple space and increment the ticket number
            allTuples = state.getTupleSpacesState(client_id);
            clientTickets.put(client_id, requestTicketNumber + 1);

            // Notify all waiting threads for this client
            clientLock.notifyAll();
        }

        // Send the response with the state of the tuple spaces
        ReplicaServerOuterClass.getTupleSpacesStateResponseServer response = ReplicaServerOuterClass.getTupleSpacesStateResponseServer.newBuilder().addAllTuple(allTuples).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Request access to take a tuple
    @Override
    public void requestAccess(ReplicaServerOuterClass.GrantRequest request, StreamObserver<ReplicaServerOuterClass.GrantResponse> responseObserver) {
        final int client_id = request.getClientId();
        final String pattern = request.getSearchPattern();
        final int requestTicketNumber = request.getTicketNumber();

        // Get the client lock for this client
        clientLocks.putIfAbsent(client_id, new Object());
        Object clientLock = clientLocks.get(client_id);

        List<String> tuples = null;
        boolean granted = false;

        synchronized (clientLock) {
            while (clientTickets.getOrDefault(client_id, 1) != requestTicketNumber) {
                try {
                    clientLock.wait(); // Wait for the ticket to be available
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Keep the interrupt status
                    return;
                }
            }

            // Put the tuple in the tuple space and increment the ticket number
            tuples = state.requestAccess(pattern, client_id);

            // If the index is -1, the access was not granted
            granted = (!tuples.isEmpty());
        }

        // Send the response with the result of the request
        ReplicaServerOuterClass.GrantResponse response = ReplicaServerOuterClass.GrantResponse.newBuilder()
                .setGranted(granted)
                .addAllTuples(tuples)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
