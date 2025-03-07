package pt.ulisboa.tecnico.tuplespaces.server;


import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;
// import static io.grpc.Status.NOT_FOUND;



public class TupleSpacesServerImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    private ServerState state;

    public TupleSpacesServerImpl() {
        state = new ServerState();
    }

    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        String tuple = request.getNewTuple();
        final int client_id = request.getClientId();

        state.put(tuple, client_id);
        TupleSpacesOuterClass.PutResponse response = TupleSpacesOuterClass.PutResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void read(TupleSpacesOuterClass.ReadRequest request, StreamObserver<TupleSpacesOuterClass.ReadResponse> responseObserver) {
        String pattern = request.getSearchPattern();
        final int client_id = request.getClientId();

        String tuple = state.read(pattern, client_id);

        //if (tuple == null) {
        //    responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input").asRuntimeException());
        //}

        TupleSpacesOuterClass.ReadResponse response = TupleSpacesOuterClass.ReadResponse.newBuilder().setResult(tuple).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void take(TupleSpacesOuterClass.TakeRequest request, StreamObserver<TupleSpacesOuterClass.TakeResponse> responseObserver) {
        String pattern = request.getSearchPattern();
        final int client_id = request.getClientId();

        String tuple = state.take(pattern, client_id);

        //if (tuple == null) {
            //responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input").asRuntimeException());
        //}

        TupleSpacesOuterClass.TakeResponse response = TupleSpacesOuterClass.TakeResponse.newBuilder().setResult(tuple).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> responseObserver) {
        final int client_id = request.getClientId();

        TupleSpacesOuterClass.getTupleSpacesStateResponse response = TupleSpacesOuterClass.getTupleSpacesStateResponse.newBuilder().addAllTuple(state.getTupleSpacesState(client_id)).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
