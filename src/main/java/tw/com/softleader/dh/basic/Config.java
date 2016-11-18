package tw.com.softleader.dh.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {

	@JsonProperty(required=false)
	private boolean keepBackUpFile; // 是否保留備份檔

	@JsonProperty(required=false)
	private TomcatType tomcatType; // Tomcat類型(service, bat)

	@JsonProperty(required=false)
	private String tomcatPath; // Tomcat路徑

	@JsonProperty(required=false)
	private String tomcatServiceName; // Tomcat Service名稱(如有選tomcatType=service)

	public void verify() throws VerifyException {
		final List<String> msgs = new ArrayList<>();

		if (TomcatType.service.equals(tomcatType) && tomcatServiceName.isEmpty()) {
			msgs.add("當Tomcat類型選擇service時, ServiceName為必填");
		}

		if (!msgs.isEmpty()) {
			throw new VerifyException(msgs);
		}
	}

	public boolean isKeepBackUpFile() {
		return keepBackUpFile;
	}

	public void setKeepBackUpFile(final boolean keepBackUpFile) {
		this.keepBackUpFile = keepBackUpFile;
	}

	public TomcatType getTomcatType() {
		return tomcatType;
	}

	public void setTomcatType(final TomcatType tomcatType) {
		this.tomcatType = tomcatType;
	}

	public String getTomcatPath() {
		if (this.tomcatPath != null && !this.tomcatPath.isEmpty()) {
			return this.tomcatPath;
		} else {
			return Optional.ofNullable(System.getenv("TOMCAT_HOME")).orElse(null);
		}
	}

	public void setTomcatPath(final String tomcatPath) {
		this.tomcatPath = tomcatPath;
	}

	public String getTomcatServiceName() {
		return tomcatServiceName;
	}

	public void setTomcatServiceName(final String tomcatServiceName) {
		this.tomcatServiceName = tomcatServiceName;
	}

}
