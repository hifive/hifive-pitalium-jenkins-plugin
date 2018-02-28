package org.jenkinsci.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import hudson.FilePath;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Run;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.test.SimpleCaseResult;
import hudson.tasks.test.TestObject;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test PtlTestAction
 * 
 * @author FPT
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TestObject.class, CaseResult.class, ClassResult.class, PackageResult.class, TestResult.class,
		Jenkins.class, Run.class })
public class PtlTestActionTest {

	private static final String SHOW_TABLE = "true";

	private static final String NOT_SHOW_TABLE = "false";

	private static final String DISPLAY_NAME = "結果画像";

	private static PtlTestAction testAction = null;

	private static final String PICTURE1 = "test1";

	private static final String PICTURE2 = "test2";

	private static final String PICTURE3 = "test3";

	private static final String PICTURE1_SPECIAL = "test#1";

	private static final String PICTURE2_SPECIAL = "#test2";

	private static final String PICTURE3_SPECIAL = "#test3#";

	private static final String PICTURE1_ENCODED = "test%231";

	private static final String PICTURE2_ENCODED = "%23test2";

	private static final String PICTURE3_ENCODED = "%23test3%23";

	private static final String SAMPLE_TEXT = "text";

	private static final String ATTACHMENT_HTML = "<a href=\"http://localhost:8080/jenkins/ownertestReport/testObject/attachments/{0}\">{0}</a>";

	private static final String TEST_PACKAGE_NAME = "test.package.name";

	private static final String TEST_CLASS_NAME = TEST_PACKAGE_NAME + ".testClassName";

	private static final String TEST_CASE_NAME = "testCaseName";

	@Mock
	private Jenkins jenkins;

	/**
	 * Prepare TestAction
	 * 
	 * @throws Exception
	 */
	@Before
	public void prepare() {
		if (testAction == null) {
			List<String> attachments = new ArrayList<String>();
			attachments.add(PICTURE1);
			attachments.add(PICTURE2);
			attachments.add(PICTURE3);
			Map<String, Map<String, List<String>>> pictureMap = createPictureMap(attachments);
			FilePath storage = new FilePath(new File("../"));
			TestObject testObject = PowerMockito.mock(TestObject.class);

			testAction = new PtlTestAction(testObject, storage, pictureMap);
		}
	}

	/**
	 * Create pictures map
	 * 
	 * @param lstPictures list of pictures
	 * @return pictures map
	 */
	private Map<String, Map<String, List<String>>> createPictureMap(List<String> lstPictures) {
		Map<String, Map<String, List<String>>> pictureMap = new HashMap<String, Map<String, List<String>>>();
		Map<String, List<String>> caseMap = new HashMap<String, List<String>>();
		caseMap.put(TEST_CASE_NAME, lstPictures);
		pictureMap.put(TEST_CLASS_NAME, caseMap);

		return pictureMap;
	}

	/**
	 * Mock Jenkins Object
	 * 
	 * @param testObject TestObject
	 */
	@SuppressWarnings({ "deprecation", "unchecked" , "rawtypes"})
	private void mockJenkinsServer(TestObject testObject) {
		Run owner = PowerMockito.mock(Run.class);
		PowerMockito.when(testObject.getRun()).thenReturn(owner);
		PowerMockito.when(owner.getUrl()).thenReturn("/owner");
		PowerMockito.when(testObject.getUrl()).thenReturn("/testObject");

		PowerMockito.mockStatic(Jenkins.class);
		PowerMockito.when(Jenkins.getActiveInstance()).thenReturn(jenkins);
		PowerMockito.when(jenkins.getRootUrl()).thenReturn("http://localhost:8080/jenkins");
	}

	/**
	 * testObjectがCaseResultのインスタンスである。
	 */
	@Test
	public void testPtlTestAction_CaseResult() {
		FilePath storage = null;
		Map<String, Map<String, List<String>>> pictureMap = new HashMap<String, Map<String, List<String>>>();

		// Mock object
		TestObject testObject = PowerMockito.mock(CaseResult.class);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		// Assert
		assertEquals(NOT_SHOW_TABLE, ptlTestAction.getShowTable());
		assertNull(ptlTestAction.getPackageName());
		assertNull(ptlTestAction.getClassName());
		// testObjectがCaseResultのインスタンスである。
		assertTrue(ptlTestAction.getTestObject() instanceof CaseResult);
	}

	/**
	 * testObjectがClassResultのインスタンスである。
	 */
	@Test
	public void testPtlTestAction_ClassResult() {
		FilePath storage = null;
		Map<String, Map<String, List<String>>> pictureMap = new HashMap<String, Map<String, List<String>>>();
		String packageName = TEST_PACKAGE_NAME;
		String className = TEST_CLASS_NAME;

		// Mock object
		TestObject testObject = PowerMockito.mock(ClassResult.class);
		TestObject parent = PowerMockito.mock(PackageResult.class);
		PowerMockito.when(testObject.getParent()).thenReturn(parent);
		PowerMockito.when(parent.getName()).thenReturn(packageName);
		PowerMockito.when(testObject.getName()).thenReturn(className);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		// Assert
		assertEquals(SHOW_TABLE, ptlTestAction.getShowTable());
		assertEquals(packageName, ptlTestAction.getPackageName());
		assertEquals(packageName + "." + className, ptlTestAction.getClassName());
		// testObjectがClassResultのインスタンスである。
		assertTrue(ptlTestAction.getTestObject() instanceof ClassResult);
	}

	/**
	 * testObjectがPackageResultのインスタンスである。
	 */
	@Test
	public void testPtlTestAction_PackageResult() {
		FilePath storage = null;
		Map<String, Map<String, List<String>>> pictureMap = new HashMap<String, Map<String, List<String>>>();
		String packageName = TEST_PACKAGE_NAME;

		// Mock object
		TestObject testObject = PowerMockito.mock(PackageResult.class);
		PowerMockito.when(testObject.getName()).thenReturn(packageName);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		// Assert
		assertEquals(SHOW_TABLE, ptlTestAction.getShowTable());
		assertEquals(packageName, ptlTestAction.getPackageName());
		assertNull(ptlTestAction.getClassName());
		// testObjectがPackageResultのインスタンスである。
		assertTrue(ptlTestAction.getTestObject() instanceof PackageResult);
	}

	/**
	 * testObjectがTestResultのインスタンスである。
	 */
	@Test
	public void testPtlTestAction_TestResult() {
		FilePath storage = null;
		Map<String, Map<String, List<String>>> pictureMap = new HashMap<String, Map<String, List<String>>>();

		// Mock object
		TestObject testObject = PowerMockito.mock(TestResult.class);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		// Assert
		assertEquals(SHOW_TABLE, ptlTestAction.getShowTable());
		assertNull(ptlTestAction.getPackageName());
		assertNull(ptlTestAction.getClassName());
		// testObjectがTestResultのインスタンスである。
		assertTrue(ptlTestAction.getTestObject() instanceof TestResult);
	}

	/**
	 * testObjectが別のインスタンスである。
	 */
	@Test
	public void testPtlTestAction_other() {
		FilePath storage = null;
		Map<String, Map<String, List<String>>> pictureMap = new HashMap<String, Map<String, List<String>>>();

		// Mock object
		TestObject testObject = PowerMockito.mock(SimpleCaseResult.class);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		// Assert
		assertEquals(NOT_SHOW_TABLE, ptlTestAction.getShowTable());
		assertNull(ptlTestAction.getPackageName());
		assertNull(ptlTestAction.getClassName());
		assertTrue(ptlTestAction.getTestObject() instanceof SimpleCaseResult);
	}

	/**
	 * attachmentsが空のリストである。
	 */
	@Test
	public void testPtlTestAction_empty() {
		FilePath storage = null;
		// 1. no test class
		Map<String, Map<String, List<String>>> pictureMap = new HashMap<String, Map<String, List<String>>>();
		// Mock object
		TestObject testObject = PowerMockito.mock(TestObject.class);
		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);
		// attachmentsが空のリストである。
		assertEquals(0, ptlTestAction.getAttachments().size());

		// 2. no test method
		pictureMap.put(TEST_CLASS_NAME, new HashMap<String, List<String>>());
		// Execute
		ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);
		// attachmentsが空のリストである。
		assertEquals(0, ptlTestAction.getAttachments().get(TEST_CLASS_NAME).size());

		// 3. no picture
		pictureMap.get(TEST_CLASS_NAME).put(TEST_CASE_NAME, new ArrayList<String>());
		// Execute
		ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);
		// attachmentsが空のリストである。
		assertEquals(0, ptlTestAction.getAttachments().get(TEST_CLASS_NAME).get(TEST_CASE_NAME).size());

	}

	/**
	 * attachmentsの項目が「#」を含まない。
	 */
	@Test
	public void testPtlTestAction_withoutHashTag() {
		FilePath storage = null;
		List<String> attachments = new ArrayList<String>();
		attachments.add(PICTURE1);
		Map<String, Map<String, List<String>>> pictureMap = createPictureMap(attachments);

		// Mock object
		TestObject testObject = PowerMockito.mock(TestObject.class);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		// Assert
		Map<String, Map<String, List<String>>> actualAttachments = ptlTestAction.getAttachments();
		List<String> actualPictures = actualAttachments.get(TEST_CLASS_NAME).get(TEST_CASE_NAME);
		assertEquals(attachments.size(), actualPictures.size());
		for (int i = 0; i < actualPictures.size(); i++) {
			assertEquals(attachments.get(i), actualPictures.get(i));
		}
	}

	/**
	 * attachmentsの項目が1つの「#」を含む。
	 */
	@Test
	public void testPtlTestAction_singleHashTag() {
		FilePath storage = null;
		List<String> attachments = new ArrayList<String>();
		attachments.add(PICTURE1);
		attachments.add(PICTURE2_SPECIAL);
		attachments.add(PICTURE3);
		Map<String, Map<String, List<String>>> pictureMap = createPictureMap(attachments);

		// Mock object
		TestObject testObject = PowerMockito.mock(TestObject.class);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		// Assert
		Map<String, Map<String, List<String>>> actualAttachments = ptlTestAction.getAttachments();
		List<String> actualPictures = actualAttachments.get(TEST_CLASS_NAME).get(TEST_CASE_NAME);
		assertEquals(attachments.size(), actualPictures.size());
		assertEquals(attachments.get(0), actualPictures.get(0));
		// 「#」が「%23」に変換されることをassertする。
		assertEquals(PICTURE2_ENCODED, actualPictures.get(1));
		assertEquals(attachments.get(2), actualPictures.get(2));
	}

	/**
	 * attachmentsの項目が複数の「#」を含む。
	 */
	@Test
	public void testPtlTestAction_multipleHashTags() {
		FilePath storage = null;
		List<String> attachments = new ArrayList<String>();
		attachments.add(PICTURE1_SPECIAL);
		attachments.add(PICTURE2_SPECIAL);
		attachments.add(PICTURE3_SPECIAL);
		Map<String, Map<String, List<String>>> pictureMap = createPictureMap(attachments);

		// Mock object
		TestObject testObject = PowerMockito.mock(TestObject.class);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		// Assert
		Map<String, Map<String, List<String>>> actualAttachments = ptlTestAction.getAttachments();
		List<String> actualPictures = actualAttachments.get(TEST_CLASS_NAME).get(TEST_CASE_NAME);
		assertEquals(attachments.size(), actualPictures.size());
		// 全ての「#」が「%23」に変換される
		assertEquals(PICTURE1_ENCODED, actualPictures.get(0));
		assertEquals(PICTURE2_ENCODED, actualPictures.get(1));
		assertEquals(PICTURE3_ENCODED, actualPictures.get(2));
	}

	/**
	 * 表示名を確認する。
	 */
	@Test
	public void testGetDisplayName() {
		assertEquals(DISPLAY_NAME, testAction.getDisplayName());
	}

	/**
	 * ファイル名のアイコンを確認する。
	 */
	@Test
	public void testGetIconFileName() {
		assertEquals("package.gif", testAction.getIconFileName());
	}

	/**
	 * URL名を確認する。
	 */
	@Test
	public void testGetUrlName() {
		assertEquals("attachments", testAction.getUrlName());
	}

	/**
	 * 新しいDirectoryBrowserSupportを初期化することを確認する。
	 */
	@Test
	public void testDoDynamic() {
		DirectoryBrowserSupport directoryBrowserSupport = testAction.doDynamic();

		// Assert
		assertEquals(testAction, directoryBrowserSupport.owner);
		assertEquals(DISPLAY_NAME, directoryBrowserSupport.title);
	}

	/**
	 * テキストが変わらないことを確認する。 
	 *  ・attachmentsが空のリストである。
	 */
	@Test
	public void testAnnotate_empty() {
		// Mock object
		TestObject testObject = PowerMockito.mock(TestObject.class);
		mockJenkinsServer(testObject);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, null,
				new HashMap<String, Map<String, List<String>>>());

		String text = SAMPLE_TEXT;
		assertEquals(text, ptlTestAction.annotate(text));
	}

	/**
	 * テキストが変わらないことを確認する。 
	 *  ・attachmentsに1項目が存在する。
	 *  ・テキストにattachmentsの項目が存在しない。
	 */
	@Test
	public void testAnnotate_singleAttachment_notIncluded() {
		FilePath storage = null;
		List<String> attachments = new ArrayList<String>();
		attachments.add(PICTURE1);
		Map<String, Map<String, List<String>>> pictureMap = createPictureMap(attachments);

		// Mock object
		TestObject testObject = PowerMockito.mock(TestObject.class);
		mockJenkinsServer(testObject);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		String text = SAMPLE_TEXT;
		assertEquals(text, ptlTestAction.annotate(text));
	}

	/**
	 * テキストが変わらないことを確認する。
	 *  ・attachmentsに複数の項目が存在する。 
	 *  ・テキストにattachmentsの項目が存在しない。
	 */
	@Test
	public void testAnnotate_manyAttachment_notIncluded() {
		FilePath storage = null;
		List<String> attachments = new ArrayList<String>();
		attachments.add(PICTURE1);
		attachments.add(PICTURE2);
		attachments.add(PICTURE3);
		Map<String, Map<String, List<String>>> pictureMap = createPictureMap(attachments);

		// Mock object
		TestObject testObject = PowerMockito.mock(TestObject.class);
		mockJenkinsServer(testObject);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		String text = SAMPLE_TEXT;
		assertEquals(text, ptlTestAction.annotate(text));
	}

	/**
	 * テキストが画像パスへのハイパリンクに置き換えられることを確認する。
	 *  ・attachmentsに1項目が存在する。
	 *  ・テキストにattachmentsの項目が存在する。
	 */
	@Test
	public void testAnnotate_singleAttachment_included() {
		FilePath storage = null;
		List<String> attachments = new ArrayList<String>();
		attachments.add(PICTURE1);
		Map<String, Map<String, List<String>>> pictureMap = createPictureMap(attachments);

		// Mock object
		TestObject testObject = PowerMockito.mock(TestObject.class);
		mockJenkinsServer(testObject);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		String text = PICTURE1 + SAMPLE_TEXT;
		assertEquals(MessageFormat.format(ATTACHMENT_HTML, PICTURE1) + SAMPLE_TEXT, ptlTestAction.annotate(text));
	}

	/**
	 * テキストが画像パスへのハイパリンクに置き換えられることを確認する。
	 *  ・attachmentsに複数の項目が存在する。
	 *  ・テキストにattachmentsの項目が存在する。
	 */
	@Test
	public void testAnnotate_manyAttachment_included() {
		FilePath storage = null;
		List<String> attachments = new ArrayList<String>();
		attachments.add(PICTURE1);
		attachments.add(PICTURE2);
		attachments.add(PICTURE3);
		Map<String, Map<String, List<String>>> pictureMap = createPictureMap(attachments);

		// Mock object
		TestObject testObject = PowerMockito.mock(TestObject.class);
		mockJenkinsServer(testObject);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		String text = SAMPLE_TEXT + PICTURE2 + PICTURE3;
		assertEquals(SAMPLE_TEXT
				+ MessageFormat.format(ATTACHMENT_HTML, PICTURE2)
				+ MessageFormat.format(ATTACHMENT_HTML, PICTURE3),
				ptlTestAction.annotate(text));
	}

	/**
	 * テキストが画像パスへのハイパリンクに置き換えられることを確認する。
	 *  ・attachmentsに複数の項目が存在する。
	 *  ・テキストにattachmentsの全ての項目が存在する。
	 */
	@Test
	public void testAnnotate_manyAttachment_includedAll() {
		FilePath storage = null;
		List<String> attachments = new ArrayList<String>();
		attachments.add(PICTURE1);
		attachments.add(PICTURE2);
		attachments.add(PICTURE3);
		Map<String, Map<String, List<String>>> pictureMap = createPictureMap(attachments);

		// Mock object
		TestObject testObject = PowerMockito.mock(TestObject.class);
		mockJenkinsServer(testObject);

		// Execute
		PtlTestAction ptlTestAction = new PtlTestAction(testObject, storage, pictureMap);

		String text = PICTURE1 + PICTURE2 + PICTURE3;
		assertEquals(MessageFormat.format(ATTACHMENT_HTML, PICTURE1) + MessageFormat.format(ATTACHMENT_HTML, PICTURE2)
				+ MessageFormat.format(ATTACHMENT_HTML, PICTURE3), ptlTestAction.annotate(text));
	}

	/**
	 * null入力時のエラーを確認する。
	 */
	@Test(expected = NullPointerException.class)
	public void testAnnotate_nullValue() {
		PowerMockito.mockStatic(Jenkins.class);
		PowerMockito.when(Jenkins.getActiveInstance()).thenReturn(jenkins);
		PowerMockito.when(jenkins.getRootUrl()).thenReturn("http://localhost:8080/jenkins/");

		String text = null;
		assertEquals(text, testAction.annotate(text));
	}

	/**
	 * 指定ファイルが画像以外であることを確認する。
	 *  ・filename = null
	 */
	@Test(expected = NullPointerException.class) 
	public void testIsImageFile_null() {
		String filename = null;
		assertEquals(filename, PtlTestAction.isImageFile(filename));
	}

	/**
	 * 指定ファイルが画像以外であることを確認する。 
	 *  ・filenameが空文字列である。
	 */
	@Test
	public void testIsImageFile_empty() {
		String filename = "";
		boolean result = PtlTestAction.isImageFile(filename);
		assertEquals(false, result);
	}

	/**
	 * 指定ファイルが画像以外であることを確認する。
	 *  ・filename = "testFile.txt"
	 */
	@Test
	public void testIsImageFile_notImage() {
		String filename = "fileName.txt";
		boolean result = PtlTestAction.isImageFile(filename);
		assertEquals(false, result);
	}

	/**
	 * 指定ファイルが画像以外であることを確認する。 
	 *  ・.gif、.jpg、.jpeg、.pngを含むファイルの拡張子が.gif、.jpg、.jpeg、.png以外である。
	 */
	@Test
	public void testIsImageFile_notEndedWithImageFormats() {
		String[] filenames = new String[] { "testFile.gif.bak", "testFile.jpg.bak", "testFile.jpeg.bak",
			"testFile.png.bak" };
		for (String fileName : filenames) {
			assertEquals(false, PtlTestAction.isImageFile(fileName));
		}
	}

	/**
	 * 指定ファイルが画像以外であることを確認する。 
	 *  ・ファイルの拡張子がサポートしない画像形式（.tiff、.bmp、.webp等）である。
	 */
	@Test
	public void testIsImageFile_unsupportedImageFormats() {
		String[] filenames = new String[] { "testFile.tiff", "testFile.bmp", "testFile.webp" };
		for (String fileName : filenames) {
			assertEquals(false, PtlTestAction.isImageFile(fileName));
		}
	}

	/**
	 * 指定ファイルが画像であることを確認する。 
	 *  ・ファイルの拡張子が.gif、.jpg、.jpeg、.pngのいずれかの値である。
	 */
	@Test
	public void testIsImageFile_endedWithImageFormats() {
		String[] filenames = new String[] { "testFile.gif", "testFile.jpg", "testFile.jpeg", "testFile.png" };
		for (String fileName : filenames) {
			assertEquals(true, PtlTestAction.isImageFile(fileName));
		}
	}

	/**
	 * 指定ファイルが画像であることを確認する。 
	 *  ・ファイルの拡張子が.GIF、.JPG、.JPEG、.PNGのいずれかの値である。
	 */
	@Test
	public void testIsImageFile_endedWithImageFormats_sensitive() {
		String[] filenames = new String[] { "testFile.GIF", "testFile.JPG", "testFile.JPEG", "testFile.PNG" };
		for (String fileName : filenames) {
			assertEquals(true, PtlTestAction.isImageFile(fileName));
		}
	}

	/**
	 * テストケース名からテストメソッド名を取得するテストケースである。
	 * ・テストケース名がCapabilitiesを含む場合。
	 */
	@Test
	public void testGetTestMethodName() {
		String methodName = "compareDifferentBorderWidth";
		String capabilities = "[Capabilities [{browser=chrome, platform=VISTA}]]";

		assertEquals(methodName, PtlTestAction.getTestMethodName(methodName + capabilities));
	}

	/**
	 * テストケース名からテストメソッド名を取得するテストケースである。
	 * ・テストケース名がCapabilitiesを含まない場合。
	 */
	@Test
	public void testGetTestMethodNameWithoutCapabilities() {
		String methodName = "compareDifferentBorderWidth";
		assertEquals(methodName, PtlTestAction.getTestMethodName(methodName));

		methodName = "compareDifferentBorderWidth[Capabilities []]";
		assertEquals("compareDifferentBorderWidth", PtlTestAction.getTestMethodName(methodName));
	}
}
