package com.lind.architecture.io.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class SocketServer {

	public static void main(String[] args) throws IOException {
		// 注意：
		// - BufferedWriter 适合写入文本数据。
		// - 发送字符串后要 flush()，否则数据可能停留在缓冲区中。
		// - 一般用 \n 作为一行结束，配合 readLine() 使用。
		ServerSocket serverSocket = new ServerSocket(8888);
		System.out.println("服务器已启动，等待客户端连接...");

		try (Socket socket = serverSocket.accept();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

			String msg = reader.readLine();
			System.out.println("客户端发来：" + msg);

			writer.write("你好，客户端！\n");
			writer.flush(); // 一定要 flush，数据才会真正发出去
		}
	}

}