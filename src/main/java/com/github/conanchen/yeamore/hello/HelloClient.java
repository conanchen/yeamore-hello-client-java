package com.github.conanchen.yeamore.hello;

import com.github.conanchen.yeamore.hello.grpc.HelloGrpc;
import com.github.conanchen.yeamore.hello.grpc.HelloReply;
import com.github.conanchen.yeamore.hello.grpc.HelloRequest;
import com.google.gson.Gson;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class HelloClient {

    public static void main(String[] args) {
        HelloClient helloClient = new HelloClient();
        helloClient.sayHello("Conan Chen");

        BufferedReader br = null;

        try {

            br = new BufferedReader(new InputStreamReader(System.in));

            while (true) {

                System.out.println("Enter something ( q to exit): ");
                String input = br.readLine();

                if ("q".equals(input)) {
                    System.out.println("Exit!");
                    System.exit(0);
                }

                System.out.println("input : " + input);
                System.out.println("-----------\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private final static Gson gson = new Gson();
    final HealthCheckRequest helloGrpcHealthCheckRequest = HealthCheckRequest
            .newBuilder()
            .setService(HelloGrpc.getServiceDescriptor().getName())
            .build();

    private ManagedChannel getManagedChannel() {
        return NettyChannelBuilder
                .forAddress(BuildConfig.GRPC_SERVER_HOST, BuildConfig.GRPC_SERVER_PORT)
                .usePlaintext(true)
                //                .keepAliveTime(60, TimeUnit.SECONDS)
                .build();
    }

    public void sayHello(String name) {
        ManagedChannel channel = getManagedChannel();

        ConnectivityState connectivityState = channel.getState(true);
        System.out.println(String.format("sayHello connectivityState = [%s]", gson.toJson(connectivityState)));

        HealthGrpc.HealthStub healthStub = HealthGrpc.newStub(channel);
        HelloGrpc.HelloBlockingStub helloBlockingStub = HelloGrpc.newBlockingStub(channel);

        healthStub.withDeadlineAfter(60, TimeUnit.SECONDS).check(helloGrpcHealthCheckRequest,
                new StreamObserver<HealthCheckResponse>() {
                    @Override
                    public void onNext(HealthCheckResponse value) {

                        if (value.getStatus() == HealthCheckResponse.ServingStatus.SERVING) {
                            HelloReply helloReply = helloBlockingStub.sayHello(HelloRequest.newBuilder().setName(name).build());

                            System.out.println(String.format("sayHello got helloReply %d:%s gson=[%s]",
                                    helloReply.getId(),helloReply.getMessage(),
                                    gson.toJson(helloReply)));

                        } else {
                            System.out.println(String.format("sayHello healthStub.check onNext NOT! ServingStatus.SERVING name = [%s]", name));
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println(String.format("sayHello healthStub.check onError grpc service check health\n%s", t.getMessage()));
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println(String.format("sayHello healthStub.check onCompleted grpc service check health\n%s", ""));
                    }
                });


    }
}
