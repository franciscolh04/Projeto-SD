package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.ResponseCollector;


public class ReadObserver implements StreamObserver<TupleSpacesOuterClass.ReadResponse> {
    private final ResponseCollector collector;

    public ReadObserver(ResponseCollector collector) {
        this.collector = collector;
    }

    @Override
    public void onNext(TupleSpacesOuterClass.ReadResponse response) {
        collector.addString(response.getResult());
        System.out.println("Received response: " + response.toString());
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
