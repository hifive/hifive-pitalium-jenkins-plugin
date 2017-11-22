package hudson.plugins.pitalium.junitattachments;

import java.util.Map;

//テストケースについて結果を格納
public class PtlCaseResult {
	public boolean failed=false;

	public String testName="";
	public String errName="";
	public String errLocation="";

	public String os="";
	public String browser="";
	public String browserVersion="";

	private static final String KEY_OS="platform";
	private static final String KEY_BROWSER="browserName";
	private static final String KEY_VERSION="platform";

	public boolean isFailed() {
		return failed;
	}

	public String getTestName() {
		return testName;
	}

	public String getErrName() {
		return errName;
	}

	public String getErrLocation() {
		return errLocation;
	}

	public String getOs() {
		return os;
	}

	public String getBrowser() {
		return browser;
	}

	public String getBrowserVersion() {
		return browserVersion;
	}
	public void setFailed(){
		failed=true;
	}
	public void setTestName(String value){
		testName=value;
	}
	public void setErrName(String value){
		errName=value;
	}
	public void setErrLocation(String value){
		errLocation=value;
	}

	public void setEnvironment(Map<String,String> env){
		for(Map.Entry<String, String> e : env.entrySet()) {
		    String key=e.getKey();
		    String value=e.getValue();
			if(key.equals(KEY_OS))this.os=value;
			else if(key.equals(KEY_BROWSER))this.browser=value;
			else if(key.equals(KEY_VERSION))this.browserVersion=value;
		}
	}



}
