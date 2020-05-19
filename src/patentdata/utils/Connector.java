package patentdata.utils;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import patentdata.utils.Config.ConfigInfo;

public class Connector {

	protected Common common = new Common();
	protected Config config = null;
	protected ConfigInfo configInfo = null;
	protected Log log = null;

	public Connector(Config config) throws Exception {
		this.config = config;
		configInfo = config._config;

		log = new Log(configInfo.WorkingDir);
	}

	/* HTTP */
	public HttpResponse goTo(String url) throws Exception {
		HttpResponse response = null;
		try {
			if (common.IsEmpty(configInfo.AccessToken))
				getToken();

			URI uri = new URI(url);
			HttpGet request = new HttpGet(uri);
			request.addHeader("Authorization", "Bearer " + configInfo.AccessToken);

			HttpClient client = HttpClients.createDefault();
			response = client.execute(request);
			log.print(String.format("%s : %s", response.getStatusLine(), url));
		} catch (Exception e) {
			log.printErr(e);
			throw e;
		}
		return response;
	}

	public void getToken() throws Exception {
		CloseableHttpResponse response = null;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			/* prepare data for HttpPost */
			URI uri = new URI(configInfo.AuthenURL);

			String auth = Base64
					.encodeBase64String((configInfo.ConsumerKey + ":" + configInfo.ConsumerSecret).getBytes());

			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("grant_type", "client_credentials"));

			/* set HttpPost to get HttpResponse */
			HttpPost request = new HttpPost(uri);
			request.addHeader("Authorization", "Basic " + auth);
			request.addHeader("Content-Type", "application/x-www-form-urlencoded");
			request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			response = client.execute(request);

			String output = toString(response.getEntity().getContent());

			JSONObject jToken = common.getJSONObject(output);
			config.updateToken(common.getJSONValue(jToken, "access_token"));
		} catch (Exception e) {
			log.printErr(e);
			throw e;
		} finally {
			response.close();
		}
	}

	public String toString(InputStream inputStream) throws Exception {
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer, "UTF-8");
		return writer.toString().trim();
	}

	/* DB */
	public Connection get(Connection con) throws Exception {
		try {
			if (con == null || con.isClosed()) {
				String sJdbc = configInfo.Jdbc;
				String sDriver = configInfo.DbDriver;// ("com.mysql.cj.jdbc.Driver");
				StringBuilder sbUrl = new StringBuilder(sJdbc).append("://").append(configInfo.DbHost).append(":")
						.append(configInfo.DbPort).append("/").append(configInfo.DbSchema);
				Class.forName(sDriver);
				con = DriverManager.getConnection(sbUrl.toString(), configInfo.DbUser, configInfo.DbPassword);
			}
		} catch (Exception e) {
			log.printErr(e);
		}
		return con;
	}

	public void close(ResultSet rs) throws Exception {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
			}
		}
	}

	public void close(PreparedStatement pstmt) throws Exception {
		if (pstmt != null) {
			try {
				pstmt.close();
			} catch (SQLException e) {
			}
		}
	}

	public void close(Connection con) throws Exception {
		if (con != null) {
			try {
				con.close();
			} catch (SQLException e) {
			}
		}
	}
}
