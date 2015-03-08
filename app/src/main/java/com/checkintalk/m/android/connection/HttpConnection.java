package com.checkintalk.m.android.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class HttpConnection {
	public static String readUrl(String mapsApiDirectionsUrl) throws IOException {
		String data = "";
		InputStream iStream = null;
		HttpURLConnection urlConnection = null;
		BufferedReader br = null;
		
		try {
			URL url = new URL(mapsApiDirectionsUrl);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.connect();
			iStream = urlConnection.getInputStream();
			br = new BufferedReader(new InputStreamReader(iStream));
			StringBuffer sb = new StringBuffer();
			String line = "";
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			data = sb.toString();
			br.close();
		} catch (Exception e) {
			Log.d("Exception while reading url", e.toString());
		} finally {
			try{
				br.close();
			}catch(Exception ex){
				Log.e("Exception closing stream after reading url", ex.getMessage());
			}
			
			try{
				iStream.close();
			}catch(Exception ex){
				Log.e("Exception closing stream after reading url", ex.getMessage());
			}
			
			try{
				urlConnection.disconnect();
			}catch(Exception ex){
				Log.e("Exception closing stream after reading url", ex.getMessage());
			}
		}
		return data;
	}

}