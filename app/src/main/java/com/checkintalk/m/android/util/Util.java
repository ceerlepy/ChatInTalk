package com.checkintalk.m.android.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.checkintalk.m.android.model.User;
import com.checkintalk.m.android.parser.JsonReaderUtil;
import com.checkintalk.m.android.parser.JsonWriterUtil;

public class Util {
	private static Util instance = null;
	public static synchronized Util getInstance() {
		if (instance == null)
			instance = new Util();
		return instance ;
	}
	
	public void deleteUserFromLocal(File dir) {
		File file = new File(dir.toString() + File.separator + "user.json");
		if(file.exists())
			file.delete();
	}
	
	/**
	 * 
	 * @param user
	 * @param dir
	 *            : getFilesDir()
	 * @return
	 */
	public boolean writeUserOnLocal(User user, File dir) {
		deleteUserFromLocal(dir);
		try {
			JsonWriterUtil.getInstance().jWriter(user,
					new FileWriter(dir.toString() + File.separator + "user.json"));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 
	 * @param dir
	 *            : getFilesDir()
	 * @return
	 */
	public User readUserFromLocal(File dir) {
		try {
			return JsonReaderUtil.getInstance().jReader(
					new FileReader(dir.toString() + File.separator + "user.json"));
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		return null;
	}

}
