package com.lind.architecture.io.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolServer {

	public static void main(String[] args) {
		// 说明：
		// - Executors.newFixedThreadPool(poolSize) 创建固定大小线程池
		// - 每来一个连接，交给线程池处理
		// - 比“每连接一个线程”的方式更适合高并发场景
		int port = 8888;
		int poolSize = 10;

		ExecutorService executor = Executors.newFixedThreadPool(poolSize);

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("服务器已启动，监听端口 " + port + "...");

			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("客户端连接：" + socket.getRemoteSocketAddress());

				executor.submit(() -> handleClient(socket));
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			executor.shutdown();
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

			System.out.println("客户端断开：" + socket.getRemoteSocketAddress());
		}
		catch (IOException e) {
			System.out.println("连接异常：" + socket.getRemoteSocketAddress());
		}
	}

}