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
        // Criar um canal gRPC para o servidor TupleSpaces recebido nos argumentos
        ManagedChannel channel = ManagedChannelBuilder.forTarget(serverAddress).usePlaintext().build();

        backendStub = TupleSpacesGrpc.newBlockingStub(channel);
        System.out.println("Frontend conectado ao servidor TupleSpaces: " + serverAddress);
    }

    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        // Imprime os detalhes da requisição recebida
        debug("Received put request from Client. Forwarding to Server. Tuple to add: " + request.getNewTuple());
        
        // Realiza a operação de put no backend
        TupleSpacesOuterClass.PutResponse response = backendStub.put(request);
        
        // Envia a resposta ao cliente
        responseObserver.onNext(response); //Nota: Aqui a resposta é vazio
        responseObserver.onCompleted();
        
        // Imprime os detalhes da resposta enviada
        debug("Received put response from Server. Forwarding to Client. Feedback Status: Success");
    }

    @Override
    public void read(TupleSpacesOuterClass.ReadRequest request, StreamObserver<TupleSpacesOuterClass.ReadResponse> responseObserver) {
        debug("Received read request from Client. Forwarding to Server. Tuple to read: " + request.getSearchPattern());
        TupleSpacesOuterClass.ReadResponse response = backendStub.read(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        debug("Received read response from Server. Forwarding to Client. Response: " + response.getResult());
    }

    @Override
    public void take(TupleSpacesOuterClass.TakeRequest request, StreamObserver<TupleSpacesOuterClass.TakeResponse> responseObserver) {
        debug("Received take request from Client. Forwarding to Server. Tuple to take: " + request.getSearchPattern());
        TupleSpacesOuterClass.TakeResponse response = backendStub.take(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        debug("Received take response from Server. Forwarding to Client. Response: " + response.getResult());
    }

    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> responseObserver) {
        debug("Received getTupleSpacesState request from Client. Forwarding to Server.");
        TupleSpacesOuterClass.getTupleSpacesStateResponse response = backendStub.getTupleSpacesState(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        debug("Received getTupleSpacesState response from Server. Forwarding to Client. Response: " + response.getTupleList());
    }
}
