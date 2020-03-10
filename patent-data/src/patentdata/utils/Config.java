package patentdata.utils;

import org.json.JSONObject;

/**
 * @author MickeyVI
 */
public class Config {

	private Common common = new Common();
	private String _path = "";
	public ConfigInfo _config = new ConfigInfo();

	public Config(String path) throws Exception {
		_path = path;
		load();
	}

	private void load() throws Exception {
		if (!common.IsExist(_path)) {
			return;
		}

		String sConfig = common.readFile(_path);

		if (common.IsEmpty(sConfig)) {
			return;
		}

		JSONObject json = common.getJSONObject(sConfig);

		_config = new ConfigInfo();
		_config.ConsumerKey = common.getJSONValue(json, "ConsumerKey");
		_config.ConsumerSecret = common.getJSONValue(json, "ConsumerSecretKey");
		_config.Protocol = common.getJSONValue(json, "Protocol");
		_config.Host = common.getJSONValue(json, "Host");
		_config.AuthenURL = _config.Protocol + "://" + _config.Host + "/" + common.getJSONValue(json, "AuthenURL");
		_config.ServiceURL = _config.Protocol + "://" + _config.Host + "/" + common.getJSONValue(json, "ServiceURL");
		_config.WorkingDir = common.getJSONValue(json, "WorkingDir");
		_config.AccessToken = common.getJSONValue(json, "AccessToken");
		_config.Jdbc = common.getJSONValue(json, "Jdbc");
		_config.DbDriver = common.getJSONValue(json, "DbDriver");
		_config.DbHost = common.getJSONValue(json, "DbHost");
		_config.DbPort = common.getJSONValue(json, "DbPort");
		_config.DbSchema = common.getJSONValue(json, "DbSchema");
		_config.DbUser = common.getJSONValue(json, "DbUser");
		_config.DbPassword = common.getJSONValue(json, "DbPassword");
		_config.CCPath = common.getJSONValue(json, "CCPath");
	}

	public void updateToken(String newToken) {
		_config.AccessToken = newToken;

		String sConfig;
		try {
			sConfig = common.readFile(_path);
			JSONObject json = common.getJSONObject(sConfig);
			json.put("AccessToken", newToken);
			common.WriteFile(_path, json.toString(2));
		} catch (Exception e) {

		}
	}

	public class ConfigInfo {
		public String ConsumerKey = "";
		public String ConsumerSecret = "";
		public String Protocol = "";
		public String Host = "";
		public String AuthenURL = "";
		public String AccessToken = "";
		public String ServiceURL = "";
		public String WorkingDir = "";
		public String DbDriver = "";
		public String Jdbc = "";
		public String DbHost = "";
		public String DbPort = "";
		public String DbSchema = "";
		public String DbUser = "";
		public String DbPassword = "";
		public String CCPath = "";
	}

}