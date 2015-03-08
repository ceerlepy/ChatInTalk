package com.checkintalk.m.android.parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import com.checkintalk.m.android.model.User;
import com.google.gson.Gson;

public class JsonWriterUtil {
	
	private static JsonWriterUtil instance = null;
	public static synchronized JsonWriterUtil getInstance() {
		if (instance == null)
			instance = new JsonWriterUtil();
		return instance ;
	}
	
	public void jListWriter(List<User> users, FileWriter file) throws IOException, JSONException {
		BufferedWriter bufferedWriter = new BufferedWriter(file);

		Gson gson = new Gson();
		String json = gson.toJson(users);

		try {
			bufferedWriter.write(json);
		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			bufferedWriter.flush();
			bufferedWriter.close();
		}
	}
	
	public void jWriter(User user, FileWriter file) throws IOException, JSONException {
		BufferedWriter bufferedWriter = new BufferedWriter(file);
		
		Gson gson = new Gson();
		String json = gson.toJson(user);
		
		try {
			bufferedWriter.write(json);
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			bufferedWriter.flush();
			bufferedWriter.close();
		}
	}
}
