package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.TakeResponseCollector;

public class TakeObserver implements StreamObserver<TupleSpacesOuterClass.TakeResponse> {

    private final TakeResponseCollector<TupleSpacesOuterClass.TakeResponse> collector;

    public TakeObserver(TakeResponseCollector<TupleSpacesOuterClass.TakeResponse> collector) {
        this.collector = collector;
    }

    @Override
    public void onNext(TupleSpacesOuterClass.TakeResponse response) {
        collector.addResponse(response);
    }

    @Override
    public void onError(Throwable t) {
        System.err.println("[gRPC] Error during take: " + t.getMessage());
        collector.addResponse(null);
    }

    @Override
    public void onCompleted() {
        // Nothing to do
    }
}
