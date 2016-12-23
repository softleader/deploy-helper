package tw.com.softleader.dh.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {

	@JsonProperty
	private boolean keepBackUpFile; // 是否保留備份檔

	@JsonProperty
	private TomcatType tomcatType; // Tomcat類型(service, bat)

	@JsonProperty
	private String warPath; // war路徑

	@JsonProperty
	private String backupPath; // backup路徑

	@JsonProperty
	private String tomcatPath; // Tomcat路徑

	@JsonProperty
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

	public String getWarPath() {
		return warPath;
	}

	public void setWarPath(String warPath) {
		this.warPath = warPath;
	}

	public String getBackupPath() {
		return backupPath;
	}

	public void setBackupPath(String backupPath) {
		this.backupPath = backupPath;
	}

}
