import socket
import threading
import socketserver

class ResourceManager:
    global mutex, __resource, __hosts
    def __init__(self):
        self.__resource = {
            'hosts': [
                {"ip": "127.0.0.1", "port": 12345, "gpus": [0, 1, 2, 3], "slots": ["None", "None", "None", "None"]},
                {"ip": "127.0.0.2", "port": 12345, "gpus": [0, 1, 2, 3], "slots": ["None", "None", "None", "None"]},
            ]
        }
        self.filledInMemory()

    def filledInMemory(self):
        self.__hosts = {}
        for element in self.__resource['hosts']:
            self.__hosts[element['ip']]= element

    def getAndSet(self, node, task):
        try:
            try:
                index = -1
                self.mutex = threading.Lock()
                if self.mutex.acquire():
                    resource = self.__hosts[node]
                    gpus = resource['gpus']
                    tasks = resource['slots']
                    for idx in range(len(tasks)):
                        tasks = self.__hosts[node]['slots']
                        if tasks[idx] == "None":
                            index = idx
                            tasks[idx] = task
                            break

            finally:
                self.mutex.release
                return node, task, index
        except:
            print("")
            raise

class ThreadTCPRequestHandler(socketserver.BaseRequestHandler):
    __rm = ResourceManager()

    def handle(self):
        data = str(self.request.recv(1024), 'ascii')
        print("Received data from client: {}".format(data))
        cur_thread = threading.current_thread()
        response = bytes("Send from {} DATA >>> {} : {}".format(self.server.server_address, cur_thread.name, data), 'ascii')
        self.request.sendall(response)

        node = "127.0.0.1"
        task = "s1"
        node, task, index = self.__rm.getAndSet(node, task)
        self.request.sendall(bytes("You will running on gpu {} on {}".format(node, index), 'ascii'))
        self.request.sendall(bytes(str(index), 'ascii'))

class ThreadTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):

    def service_actions(self):
        print("this is my self service actions...")

    def server_activate(self):
        print("Start listening server address: {}".format(self.server_address))
        self.socket.listen(self.request_queue_size)

if __name__ == "__main__":
    HOST, PORT = "localhost", 21479

    import pprint
    pp = pprint.PrettyPrinter(indent=4)

    server = ThreadTCPServer((HOST, PORT), ThreadTCPRequestHandler)
    ip, port = server.server_address
    server_thread = threading.Thread(target=server.serve_forever())

    #server_thread.daemon = False
    server_thread.daemon = True
    server_thread.start()

    print("Server loop running in Thread {}".format(server_thread.name))
    server.shutdown()
    server.server_close()
