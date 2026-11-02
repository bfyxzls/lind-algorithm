package com.lind.architecture.io;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class SocketClient {

	public static void main(String[] args) throws IOException {
		try (Socket socket = new Socket("127.0.0.1", 8888);
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

			writer.write("你好，服务器,hello,server！\n");
			writer.flush();

			String response = reader.readLine();
			System.out.println("服务端回复：" + response);
		}
	}

}