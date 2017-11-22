package hudson.plugins.pitalium.junitattachments;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.junitattachments.AttachmentPublisher;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;

public class PtlAttachmentPublisher extends AttachmentPublisher {
	private final String resultPicsAddr;

    @DataBoundConstructor
    public PtlAttachmentPublisher(String resultPicsAddr) {
    	this.resultPicsAddr=resultPicsAddr;
    }

    public String getResultPicsAddr(){
    	return this.resultPicsAddr;
    }

    @Override
    public Data contributeTestData(Run<?, ?> build, FilePath workspace, Launcher launcher,
                                   TaskListener listener, TestResult testResult) throws IOException,
            InterruptedException {
        final GetPtlTestDataMethodObject methodObject = new GetPtlTestDataMethodObject(build, workspace, launcher, listener, testResult);
        Map<String, Map<String, List<String>>> attachments = methodObject.getAttachments();

        if (attachments.isEmpty()) {
            return null;
        }

        return new Data(attachments);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {

        @Override
        public String getDisplayName() {
            return "Pitalium Test Result Publisher";
        }

    }

}
