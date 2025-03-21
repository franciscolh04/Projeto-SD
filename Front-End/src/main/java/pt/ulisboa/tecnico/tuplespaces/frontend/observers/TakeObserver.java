package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.TakeResponseCollector;


public class TakeObserver implements StreamObserver<ReplicaServerOuterClass.TakeResponseServer> {

    private final TakeResponseCollector<ReplicaServerOuterClass.TakeResponseServer> collector;

    public TakeObserver(TakeResponseCollector<ReplicaServerOuterClass.TakeResponseServer> collector) {
        this.collector = collector;
    }

    // This method is called whenever a new response is received from the server
    @Override
    public void onNext(ReplicaServerOuterClass.TakeResponseServer response) {
        collector.addResponse(response);
    }

    // This method is called whenever an error occurs
    @Override
    public void onError(Throwable t) {
        System.err.println("[gRPC] Error during take: " + t.getMessage());
        collector.addResponse(null);
    }

    // This method is called when the server finishes sending responses
    @Override
    public void onCompleted() {
        // Nothing to do here.
    }
}
