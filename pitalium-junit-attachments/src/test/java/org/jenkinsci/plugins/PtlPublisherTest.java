package org.jenkinsci.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import hudson.FilePath;
import hudson.model.Run;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.PtlPublisher.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test PtlPublisher
 * 
 * @author FPT
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TestResult.class, SuiteResult.class, ClassResult.class, PackageResult.class })
public class PtlPublisherTest {
	/**
	 * Mock TestResult
	 */
	@Mock
	private TestResult testResult;

	@Mock
	SuiteResult suiteResult;

	@SuppressWarnings("rawtypes")
	private static Run build;
	private static String resultPicsAddr_ = "src/test/resources/single";
	private static File rootDir = new File(resultPicsAddr_);
	private static FilePath workspace = new FilePath(new File("").getAbsoluteFile());
	private static String child_ = "samples";
	private PtlPublisher publisher_ = new PtlPublisher(resultPicsAddr_);

	/**
	 * Prepare RootDir
	 */
	@Before
	public void prepare() {
		build = PowerMockito.mock(Run.class);
		PowerMockito.when(build.getRootDir()).thenReturn(rootDir);
	}

	/**
	 * PtlPublisherコンストラクタのテスト
	 */
	@Test
	public void testPtlPublisher() {
		PtlPublisher publisher = new PtlPublisher(resultPicsAddr_);
		assertEquals(resultPicsAddr_, publisher.getResultPicsAddr());
	}

	/**
	 * attachmentパスを確認する。
	 */
	@Test
	public void testGetAttachmentPath() {
		FilePath attachmentPath = PtlPublisher.getAttachmentPath(build);
		assertEquals(new File(resultPicsAddr_ + "/pictures").getAbsolutePath(), attachmentPath.getRemote());
	}

	/**
	 * attachmentパスが変わらないことを確認する。
	 * root = null, child = null
	 */
	@Test
	public void testGetAttachmentPath_null_null() {
		assertNull(PtlPublisher.getAttachmentPath(null, null));
	}

	/**
	 * attachmentパスが変わらないことを確認する。
	 * root != null, child = null
	 */
	@Test
	public void testGetAttachmentPath_null() {
		assertEquals(workspace, PtlPublisher.getAttachmentPath(workspace, null));
	}

	/**
	 * attachmentパスが変わらないことを確認する。
	 * root != null, child = ""
	 */
	@Test
	public void testGetAttachmentPath_empty() {
		assertEquals(workspace, PtlPublisher.getAttachmentPath(workspace, ""));
	}

	/**
	 * 新しいattachmentパスを確認する。
	 * root != null, child = "samples"
	 */
	@Test
	public void testGetAttachmentPath_valid() {
		assertEquals(workspace.child(child_), PtlPublisher.getAttachmentPath(workspace, child_));
	}

	/**
	 * 新しいattachmentパスを確認する。
	 * root != null, child = "samples#test"
	 */
	@Test
	public void testGetAttachmentPath_validSpecialCharacters() {
		String specialChild = "samples#test";
		assertEquals(workspace.child("samples_test"), PtlPublisher.getAttachmentPath(workspace, specialChild));
	}

	/**
	 * フォルダ作成・ファイル書き込み不可の場合、エラーとなることを確認する。
	 */
	@Test(expected = NullPointerException.class) 
	@SuppressWarnings("unchecked")
	public void testContributeTestData_NullPointerException() throws Exception {
		// Mock
		PowerMockito.when(testResult.getSuites()).thenThrow(IOException.class);
		publisher_.contributeTestData(build, null, null, null, testResult);
	}

	/**
	 * 画像が見つからない場合の結果を確認する。
	 */
	@Test
	public void testContributeTestData_empty() throws Exception {
		// Mock
		PowerMockito.when(testResult.getSuites()).thenReturn(new ArrayList<SuiteResult>());

		Data data = publisher_.contributeTestData(build, null, null, null, testResult);
		assertNull(data);
	}

	/**
	 * 画像が見つかった場合の結果を確認する。
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testContributeTestData() throws Exception {
		String packageName = "com.htmlhifive.pitalium.it.assertion.scroll";
		String className = "CompareScrollBorderElementTest";
		String methodName = "compareDifferentBorderWidth";

		// Mock input
		CaseResult caseResult = mock(CaseResult.class);
		when(caseResult.getPackageName()).thenReturn(packageName);
		when(caseResult.getClassName()).thenReturn(packageName + "." + className);
		when(caseResult.getName()).thenReturn(methodName);
		when(caseResult.isPassed()).thenReturn(true);
		List<CaseResult> lstCaseResult = new ArrayList<CaseResult>();
		lstCaseResult.add(caseResult);
		when(suiteResult.getCases()).thenReturn(lstCaseResult);
		List<SuiteResult> lstSuiteResult = new ArrayList<SuiteResult>();
		lstSuiteResult.add(suiteResult);
		when(testResult.getSuites()).thenReturn(lstSuiteResult);

		// Mock output
		Run owner = mock(Run.class);
		File file = new File("C:\\samples\\");
		when(caseResult.getRun()).thenReturn(owner);
		when(owner.getRootDir()).thenReturn(file);
		ClassResult parent = mock(ClassResult.class);
		PackageResult grandParent = mock(PackageResult.class);
		when(caseResult.getParent()).thenReturn(parent);
		when(parent.getParent()).thenReturn(grandParent);
		when(grandParent.getName()).thenReturn(packageName);
		when(parent.getName()).thenReturn(className);
		when(caseResult.getName()).thenReturn(methodName);

		Data data = publisher_.contributeTestData(build, workspace, null, null, testResult);
		List<TestAction> lstTestAction = data.getTestAction(caseResult);
		assertEquals(1, lstTestAction.size());
		PtlTestAction testAction = (PtlTestAction) lstTestAction.get(0);
		assertTrue(testAction.getTestObject() instanceof CaseResult);
		assertEquals(String.valueOf(false), testAction.getShowTable());
		assertNull(testAction.getPackageName());
		assertNull(testAction.getClassName());
	}

}
