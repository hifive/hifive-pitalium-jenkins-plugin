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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** テスト結果ページの表示で利用 */
public class PtlTestAction extends TestAction {

	private final FilePath storage;
	private final Map<String, Map<String, List<String>>> attachments;
	private final TestObject testObject;
	private final String showTable;
	private final String packageName;
	private final String className;

	/**
	 * コンストラクタ
	 * 
	 * @param testObject Test Object
	 * @param storage File path
	 * @param attachments pictures data (included test class names, test method names and path of pictures)
	 */
	public PtlTestAction(TestObject testObject, FilePath storage, Map<String, Map<String, List<String>>> attachments) {
		this.storage = storage;
		this.testObject = testObject;
		// URI Encoder
		for (Map.Entry<String, Map<String, List<String>>> tstCls : attachments.entrySet()) {
			for (Map.Entry<String, List<String>> tstCase : tstCls.getValue().entrySet()) {
				List<String> pictures = tstCase.getValue();
				for (int i = 0; i < pictures.size(); i++) {
					pictures.set(i, pictures.get(i).replace("#", "%23"));
				}
			}
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
		for (Map.Entry<String, Map<String, List<String>>> tstCls : attachments.entrySet()) {
			for (Map.Entry<String, List<String>> tstCase : tstCls.getValue().entrySet()) {
				for (String picture : tstCase.getValue()) {
					result = result.replace(picture, "<a href=\"" + url + picture + "\">" + picture + "</a>");
				}
			}
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
	 * @return pictures data
	 */
	public Map<String, Map<String, List<String>>> getAttachments() {
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

	/**
	 * Get test method name from test case name
	 * 
	 * @param testName Test name
	 * @return test method
	 */
	public static String getTestMethodName(String testName) {
		String testMethodName = testName;
		Pattern p = Pattern.compile("^(.*)\\s?\\[Capabilities.*\\]$");
		Matcher m = p.matcher(testMethodName);
		if (m.find()) {
			testMethodName = m.group(1);
		}

		return testMethodName;
	}
}
