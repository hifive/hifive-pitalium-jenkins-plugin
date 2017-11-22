package hudson.plugins.pitalium.junitattachments;




public class PtlJsonScreenshotResult {
    private String screenshotId="";
    private String testMethod="";
    private String testClass="";
    private PtlJsonCapabilities capabilities;

    public String getTestClass() {
		return testClass;
	}


    @Override
    public String toString(){
    	return testClass+"."+testMethod+capabilities;
    }

    public String[] getFileName(){
    	String capability=capabilities.getName();
    	String addr[]=new String[2];
    	addr[0]=testClass;
    	addr[1]=testMethod+"_"+screenshotId+"_"+capability;
    	return addr;
    }
    public String getFolderName(){
    	//like compareScrollableTextareaElement[Capabilities [{os=WINDOWS, browserName=internet explorer, version=9, platform=WINDOWS}]
    	//前方一致で検索
    	return testMethod+"[Capabilities [{"+capabilities.getFolderName();
    }
}
