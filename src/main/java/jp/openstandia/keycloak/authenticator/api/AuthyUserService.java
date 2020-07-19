package jp.openstandia.keycloak.authenticator.api;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;
import com.fasterxml.jackson.databind.ObjectMapper;


import javax.net.ssl.HttpsURLConnection;

import org.jboss.logging.Logger;

import jdk.incubator.http.HttpResponse;


public class AuthyUserService {
    private static final Logger logger = Logger.getLogger(SMSSendVerify.class.getPackage().getName());
    
    public static final String DEFAULT_API_URI = "https://api.authy.com";
    public static final String PHONE_VERIFICATION_API_PATH = "/protected/json/users/new";
    
    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";
    
    private final String apiKey;
    
    ObjectMapper mapper = new ObjectMapper();
    
    
    public AuthyUserService(String apiKey){
        this.apiKey = apiKey;
    }
    
    public createAuthyUser(String email, String phoneNumber){
        AuthyUserParams data = new AuthyUserParams();
        data.setAttribute("email", email);
        data.setAttribute("phone_number", phoneNumber);
        data.setAttribute("country_code", "966"); // Saudi Arabia
        data.setAttribute("send_install_link_via_sms", "false"); // don't send Authy app installation link
        
        return request(METHOD_POST, PHONE_VERIFICATION_API_PATH, data);
    }
    
    private String createAuthyUserRequest(String method, String path, Params data) {
        boolean result = false;
        
        HttpsURLConnection conn;
        InputStream in = null;
        BufferedReader reader = null;
        try {
            StringBuilder sb = new StringBuilder();
            
            URL url = new URL(DEFAULT_API_URI + path + sb.toString());
            
            HttpURLConnection httpUrlConnection = null;
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setRequestMethod(method);
            httpUrlConnection.setRequestProperty("Accept", "application/json");
            httpUrlConnection.setRequestProperty("Content-Type", "application/json");
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.setRequestProperty("X-Authy-API-Key", apiKey); // API-KEY
            
            writeJson(conn, data);
            final int resStatus = httpUrlConnection.getResponseCode();
            logger.infov("RESPONSE STATUS : {0}", resStatus);
            
            if (resStatus == HttpURLConnection.HTTP_OK) {
                in = conn.getInputStream();
                CreateUserResponseDTO userResponse = objectMapper.readValue(in, CreateUserResponseDTO.class);
                reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.infov("RESPONSE DETAIL : {0}", line);
                    System.out.println(line);
                }
                result = userResponse.user.id;
            }
            
        } catch (IOException e) {
            logger.error(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
        
        return result;
    }
    
    private void writeJson(HttpURLConnection connection, Params data) {
        if (data == null) {
            return;
        }
        
        OutputStream os = null;
        BufferedWriter output = null;
        try {
            os = connection.getOutputStream();
            output = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            output.write(data.toJSON());
            output.flush();
            output.close();
        } catch (IOException e) {
            logger.error(e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
        
    }
    
    
}