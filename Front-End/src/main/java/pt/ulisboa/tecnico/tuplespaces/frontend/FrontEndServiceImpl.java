package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.*;

import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;


import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public class FrontEndServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    /**
     * Set flag to true to print debug messages.
     * The flag can be set using the -debug command line option.
     */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    private int num_servers;

    // Helper method to print debug messages.
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println("[DEBUG] " + debugMessage);
    }

    private ReplicaServerGrpc.ReplicaServerStub[] backendStubs;
    private ManagedChannel[] channels;
    private ConcurrentHashMap<Integer, Integer> clientTickets = new ConcurrentHashMap<>();

    // Key to send the delay value in the metadata in the header of the request
    public Metadata.Key<String> DELAY_KEY = Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);

    public FrontEndServiceImpl(List<String> serverAddresses) {
        num_servers = serverAddresses.size();

        ManagedChannel[] channels = new ManagedChannel[num_servers];
        backendStubs = new ReplicaServerGrpc.ReplicaServerStub[num_servers];

        for (int i = 0; i < num_servers; i++) {
            // Create a gRPC channel for the TupleSpaces server received in the arguments
            channels[i] = ManagedChannelBuilder.forTarget(serverAddresses.get(i)).usePlaintext().build();
            backendStubs[i] = ReplicaServerGrpc.newStub(channels[i]);
            System.out.println("Frontend connecting to server TupleSpaces: " + serverAddresses.get(i));
        }
    }

    public ReplicaServerGrpc.ReplicaServerStub[] getBackendStubs() {
        return backendStubs;
    }


    // Forward the put request to the server
    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        try {
            // Get the ticket number for the client
            int clientId = request.getClientId();
            int ticketNumber = clientTickets.getOrDefault(clientId, 0);
            ticketNumber++;

            // Update the ticket number in the HashMap
            clientTickets.put(clientId, ticketNumber);

            // Send the response to the client
            responseObserver.onNext(TupleSpacesOuterClass.PutResponse.newBuilder().build());
            responseObserver.onCompleted();

            // Get the delay values from the context
            String delaysString = FrontEndInterceptor.DELAY_VALUE_CONTEXT.get();

            // Parse the delay values from the string into a list of integers
            List<Integer> delays;
            if (delaysString.isEmpty()) {
                delays = Arrays.asList(0, 0, 0);
            } else {
                delays = Arrays.stream(delaysString.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            }

            debug("Received put request from Client. Forwarding to Server. Tuple to add: " + request.getNewTuple());

            ResponseCollector<ReplicaServerOuterClass.PutResponseServer> c = new ResponseCollector<ReplicaServerOuterClass.PutResponseServer>();
            for(int i = 0; i < num_servers; i++) {
                Metadata metadata = new Metadata();

                // Add the respective delay value to the metadata
                if (i < delays.size()) {
                    metadata.put(DELAY_KEY, String.valueOf(delays.get(i)));
                } else {
                    metadata.put(DELAY_KEY, "0"); // Default delay if not provided
                }

                // Create a stub with the metadata and send the put request to the server
                ReplicaServerGrpc.ReplicaServerStub stub = backendStubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
                stub.putServer(ReplicaServerOuterClass.PutRequestServer.newBuilder()
                        .setNewTuple(request.getNewTuple())
                        .setClientId(request.getClientId())
                        .setTicketNumber(ticketNumber)
                        .build(), new PutObserver(c));
                debug("[PUT] Sent request to Server [" + (i + 1) + "] with delay: " + delays.get(i));
            }

            // Wait until all responses are received
            c.waitUntilAllReceived(num_servers);

            debug("[PUT] Received response from Server. Forwarding to Client. Feedback Status: Success");

        } catch (io.grpc.StatusRuntimeException e) { // Capture gRPC communication failures
            System.err.println("[gRPC] Error connecting with server during the put request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Send the error to the client so they know the operation failed
        } catch (Exception e) { // Catches any other unexpected exception
            System.err.println("Unexpected Error during the put request: " + e.getMessage());
            responseObserver.onError(e); // Sends the error to the client
        }
    }


    // Forward the read request to the server
    @Override
    public void read(TupleSpacesOuterClass.ReadRequest request, StreamObserver<TupleSpacesOuterClass.ReadResponse> responseObserver) {
        try {
            // Get the ticket number for the client
            int clientId = request.getClientId();
            int ticketNumber = clientTickets.getOrDefault(clientId, 0);
            ticketNumber++;

            // Update the ticket number in the HashMap
            clientTickets.put(clientId, ticketNumber);

            // Get the delay values from the context
            String delaysString = FrontEndInterceptor.DELAY_VALUE_CONTEXT.get();

            // Parse the delay values from the string into a list of integers
            List<Integer> delays;
            if (delaysString.isEmpty()) {
                delays = Arrays.asList(0, 0, 0);
            } else {
                delays = Arrays.stream(delaysString.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            }

            debug("[READ] Received request from Client. Forwarding to Server. Tuple to read: " + request.getSearchPattern());

            ResponseCollector<ReplicaServerOuterClass.ReadResponseServer> c = new ResponseCollector();
            for(int i = 0; i < num_servers; i++) {
                Metadata metadata = new Metadata();

                // Add the respective delay value to the metadata
                if (i < delays.size()) {
                    metadata.put(DELAY_KEY, String.valueOf(delays.get(i)));
                } else {
                    metadata.put(DELAY_KEY, "0"); // Default delay if not provided
                }

                // Create a stub with the metadata and send the read request to the server
                ReplicaServerGrpc.ReplicaServerStub stub = backendStubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
                stub.readServer(ReplicaServerOuterClass.ReadRequestServer.newBuilder()
                        .setSearchPattern(request.getSearchPattern())
                        .setClientId(request.getClientId())
                        .setTicketNumber(ticketNumber)
                        .build(), new ReadObserver(c));
                debug("[READ] Sent request to Server [" + (i + 1) + "] with delay: " + delays.get(i));
            }

            // Wait until the first response is received
            c.waitUntilAllReceived(1);

            // Send the response to the client
            responseObserver.onNext(TupleSpacesOuterClass.ReadResponse.newBuilder().setResult(c.collectedResponses.get(0)).build());
            responseObserver.onCompleted();

            debug("[READ] Received response from Server. Forwarding to Client. Response: " + c.collectedResponses.get(0));

        } catch (io.grpc.StatusRuntimeException e) { // Catches gRPC communication failures
            System.err.println("[gRPC] Error connecting with server during the reading request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Send the error to the client so they know the operation failed
        } catch (Exception e) { // Catches any other unexpected exception
            System.err.println("Unexpected Error during the reading request: " + e.getMessage());
            responseObserver.onError(e); // Sends the error to the client
        }
    }


    // Forward the take request to the server
    @Override
    public void take(TupleSpacesOuterClass.TakeRequest request, StreamObserver<TupleSpacesOuterClass.TakeResponse> responseObserver) {
        try {
            // Get the ticket number for the client
            int clientId = request.getClientId();
            int ticketNumber = clientTickets.getOrDefault(clientId, 0);
            ticketNumber++;

            // Update the ticket number in the HashMap
            clientTickets.put(clientId, ticketNumber);

            // Parse delays
            String delaysString = FrontEndInterceptor.DELAY_VALUE_CONTEXT.get();
            List<Integer> delays = delaysString.isEmpty() ? Arrays.asList(0, 0, 0)
                    : Arrays.stream(delaysString.split(",")).map(Integer::parseInt).toList();
            debug("[TAKE] Delay values: " + delays);

            // Get client id and search pattern
            int client_id = request.getClientId();
            String pattern = request.getSearchPattern();
            debug("[TAKE] Received request. Pattern: " + pattern);

            // 1. DEFINE VOTER SET
            int firstReplica = (client_id - 1) % 3;
            int secondReplica = (client_id) % 3;
            List<Integer> voterSet = Arrays.asList(firstReplica, secondReplica);

            // 2. REQUEST GRANTS
            GrantResponseCollector grantCollector = new GrantResponseCollector(voterSet.size());
            for (int i : voterSet) {
                var grantReq = ReplicaServerOuterClass.GrantRequest.newBuilder()
                        .setClientId(client_id)
                        .setTicketNumber(ticketNumber)
                        .setSearchPattern(pattern).build();
                backendStubs[i].requestAccess(grantReq, new GrantObserver(grantCollector, i));
                debug("[TAKE] Sent grant to Server " + (i + 1));
            }
            grantCollector.waitUntilAllReceived();

            // Now we have to check if all grants were successful
            Map<Integer, ReplicaServerOuterClass.GrantResponse> grants = grantCollector.getResponses();
            Map<Integer, List<String>> grantedTuplesMap = new HashMap<>();
            boolean allGranted = true;
            for (int i : voterSet) {
                var grant = grants.get(i);
                if (grant == null || !grant.getGranted()) {
                    allGranted = false;
                    break;
                }
                grantedTuplesMap.put(i, grant.getTuplesList());
            }

            // If not all grants were successful, release all locks and return error
            if (!allGranted || grantedTuplesMap.size() < 2) {
                responseObserver.onError(io.grpc.Status.UNAVAILABLE.withDescription("Could not acquire lock.").asRuntimeException());
                return;
            }

            debug("[TAKE] Grants: " + grantedTuplesMap);

            // Choose tuple to take (intersection of granted tuples)
            List<String> list1 = grantedTuplesMap.get(voterSet.get(0));
            List<String> list2 = grantedTuplesMap.get(voterSet.get(1));
            String tuple = null;
            // If both lists have only one element and they are the same, choose that element
            if (list1.size() == 1 && list2.size() == 1 && list1.get(0).equals(list2.get(0))) {
                tuple = list1.get(0);
            } else { // Otherwise, choose the first common element
                Set<String> intersection = new HashSet<>(list1);
                intersection.retainAll(list2);
                if (!intersection.isEmpty()) {
                    tuple = intersection.iterator().next();
                }
            }

            //3. SEND RESPONSE TO CLIENT
            TupleSpacesOuterClass.TakeResponse response = TupleSpacesOuterClass.TakeResponse.newBuilder()
                    .setResult(tuple).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            // We have to release all locks except the one we are taking (even if it is "null", no interception)
            // so we do a release list with all tuples except the one we are taking
            Map<Integer, List<String>> toReleaseMap = new HashMap<>();
            for (int i : voterSet) {
                List<String> releaseList = new ArrayList<>();
                for (String t : grantedTuplesMap.get(i)) {
                    if (!Objects.equals(t, tuple)) {
                        releaseList.add(t);
                    }
                }
                toReleaseMap.put(i, releaseList);
            }

            // 4. TAKE TUPLE (and release the remaining ones inside this step)
            TakeResponseCollector<ReplicaServerOuterClass.TakeResponseServer> takeCollector = new TakeResponseCollector<>();
            for (int i = 0; i < backendStubs.length; i++) {
                List<String> toRelease = toReleaseMap.getOrDefault(i, new ArrayList<>());
                String takeTuple = (tuple == null) ? "" : tuple;
                var req = ReplicaServerOuterClass.TakeRequestServer.newBuilder()
                        .setClientId(client_id)
                        .setSearchPattern(pattern)
                        .setTuple(takeTuple)
                        .setServerIndex(i)
                        .setTicketNumber(ticketNumber)
                        .addAllReleaseTuples(toRelease)
                        .build();

                Metadata metadata = new Metadata();
                metadata.put(DELAY_KEY, String.valueOf(i < delays.size() ? delays.get(i) : 0));
                backendStubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                        .takeServer(req, new TakeObserver(takeCollector));
            }
            takeCollector.waitUntilAllReceived(backendStubs.length);

            // Only now we will tell that if the tuple/intersection is null, we could not take any tuple
            // We have already released all locks, so we can return an error
            if (tuple == null) {
                responseObserver.onError(io.grpc.Status.ABORTED.withDescription("No common tuple to take. Locks released.").asRuntimeException());
                return;
            }

            // For debug reasons and to ensure that the right tuple was taken, we will check the final tuple
            // that was given to the server and its response and send it to the client
            for (var resp : takeCollector.collectedResponses) {
                if (resp != null && !resp.getResult().isEmpty()) {
                    tuple = resp.getResult();
                    break;
                }
            }


            debug("[TAKE] Final tuple: " + tuple);

        } catch (io.grpc.StatusRuntimeException e) { // Catches gRPC communication failures
            System.err.println("[gRPC] Error connecting with server during the Take request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Send the error to the client so they know the operation failed
        } catch (Exception e) { // Catches any other unexpected exception
            System.err.println("Unexpected Error during the Take request: " + e.getMessage());
            responseObserver.onError(e); // Sends the error to the client
        }
    }


    // Forward the getTupleSpacesState request to the server
    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> responseObserver) {
        try {
            // Get the ticket number for the client
            int clientId = request.getClientId();
            int ticketNumber = clientTickets.getOrDefault(clientId, 0);
            ticketNumber++;

            // Update the ticket number in the HashMap
            clientTickets.put(clientId, ticketNumber);

            debug("Received getTupleSpacesState request from Client. Forwarding to Server.");

            ResponseCollector< ReplicaServerOuterClass.getTupleSpacesStateResponseServer> c = new ResponseCollector<ReplicaServerOuterClass.getTupleSpacesStateResponseServer>();

            // Send the getTupleSpacesState request to all servers
            for(int i = 0; i < num_servers; i++) {
                backendStubs[i].getTupleSpacesStateServer(ReplicaServerOuterClass.getTupleSpacesStateRequestServer.newBuilder().setClientId(request.getClientId()).setTicketNumber(ticketNumber).build(), new GetTupleSpacesStateObserver(c));
            }

            // Wait until all responses are received
            c.waitUntilAllReceived(num_servers);

            // Format and send the response to the client
            c.collectedResponses.removeIf(str -> str.trim().isEmpty());
            responseObserver.onNext(TupleSpacesOuterClass.getTupleSpacesStateResponse.newBuilder().addAllTuple(c.collectedResponses).build());
            responseObserver.onCompleted();

            debug("Received getTupleSpacesState response from Server. Forwarding to Client. Response: " + c.collectedResponses);

        } catch (io.grpc.StatusRuntimeException e) { // Catches gRPC communication failures
            System.err.println("[gRPC] Error connecting with server during the getTupleSpacesState request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Send the error to the client so they know the operation failed
        } catch (Exception e) { // Catches any other unexpected exception
            System.err.println("Unexpected Error during the getTupleSpacesState request: " + e.getMessage());
            responseObserver.onError(e); // Sends the error to the client
        }
    }

}
