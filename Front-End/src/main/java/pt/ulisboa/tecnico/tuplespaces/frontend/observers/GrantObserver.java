package pt.ulisboa.tecnico.tuplespaces.frontend.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.frontend.GrantResponseCollector;

public class GrantObserver implements StreamObserver<ReplicaServerOuterClass.GrantResponse> {
    private final GrantResponseCollector collector;
    private final int serverIndex;

    public GrantObserver(GrantResponseCollector collector, int serverIndex) {
        this.collector = collector;
        this.serverIndex = serverIndex;
    }

    @Override
    public void onNext(ReplicaServerOuterClass.GrantResponse response) {
        collector.addResponse(serverIndex, response);
    }

    @Override
    public void onError(Throwable t) {
        System.err.println("[TAKE] Error from server " + (serverIndex + 1) + " during grant: " + t.getMessage());
        collector.addResponse(serverIndex, null);
    }

    @Override
    public void onCompleted() {
        // opcional: debug ou lógica adicional
    }
}
