import sys
import grpc
import os

sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import TupleSpaces_pb2
import TupleSpaces_pb2_grpc

class ClientService:
    def __init__(self, host_port: str, client_id: int):
        # Check if the 'debug' environment variable is set to 'true'
        self.debug_flag = os.environ.get("debug") == "true"

        # Create gRPC channel to communicate with the server
        self.channel = grpc.insecure_channel(host_port)

        # Create stub for synchronous calls
        self.stub = TupleSpaces_pb2_grpc.TupleSpacesStub(self.channel)



    # Method to add a tuple to the shared space
    def put(self, tuple_str: str):
        request = TupleSpaces_pb2.PutRequest(newTuple=tuple_str, clientId=self.client_id)

        try:
            response = self.stub.put(request)
            if self.debug_flag:
                print(f"Added tuple: {tuple_str}")
            return response
        except grpc.RpcError as e:
            print(f"Error during the put request: {e.code()} - {e.details()}")
            return None
        except Exception as e:
            print(f"Unexpected Error during the put request: {str(e)}")
            return None

    # Method to read a tuple (blocks until a match is found)
    def read(self, pattern: str):
        request = TupleSpaces_pb2.ReadRequest(searchPattern=pattern)
        
        try:
            response = self.stub.read(request)
            if self.debug_flag:
                print(f"Read tuple: {response.result}")
            return response
        except grpc.RpcError as e:
            print(f"Caught exception with description: {e.details()}")
            return None
        except Exception as e:
            print(f"Unexpected Error during the read request: {str(e)}")
            return None

    # Method to read and remove a tuple from the tuple space (blocks until a match is found)
    def take(self, pattern: str):
        request = TupleSpaces_pb2.TakeRequest(searchPattern=pattern)
        
        try:
            response = self.stub.take(request)
            if self.debug_flag:
                print(f"Removed tuple: {response.result}")
            return response
        except grpc.RpcError as e:
            print(f"Caught exception with description: {e.details()}")
            return None
        except Exception as e:
            print(f"Unexpected Error during the take request: {str(e)}")
            return None

    # Método para obter o estado atual do espaço de tuplos
    def get_tuple_spaces_state(self):
        request = TupleSpaces_pb2.getTupleSpacesStateRequest()
        try:
            response = self.stub.getTupleSpacesState(request)
            if self.debug_flag:
                print(f"TupleSpaces Current State: {response.tuple}")
            return response
        except grpc.RpcError as e:
            print(f"Caught exception with description: {e.details()}")
            return None
        except Exception as e:
            print(f"Unexpected Error during the getTupleSpacesState request: {str(e)}")
            return None

    # Fechar o canal gRPC corretamente
    def shutdown(self):
        self.channel.close()
        print("Closed gRPC channel.")
