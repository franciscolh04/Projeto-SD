package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.ResponseCollector;

public class PutObserver implements StreamObserver<TupleSpacesOuterClass.PutResponse> {
    private final ResponseCollector collector;

    public PutObserver(ResponseCollector collector) {
        this.collector = collector;
    }

    @Override
    public void onNext(TupleSpacesOuterClass.PutResponse response) {
        collector.addString(response.toString());
        System.out.println("Received response: ");
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Received error: " + throwable);
    }

    @Override
    public void onCompleted() {
        System.out.println("Request completed");
    }
}
