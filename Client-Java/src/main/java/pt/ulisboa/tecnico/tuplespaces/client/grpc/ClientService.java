package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.List;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*;

import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

public class ClientService {

    /** Set flag to true to print debug messages.
     * The flag can be set using the -debug command line option. */
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println("[DEBUG] " + debugMessage);
    }
    
    private final ManagedChannel channel;
    private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;
    private final int client_id;
    static final Metadata.Key<String> DELAY_KEY = Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);
    Metadata metadata = new Metadata();


    public ClientService(String host_port, int client_id) {
        // Initialize metadata
        metadata.put(DELAY_KEY, "5");

        // Create gRPC channel to communicate with the server
        this.channel = ManagedChannelBuilder.forTarget(host_port).usePlaintext().build();

        // Create the stub for synchronous calls
        this.stub = TupleSpacesGrpc.newBlockingStub(channel).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

        // Store the client ID
        this.client_id = client_id;

        debug("Client created with ID: " + client_id);
    }

    // Method to add a tuple to the shared space
    public PutResponse put(String tuple) {
        PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).setClientId(client_id).build();

        try {
            PutResponse response = stub.put(request);
            debug("Added tuple: " + tuple);
            return response;
        } catch (StatusRuntimeException e) {
            System.err.println("Error during the put request: " + e.getStatus().getDescription() + " - " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected Error during the put request: " + e.getMessage());
            return null;
        }
    }

    // Method to read a tuple (blocks until a match is found)
    public ReadResponse read(String pattern) {
        ReadRequest request = ReadRequest.newBuilder().setSearchPattern(pattern).setClientId(client_id).build();

        try {
            ReadResponse response = stub.read(request);
            if (!response.getResult().isEmpty()) {
                debug("Read tuple: " + response.getResult());
            }
            return response;
        } catch (StatusRuntimeException e) {
            System.err.println("Error during the read request: " + e.getStatus().getDescription() + " - " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected Error during the read request: " + e.getMessage());
            return null;
        }
    }

    // Method to read and remove a tuple from the tuple space (blocks until a match is found)
    public TakeResponse take(String pattern) {
        TakeRequest request = TakeRequest.newBuilder().setSearchPattern(pattern).setClientId(client_id).build();

        try {
            TakeResponse response = stub.take(request);
            if (!response.getResult().isEmpty()) {
                debug("Removed tuple: " + response.getResult());
            }
            return response;
        } catch (StatusRuntimeException e) {
            System.err.println("Error during the take request: " + e.getStatus().getDescription() + " - " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error during the take request: " + e.getMessage());
            return null;
        }
    }


    // Method to get the current state of the tuple space
    public getTupleSpacesStateResponse getTupleSpacesState() {
        getTupleSpacesStateRequest request = getTupleSpacesStateRequest.newBuilder().setClientId(client_id).build();

        try {
            getTupleSpacesStateResponse response = stub.getTupleSpacesState(request);
            if (response != null) {
                debug("TupleSpaces Current State: " + response.getTupleList());
            }
            return response;
        } catch (StatusRuntimeException e) {
            System.err.println("Error during the getTupleSpacesState request: " + e.getStatus().getDescription() + " - " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected Error during the getTupleSpacesState request: " + e.getMessage());
            return null;
        }
    }


    // Properly close the gRPC channel
    public void shutdown() {
        channel.shutdown();
        debug("Closed gRPC channel.");
    }
}
