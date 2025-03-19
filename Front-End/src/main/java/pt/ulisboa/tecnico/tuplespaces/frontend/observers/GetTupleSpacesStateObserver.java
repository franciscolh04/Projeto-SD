package pt.ulisboa.tecnico.tuplespaces.frontend.observers;


import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.ResponseCollector;
import java.util.List;
import java.util.ArrayList;



public class GetTupleSpacesStateObserver implements StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> {
    private final ResponseCollector<TupleSpacesOuterClass.getTupleSpacesStateResponse> collector;

    public GetTupleSpacesStateObserver(ResponseCollector<TupleSpacesOuterClass.getTupleSpacesStateResponse> collector) {
        this.collector = collector;
    }

    @Override
    public void onNext(TupleSpacesOuterClass.getTupleSpacesStateResponse response) {
        collector.addString(String.join(", ", response.getTupleList()));
        //collector.addString(String.join(", ", response.getTupleList()));
        System.out.println("Received response: " + response.getTupleList());
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

