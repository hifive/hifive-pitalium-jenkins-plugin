package org.jenkinsci.plugins;

import hudson.FilePath;
import hudson.model.DirectoryBrowserSupport;
import hudson.tasks.test.TestObject;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.PackageResult;
import jenkins.model.Jenkins;

import java.util.List;

/** テスト結果ページの表示で利用 */
public class PtlTestAction extends TestAction {

	private final FilePath storage;
	private final List<String> attachments;
	private final TestObject testObject;
	private final String showTable;
	private final String packageName;
	private final String className;

	/**
	 * コンストラクタ
	 * 
	 * @param testObject Test Object
	 * @param storage File path
	 * @param attachments List of file paths
	 */
	public PtlTestAction(TestObject testObject, FilePath storage, List<String> attachments) {
		this.storage = storage;
		this.testObject = testObject;
		// URI Encoder
		for (int i = 0; i < attachments.size(); i++) {
			attachments.set(i, attachments.get(i).replace("#", "%23"));
		}
		this.attachments = attachments;
		if (testObject instanceof CaseResult) {
			//ケースレベルではテーブルを表示しない
			this.showTable = "false";
			this.packageName = null;
			this.className = null;
		} else if (testObject instanceof ClassResult) {
			this.showTable = "true";
			this.packageName = testObject.getParent().getName();
			this.className = this.packageName + "." + testObject.getName();
		} else if (testObject instanceof PackageResult) {
			this.showTable = "true";
			this.packageName = testObject.getName();
			this.className = null;
		} else if (testObject instanceof TestResult) {
			this.showTable = "true";
			this.packageName = null;
			this.className = null;
		} else {
			//例外，テーブルは表示しない
			this.showTable = "false";
			this.packageName = null;
			this.className = null;
		}
	}

	/**
	 * @return display Name
	 */
	public String getDisplayName() {
		return "結果画像";
	}

	/**
	 * @return icon file name
	 */
	public String getIconFileName() {
		return "package.gif";
	}

	/**
	 * @return URL name
	 */
	public String getUrlName() {
		return "attachments";
	}

	/**
	 * 新しいDirectoryBrowserSupportを初期化
	 * 
	 * @return DirectoryBrowserSupport
	 */
	public DirectoryBrowserSupport doDynamic() {
		return new DirectoryBrowserSupport(this, storage, "結果画像", "package.gif", true);
	}

	@Override
	@SuppressWarnings("deprecation")
	public String annotate(String text) {
		String url = Jenkins.getActiveInstance().getRootUrl() + testObject.getRun().getUrl() + "testReport"
				+ testObject.getUrl() + "/attachments/";
		String result = text;
		for (String attachment : attachments) {
			result = result.replace(attachment, "<a href=\"" + url + attachment + "\">" + attachment + "</a>");
		}
		return result;
	}

	/**
	 * @return show table condition
	 */
	public String getShowTable() {
		return showTable;
	}

	/**
	 * @return package name
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * @return class name
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @return list of files path
	 */
	public List<String> getAttachments() {
		return attachments;
	}

	/**
	 * @return test object
	 */
	public TestObject getTestObject() {
		return testObject;
	}

	/**
	 * Check if file is image
	 * 
	 * @param filename file name
	 * @return true if specified file is image, else false
	 */
	public static boolean isImageFile(String filename) {
		return filename.matches("(?i).+\\.(gif|jpe?g|png)$");
	}

}
