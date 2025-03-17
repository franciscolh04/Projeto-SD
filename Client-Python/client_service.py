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

        # Set the client ID
        self.client_id = client_id

        self.debug(f"Client created with ID: {client_id}")
    

    # Method to print debug messages
    def debug(self, message: str):
        if self.debug_flag:
            print(f"[DEBUG] " + message)


    # Method to add a tuple to the shared space
    def put(self, tuple_str: str):
        request = TupleSpaces_pb2.PutRequest(newTuple=tuple_str, client_id=int(self.client_id))

        try:
            response = self.stub.put(request)
            self.debug(f"Added tuple: {tuple_str}")
            return response
        except grpc.RpcError as e:
            print(f"Error during the put request: {e.code()} - {e.details()}")
            return None
        except Exception as e:
            print(f"Unexpected Error during the put request: {str(e)}")
            return None

    # Method to read a tuple (blocks until a match is found)
    def read(self, pattern: str):
        request = TupleSpaces_pb2.ReadRequest(searchPattern=pattern, client_id=int(self.client_id))
        
        try:
            response = self.stub.read(request)
            self.debug(f"Read tuple: {response.result}")
            return response
        except grpc.RpcError as e:
            print(f"Error during the read request: {e.code()} - {e.details()}")
            return None
        except Exception as e:
            print(f"Unexpected Error during the read request: {str(e)}")
            return None

    # Method to read and remove a tuple from the tuple space (blocks until a match is found)
    def take(self, pattern: str):
        request = TupleSpaces_pb2.TakeRequest(searchPattern=pattern, client_id=int(self.client_id))
        
        try:
            response = self.stub.take(request)
            self.debug(f"Removed tuple: {response.result}")
            return response
        except grpc.RpcError as e:
            print(f"Error during the take request: {e.code()} - {e.details()}")
            return None
        except Exception as e:
            print(f"Unexpected Error during the take request: {str(e)}")
            return None

    # Method to get the current state of the tuple space
    def get_tuple_spaces_state(self):
        request = TupleSpaces_pb2.getTupleSpacesStateRequest(client_id=int(self.client_id))
        try:
            response = self.stub.getTupleSpacesState(request)
            self.debug(f"TupleSpaces Current State: {response.tuple}")
            return response
        except grpc.RpcError as e:
            print(f"Error during the getTupleSpacesState request: {e.code()} - {e.details()}")
            return None
        except Exception as e:
            print(f"Unexpected Error during the getTupleSpacesState request: {str(e)}")
            return None

    # Properly close the gRPC channel
    def shutdown(self):
        self.debug("Closed gRPC channel.")
        self.channel.close()
