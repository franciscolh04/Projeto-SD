import sys
import grpc
import os

sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import TupleSpaces_pb2
import TupleSpaces_pb2_grpc

class ClientService:
    def __init__(self, host_port: str, client_id: int):
        # Verifica se a variável de ambiente 'debug' está definida como 'true'
        self.debug_flag = os.environ.get("debug") == "true"
        
        # Criar canal gRPC para comunicar com o servidor
        self.channel = grpc.insecure_channel(host_port)
        
        # Criar stub para chamadas síncronas
        self.stub = TupleSpaces_pb2_grpc.TupleSpacesStub(self.channel)
        
        #print(f"Client created with ID: {client_id}")

    # Método para adicionar um tuplo ao espaço partilhado
    def put(self, tuple_str: str):
        request = TupleSpaces_pb2.PutRequest(newTuple=tuple_str)
        response = self.stub.put(request)
        if self.debug_flag:
            print(f"Added tuple: {tuple_str}")
        return response

    # Método para ler um tuplo sem remover (bloqueia até encontrar um matching)
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

    # Método para ler e remover um tuplo do espaço de tuplos (bloqueia até encontrar um matching)
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

    # Método para obter o estado atual do espaço de tuplos
    def get_tuple_spaces_state(self):
        request = TupleSpaces_pb2.getTupleSpacesStateRequest()
        response = self.stub.getTupleSpacesState(request)
        if self.debug_flag:
            print(f"TupleSpaces Current State: {response.tuple}")
        return response

    # Fechar o canal gRPC corretamente
    def shutdown(self):
        self.channel.close()
        print("Closed gRPC channel.")
