package org.hwbot.bench.service;

import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

public class SubmitWorker implements Callable<String> {

	private final String xml;

	public SubmitWorker(String xml) {
		this.xml = xml;
	}

	public String call() throws Exception {

		HttpParams httpParameters = new BasicHttpParams();
		int timeoutConnection = 20;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection * 1000);
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutConnection * 1000);
		HttpClient httpclient = new DefaultHttpClient(httpParameters);
		try {
			// Create a response handler
			// BasicResponseStatusHandler responseHandler = new BasicResponseStatusHandler();
			HttpPost req = new HttpPost("http://uat.hwbot.org/submit/api?client=BenchBot&clientVersion=1.0.0");
			MultipartEntity mpEntity = new MultipartEntity();
			// System.out.println(xml);
			// byte[] bytes = encrypt("AES/CBC/PKCS5Padding", xml.getBytes("utf8"));
			// System.out.println("encrypted: " + new String(bytes));
			// mpEntity.addPart("data", new ByteArrayBody(bytes, "data"));
			mpEntity.addPart("data", new StringBody(xml));
			req.setEntity(mpEntity);

			BasicResponseStatusHandler responseHandler = new BasicResponseStatusHandler();
			String response = httpclient.execute(req, responseHandler);
			String status = StringUtils.substringBetween(response, "<status>", "</status>");

			if ("success".equals(status)) {
				String url = StringUtils.substringBetween(response, "<url>", "</url>");
				return url;
			} else {
				Log.i(this.getClass().getName(), "Failed to submit score. Status was: " + status);
				Log.i(this.getClass().getName(), response);
			}

		} catch (HttpHostConnectException e) {
			Log.i(this.getClass().getName(), "Failed to connect to HWBOT server! Are you connected to the internet?");
			e.printStackTrace();
		} catch (Exception e) {
			Log.i(this.getClass().getName(),
					"Error communicating with online service. If this issue persists, please contact HWBOT crew. Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			httpclient.getConnectionManager().shutdown();
		}

		return null;

		// try {
		// Log.i(this.getClass().getName(), xml);
		//
		// String data = "data=" + xml;
		// URL url = new URL("http://uat.hwbot.org/data/api?client=BenchBot&version=1.0.0");
		// // URL url = new URL("http://google.com");
		// URLConnection conn;
		// conn = url.openConnection();
		// // conn.setDoOutput(true);
		// conn.setDoInput(true);
		// OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		// wr.write(data);
		// wr.flush();
		// // Get the response
		// BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		// String line;
		// while ((line = rd.readLine()) != null) {
		// // Process line...
		// }
		// wr.close();
		// rd.close();
		// Log.i(this.getClass().getName(), rd.toString());
		//
		// return rd.toString();
		// } catch (Throwable e) {
		// e.printStackTrace();
		// Log.e(this.getClass().getName(), e.getMessage());
		// return "error: " + e.getMessage();
		// }
	}

}
