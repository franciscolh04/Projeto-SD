package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.PutObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.ReadObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.GetTupleSpacesStateObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.TakeObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.GrantObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.ReleaseObserver;

import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;


import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

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

    private TupleSpacesGrpc.TupleSpacesStub[] backendStubs;
    private ManagedChannel[] channels;

    // Key to send the delay value in the metadata in the header of the request
    public Metadata.Key<String> DELAY_KEY = Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);

    public FrontEndServiceImpl(List<String> serverAddresses) {
        num_servers = serverAddresses.size();

        ManagedChannel[] channels = new ManagedChannel[num_servers];
        backendStubs = new TupleSpacesGrpc.TupleSpacesStub[num_servers];

        for (int i = 0; i < num_servers; i++) {
            // Create a gRPC channel for the TupleSpaces server received in the arguments
            channels[i] = ManagedChannelBuilder.forTarget(serverAddresses.get(i)).usePlaintext().build();
            backendStubs[i] = TupleSpacesGrpc.newStub(channels[i]);
            System.out.println("Frontend connecting to server TupleSpaces: " + serverAddresses.get(i));
        }
    }


    // Forward the put request to the server
    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        try {
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
            System.out.println("[PUT] Delay values: " + delays);

            debug("Received put request from Client. Forwarding to Server. Tuple to add: " + request.getNewTuple());

            ResponseCollector<TupleSpacesOuterClass.PutResponse> c = new ResponseCollector<TupleSpacesOuterClass.PutResponse>();
            for(int i = 0; i < num_servers; i++) {
                Metadata metadata = new Metadata();

                // Add the respective delay value to the metadata
                if (i < delays.size()) {
                    metadata.put(DELAY_KEY, String.valueOf(delays.get(i)));
                    System.out.println("[PUT] Delay value: " + delays.get(i));
                } else {
                    metadata.put(DELAY_KEY, "0"); // Default delay if not provided
                }

                // Create a stub with the metadata and send the put request to the server
                TupleSpacesGrpc.TupleSpacesStub stub = backendStubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
                stub.put(request, new PutObserver(c));
            }

            // Wait until all responses are received
            c.waitUntilAllReceived(num_servers);

            // Send the response to the client
            responseObserver.onNext(TupleSpacesOuterClass.PutResponse.newBuilder().build());
            responseObserver.onCompleted();

            debug("Received put response from Server. Forwarding to Client. Feedback Status: Success");

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
            System.out.println("[READ] Delay values: " + delaysString);

            debug("Received read request from Client. Forwarding to Server. Tuple to read: " + request.getSearchPattern());

            ResponseCollector<TupleSpacesOuterClass.ReadResponse> c = new ResponseCollector();
            for(int i = 0; i < num_servers; i++) {
                Metadata metadata = new Metadata();

                // Add the respective delay value to the metadata
                if (i < delays.size()) {
                    metadata.put(DELAY_KEY, String.valueOf(delays.get(i)));
                    System.out.println("[READ] Delay value: " + delays.get(i));
                } else {
                    metadata.put(DELAY_KEY, "0"); // Default delay if not provided
                }

                // Create a stub with the metadata and send the read request to the server
                TupleSpacesGrpc.TupleSpacesStub stub = backendStubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
                stub.read(request, new ReadObserver(c));
            }

            // Wait until the first response is received
            c.waitUntilAllReceived(1);

            // Send the response to the client
            responseObserver.onNext(TupleSpacesOuterClass.ReadResponse.newBuilder().setResult(c.collectedResponses.get(0)).build());
            responseObserver.onCompleted();

            debug("Received read response from Server. Forwarding to Client. Response: " + c.collectedResponses.get(0));

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
            System.out.println("[TAKE] Delay values: " + delaysString);

            final int client_id = request.getClientId();
            final String pattern = request.getSearchPattern();
            debug("Received take request from Client. Pattern: " + pattern);

            // 1. Calculate the voter set based on the client id
            int firstReplica = (client_id - 1) % 3;
            int secondReplica = (client_id ) % 3;
            List<Integer> voterSet = Arrays.asList(firstReplica, secondReplica);
            debug("Voter set: " + voterSet);

            // 2. Request access to the tuple to all servers in the voter set
            TakeResponseCollector<TupleSpacesOuterClass.GrantResponse> grantCollector = new TakeResponseCollector<>();
            for (int i : voterSet) {
                Metadata metadata = new Metadata();

                // Add the respective delay value to the metadata
                if (i < delays.size()) {
                    metadata.put(DELAY_KEY, String.valueOf(delays.get(i)));
                    System.out.println("[READ] Delay value: " + delays.get(i));
                } else {
                    metadata.put(DELAY_KEY, "0"); // Default delay if not provided
                }

                // Create the grant request to send to the server
                TupleSpacesOuterClass.GrantRequest grantReq = TupleSpacesOuterClass.GrantRequest.newBuilder()
                        .setClientId(client_id)
                        .setSearchPattern(pattern)
                        .build();

                // Create a stub with the metadata and send the requestAccess to the server
                TupleSpacesGrpc.TupleSpacesStub stub = backendStubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
                stub.requestAccess(grantReq, new GrantObserver(grantCollector));
            }

            // Wait until all responses from the voter set are received
            grantCollector.waitUntilAllReceived(2);

            // 3. Verify if all grants were acquired
            List<TupleSpacesOuterClass.GrantResponse> grants = grantCollector.collectedResponses;
            int[] tupleIndexes = new int[2];
            boolean allGranted = true;
            for (int j = 0; j < 2; j++) {
                if (grants.get(j) == null || !grants.get(j).getGranted()) {
                    allGranted = false;
                    break;
                }
                tupleIndexes[j] = grants.get(j).getTupleIndex();
            }
            // If not all grants were acquired, fail the operation and return an error to the client
            if (!allGranted) {
                System.err.println("Failed to acquire grants from all replicas.");
                responseObserver.onError(io.grpc.Status.UNAVAILABLE.withDescription("Could not acquire lock.").asRuntimeException());
                return;
            }
            // If all grants were acquired, proceed with the take operation
            debug("Grants acquired: Indexes " + Arrays.toString(tupleIndexes));

            // 4. Take the tuple from all servers in the voter set
            int tupleIndex = tupleIndexes[0];
            TakeResponseCollector<TupleSpacesOuterClass.TakeResponse> takeCollector = new TakeResponseCollector<TupleSpacesOuterClass.TakeResponse>();
            for (int i = 0; i < num_servers; i++) {
                TupleSpacesOuterClass.TakeRequest takeReq = TupleSpacesOuterClass.TakeRequest.newBuilder()
                        .setSearchPattern(pattern)
                        .setClientId(client_id)
                        .setTupleIndex(tupleIndex)
                        .build();
                // Send the take request to the server
                backendStubs[i].take(takeReq, new TakeObserver(takeCollector));
            }

            // Wait until all responses are received
            takeCollector.waitUntilAllReceived(num_servers);

            // 5. Release access to the tuple from all servers in the voter set
            ResponseCollector<TupleSpacesOuterClass.ReleaseResponse> releaseCollector = new ResponseCollector<>();
            for (int i : voterSet) {
                TupleSpacesOuterClass.ReleaseRequest releaseReq = TupleSpacesOuterClass.ReleaseRequest.newBuilder()
                        .setClientId(client_id)
                        .setTupleIndex(tupleIndex)
                        .build();
                // Send the release request to the server
                backendStubs[i].releaseAccess(releaseReq, new ReleaseObserver(releaseCollector));
            }

            // Wait until all responses from the voter set are received
            releaseCollector.waitUntilAllReceived(2);
            debug("Released access from Voter Set servers.");

            // 6. Return the taken tuple to the client
            String finalTuple = null;
            for (TupleSpacesOuterClass.TakeResponse resp : takeCollector.collectedResponses) {
                if (resp != null && !resp.getResult().isEmpty()) {
                    finalTuple = resp.getResult();
                    break;
                }
            }
            if (finalTuple == null) {
                System.err.println("Failed to take tuple from replicas.");
                responseObserver.onError(io.grpc.Status.UNAVAILABLE.withDescription("Take operation failed.").asRuntimeException());
                return;
            }

            debug("Tuple taken successfully: " + finalTuple);
            TupleSpacesOuterClass.TakeResponse response = TupleSpacesOuterClass.TakeResponse.newBuilder()
                    .setResult(finalTuple)
                    .build();

            // Send the response to the client
            responseObserver.onNext(response);
            responseObserver.onCompleted();

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
            debug("Received getTupleSpacesState request from Client. Forwarding to Server.");

            ResponseCollector<TupleSpacesOuterClass.getTupleSpacesStateResponse> c = new ResponseCollector<TupleSpacesOuterClass.getTupleSpacesStateResponse>();

            // Send the getTupleSpacesState request to all servers
            for(int i = 0; i < num_servers; i++) {
                backendStubs[i].getTupleSpacesState(request, new GetTupleSpacesStateObserver(c));
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
