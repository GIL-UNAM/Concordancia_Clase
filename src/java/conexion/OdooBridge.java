/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexion;

import static conexion.ClientIPAddress.getClientIpAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author JSolorzanoS
 */
public class OdooBridge {

    public static final String LOGIN_URL = "/web/login";
    public static final String LOGOUT_URL = "/api/logout";
    public static final String REGISTER_URL = "/register";
    public static final String USER_URL = "/api/user";
    public static final String LOG_URL = "/api/log";
    public static final String DIR_URL = "/api/directory";
    public static final String ZIP_URL = "/api/directory/download";
    public static final String DOC_URL = "/api/document";
    public static final String PROJECT_URL = "/api/project";
    public static final String PROJECT_DOCS_URL = "/api/project/documents";
    public static final String PROJECT_METADATA_URL = "/api/project/metadata";
    public static final String COOKIE = "session_id";
    private static final String DOMAIN = "http://www.corpus.unam.mx";

    private static String getOdooUrl() {
        //String host = System.getenv("DOMAIN") + ":8069";
        String host = DOMAIN+":8069";
        return host;
    }

    private static void setCookie(HttpServletRequest request, HttpURLConnection conn) {
        Cookie[] cookies = request.getCookies();
        String sid = "";
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(COOKIE)) {
                    sid = cookie.getValue();
                }
            }
            conn.setRequestProperty("Cookie", COOKIE + "=" + sid);
        }
    }

    private static HttpURLConnection getConnection(HttpServletRequest request, String action, String method) throws Exception {
        URL url = new URL(getOdooUrl() + action);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        setCookie(request, conn);
        return conn;
    }

    public static JSONObject getUser(HttpServletRequest request) throws Exception {
        HttpURLConnection conn = getConnection(request, USER_URL, "GET");
        conn.setDoOutput(true);
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        String jsonStr = in.readLine();
        return (JSONObject) JSONValue.parse(jsonStr);
    }

    public static void logout(HttpServletRequest request) throws Exception {
        HttpURLConnection conn = getConnection(request, LOGOUT_URL, "POST");
        conn.getResponseCode();
    }

    public static String getURLWithRedirect(HttpServletRequest request, String url) {
        url = getOdooUrl() + url;
        // gch String currentURL = System.getenv("DOMAIN") + request.getContextPath();
        String currentURL = "DOMAIN" + request.getContextPath();
        if(request.getPathInfo() != null){
            currentURL += request.getPathInfo();
        }
        String query = request.getQueryString();
        if (query != null) {
            currentURL += "?" + query;
        }
        try {
            url += "?redirect=" + URLEncoder.encode(currentURL, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            url += "?redirect=" + URLEncoder.encode(currentURL.toString());
        }
        System.out.println(url);
        return url;
    }

    public void logQuery(HttpServletRequest request, String origin, String query) throws Exception {
        JSONObject jsonObj = new JSONObject();
        JSONObject params = new JSONObject();
        jsonObj.put("jsonrpc", "2.0");
        jsonObj.put("method", "call");
        params.put("origin", origin);
        params.put("ip", getClientIpAddress(request));
        params.put("query", query);
        jsonObj.put("params", params);
        String jsonPayload = jsonObj.toJSONString();
        byte[] postDataBytes = jsonPayload.getBytes("UTF-8");

        HttpURLConnection conn = getConnection(request, LOG_URL, "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);
        Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
    }

    public static String downloadZip(HttpServletRequest request, int[] docIds, int projectId, boolean copyright) throws Exception {
        JSONObject jsonObj = new JSONObject();
        JSONObject params = new JSONObject();
        JSONArray docIdsJsonArray = new JSONArray();
        for (int i : docIds) {
            docIdsJsonArray.add(i);
        }
        jsonObj.put("jsonrpc", "2.0");
        jsonObj.put("method", "call");
        params.put("doc_ids", docIdsJsonArray);
        params.put("project_id", projectId);
        params.put("same_dir", true);
        params.put("processor_id", 1);
        params.put("copyright", copyright);
        jsonObj.put("params", params);
        String jsonPayload = jsonObj.toJSONString();
        byte[] postDataBytes = jsonPayload.getBytes("UTF-8");

        HttpURLConnection conn = getConnection(request, ZIP_URL, "POST");
        conn.setReadTimeout(1000 * 60 * 10);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        String jsonStr = in.readLine();
        jsonObj = (JSONObject) JSONValue.parse(jsonStr);
        JSONObject result = (JSONObject) jsonObj.get("result");
        String datas = (String) result.get("datas");
        return datas;
    }

    public static JSONArray listProjects(HttpServletRequest request, boolean includePublic) throws Exception {
        String url = PROJECT_URL;
        if (includePublic) {
            url += "?include_public=1";
        }
        HttpURLConnection conn = getConnection(request, url, "GET");
        conn.setDoOutput(true);
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        String jsonStr = in.readLine();
        return (JSONArray) JSONValue.parse(jsonStr);
    }
    
    public static JSONObject getProject(HttpServletRequest request, int projectId) throws Exception {
        String url = PROJECT_URL + "?id=" + projectId;
        HttpURLConnection conn = getConnection(request, url, "GET");
        conn.setDoOutput(true);
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        String jsonStr = in.readLine();
        return (JSONObject) ((JSONArray) JSONValue.parse(jsonStr)).get(0);
    }
    
    public static String getProjectMetadata(HttpServletRequest request, int projectId, boolean onlyHeader) throws Exception{
        String url = PROJECT_METADATA_URL + "?project_id=" + projectId;
        if(onlyHeader){
            url += "&only_header=1";
        }
        HttpURLConnection conn = getConnection(request, url, "GET");
        conn.setDoOutput(true);
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        String csv = "";
        String line = null;
        while((line = in.readLine()) != null){
            csv += line + "\n";
        }
        return csv;
    }
}
