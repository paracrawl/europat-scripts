package patentdata.utils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import patentdata.utils.Config.ConfigInfo;

public class PatentData {

	protected Common common = new Common();
	protected Config config = null;
	protected ConfigInfo configInfo = null;

	protected Log log = null;
	protected Connector connector = null;

	private void initial(String path) throws Exception {
		try {
		} catch (Exception e) {
			throw new Exception("initial failed. " + e.getMessage());
		}
	}

	public PatentData() throws Exception {
	}

	public PatentData(String path) throws Exception {
		initial(path);
	}

	public String SearchPatents(String service, String[] constituents, String datePattern, Date dateBegin, Date dateEnd,
			Integer rangeBegin, Integer rangeEnd, String countryCode) throws Exception {
		String contents = "";
		StringBuilder sbLink = new StringBuilder();
		try {
			sbLink = new StringBuilder(configInfo.ServiceURL).append("/").append(service).append("/search");
			if (null != constituents)
				sbLink.append("/").append(String.join(",", constituents));

			StringBuilder sbCql = new StringBuilder().append("pd=\"").append(formatDate(dateBegin, datePattern))
					.append((null != dateBegin && null != dateEnd ? ":" : "")).append(formatDate(dateEnd, datePattern))
					.append("\"");
			if (!StringUtils.isEmpty(countryCode))
				sbCql.append(" ").append(countryCode);

			sbCql = new StringBuilder(URLEncoder.encode(sbCql.toString(), StandardCharsets.UTF_8.toString()))
					.append(formatRange(rangeBegin, rangeEnd, "&Range="));
			contents = getContent(new URL(formatUrl(sbLink.toString()) + "?q=" + sbCql.toString()));
		} catch (Exception e) {
			log.printErr(e, sbLink.toString());
		}
		return contents;
	}

	public String ListFamily(String referenceType, String inputFormat, String documentNumber, String[] constituents)
			throws Exception {
		String contents = "";
		StringBuilder sbLink = new StringBuilder();
		try {
			sbLink.append(configInfo.ServiceURL).append("/family").append("/").append(referenceType).append("/")
					.append(inputFormat).append("/").append(documentNumber);
			if (null != constituents)
				sbLink.append("/").append(String.join(",", constituents));
			contents = getContent(formatUrl(sbLink.toString()));
		} catch (Exception e) {
			log.printErr(e, sbLink.toString());
		}
		return contents;
	}

	public String GetPatentsData(String service, String referenceType, String inputFormat, String[] documentNumber,
			String endpoint, String[] constituents) throws Exception {
		String contents = "";
		StringBuilder sbLink = new StringBuilder();
		try {
			sbLink = new StringBuilder(configInfo.ServiceURL).append("/").append(service).append("/")
					.append(referenceType).append("/").append(inputFormat).append("/")
					.append(String.join(",", documentNumber));
			if (!StringUtils.isEmpty(endpoint))
				sbLink.append("/").append(endpoint);
			if (null != constituents)
				sbLink.append("/").append(String.join(",", constituents));
			contents = getContent(formatUrl(sbLink.toString()));
		} catch (Exception e) {
			log.printErr(e, sbLink.toString());
		}
		return contents;
	}

	public String GetPatentsFileData(String referenceType, String inputFormat, String[] documentNumber)
			throws Exception {
		String contents = "";
		StringBuilder sbLink = new StringBuilder();
		try {
			sbLink = new StringBuilder(configInfo.ServiceURL).append("/").append(Service.PUBLISHED.getServiceName())
					.append("/").append(referenceType).append("/").append(inputFormat).append("/")
					.append(String.join(",", documentNumber)).append("/").append(PUBLISHED_ENDPOINT.images);
			contents = getContent(formatUrl(sbLink.toString()));
		} catch (Exception e) {
			log.printErr(e, sbLink.toString());
		}
		return contents;
	}

	public String DownloadPatentFile(String countryCode, String number, String kindCode, Integer pageNo,
			String outputPath, String extension) throws Exception {
		String res = "";
		StringBuilder sbLink = new StringBuilder();
		try {
			sbLink = new StringBuilder(configInfo.ServiceURL).append("/").append(Service.PUBLISHED.getServiceName())
					.append("/").append(PUBLISHED_ENDPOINT.images).append("/").append(countryCode).append("/")
					.append(number).append("/").append(kindCode).append("/fullimage").append("?Range=").append(pageNo);

			URL url = formatUrl(sbLink.toString());
			HttpResponse httpResponse = connector.goTo(url.toString());
			if (HttpStatus.SC_BAD_REQUEST == httpResponse.getStatusLine().getStatusCode()) {
				connector.getToken();
				httpResponse = connector.goTo(url.toString());
			}

			if (HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode()) {
				InputStream inputStream = httpResponse.getEntity().getContent();
				String filename = countryCode + number + kindCode + "-" + pageNo + "." + extension;
				Files.copy(inputStream, new File(outputPath, filename).toPath());
				res = "downloaded success";
			} else {
				res = "downloaded failed";
			}
		} catch (Exception e) {
			log.printErr(e, sbLink.toString());
		}
		return res;
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
		return new SimpleDateFormat(pattern).format(date);
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

	private String getContent(URL url) throws Exception {
		HttpResponse httpResponse = connector.goTo(url.toString());
		String output = connector.toString(httpResponse.getEntity().getContent());
		if (HttpStatus.SC_OK != httpResponse.getStatusLine().getStatusCode()) {
			if (output.contains("<message>invalid_access_token</message>")) {
				connector.getToken();
				output = connector.toString(connector.goTo(url.toString()).getEntity().getContent());
			} else if (output.contains("<code>CLIENT.RobotDetected</code>")) {
				Integer n = 3;
				log.print(String.format("CLIENT.RobotDetected. Wait %d seconds to reconnect...", n));
				TimeUnit.SECONDS.sleep(n);
				return getContent(url);
			}
		}
		return output;
	}

}
