package hudson.plugins.pitalium.junitattachments;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.test.TestObject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;

public class PtlResultSummaryPublisher  extends TestDataPublisher {

    @DataBoundConstructor
    public PtlResultSummaryPublisher(String resultPicsAddr) {

    }

    @Override
    public Data contributeTestData(Run<?, ?> build, FilePath workspace, Launcher launcher,
                                   TaskListener listener, TestResult testResult) throws IOException,
            InterruptedException {
    	//結果のXMLをすべてよみこみ，結果ファイルを生成
        PtlMakeJson jsonData=new PtlMakeJson();
        for (SuiteResult suiteResult : testResult.getSuites()) {
        	for (CaseResult caseResult : suiteResult.getCases()) {
        		PtlCaseResult pitaResult=new PtlCaseResult();
        		String str = caseResult.getName();
        		pitaResult.setTestName(str);
    			//テスト環境情報
        		Pattern p = Pattern.compile("\\{(.+)\\}\\]\\]$");
        		Matcher m = p.matcher(str);
        		if(m.find()){
        			HashMap<String,String> env = new HashMap<String,String>();
        			str=m.group(1);
            		p = Pattern.compile(",\\s+");
            		String[] option=p.split(str);
            		for (String str1 : option) {
            			p = Pattern.compile("\\s?=\\s?");
            			String[] res=p.split(str1);
            			env.put(res[0],res[1]);
            		}
            		pitaResult.setEnvironment(env);
        		}
        		if(caseResult.isFailed()){
            		//テスト結果情報
        			pitaResult.setFailed();
            		String trace=caseResult.getErrorStackTrace();
            		String[] traceLine=trace.split("\n");
            		pitaResult.setErrName(traceLine[0].split(":")[0]);
        			p = Pattern.compile("\\A\\t?at\\s?(.+)");
            		for(String line:traceLine){
            			m = p.matcher(line);
                		if(m.find()){
                			pitaResult.setErrLocation(m.group(1));
                			break;
                		}
            		}
        		}
        		jsonData.putList(pitaResult);
            }
        }
        //Gen file build/PitaResults.txtワークスペースに生成
        jsonData.makeJson(new FilePath(new File(workspace.getRemote())));
    	return new Data("foo");

    }
    public static class Data extends TestResultAction.Data {
    	public Data(String str) {

        }
    	@Override
        @SuppressWarnings("deprecation")
        public List<TestAction> getTestAction(hudson.tasks.junit.TestObject t) {
    		TestObject testObject = (TestObject) t;
    		if (testObject instanceof ClassResult) {
    			return Collections.emptyList();
            } else if (testObject instanceof CaseResult) {
            	return Collections.emptyList();
            } else {
                // Otherwise, at the package level
            }
    		PtlResultSummaryAction action = new PtlResultSummaryAction();
    		return Collections.<TestAction> singletonList(action);
    	}
    }
    @Extension
    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {

        @Override
        public String getDisplayName() {
            return "Pitalium Test Result Summary";
        }

    }
}
