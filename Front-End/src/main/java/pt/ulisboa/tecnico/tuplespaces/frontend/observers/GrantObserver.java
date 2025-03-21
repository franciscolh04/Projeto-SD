package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.TakeResponseCollector;

public class GrantObserver implements StreamObserver<ReplicaServerOuterClass.GrantResponse> {

    private final TakeResponseCollector<ReplicaServerOuterClass.GrantResponse> collector;

    public GrantObserver(TakeResponseCollector<ReplicaServerOuterClass.GrantResponse> collector) {
        this.collector = collector;
    }

    // This method is called whenever a new response is received from the server
    @Override
    public void onNext(ReplicaServerOuterClass.GrantResponse response) {
        collector.addResponse(response);
    }

    // This method is called whenever an error occurs
    @Override
    public void onError(Throwable t) {
        System.err.println("[gRPC] Error during requestAccess: " + t.getMessage());
        collector.addResponse(null);
    }

    // This method is called when the server finishes sending responses
    @Override
    public void onCompleted() {
        // Nothing to do here.
    }
}
