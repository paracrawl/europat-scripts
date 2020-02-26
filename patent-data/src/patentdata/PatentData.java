package patentdata;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import patentdata.Config.ConfigInfo;

@SuppressWarnings("deprecation")
public class PatentData {

	private Common common = new Common();
	private Config config = null;
	private ConfigInfo configInfo = null;

	private void initial() throws Exception {
		try {
			config = new Config(common.getConfigPath());
			configInfo = config._config;
		} catch (Exception e) {
			throw new Exception("initial failed. " + e.getMessage());
		}
	}

	public PatentData() throws Exception {
		initial();
	}

	public String SearchPatentsByDate(String service, String[] constituents, String datePattern, Date dateBegin,
			Date dateEnd, Integer rangeBegin, Integer rangeEnd) throws Exception {
		StringBuilder sbLink = new StringBuilder(configInfo.ServiceURL).append("/").append(service).append("/search");
		if (null != constituents)
			sbLink.append("/").append(String.join(",", constituents));

		StringBuilder sbCql = new StringBuilder().append("pd=\"").append(formatDate(dateBegin, datePattern))
				.append((null != dateBegin && null != dateEnd ? ":" : "")).append(formatDate(dateEnd, datePattern))
				.append("\"");

		sbCql = new StringBuilder(URLEncoder.encode(sbCql.toString(), StandardCharsets.UTF_8.toString()))
				.append(formatRange(rangeBegin, rangeEnd, "&Range="));

		return getContent(new URL(formatUrl(sbLink.toString()) + "?q=" + sbCql.toString()));
	}

	public String ListFamily(String referenceType, String inputFormat, String documentNumber, String[] constituents)
			throws Exception {
		StringBuilder sbLink = new StringBuilder(configInfo.ServiceURL).append("/family").append("/")
				.append(referenceType).append("/").append(inputFormat).append("/").append(documentNumber);
		if (null != constituents)
			sbLink.append("/").append(String.join(",", constituents));
		return getContent(formatUrl(sbLink.toString()));
	}

	public String GetPatentsData(String service, String referenceType, String inputFormat, String[] documentNumber,
			String endpoint, String[] constituents) throws Exception {
		StringBuilder sbLink = new StringBuilder(configInfo.ServiceURL).append("/").append(service).append("/")
				.append(referenceType).append("/").append(inputFormat).append("/")
				.append(String.join(",", documentNumber));
		if (!StringUtils.isEmpty(endpoint))
			sbLink.append("/").append(endpoint);
		if (null != constituents)
			sbLink.append("/").append(String.join(",", constituents));
		return getContent(formatUrl(sbLink.toString()));
	}

	public String GetPatentsFileData(String referenceType, String inputFormat, String[] documentNumber)
			throws Exception {
		StringBuilder sbLink = new StringBuilder(configInfo.ServiceURL).append("/")
				.append(Service.PUBLISHED.getServiceName()).append("/").append(referenceType).append("/")
				.append(inputFormat).append("/").append(String.join(",", documentNumber)).append("/")
				.append(PUBLISHED_ENDPOINT.images);
		return getContent(formatUrl(sbLink.toString()));
	}

	public String DownloadPatentFile(String countryCode, String number, String kindCode, Integer pageNo,
			String outputPath, String extension) throws Exception {
		StringBuilder sbLink = new StringBuilder(configInfo.ServiceURL).append("/")
				.append(Service.PUBLISHED.getServiceName()).append("/").append(PUBLISHED_ENDPOINT.images).append("/")
				.append(countryCode).append("/").append(number).append("/").append(kindCode).append("/fullimage")
				.append("?Range=").append(pageNo);

		URL url = formatUrl(sbLink.toString());
		HttpResponse httpResponse = goTo(url.toString());
		if (HttpStatus.SC_BAD_REQUEST == httpResponse.getStatusLine().getStatusCode()) {
			getToken();
			httpResponse = goTo(url.toString());
		}

		if (HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode()) {
			InputStream inputStream = httpResponse.getEntity().getContent();
			String filename = countryCode + number + kindCode + "-" + pageNo + "." + extension;
			Files.copy(inputStream, new File(outputPath, filename).toPath());
			return "downloaded success";
		} else {
			return "downloaded failed";
		}
	}

	public enum REF_TYPE {
		publication, application, priority
	}

	public enum INPUT_FORMAT {
		docdb, epodoc
	}

	public enum PUBLISHED_ENDPOINT {
		fulltext, claims, description, images
	}

	public enum Service {
		PUBLISHED("published-data"), REGISTER("register");

		private String serviceName;

		Service(String serviceName) {
			this.serviceName = serviceName;
		}

		public String getServiceName() {
			return serviceName;
		}
	}

	public interface Constituents {
		String getConstituent();
	}

	public enum Published implements Constituents {
		_biblio {
			@Override
			public String getConstituent() {
				return "biblio";
			}
		},
		_abstract {
			@Override
			public String getConstituent() {
				return "abstract";
			}
		},
		_fullcycle {
			@Override
			public String getConstituent() {
				return "full-cycle";
			}
		}
	}

	public enum Register implements Constituents {
		_biblio {
			@Override
			public String getConstituent() {
				return "biblio";
			}
		},
		_events {
			@Override
			public String getConstituent() {
				return "events";
			}
		},
		_procedural {
			@Override
			public String getConstituent() {
				return "procedural-steps";
			}
		}
	}

	public enum Family implements Constituents {
		_biblio {
			@Override
			public String getConstituent() {
				return "biblio";
			}
		},
		_legal {
			@Override
			public String getConstituent() {
				return "legal";
			}
		}
	}

	public String formatDate(Date date, String pattern) throws Exception {
		if (null == date)
			return "";
		SimpleDateFormat formatter = new SimpleDateFormat(pattern);
		return formatter.format(date);
	}

	public String formatRange(Integer rangeBegin, Integer rangeEnd) throws Exception {
		return formatRange(rangeBegin, rangeEnd, "");
	}

	public String formatRange(Integer rangeBegin, Integer rangeEnd, String criteria) throws Exception {
		StringBuilder sbRange = new StringBuilder();
		boolean hasRangeBegin = Optional.ofNullable(rangeBegin).orElse(0) != 0;
		boolean hasRangeEnd = Optional.ofNullable(rangeEnd).orElse(0) != 0;
		if (hasRangeBegin || hasRangeEnd) {
			sbRange.append(criteria);
			if (!hasRangeBegin) {
				sbRange.append("1-").append(rangeEnd);
			} else if (!hasRangeEnd) {
				sbRange.append(rangeBegin);
			} else {
				sbRange.append(rangeBegin).append("-").append(rangeEnd);
			}
		}
		return sbRange.toString();
	}

	public URL formatUrl(String sUrl) throws Exception {
		return new URL(sUrl.replaceAll("(?i)(?<!(http:|https:))/+", "/"));
	}

	private String toString(InputStream inputStream) throws Exception {
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer, "UTF-8");
		return writer.toString().trim();
	}

	private String getContent(URL url) throws Exception {
		String output = toString(goTo(url.toString()).getEntity().getContent());
		if (output.contains("<message>invalid_access_token</message>")) {
			getToken();
			output = toString(goTo(url.toString()).getEntity().getContent());
		}
		return output;
	}

	private HttpResponse goTo(String url) throws Exception {
		HttpResponse httpResponse = null;
		try {
			if (common.IsEmpty(configInfo.AccessToken))
				getToken();

			URI uri = new URI(url);
			HttpGet httpRequest = new HttpGet(uri);
			httpRequest.addHeader("Authorization", "Bearer " + configInfo.AccessToken);

			HttpHost target = new HttpHost(uri.getHost(), -1, uri.getScheme());
			httpResponse = new DefaultHttpClient().execute(target, httpRequest);
			System.out.println(String.format("%s : %s", httpResponse.getStatusLine(), url));
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return httpResponse;
	}

	private void getToken() throws Exception {
		try {
			/* prepare data for HttpPost */
			URI uri = new URI(configInfo.AuthenURL);

			String auth = Base64
					.encodeBase64String((configInfo.ConsumerKey + ":" + configInfo.ConsumerSecret).getBytes());

			HttpParams params = new BasicHttpParams();
			params.setParameter("grant_type", "client_credentials");

			ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			postParameters.add(new BasicNameValuePair("grant_type", "client_credentials"));

			HttpHost target = new HttpHost(uri.getHost(), -1, uri.getScheme());

			/* set HttpPost to get HttpResponse */
			HttpPost httpRequest = new HttpPost(uri);
			httpRequest.addHeader("Authorization", "Basic " + auth);
			httpRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
			httpRequest.setParams(params);
			httpRequest.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
			HttpResponse httpResponse = new DefaultHttpClient().execute(target, httpRequest);

			String output = toString(httpResponse.getEntity().getContent());

			JSONObject jToken = common.getJSONObject(output);
			// configInfo.AccessToken = common.getJSONValue(jToken, "access_token");
			config.updateToken(common.getJSONValue(jToken, "access_token"));
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

}
