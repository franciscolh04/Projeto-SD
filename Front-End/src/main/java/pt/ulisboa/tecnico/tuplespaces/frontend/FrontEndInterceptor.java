package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import io.grpc.Context;
import io.grpc.Contexts;

public class FrontEndInterceptor implements ServerInterceptor {

    // Key to retrieve the delay value from the metadata in the header of the request
    static final Metadata.Key<String> DELAY_KEY = Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);

    // Create a context key to store the delay value
    public static final Context.Key<String> DELAY_VALUE_CONTEXT = Context.key("delay");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call,
        final Metadata requestHeaders,
        ServerCallHandler<ReqT, RespT> next) {

        // Get the delay value from the metadata in the header of the request
        String delaysString = requestHeaders.get(DELAY_KEY);

        if (delaysString != null) {
            System.out.println("Arrived delay value: " + delaysString);

            // Set the delay value in the context
            Context context = Context.current().withValue(DELAY_VALUE_CONTEXT, delaysString);
            return Contexts.interceptCall(context, call, requestHeaders, next);
        }

        return next.startCall(call, requestHeaders);
    }
}