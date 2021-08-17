package com.tejsumeru.mailifire.Utility;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class GmailUtility {

    Context context;
    private final String APPLICATION_NAME = "Mailifire";
    private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final String USER_ID = "me";
    //private String TOKENS_DIRECTORY_PATH;
    private final String TOKENS_DIRECTORY_PATH = "G:"
            + File.separator + "Sem-7"
            + File.separator + "MP"
            + File.separator + "Mailifire"
            + File.separator + "app"
            + File.separator + "src"
            + File.separator + "main"
            + File.separator + "assets";

    private final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    private final String CREDENTIALS_FILE_PATH = "G:"
            + File.separator + "Sem-7"
            + File.separator + "MP"
            + File.separator + "Mailifire"
            + File.separator + "app"
            + File.separator + "src"
            + File.separator + "main"
            + File.separator + "assets"
            + File.separator + "credential.json";
    com.google.api.services.gmail.Gmail service;

    public GmailUtility(Context context){
        this.context=context;
        //TOKENS_DIRECTORY_PATH = context.getApplicationContext().getAssets()
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        //Error is here exception throws
        InputStream in = context.getApplicationContext().getAssets().open("credential.json");
//        InputStream in = new FileInputStream("G://Sem-7//MP//Mailifire//app//src//main//assets//credential.json");
        //InputStream in = new FileInputStream(new File(CREDENTIALS_FILE_PATH));
        Log.e("not found",in.toString());
        if (in == null) {

            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        Log.e("token",TOKENS_DIRECTORY_PATH);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public Gmail getService() throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT;

        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail  service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        return service;
    }

    public List<Message> listMsgMatchingQuery(Gmail service,String userId, String query) throws IOException{
        ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();
        List<Message> messages = new ArrayList<>();

        while (response.getMessages()!=null){
            messages.addAll(response.getMessages());
            if(response.getNextPageToken()!=null) {
                String pageToken = response.getNextPageToken();
                response = service.users().messages().list(userId).setQ(query).setPageToken(pageToken).execute();
            }else {
                break;
            }
        }

        return messages;
    }

    public Message getMessage(Gmail service,String userId,List<Message> messages,int index) throws IOException{
        Message message = service.users().messages().get(userId,messages.get(index).getId()).execute();
        return message;
    }

    /*public static HashMap<String,String> getGmailData(String query){
        try {
            Gmail service = getService();
            List<Message> messages = listMsgMatchingQuery(service,USER_ID,query);
            Message message = getMessage(service,USER_ID,messages,0);
            JsonPath jp = new JsonPath(message.toString());
            String subject = jp.getString("payload.headers.find { it.name == 'Subject' }.value");
            String body = new String(Base64.getDecoder().decode(jp.getString("payload.parts[0].body.data")));
            String link = null;
            String arr[] = body.split("\n");
            for(String s:arr){
                s=s.trim();
                if(s.startsWith("http")){
                    link=s.trim();
                }
            }

            HashMap<String,String> hm = new HashMap<>();
            hm.put("subject",subject);
            hm.put("body",body);
            hm.put("link",link);

            return hm;

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }*/

    public int getTotalCountOfMail(){
        int size;
        try {
            final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
            Gmail service = new Gmail.Builder(HTTP_TRANSPORT,JSON_FACTORY,getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            List<Thread> threads = service
                    .users()
                    .threads()
                    .list("me")
                    .execute()
                    .getThreads();

            size = threads.size();
            Log.e("count", String.valueOf(size));
        }catch (Exception e){
            Log.e("count",e.toString());
            Log.e("count",CREDENTIALS_FILE_PATH);
            e.printStackTrace();
            size=-1;
        }
        return size;

    }

    public List<Message> getMessages(ListMessagesResponse response){
        List<Message> messages = new ArrayList<>();

        try {

            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Gmail service = new Gmail.Builder(HTTP_TRANSPORT,JSON_FACTORY,getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            while (response.getMessages()!=null){
                messages.addAll(response.getMessages());
                if(response.getNextPageToken()!=null) {
                    String pageToken = response.getNextPageToken();
                    response = service.users().messages().list(USER_ID).setPageToken(pageToken).execute();
                }else {
                    break;
                }
            }

            return messages;
        }catch (Exception e){
            Log.e("e",e.toString());
            return messages;
        }
    }
}
