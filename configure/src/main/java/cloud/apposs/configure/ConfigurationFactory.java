package cloud.apposs.configure;

public final class ConfigurationFactory {
	public static final int XML = 0;
	public static final int JSON = 1;
	
	public static ConfigurationParser getConfigurationParser(final int type) {
		switch(type) {
		case XML:
			return new XmlConfigParser();
		case JSON:
			return new JsonConfigParser();
		default:
			return null;
		}
	}
}
