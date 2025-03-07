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
        System.out.println("Frontend connecting to server TupleSpaces: " + serverAddress);
    }

    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        try {
            // Imprime os detalhes da requisição recebida
            debug("Received put request from Client. Forwarding to Server. Tuple to add: " + request.getNewTuple());

            // Realiza a operação de put no backend
            TupleSpacesOuterClass.PutResponse response = backendStub.put(request);

            // Envia a resposta ao cliente
            responseObserver.onNext(response); // Nota: Aqui a resposta é vazia
            responseObserver.onCompleted();

            // Imprime os detalhes da resposta enviada
            debug("Received put response from Server. Forwarding to Client. Feedback Status: Success");

        } catch (io.grpc.StatusRuntimeException e) { // Captura falhas na comunicação gRPC
            System.err.println("[gRPC] Error connecting with server during the put request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Envia o erro ao cliente para que ele saiba que a operação falhou
        } catch (Exception e) { // Captura qualquer outra exceção inesperada
            System.err.println("Unexpected Error during the put request: " + e.getMessage());
            responseObserver.onError(e); // Envia o erro ao cliente
        }
    }

    @Override
    public void read(TupleSpacesOuterClass.ReadRequest request, StreamObserver<TupleSpacesOuterClass.ReadResponse> responseObserver) {
        try {
            debug("Received read request from Client. Forwarding to Server. Tuple to read: " + request.getSearchPattern());

            // Tenta enviar a requisição ao backend
            TupleSpacesOuterClass.ReadResponse response = backendStub.read(request);

            // Envia a resposta ao cliente
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            debug("Received read response from Server. Forwarding to Client. Response: " + response.getResult());

        } catch (io.grpc.StatusRuntimeException e) { // Captura falhas na comunicação gRPC
            System.err.println("[gRPC] Error connecting with server during the reading request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Notifica o cliente sobre o erro
        } catch (Exception e) { // Captura qualquer outra exceção inesperada
            System.err.println("Unexpected Error during the reading request: " + e.getMessage());
            responseObserver.onError(e); // Notifica o cliente sobre o erro
        }
    }

    @Override
    public void take(TupleSpacesOuterClass.TakeRequest request, StreamObserver<TupleSpacesOuterClass.TakeResponse> responseObserver) {
        try {
            debug("Received take request from Client. Forwarding to Server. Tuple to take: " + request.getSearchPattern());

            // Tenta enviar a requisição ao backend
            TupleSpacesOuterClass.TakeResponse response = backendStub.take(request);

            // Envia a resposta ao cliente
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            debug("Received take response from Server. Forwarding to Client. Response: " + response.getResult());

        } catch (io.grpc.StatusRuntimeException e) { // Captura falhas na comunicação gRPC
            System.err.println("[gRPC] Error connecting with server during the take request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Notifica o cliente sobre o erro
        } catch (Exception e) { // Captura qualquer outra exceção inesperada
            System.err.println("Unexpected Error during the take request: " + e.getMessage());
            responseObserver.onError(e); // Notifica o cliente sobre o erro
        }
    }

    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> responseObserver) {
        try {
            debug("Received getTupleSpacesState request from Client. Forwarding to Server.");

            // Tenta enviar a requisição ao backend
            TupleSpacesOuterClass.getTupleSpacesStateResponse response = backendStub.getTupleSpacesState(request);

            // Envia a resposta ao cliente
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            debug("Received getTupleSpacesState response from Server. Forwarding to Client. Response: " + response.getTupleList());

        } catch (io.grpc.StatusRuntimeException e) { // Captura falhas na comunicação gRPC
            System.err.println("[gRPC] Error connecting with server during the getTupleSpacesState request: " + e.getStatus().getDescription());
            responseObserver.onError(e); // Notifica o cliente sobre o erro
        } catch (Exception e) { // Captura qualquer outra exceção inesperada
            System.err.println("Unexpected Error during the getTupleSpacesState request: " + e.getMessage());
            responseObserver.onError(e); // Notifica o cliente sobre o erro
        }
    }
}
