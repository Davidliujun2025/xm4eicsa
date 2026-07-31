"""Local TCP forwarder for development MySQL access through SSH.

Credentials are read from environment variables and are never persisted.
"""

from __future__ import annotations

import os
import select
import socket
import sys
import threading

import paramiko


class TransportManager:
    def __init__(
        self,
        host: str,
        port: int,
        user: str,
        password: str,
    ) -> None:
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.lock = threading.Lock()
        self.ssh: paramiko.SSHClient | None = None
        self.transport: paramiko.Transport | None = None

    def get_transport(self) -> paramiko.Transport:
        with self.lock:
            if self.transport is not None and self.transport.is_active():
                return self.transport

            self._close_unlocked()
            ssh = paramiko.SSHClient()
            ssh.load_system_host_keys()
            ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
            ssh.connect(
                self.host,
                port=self.port,
                username=self.user,
                password=self.password,
                look_for_keys=False,
                allow_agent=False,
                timeout=10,
            )
            transport = ssh.get_transport()
            if transport is None:
                ssh.close()
                raise RuntimeError("SSH transport was not established")

            transport.set_keepalive(20)
            self.ssh = ssh
            self.transport = transport
            print("SSH_TUNNEL_CONNECTED=true", flush=True)
            return transport

    def invalidate(self, transport: paramiko.Transport) -> None:
        with self.lock:
            if self.transport is transport:
                self._close_unlocked()

    def close(self) -> None:
        with self.lock:
            self._close_unlocked()

    def _close_unlocked(self) -> None:
        if self.ssh is not None:
            self.ssh.close()
        self.ssh = None
        self.transport = None


def forward(client_socket: socket.socket, manager: TransportManager) -> None:
    channel = None
    try:
        transport = manager.get_transport()
        try:
            channel = transport.open_channel(
                "direct-tcpip",
                ("127.0.0.1", 3306),
                client_socket.getpeername(),
            )
        except Exception:
            manager.invalidate(transport)
            transport = manager.get_transport()
            channel = transport.open_channel(
                "direct-tcpip",
                ("127.0.0.1", 3306),
                client_socket.getpeername(),
            )

        while True:
            readable, _, _ = select.select([client_socket, channel], [], [])
            if client_socket in readable:
                data = client_socket.recv(65536)
                if not data:
                    break
                channel.sendall(data)
            if channel in readable:
                data = channel.recv(65536)
                if not data:
                    break
                client_socket.sendall(data)
    except Exception as exc:
        print(
            f"SSH_TUNNEL_FORWARD_ERROR={type(exc).__name__}",
            file=sys.stderr,
            flush=True,
        )
    finally:
        if channel is not None:
            channel.close()
        client_socket.close()


def main() -> None:
    ssh_host = os.environ.get("REMOTE_SSH_HOST", "42.193.201.236")
    ssh_port = int(os.environ.get("REMOTE_SSH_PORT", "22"))
    ssh_user = os.environ.get("REMOTE_SSH_USER", "ubuntu")
    ssh_password = os.environ["REMOTE_SSH_PASSWORD"]
    local_port = int(os.environ.get("MYSQL_TUNNEL_LOCAL_PORT", "13306"))

    manager = TransportManager(
        ssh_host,
        ssh_port,
        ssh_user,
        ssh_password,
    )
    manager.get_transport()

    listener = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    listener.bind(("127.0.0.1", local_port))
    listener.listen(20)
    print(f"SSH_TUNNEL_READY=127.0.0.1:{local_port}", flush=True)

    try:
        while True:
            client_socket, _ = listener.accept()
            threading.Thread(
                target=forward,
                args=(client_socket, manager),
                daemon=True,
            ).start()
    finally:
        listener.close()
        manager.close()


if __name__ == "__main__":
    main()
