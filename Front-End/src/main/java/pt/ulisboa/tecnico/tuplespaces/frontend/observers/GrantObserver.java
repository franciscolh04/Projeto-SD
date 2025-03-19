package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.TakeResponseCollector;

public class GrantObserver implements StreamObserver<TupleSpacesOuterClass.GrantResponse> {

    private final TakeResponseCollector<TupleSpacesOuterClass.GrantResponse> collector;

    public GrantObserver(TakeResponseCollector<TupleSpacesOuterClass.GrantResponse> collector) {
        this.collector = collector;
    }

    @Override
    public void onNext(TupleSpacesOuterClass.GrantResponse response) {
        collector.addResponse(response);
    }

    @Override
    public void onError(Throwable t) {
        System.err.println("[gRPC] Error during requestAccess: " + t.getMessage());
        collector.addResponse(null);
    }

    @Override
    public void onCompleted() {
        // Nothing to do here.
    }
}
