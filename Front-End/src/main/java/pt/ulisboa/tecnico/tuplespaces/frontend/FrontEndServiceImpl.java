package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.PutObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.ReadObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.observers.GetTupleSpacesStateObserver;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;


import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class FrontEndServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    /** Set flag to true to print debug messages.
     * The flag can be set using the -debug command line option. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    private int num_servers;

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println("[DEBUG] " + debugMessage);
    }

    private TupleSpacesGrpc.TupleSpacesStub[] backendStubs;
    private ManagedChannel[] channels;
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


    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        try {
            String delaysString = FrontEndInterceptor.DELAY_VALUE_CONTEXT.get();

            List<Integer> delays;
            if (delaysString.isEmpty()) {
                delays = Arrays.asList(0, 0, 0);
            } else {
                delays = Arrays.stream(delaysString.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            }

            System.out.println("[PUT] Delay values: " + delays);

            // Print the details of the received request
            debug("Received put request from Client. Forwarding to Server. Tuple to add: " + request.getNewTuple());
            ResponseCollector<TupleSpacesOuterClass.PutResponse> c = new ResponseCollector<TupleSpacesOuterClass.PutResponse>();
            for(int i = 0; i < num_servers; i++) {
                Metadata metadata = new Metadata();

                if (i < delays.size()) {
                    metadata.put(DELAY_KEY, String.valueOf(delays.get(i)));
                    System.out.println("[PUT] Delay value: " + delays.get(i));
                } else {
                    metadata.put(DELAY_KEY, "0"); // Default delay if not provided
                }

                TupleSpacesGrpc.TupleSpacesStub stub = backendStubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
                stub.put(request, new PutObserver(c));
            }
            c.waitUntilAllReceived(num_servers);
            // Perform the read operation in the backend
            //TupleSpacesOuterClass.ReadResponse response = backendStub.read(request);
            responseObserver.onNext(TupleSpacesOuterClass.PutResponse.newBuilder().build());

            // Send the response to the client
            responseObserver.onCompleted();
            // Perform the put operation in the backend

            //Context.current().withValue(FrontEndInterceptor.DELAY_VALUE_CONTEXT, null).attach();

            // Print the details of the response
            debug("Received put response from Server. Forwarding to Client. Feedback Status: Success");

        } catch (io.grpc.StatusRuntimeException e) { // Capture gRPC communication failures
            System.err.println("[gRPC] Error connecting with server during the put request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Send the error to the client so they know the operation failed
        } catch (Exception e) { // Catches any other unexpected exception
            System.err.println("Unexpected Error during the put request: " + e.getMessage());
            responseObserver.onError(e); // Sends the error to the client
        }
    }


    @Override
    public void read(TupleSpacesOuterClass.ReadRequest request, StreamObserver<TupleSpacesOuterClass.ReadResponse> responseObserver) {
        try {
            String delaysString = FrontEndInterceptor.DELAY_VALUE_CONTEXT.get();

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

                if (i < delays.size()) {
                    metadata.put(DELAY_KEY, String.valueOf(delays.get(i)));
                    System.out.println("[READ] Delay value: " + delays.get(i));
                } else {
                    metadata.put(DELAY_KEY, "0"); // Default delay if not provided
                }

                TupleSpacesGrpc.TupleSpacesStub stub = backendStubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
                stub.read(request, new ReadObserver(c));
            }
            c.waitUntilAllReceived(1);

            // Perform the read operation in the backend
            //TupleSpacesOuterClass.ReadResponse response = backendStub.read(request);

            responseObserver.onNext(TupleSpacesOuterClass.ReadResponse.newBuilder().setResult(c.collectedResponses.get(0)).build());

            // Send the response to the client
            responseObserver.onCompleted();
            // Perform the put operation in the backend

            //debug("Received read response from Server. Forwarding to Client. Response: " + response.getResult());

        } catch (io.grpc.StatusRuntimeException e) { // Catches gRPC communication failures
            System.err.println("[gRPC] Error connecting with server during the reading request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Send the error to the client so they know the operation failed
        } catch (Exception e) { // Catches any other unexpected exception
            System.err.println("Unexpected Error during the reading request: " + e.getMessage());
            responseObserver.onError(e); // Sends the error to the client
        }
    }


    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> responseObserver) {
        try {
            // Print the details of the received request
            debug("Received getTupleSpacesState request from Client. Forwarding to Server.");

            ResponseCollector<TupleSpacesOuterClass.getTupleSpacesStateResponse> c = new ResponseCollector<TupleSpacesOuterClass.getTupleSpacesStateResponse>();

            for(int i = 0; i < num_servers; i++) {
                backendStubs[i].getTupleSpacesState(request, new GetTupleSpacesStateObserver(c));
                debug("processou a informação de um servidor");
            }

            c.waitUntilAllReceived(num_servers);
            debug("Waited until all finished.");
            c.collectedResponses.removeIf(str -> str.trim().isEmpty());
            responseObserver.onNext(TupleSpacesOuterClass.getTupleSpacesStateResponse.newBuilder().addAllTuple(c.collectedResponses).build());

            // Send the response to the client
            responseObserver.onCompleted();
            // Perform the put operation in the backend

            // Print the details of the response

            debug("Received getTupleSpacesState response from Server. Forwarding to Client. Response: " + c.collectedResponses);

        }
        catch (io.grpc.StatusRuntimeException e) { // Catches gRPC communication failures
            System.err.println("[gRPC] Error connecting with server during the getTupleSpacesState request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Send the error to the client so they know the operation failed
        } catch (Exception e) { // Catches any other unexpected exception
            System.err.println("Unexpected Error during the getTupleSpacesState request: " + e.getMessage());
            responseObserver.onError(e); // Sends the error to the client
        }
    }

    /*
    @Override
    public void take(TupleSpacesOuterClass.TakeRequest request, StreamObserver<TupleSpacesOuterClass.TakeResponse> responseObserver) {
        try {
            debug("Received take request from Client. Forwarding to Server. Tuple to take: " + request.getSearchPattern());

            // Perform the take operation in the backend
            TupleSpacesOuterClass.TakeResponse response = backendStub.take(request);

            // Send the response to the client
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            debug("Received take response from Server. Forwarding to Client. Response: " + response.getResult());

        } catch (io.grpc.StatusRuntimeException e) { // Catches gRPC communication failures
            System.err.println("[gRPC] Error connecting with server during the take request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Send the error to the client so they know the operation failed
        } catch (Exception e) { // Catches any other unexpected exception
            System.err.println("Unexpected Error during the take request: " + e.getMessage());
            responseObserver.onError(e); // Sends the error to the client
        }
    }

    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> responseObserver) {
        try {
            debug("Received getTupleSpacesState request from Client. Forwarding to Server.");

            // Perform the getTupleSpacesState operation in the backend
            TupleSpacesOuterClass.getTupleSpacesStateResponse response = backendStub.getTupleSpacesState(request);

            // Send the response to the client
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            debug("Received getTupleSpacesState response from Server. Forwarding to Client. Response: " + response.getTupleList());

        } catch (io.grpc.StatusRuntimeException e) { // Catches gRPC communication failures
            System.err.println("[gRPC] Error connecting with server during the getTupleSpacesState request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Send the error to the client so they know the operation failed
        } catch (Exception e) { // Catches any other unexpected exception
            System.err.println("Unexpected Error during the getTupleSpacesState request: " + e.getMessage());
            responseObserver.onError(e); // Sends the error to the client
        }
    }
     */
}
