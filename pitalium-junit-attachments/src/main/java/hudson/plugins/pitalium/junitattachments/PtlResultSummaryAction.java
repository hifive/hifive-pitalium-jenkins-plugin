package hudson.plugins.pitalium.junitattachments;

import hudson.tasks.junit.TestAction;

public class PtlResultSummaryAction  extends TestAction {

	public PtlResultSummaryAction() {

	}

	public String getDisplayName() {
		return "Attachments";
	}

	public String getIconFileName() {
		return "package.gif";
	}

	public String getUrlName() {
		return "attachments";
	}
	@Override
	public String annotate(String text) {
	return "foo";
	}

}
