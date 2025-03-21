package pt.ulisboa.tecnico.tuplespaces.server;


import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import io.grpc.stub.StreamObserver;



public class TupleSpacesServerImpl extends ReplicaServerGrpc.ReplicaServerImplBase {

    private ServerState state;

    public TupleSpacesServerImpl() {
        state = new ServerState();
    }

    // Put a tuple in the tuple space
    @Override
    public void putServer(ReplicaServerOuterClass.PutRequestServer request, StreamObserver<ReplicaServerOuterClass.PutResponseServer> responseObserver) {
        String tuple = request.getNewTuple();
        final int client_id = request.getClientId();

        state.put(tuple, client_id);

        // Send an empty response
        ReplicaServerOuterClass.PutResponseServer response = ReplicaServerOuterClass.PutResponseServer.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Read a tuple from the tuple space
    @Override
    public void readServer(ReplicaServerOuterClass.ReadRequestServer request, StreamObserver<ReplicaServerOuterClass.ReadResponseServer> responseObserver) {
        String pattern = request.getSearchPattern();
        final int client_id = request.getClientId();

        String tuple = state.read(pattern, client_id);

        // Send the response with the read tuple
        ReplicaServerOuterClass.ReadResponseServer response = ReplicaServerOuterClass.ReadResponseServer.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Take a tuple from the tuple space
    @Override
    public void takeServer(ReplicaServerOuterClass.TakeRequestServer request, StreamObserver<ReplicaServerOuterClass.TakeResponseServer> responseObserver) {
        String pattern = request.getSearchPattern();
        final int client_id = request.getClientId();
        final int index = request.getTupleIndex();

        String tuple = state.takeWithIndex(index, client_id);

        // Send the response with the taken tuple
        ReplicaServerOuterClass.TakeResponseServer response = ReplicaServerOuterClass.TakeResponseServer.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Get the state of the tuple spaces
    @Override
    public void getTupleSpacesStateServer(ReplicaServerOuterClass.getTupleSpacesStateRequestServer request, StreamObserver<ReplicaServerOuterClass.getTupleSpacesStateResponseServer> responseObserver) {
        final int client_id = request.getClientId();

        // Send the response with the state of the tuple spaces
        ReplicaServerOuterClass.getTupleSpacesStateResponseServer response = ReplicaServerOuterClass.getTupleSpacesStateResponseServer.newBuilder().addAllTuple(state.getTupleSpacesState(client_id)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Request access to take a tuple
    @Override
    public void requestAccess(ReplicaServerOuterClass.GrantRequest request, StreamObserver<ReplicaServerOuterClass.GrantResponse> responseObserver) {
        final int client_id = request.getClientId();
        final String pattern = request.getSearchPattern();

        int index = state.requestAccess(pattern, client_id);

        // If the index is -1, the access was not granted
        boolean granted = (index != -1);

        // Send the response with the result of the request
        ReplicaServerOuterClass.GrantResponse response = ReplicaServerOuterClass.GrantResponse.newBuilder()
                .setGranted(granted)
                .setTupleIndex(index)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
