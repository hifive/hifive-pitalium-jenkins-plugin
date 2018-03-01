package org.jenkinsci.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestObject;

/** テスト実行後に呼び出される． */
public class PtlPublisher extends TestDataPublisher {

	private final String resultPicsAddr;

	/**
	 * コンストラクタ
	 *
	 * @param resultPicsAddr 結果画像フォルダの探索開始パス
	 */
	@DataBoundConstructor
	public PtlPublisher(String resultPicsAddr) {
		this.resultPicsAddr = resultPicsAddr;
	}

	/**
	 * @return 結果画像フォルダの探索開始パス
	 */
	public String getResultPicsAddr() {
		return resultPicsAddr;
	}

	/**
	 * @param build Run contributing test data
	 * @return pictures path
	 */
	public static FilePath getAttachmentPath(Run<?, ?> build) {
		return new FilePath(new File(build.getRootDir().getAbsolutePath())).child("pictures");
	}

	/**
	 * @param root parent path
	 * @param child folder path
	 * @return Attachment Path
	 */
	public static FilePath getAttachmentPath(FilePath root, String child) {
		FilePath dir = root;
		if (!StringUtils.isEmpty(child)) {
			dir = dir.child(TestObject.safe(child));
		}
		return dir;
	}

	@Override
	public Data contributeTestData(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener,
			TestResult testResult) throws IOException, InterruptedException {
		final GetResultOutput obj = new GetResultOutput(build, workspace, launcher, listener, testResult,
				resultPicsAddr);
		Map<String, Map<String, Map<String, List<String>>>> pictures = obj.getPictures();

		if (pictures.isEmpty()) {
			return null;
		}

		return new Data(pictures);
	}

	/**
	 * Test Result Action's Data
	 * TODO 動作チェック：パスにString利用有り．
	 */
	public static class Data extends TestResultAction.Data {
		private Map<String, Map<String, Map<String, List<String>>>> pictures;

		/**
		 * コンストラクタ
		 *
		 * @param pictures pictures information
		 */
		public Data(Map<String, Map<String, Map<String, List<String>>>> pictures) {
			this.pictures = pictures;
		}

		@Override
		@SuppressWarnings("deprecation")
		public List<TestAction> getTestAction(hudson.tasks.junit.TestObject t) {
			TestObject testObject = (TestObject) t;
			String packageName;
			String className;
			String testName;
			FilePath storage = getAttachmentPath(testObject.getRun());

			Map<String, Map<String, List<String>>> picturesMap = new HashMap<String, Map<String, List<String>>>();
			if (testObject instanceof CaseResult) {
				packageName = testObject.getParent().getParent().getName();
				storage = getAttachmentPath(storage, packageName);
				className = packageName + "." + testObject.getParent().getName();
				storage = getAttachmentPath(storage, className);
				testName = testObject.getName();
				storage = getAttachmentPath(storage, testName);
				setPicturesMap(picturesMap, className, testName, "",
						pictures.get(packageName).get(className).get(testName));
			} else if (testObject instanceof ClassResult) {
				packageName = testObject.getParent().getName();
				storage = getAttachmentPath(storage, packageName);
				className = packageName + "." + testObject.getName();
				storage = getAttachmentPath(storage, className);
				for (Map.Entry<String, List<String>> tstCase : pictures.get(packageName).get(className).entrySet()) {
					testName = tstCase.getKey();
					setPicturesMap(picturesMap, className, testName, testName + "/", tstCase.getValue());
				}
			} else if (testObject instanceof PackageResult) {
				packageName = testObject.getName();
				storage = getAttachmentPath(storage, packageName);
				for (Map.Entry<String, Map<String, List<String>>> tstCls : pictures.get(packageName).entrySet()) {
					className = tstCls.getKey();
					for (Map.Entry<String, List<String>> tstCase : tstCls.getValue().entrySet()) {
						testName = tstCase.getKey();
						setPicturesMap(picturesMap, className, testName, className + "/" + testName + "/",
								tstCase.getValue());
					}
				}
			} else if (testObject instanceof TestResult) {
				for (Map.Entry<String, Map<String, Map<String, List<String>>>> tstPkg : pictures.entrySet()) {
					packageName = tstPkg.getKey();
					for (Map.Entry<String, Map<String, List<String>>> tstCls : tstPkg.getValue().entrySet()) {
						className = tstCls.getKey();
						for (Map.Entry<String, List<String>> tstCase : tstCls.getValue().entrySet()) {
							testName = tstCase.getKey();
							setPicturesMap(picturesMap, className, testName,
									packageName + "/" + className + "/" + testName + "/", tstCase.getValue());
						}
					}
				}
			} else {
				// EXCEPTION?
				return Collections.emptyList();
			}

			PtlTestAction action = new PtlTestAction(testObject, storage, picturesMap);
			return Collections.<TestAction>singletonList(action);
		}

		/**
		 * Set Pictures Map
		 *
		 * @param className test class name
		 * @param testName test case name (with Capabilities)
		 * @param path appended path
		 * @param lstPictures list of picture
		 */
		private void setPicturesMap(Map<String, Map<String, List<String>>> picturesMap, String className,
				String testName, String path, List<String> lstPictures) {
			List<String> picturesList = new ArrayList<String>();
			for (String var : lstPictures) {
				picturesList.add(path + var);
			}

			// Set pictures map
			if (!picturesMap.containsKey(className)) {
				picturesMap.put(className, new HashMap<String, List<String>>());
			}
			picturesMap.get(className).put(testName, picturesList);
		}
	}

	/**
	 * Descriptor
	 */
	@Extension
	public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
		@Override
		public String getDisplayName() {
			return "Pitaliumプラグイン";
		}
	}
}
