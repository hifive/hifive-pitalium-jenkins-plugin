package hudson.plugins.pitalium.junitattachments;

public class PtlJsonCapabilities {
private String browserName="";
private String platform="";
private String version="";
private String os="";
@Override
public String toString(){
	return "Capabilities [{os="+platform+"_"+browserName+"_"+version;
}

public String getName(){
	return platform+"_"+browserName;
}

public String getFolderName(){
	//like os=WINDOWS, browserName=internet explorer, version=9, platform=WINDOWS
	String addr="";
	if (os!="") addr="os="+os+", ";
	if (browserName!="") addr=addr+"browserName="+browserName+", ";
	if (version!="") addr=addr+"version="+version+", ";
	if (platform!="") addr=addr+"platform="+platform;
	return addr;

}
}
