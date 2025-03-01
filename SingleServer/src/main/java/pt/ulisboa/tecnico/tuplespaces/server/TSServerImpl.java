package pt.ulisboa.tecnico.tuplespaces.server;


import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import io.grpc.stub.StreamObserver;
import static io.grpc.Status.INVALID_ARGUMENT;
// import static io.grpc.Status.NOT_FOUND;



public class TSServerImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    private ServerState ss;

    public TSServerImpl() {
        ss = new ServerState();
    }

    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        String tuple = request.getNewTuple();

        ss.put(tuple);
        TupleSpacesOuterClass.PutResponse response = TupleSpacesOuterClass.PutResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void read(TupleSpacesOuterClass.ReadRequest request, StreamObserver<TupleSpacesOuterClass.ReadResponse> responseObserver) {
        String pattern = request.getSearchPattern();

        String tuple = ss.read(pattern);

        if (tuple == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input").asRuntimeException());
        }

        TupleSpacesOuterClass.ReadResponse response = TupleSpacesOuterClass.ReadResponse.newBuilder().setResult(tuple).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void take(TupleSpacesOuterClass.TakeRequest request, StreamObserver<TupleSpacesOuterClass.TakeResponse> responseObserver) {
        String pattern = request.getSearchPattern();

        String tuple = ss.take(pattern);

        //if (tuple == null) {
            //responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input").asRuntimeException());
        //}

        TupleSpacesOuterClass.TakeResponse response = TupleSpacesOuterClass.TakeResponse.newBuilder().setResult(tuple).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> responseObserver) {

        TupleSpacesOuterClass.getTupleSpacesStateResponse response = TupleSpacesOuterClass.getTupleSpacesStateResponse.newBuilder().addAllTuple(ss.getTupleSpacesState()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
