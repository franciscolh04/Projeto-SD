package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import io.grpc.Context;
import io.grpc.Contexts;

public class HeadServerInterceptor implements ServerInterceptor {

    // Key to retrieve the delay value from the metadata in the header of the request
    public Metadata.Key<String> DELAY_KEY = Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);

    // Create a context key to store the delay value
    public Context.Key<String> DELAY_VALUE_CONTEXT = Context.key("delay");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call,
        final Metadata requestHeaders,
        ServerCallHandler<ReqT, RespT> next) {

        // Get the delay value from the metadata in the header of the request
        String delayValue = requestHeaders.get(DELAY_KEY);

        if (delayValue != null) {
            System.out.println("Arrived delay value: " + delayValue);

            // Sleep for the delay value
            try {
                Thread.sleep(Integer.parseInt(delayValue) * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Set the delay value in the context
            Context context = Context.current().withValue(DELAY_VALUE_CONTEXT, delayValue);
            return Contexts.interceptCall(context, call, requestHeaders, next);
        }

        return next.startCall(call, requestHeaders);
    }
}