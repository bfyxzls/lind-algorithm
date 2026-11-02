package com.lind.architecture.io.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MultiClientServer {

	public static void main(String[] args) {
		// 说明：
		// - while (true) 持续监听新连接
		// - accept() 阻塞等待客户端
		// - 每个客户端创建一个线程，支持并发连接
		try (ServerSocket serverSocket = new ServerSocket(8888)) {
			System.out.println("服务器已启动，监听端口 8888...");

			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("客户端连接：" + socket.getRemoteSocketAddress());

				// 每个客户端单独一个线程处理
				new Thread(() -> handleClient(socket)).start();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void handleClient(Socket socket) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println("收到消息：" + line);
				writer.write("服务器已收到： " + line + "\n");
				writer.flush();
			}

		}
		catch (IOException e) {
			System.out.println("客户端断开连接：" + socket.getRemoteSocketAddress());
		}
	}

}