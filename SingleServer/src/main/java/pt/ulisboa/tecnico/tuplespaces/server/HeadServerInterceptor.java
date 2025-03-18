package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import io.grpc.Context;
import io.grpc.Contexts;

public class HeadServerInterceptor implements ServerInterceptor {

    static final Metadata.Key<String> DELAY_KEY = Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);
    public static final Context.Key<String> DELAY_VALUE_CONTEXT = Context.key("delay");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next) {

        String delayValue = requestHeaders.get(DELAY_KEY);

        if (delayValue != null) {
            System.out.println("Arrived delay value: " + delayValue);
            Context context = Context.current().withValue(DELAY_VALUE_CONTEXT, delayValue);
            return Contexts.interceptCall(context, call, requestHeaders, next);
            /*
            try {
                Thread.sleep(Integer.parseInt(delayValue));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }*/
        }

        return next.startCall(call, requestHeaders);
    }
}