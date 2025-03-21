package pt.ulisboa.tecnico.tuplespaces.frontend.observers;


import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.ResponseCollector;


public class GetTupleSpacesStateObserver implements StreamObserver<ReplicaServerOuterClass.getTupleSpacesStateResponseServer> {
    private final ResponseCollector<ReplicaServerOuterClass.getTupleSpacesStateResponseServer> collector;

    public GetTupleSpacesStateObserver(ResponseCollector<ReplicaServerOuterClass.getTupleSpacesStateResponseServer> collector) {
        this.collector = collector;
    }

    // This method is called whenever a new response is received from the server
    @Override
    public void onNext(ReplicaServerOuterClass.getTupleSpacesStateResponseServer response) {
        collector.addString(String.join(", ", response.getTupleList()));
    }

    // This method is called whenever an error occurs
    @Override
    public void onError(Throwable throwable) {
        System.err.println("[gRPC] Error during requestAccess: " + throwable.getMessage());
    }

    // This method is called when the server finishes sending responses
    @Override
    public void onCompleted() {
        // Nothing to do here.
    }
}

