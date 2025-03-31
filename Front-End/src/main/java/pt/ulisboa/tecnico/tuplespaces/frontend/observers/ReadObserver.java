package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.ResponseCollector;


public class ReadObserver implements StreamObserver<ReplicaServerOuterClass.ReadResponseServer> {
    private final ResponseCollector collector;

    public ReadObserver(ResponseCollector collector) {
        this.collector = collector;
    }

    // This method is called whenever a new response is received from the server
    @Override
    public void onNext(ReplicaServerOuterClass.ReadResponseServer response) {
        collector.addString(response.getResult());
    }

    // This method is called whenever an error occurs
    @Override
    public void onError(Throwable throwable) {
        System.err.println("[gRPC] Error during request Read Command: " + throwable.getMessage());
        collector.addError(throwable);
    }

    // This method is called when the server finishes sending responses
    @Override
    public void onCompleted() {
        // Nothing to do here.
    }
}
