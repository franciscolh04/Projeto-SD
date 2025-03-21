package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.ResponseCollector;

public class ReleaseObserver implements StreamObserver<TupleSpacesOuterClass.ReleaseResponse> {

    private final ResponseCollector<TupleSpacesOuterClass.ReleaseResponse> collector;

    public ReleaseObserver(ResponseCollector<TupleSpacesOuterClass.ReleaseResponse> collector) {
        this.collector = collector;
    }

    // This method is called whenever a new response is received from the server
    @Override
    public void onNext(TupleSpacesOuterClass.ReleaseResponse response) {
        collector.addString(response.toString());
    }

    // This method is called whenever an error occurs
    @Override
    public void onError(Throwable t) {
        System.err.println("[gRPC] Error during releaseAccess: " + t.getMessage());
    }

    // This method is called when the server finishes sending responses
    @Override
    public void onCompleted() {
        // Nothing to do here.
    }
}
