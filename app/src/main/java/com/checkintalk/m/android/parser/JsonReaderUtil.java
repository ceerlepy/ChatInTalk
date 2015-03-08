package com.checkintalk.m.android.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.checkintalk.m.android.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonReaderUtil {

	private static JsonReaderUtil instance = null;

	public static synchronized JsonReaderUtil getInstance() {
		if (instance == null)
			instance = new JsonReaderUtil();
		return instance;
	}

	public List<User> jListReader(FileReader file) throws IOException {

		BufferedReader bufferReader = new BufferedReader(file);
		StringBuilder sb = new StringBuilder();
		String line = "";

		while ((line = bufferReader.readLine()) != null) 
			sb.append(line + "\n");

		JsonElement json = new JsonParser().parse(sb.toString());

		JsonArray array = json.getAsJsonArray();
		Iterator<JsonElement> iterator = array.iterator();

		List<User> users = new ArrayList<User>();

		Gson gson;
		User user = new User();
		while (iterator.hasNext()) {
			gson = new Gson();
			user = gson.fromJson((JsonElement) iterator.next(), User.class);
			users.add(user);
		}
		return users;
	}

	public User jReader(FileReader file) throws IOException {

		BufferedReader bufferReader = new BufferedReader(file);
		StringBuilder sb = new StringBuilder();
		String line = "";

		while ((line = bufferReader.readLine()) != null) {
			sb.append(line + "\n");
		}

		JsonElement json = new JsonParser().parse(sb.toString());
		Gson gson = new Gson();

		return gson.fromJson(json, User.class);
	}
}
