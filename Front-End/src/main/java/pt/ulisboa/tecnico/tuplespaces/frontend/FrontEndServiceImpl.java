package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;

public class FrontEndServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    private final TupleSpacesGrpc.TupleSpacesBlockingStub backendStub;

    public FrontEndServiceImpl(String serverAddress) {
        // Criar um canal gRPC para o servidor TupleSpaces recebido nos argumentos
        ManagedChannel channel = ManagedChannelBuilder.forTarget(serverAddress).usePlaintext().build();

        backendStub = TupleSpacesGrpc.newBlockingStub(channel);
        System.out.println("Frontend conectado ao servidor TupleSpaces: " + serverAddress);
    }

    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        TupleSpacesOuterClass.PutResponse response = backendStub.put(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void read(TupleSpacesOuterClass.ReadRequest request, StreamObserver<TupleSpacesOuterClass.ReadResponse> responseObserver) {
        TupleSpacesOuterClass.ReadResponse response = backendStub.read(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void take(TupleSpacesOuterClass.TakeRequest request, StreamObserver<TupleSpacesOuterClass.TakeResponse> responseObserver) {
        TupleSpacesOuterClass.TakeResponse response = backendStub.take(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> responseObserver) {
        TupleSpacesOuterClass.getTupleSpacesStateResponse response = backendStub.getTupleSpacesState(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
