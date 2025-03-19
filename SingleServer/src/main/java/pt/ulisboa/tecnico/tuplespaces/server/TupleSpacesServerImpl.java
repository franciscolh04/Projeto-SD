package pt.ulisboa.tecnico.tuplespaces.server;


import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import io.grpc.stub.StreamObserver;



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

        TupleSpacesOuterClass.ReadResponse response = TupleSpacesOuterClass.ReadResponse.newBuilder().setResult(tuple).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void take(TupleSpacesOuterClass.TakeRequest request, StreamObserver<TupleSpacesOuterClass.TakeResponse> responseObserver) {
        String pattern = request.getSearchPattern();
        final int client_id = request.getClientId();
        final int index = request.getTupleIndex();

        String tuple = state.takeWithIndex(index, client_id);

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

    @Override
    public void requestAccess(TupleSpacesOuterClass.GrantRequest request, StreamObserver<TupleSpacesOuterClass.GrantResponse> responseObserver) {
        final int client_id = request.getClientId();
        final String pattern = request.getSearchPattern();

        int index = state.requestAccess(pattern, client_id);

        boolean granted = (index != -1);

        TupleSpacesOuterClass.GrantResponse response = TupleSpacesOuterClass.GrantResponse.newBuilder()
                .setGranted(granted)
                .setTupleIndex(index)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
