/*
 * Copyright 2022 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.testing.integration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.gcp.observability.GcpObservability;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.integration.Messages.Payload;
import io.grpc.testing.integration.Messages.ResponseParameters;
import io.grpc.testing.integration.Messages.SimpleRequest;
import io.grpc.testing.integration.Messages.SimpleResponse;
import io.grpc.testing.integration.Messages.StreamingOutputCallRequest;
import io.grpc.testing.integration.Messages.StreamingOutputCallResponse;

/**
 * Server that manages startup/shutdown of a {@code TestService} server.
 */
public class ObservabilityTestServer {
  private static final Logger logger = Logger.getLogger(ObservabilityTestServer.class.getName());

  private Server server;

  private void start() throws IOException {
    /* The port on which the server should run */
    int port = 10000;
    server = ServerBuilder.forPort(port)
             .addService(new TestServiceImpl())
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      @SuppressWarnings("CatchAndPrintStackTrace")
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          ObservabilityTestServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    try (GcpObservability gcpObservability = GcpObservability.grpcInit()) {
      final ObservabilityTestServer server = new ObservabilityTestServer();
      server.start();
      server.blockUntilShutdown();
    }
  }

  static class TestServiceImpl extends TestServiceGrpc.TestServiceImplBase {
    @Override
    public void emptyCall(
        EmptyProtos.Empty req, StreamObserver<EmptyProtos.Empty> responseObserver) {
      responseObserver.onNext(EmptyProtos.Empty.getDefaultInstance());
      responseObserver.onCompleted();
    }

    @Override
    public void unaryCall(SimpleRequest req, StreamObserver<SimpleResponse> responseObserver) {
      logger.info("unaryCall server receives request size "+req.getPayload().getBody().size());
      responseObserver.onNext(
          SimpleResponse.newBuilder().setPayload(
              Payload.newBuilder().setBody(
                  ByteString.copyFrom(new byte[req.getResponseSize()]))).build());
      responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<StreamingOutputCallRequest> fullDuplexCall(
        final StreamObserver<StreamingOutputCallResponse> responseObserver) {
      return new StreamObserver<StreamingOutputCallRequest>() {
        @Override
        public void onNext(StreamingOutputCallRequest req) {
          logger.info("fullDuplexCall server receives request size "+req.getPayload().getBody().size());
          for (ResponseParameters params : req.getResponseParametersList()) {
            responseObserver.onNext(StreamingOutputCallResponse.newBuilder().setPayload(
                Payload.newBuilder().setBody(
                    ByteString.copyFrom(new byte[params.getSize()]))).build());
          }
        }
        @Override
        public void onError(Throwable t) {
          logger.info("fullDuplexCall server onError");
          responseObserver.onError(t);
        }
        @Override
        public void onCompleted() {
          responseObserver.onCompleted();
        }
      };
    }
  }
}
