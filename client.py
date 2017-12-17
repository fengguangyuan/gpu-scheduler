import threading
import socket
import socketserver

def client(ip, port, message):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect((ip, port))
    try:
        sock.sendall(bytes(message, 'ascii'))
        response = str(sock.recv(1024), 'ascii')
        print("Received: {}".format(response))
        response = str(sock.recv(1024), 'ascii')
        print("Received: {}".format(response))
        response = str(sock.recv(1024), 'ascii')
        print("Type: ".format(type(response)))
        print("GPU index: {}".format(response))

        ## To set container GPU

    finally:
        sock.close()

if __name__ == "__main__":
    ip, port = "localhost", 21479
    client(ip, port, "Hello world...")
