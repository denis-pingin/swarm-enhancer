package swarm.enhancer.foursquare;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileTokenStore {

    private static final String TOKEN_FILENAME = ".token";

    private static FileTokenStore sInstance;
    private final Context context;
    private String token;

    public FileTokenStore(Context context) {
        this.context = context;
    }

    public static FileTokenStore get(Context context) {
        if (sInstance == null) {
            sInstance = new FileTokenStore(context);
        }

        return sInstance;
    }

    public String getToken() {
        if (token == null) {
            token = readToken();
        }
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        saveToken();
    }

    private void saveToken() {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(TOKEN_FILENAME, Context.MODE_PRIVATE);
            fileOutputStream.write(token.getBytes());
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d(FileTokenStore.class.getSimpleName(), e.getMessage(), e);
        }
    }

    private String readToken() {
        try {
            FileInputStream fileInputStream = context.openFileInput(TOKEN_FILENAME);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            return bufferedReader.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}