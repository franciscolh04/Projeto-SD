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

    // Put a tuple in the tuple space
    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        String tuple = request.getNewTuple();
        final int client_id = request.getClientId();

        state.put(tuple, client_id);

        // Send an empty response
        TupleSpacesOuterClass.PutResponse response = TupleSpacesOuterClass.PutResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Read a tuple from the tuple space
    @Override
    public void read(TupleSpacesOuterClass.ReadRequest request, StreamObserver<TupleSpacesOuterClass.ReadResponse> responseObserver) {
        String pattern = request.getSearchPattern();
        final int client_id = request.getClientId();

        String tuple = state.read(pattern, client_id);

        // Send the response with the read tuple
        TupleSpacesOuterClass.ReadResponse response = TupleSpacesOuterClass.ReadResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Take a tuple from the tuple space
    @Override
    public void take(TupleSpacesOuterClass.TakeRequest request, StreamObserver<TupleSpacesOuterClass.TakeResponse> responseObserver) {
        String pattern = request.getSearchPattern();
        final int client_id = request.getClientId();
        final int index = request.getTupleIndex();

        String tuple = state.takeWithIndex(index, client_id);

        // Send the response with the taken tuple
        TupleSpacesOuterClass.TakeResponse response = TupleSpacesOuterClass.TakeResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Get the state of the tuple spaces
    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> responseObserver) {
        final int client_id = request.getClientId();

        // Send the response with the state of the tuple spaces
        TupleSpacesOuterClass.getTupleSpacesStateResponse response = TupleSpacesOuterClass.getTupleSpacesStateResponse.newBuilder().addAllTuple(state.getTupleSpacesState(client_id)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Request access to take a tuple
    @Override
    public void requestAccess(TupleSpacesOuterClass.GrantRequest request, StreamObserver<TupleSpacesOuterClass.GrantResponse> responseObserver) {
        final int client_id = request.getClientId();
        final String pattern = request.getSearchPattern();

        int index = state.requestAccess(pattern, client_id);

        // If the index is -1, the access was not granted
        boolean granted = (index != -1);

        // Send the response with the result of the request
        TupleSpacesOuterClass.GrantResponse response = TupleSpacesOuterClass.GrantResponse.newBuilder()
                .setGranted(granted)
                .setTupleIndex(index)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Release access after taking a tuple
    @Override
    public void releaseAccess(TupleSpacesOuterClass.ReleaseRequest request, StreamObserver<TupleSpacesOuterClass.ReleaseResponse> responseObserver) {
        final int client_id = request.getClientId();
        final int index = request.getTupleIndex();

        state.releaseAccess(index, client_id);

        // Send an empty response
        TupleSpacesOuterClass.ReleaseResponse response = TupleSpacesOuterClass.ReleaseResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
