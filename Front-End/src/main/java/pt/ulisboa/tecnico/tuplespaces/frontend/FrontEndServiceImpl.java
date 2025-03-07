package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;

public class FrontEndServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    /** Set flag to true to print debug messages.
     * The flag can be set using the -debug command line option. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println("[DEBUG] " + debugMessage);
    }

    private final TupleSpacesGrpc.TupleSpacesBlockingStub backendStub;

    public FrontEndServiceImpl(String serverAddress) {
        // Create a gRPC channel for the TupleSpaces server received in the arguments
        ManagedChannel channel = ManagedChannelBuilder.forTarget(serverAddress).usePlaintext().build();

        backendStub = TupleSpacesGrpc.newBlockingStub(channel);
        System.out.println("Frontend connecting to server TupleSpaces: " + serverAddress);
    }

    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        try {
            // Print the details of the received request
            debug("Received put request from Client. Forwarding to Server. Tuple to add: " + request.getNewTuple());

            // Perform the put operation in the backend
            TupleSpacesOuterClass.PutResponse response = backendStub.put(request);

            // Send the response to the client
            responseObserver.onNext(response); // Nota: Aqui a resposta é vazia
            responseObserver.onCompleted();

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
            debug("Received read request from Client. Forwarding to Server. Tuple to read: " + request.getSearchPattern());

            // Perform the read operation in the backend
            TupleSpacesOuterClass.ReadResponse response = backendStub.read(request);

            // Send the response to the client
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            debug("Received read response from Server. Forwarding to Client. Response: " + response.getResult());

        } catch (io.grpc.StatusRuntimeException e) { // Catches gRPC communication failures
            System.err.println("[gRPC] Error connecting with server during the reading request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Send the error to the client so they know the operation failed
        } catch (Exception e) { // Catches any other unexpected exception
            System.err.println("Unexpected Error during the reading request: " + e.getMessage());
            responseObserver.onError(e); // Sends the error to the client
        }
    }

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
}
