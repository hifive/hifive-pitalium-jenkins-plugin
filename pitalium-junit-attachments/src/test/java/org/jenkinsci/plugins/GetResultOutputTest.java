/*
 * Copyright (C) 2018 NS Solutions Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import hudson.FilePath;
import hudson.model.Run;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.CaseResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.DirectoryScanner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;

/**
 * Test GetResultOutput
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TestResult.class, SuiteResult.class, PtlPublisher.class, FilePath.class })
public class GetResultOutputTest {

	private static final String RESULT_SKIPPED = "SKIPPED";

	private static final String RESULT_SUCCESS = "SUCCESS";

	private static final String CAPABILITY_BROWSER = "browser";

	private static final String CAPABILITY_PLATFORM = "platform";

	private static final String CAPABILITY_OS = "os";

	private static final String CAPABILITY_BROWSER_NAME = "browserName";

	private static final String CAPABILITY_VERSION = "version";

	private static final String OS_WINDOWS = "WINDOWS";

	private static final String PLATFORM_VISTA = "VISTA";

	private static final String BROWSER_CHROME = "chrome";

	private static final String BROWSER_INTERNET_EXPLORER = "internet explorer";

	private static final String ERR_LOCATION = "errLocation";

	private static final String ERR_NAME = "errName";

	private static final String DEFAULT_CAPATILITIES = "[Capabilities [{os=" + OS_WINDOWS + ", browser="
			+ BROWSER_CHROME + ", platform=" + PLATFORM_VISTA + "}]]";

	private static final String PICTURES_FOLDER_PATH = "src/test/resources/captured/single";

	private static final String PICTURES_FOLDER_PATH_NO_PICTURE = "src/test/resources/captured/noPicture";

	private static final String PICTURES_FOLDER_PATH_SINGLE_CAPABILITY = "src/test/resources/captured/singleCapability";

	private static final String packageName1 = "com.htmlhifive.pitalium.it.assertion.scroll";

	private static final String packageName2 = "com.htmlhifive.pitalium.it.assertion.fullPage";

	private static final String className1 = packageName1 + ".CompareScrollBorderElementTest";

	private static final String className2 = packageName1 + ".CompareSingleScrollElementTest";

	private static final String className3 = packageName2 + ".CompareEntirePageTest";

	private static final String caseName1 = "compareDifferentBorderWidth";

	private static final String caseName2 = "compareDifferentBorderColor";

	private static final String[] caseNames2 = new String[] { "compareScrollableDivElement",
			"compareScrollableIFrameElement", "compareScrollableTableElement", "compareScrollableTextareaElement" };

	private static final String[] caseNames3 = new String[] { "compareScrollPage_v0_h1", "compareScrollPage_v0_h2",
			"compareScrollPage_v1_h0", "compareScrollPage_v1_h1", "compareScrollPage_v2_h0", "compareScrollPage_v2_h2" };

	private static final String resultPicsAddr_ = "src/test/resources";

	private File rootDir = new File(resultPicsAddr_);

	private FilePath workspace = new FilePath(new File("").getAbsoluteFile());

	private FilePath picturesPath = null;

	private FilePath resultPath = null;

	@Mock
	private FilePath resultjspath;

	@SuppressWarnings("rawtypes")
	private Run build;

	@Mock
	private TestResult testResult;

	@Mock
	private SuiteResult suiteResult1;

	@Mock
	private SuiteResult suiteResult2;

	@Mock
	private SuiteResult suiteResult3;

	private Map<String, SuiteResult> mapSuiteResult = new HashMap<String, SuiteResult>();

	/**
	 * Prepare Mock
	 */
	@Before
	public void prepare() throws Exception {
		build = mock(Run.class);
		when(build.getRootDir()).thenReturn(rootDir);

		picturesPath = PtlPublisher.getAttachmentPath(build);
		try {
			// Execute clean
			picturesPath.deleteRecursive();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
			picturesPath.mkdirs();
		}
		resultPath = PtlPublisher.getAttachmentPath(picturesPath, "result.js");

		mapSuiteResult.put(className1, suiteResult1);
		mapSuiteResult.put(className2, suiteResult2);
		mapSuiteResult.put(className3, suiteResult3);
	}

	/**
	 * GetResultOutput()のコンストラクタのテス
	 */
	@Test
	public void testGetResultOutput() {
		try {
			new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr_);
		} catch (Throwable ex) {
			fail(ex.getMessage());
		}
	}

	/**
	 * TestObjectがnullであることを確認する。
	 */
	@Test(expected = NullPointerException.class)
	public void testGetPictures_null() {
		TestResult result = null;
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, result, resultPicsAddr_);
		obj.getPictures();
	}

	/**
	 * どのテストケースも実施されないことを確認する。
	 */
	@Test
	public void testGetPictures_emptyTestSuit() throws Exception {
		// testResult.getSuites()が空のリストである。
		when(testResult.getSuites()).thenReturn(new ArrayList<SuiteResult>());

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr_);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// picturesMapが返却される。
		assertEquals(0, results.size());

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> jsonResult = readResult();
		assertEquals(0, jsonResult.size());

		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertNoPicture();
	}

	/**
	 * picturesフォルダに画像ファイルが1つもコピーされない。
	 */
	private void assertNoPicture() {
		DirectoryScanner ds = new DirectoryScanner();
		ds.setIncludes(new String[] { "**/*.png" });
		ds.setBasedir(picturesPath.getRemote());
		ds.scan();
		String pics[] = ds.getIncludedFiles();
		assertEquals(0, pics.length);
	}

	/**
	 * Prepare simple test Map
	 * 
	 * @return Map of test data
	 */
	private Map<String, Map<String, List<String>>> prepareSampleTestMap() {
		List<String> lstCases = new ArrayList<String>();
		lstCases.add(caseName1);
		Map<String, List<String>> mapClasses = new HashMap<String, List<String>>();
		mapClasses.put(className1, lstCases);
		Map<String, Map<String, List<String>>> testData = new HashMap<String, Map<String, List<String>>>();
		testData.put(packageName1, mapClasses);

		return testData;
	}

	/**
	 * Execute test: 1. Prepare test data 2. Execute GetResultOutput.getPictures() 3. Assert result Map 4. Assert
	 * result.js
	 * 
	 * @param testData
	 * @param expectedImageCount
	 * @throws Exception
	 */
	private void executeTestGetPictures(Map<String, Map<String, List<String>>> testData, int expectedImageCount)
			throws Exception {
		// 1. Prepare test data 
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData,
				PICTURES_FOLDER_PATH, DEFAULT_CAPATILITIES, true, false);

		// 2. Execute GetResultOutput.getPictures() 
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, PICTURES_FOLDER_PATH);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// 3. Assert result Map
		assertPictureResults(expectedMap, results, expectedImageCount);

		// 4. Assert result.js
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		assertJsonResults(expectedMap, actualResult, RESULT_SUCCESS, RESULT_SUCCESS);
	}

	/**
	 * テスト結果を確認する。 ・テスト結果に1件のテストケースのみが存在する。
	 */
	@Test
	public void testGetPictures_singleTestCase() throws Exception {
		executeTestGetPictures(prepareSampleTestMap(), 2);
	}

	/**
	 * テスト結果を確認する。 ・テスト結果に同一クラスにある複数のテストケースが存在する。
	 */
	@Test
	public void testGetPictures_singleClass() throws Exception {
		// CompareScrollBorderElementTest
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		testData.get(packageName1).get(className1).add(caseName2);

		executeTestGetPictures(testData, 4);
	}

	/**
	 * テスト結果を確認する。 ・テスト結果に複数のクラスが存在する。
	 */
	@Test
	public void testGetPictures_multipleClasses() throws Exception {
		// packageName1 - className1
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		testData.get(packageName1).get(className1).add(caseName2);

		// packageName1 - className2
		testData.get(packageName1).put(className2, Arrays.asList(caseNames2));

		executeTestGetPictures(testData, 11);
	}

	/**
	 * テスト結果を確認する。 ・テスト結果に複数のパッケージが存在する。
	 */
	@Test
	public void testGetPictures_multiplePackages() throws Exception {
		// packageName1 - className1
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		testData.get(packageName1).get(className1).add(caseName2);

		// packageName1 - className2
		testData.get(packageName1).put(className2, Arrays.asList(caseNames2));

		// packageName2 - className3
		Map<String, List<String>> mapClasses = new HashMap<String, List<String>>();
		mapClasses.put(className3, Arrays.asList(caseNames3));
		testData.put(packageName2, mapClasses);

		executeTestGetPictures(testData, 23);
	}

	/**
	 * 例外が発生する時のテスト結果を確認する。 ・IOExceptionのエラーが発生する。
	 */
	@Test
	public void testGetPictures_IOException() throws Exception {
		// testResult.getSuites()が空のリストである。
		when(testResult.getSuites()).thenReturn(new ArrayList<SuiteResult>());
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr_);

		// IOExceptionのエラーが発生する。
		mock(FilePath.class);
		doThrow(new IOException()).when(resultjspath).write(Matchers.anyString(), Matchers.anyString());
		mockStatic(PtlPublisher.class);
		when(PtlPublisher.getAttachmentPath(Matchers.any(FilePath.class), Matchers.anyString())).thenReturn(
				resultjspath);
		assertNull(obj.getPictures());

		// result.jsファイルが存在しない。
		assertTrue(!resultPath.exists());

		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertNoPicture();
	}

	/**
	 * 例外が発生する時のテスト結果を確認する。 ・InterruptedExceptionのエラーが発生する。
	 */
	@Test
	public void testGetPictures_InterruptedException() throws Exception {
		// testResult.getSuites()が空のリストである。
		when(testResult.getSuites()).thenReturn(new ArrayList<SuiteResult>());
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr_);

		// InterruptedExceptionのエラーが発生する。
		mock(FilePath.class);
		doThrow(new InterruptedException()).when(resultjspath).write(Matchers.anyString(), Matchers.anyString());
		mockStatic(PtlPublisher.class);
		when(PtlPublisher.getAttachmentPath(Matchers.any(FilePath.class), Matchers.anyString())).thenReturn(
				resultjspath);
		assertNull(obj.getPictures());

		// result.jsファイルが存在しない。
		assertTrue(!resultPath.exists());

		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertNoPicture();
	}

	/**
	 * testNameが無効である場合を確認する。
	 */
	@Test
	public void testGetPictures_getCapbilities_notMatch() throws Exception {
		String capabilities = "[Capabilities [anything here]]";
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		prepareTestData(testData, PICTURES_FOLDER_PATH, capabilities, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, PICTURES_FOLDER_PATH);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertEquals(0, results.get(packageName1).get(className1).get(caseName1 + capabilities).size());

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		Map<String, String> properties = actualResult.get(packageName1).get(className1).get(caseName1 + capabilities);
		assertEquals(2, properties.size());
		assertTrue(properties.containsKey(ERR_NAME));
		assertTrue(properties.containsKey(ERR_LOCATION));

		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertNoPicture();
	}

	/**
	 * Capabilitiesに1値しか存在しない場合を確認する。
	 */
	@Test
	public void testGetPictures_getCapbilities_match_singleCapability() throws Exception {
		String capabilities = "[Capabilities [{browser=chrome}]]";
		String resultPicsAddr = PICTURES_FOLDER_PATH_SINGLE_CAPABILITY;
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData, resultPicsAddr,
				capabilities, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertPictureResults(expectedMap, results, 2);

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		Map<String, String> properties = actualResult.get(packageName1).get(className1).get(caseName1 + capabilities);
		assertEquals(3, properties.size());
		assertTrue(properties.containsKey(ERR_NAME));
		assertTrue(properties.containsKey(ERR_LOCATION));
		assertTrue(properties.containsKey(CAPABILITY_BROWSER));
		assertEquals(BROWSER_CHROME, properties.get(CAPABILITY_BROWSER));
	}

	/**
	 * Capabilitiesに複数の値が存在する場合を確認する。
	 */
	@Test
	public void testGetPictures_getCapbilities_match_multipleCapabilities() throws Exception {
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData,
				PICTURES_FOLDER_PATH, DEFAULT_CAPATILITIES, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, PICTURES_FOLDER_PATH);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertPictureResults(expectedMap, results, 2);

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		Map<String, String> properties = actualResult.get(packageName1).get(className1)
				.get(caseName1 + DEFAULT_CAPATILITIES);
		assertEquals(5, properties.size());
		assertTrue(properties.containsKey(ERR_NAME));
		assertTrue(properties.containsKey(ERR_LOCATION));
		assertTrue(properties.containsKey(CAPABILITY_BROWSER));
		assertTrue(properties.containsKey(CAPABILITY_OS));
		assertTrue(properties.containsKey(CAPABILITY_PLATFORM));
		assertEquals(BROWSER_CHROME, properties.get(CAPABILITY_BROWSER));
		assertEquals(OS_WINDOWS, properties.get(CAPABILITY_OS));
		assertEquals(PLATFORM_VISTA, properties.get(CAPABILITY_PLATFORM));
	}

	/**
	 * Selenium 3.7.0又はより新しい版でCapabilityパースの機能を検証する 。
	 *  ・Capabilities = "[Capabilities {os: WINDOWS, browser: chrome, platform: VISTA}]" 
	 */
	@Test
	public void testGetPictures_getCapbilities_match_newVersion() throws Exception {
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		String capabilities = "[Capabilities {os: WINDOWS, browser: chrome, platform: VISTA}]";
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData,
				PICTURES_FOLDER_PATH, capabilities, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, PICTURES_FOLDER_PATH);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertPictureResults(expectedMap, results, 2);

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		Map<String, String> properties = actualResult.get(packageName1).get(className1).get(caseName1 + capabilities);
		assertEquals(5, properties.size());
		assertTrue(properties.containsKey(ERR_NAME));
		assertTrue(properties.containsKey(ERR_LOCATION));
		assertTrue(properties.containsKey(CAPABILITY_BROWSER));
		assertTrue(properties.containsKey(CAPABILITY_OS));
		assertTrue(properties.containsKey(CAPABILITY_PLATFORM));
		assertEquals(BROWSER_CHROME, properties.get(CAPABILITY_BROWSER));
		assertEquals(OS_WINDOWS, properties.get(CAPABILITY_OS));
		assertEquals(PLATFORM_VISTA, properties.get(CAPABILITY_PLATFORM));
	}

	/**
	 * テスト名を確認する。 ・Capabilitiesにflatformが存在する
	 */
	@Test
	public void testGetPictures_getTestName_platformIncluded() throws Exception {
		String capabilities = "[Capabilities [{browser=chrome, platform=VISTA}]]";
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData,
				PICTURES_FOLDER_PATH, capabilities, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, PICTURES_FOLDER_PATH);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertPictureResults(expectedMap, results, 2);

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		Map<String, String> properties = actualResult.get(packageName1).get(className1).get(caseName1 + capabilities);
		assertEquals(4, properties.size());
		assertTrue(properties.containsKey(ERR_NAME));
		assertTrue(properties.containsKey(ERR_LOCATION));
		assertTrue(properties.containsKey(CAPABILITY_BROWSER));
		assertTrue(properties.containsKey(CAPABILITY_PLATFORM));
		assertEquals(BROWSER_CHROME, properties.get(CAPABILITY_BROWSER));
		assertEquals(PLATFORM_VISTA, properties.get(CAPABILITY_PLATFORM));
	}

	/**
	 * テスト名を確認する。 ・CapabilitiesにbrowserNameが存在する。
	 */
	@Test
	public void testGetPictures_getTestName_browserNameIncluded() throws Exception {
		String capabilities = "[Capabilities [{browserName=chrome}]]";
		String resultPicsAddr = PICTURES_FOLDER_PATH_SINGLE_CAPABILITY;
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData, resultPicsAddr,
				capabilities, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertPictureResults(expectedMap, results, 2);

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		Map<String, String> properties = actualResult.get(packageName1).get(className1).get(caseName1 + capabilities);
		assertEquals(3, properties.size());
		assertTrue(properties.containsKey(ERR_NAME));
		assertTrue(properties.containsKey(ERR_LOCATION));
		assertTrue(properties.containsKey(CAPABILITY_BROWSER_NAME));
		assertEquals(BROWSER_CHROME, properties.get(CAPABILITY_BROWSER_NAME));
	}

	/**
	 * テスト名を確認する。 ・CapabilitiesにbrowserNameとversionが存在する。
	 */
	@Test
	public void testGetPictures_getTestName_versionIncluded() throws Exception {
		String capabilities = "[Capabilities [{browserName=internet explorer, version=11}]]";
		String resultPicsAddr = "src/test/resources/captured/versionIncluded";
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData, resultPicsAddr,
				capabilities, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertPictureResults(expectedMap, results, 2);

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		Map<String, String> properties = actualResult.get(packageName1).get(className1).get(caseName1 + capabilities);
		assertEquals(4, properties.size());
		assertTrue(properties.containsKey(ERR_NAME));
		assertTrue(properties.containsKey(ERR_LOCATION));
		assertTrue(properties.containsKey(CAPABILITY_BROWSER_NAME));
		assertTrue(properties.containsKey(CAPABILITY_VERSION));
		assertEquals(BROWSER_INTERNET_EXPLORER, properties.get(CAPABILITY_BROWSER_NAME));
		assertEquals("11", properties.get(CAPABILITY_VERSION));
	}

	/**
	 * テスト結果を確認する。 ・caseResultに無視されたテストケースが存在する。
	 */
	@Test
	public void testGetPictures_caseResult_SKIPPED() throws Exception {
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData,
				PICTURES_FOLDER_PATH_NO_PICTURE, DEFAULT_CAPATILITIES, false, true);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult,
				PICTURES_FOLDER_PATH_NO_PICTURE);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertPictureResults(expectedMap, results, 0);

		// result.jsが作成される
		assertJsonResults(expectedMap, readResult(), RESULT_SKIPPED, RESULT_SKIPPED);

		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertNoPicture();
	}

	/**
	 * テスト結果を確認する。 ・caseResultに成功テストケースが存在する。
	 */
	@Test
	public void testGetPictures_caseResult_PASSED() throws Exception {
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData,
				PICTURES_FOLDER_PATH, DEFAULT_CAPATILITIES, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, PICTURES_FOLDER_PATH);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertPictureResults(expectedMap, results, 2);

		// result.jsが作成される
		assertJsonResults(expectedMap, readResult(), RESULT_SUCCESS, RESULT_SUCCESS);
	}

	/**
	 * テスト結果を確認する。 ・caseResultに失敗テストケースが存在する。
	 */
	@Test
	public void testGetPictures_caseResult_ERROR() throws Exception {
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData,
				PICTURES_FOLDER_PATH_NO_PICTURE, DEFAULT_CAPATILITIES, false, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult,
				PICTURES_FOLDER_PATH_NO_PICTURE);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertPictureResults(expectedMap, results, 0);

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		assertJsonResults(expectedMap, actualResult, "junit.framework.AssertionFailedError",
				"com.htmlhifive.pitalium.core.rules.AssertionView.assertView(AssertionView.java:428)");
	}

	/**
	 * resultsフォルダにテスト結果が存在しない。 ・resultsフォルダが存在しない。
	 */
	@Test
	public void testGetPictures_getSearchDirectory_resultPicsAddrNotExist() throws Exception {
		String resultPicsAddr = "notExist";
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData, resultPicsAddr,
				DEFAULT_CAPATILITIES, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertEquals(0, results.get(packageName1).get(className1).get(caseName1 + DEFAULT_CAPATILITIES).size());

		// result.jsが作成される
		assertJsonResults(expectedMap, readResult(), RESULT_SUCCESS, RESULT_SUCCESS);

		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertNoPicture();
	}

	/**
	 * resultsフォルダにテスト結果が存在しない。 ・resultsフォルダが空白である。
	 */
	@Test
	public void testGetPictures_getSearchDirectory_resultPicsAddrEmpty() throws Exception {
		String resultPicsAddr = "src/test/resources/captured/empty";
		FilePath resultDirectory = new FilePath(workspace, resultPicsAddr);
		try {
			// Force clean this directory
			resultDirectory.deleteRecursive();
		} catch (IOException ex) {
			System.out.println(resultPicsAddr + " not exist!");
		} finally {
			resultDirectory.mkdirs();
		}
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData, resultPicsAddr,
				DEFAULT_CAPATILITIES, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertEquals(0, results.get(packageName1).get(className1).get(caseName1 + DEFAULT_CAPATILITIES).size());

		// result.jsが作成される
		assertJsonResults(expectedMap, readResult(), RESULT_SUCCESS, RESULT_SUCCESS);

		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertNoPicture();
	}

	/**
	 * resultsフォルダにテスト結果が存在する。 ・resultsフォルダに1クラスの1テスト結果が存在する。
	 */
	@Test
	public void testGetPictures_getSearchDirectory_singleTest() throws Exception {
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData,
				PICTURES_FOLDER_PATH, DEFAULT_CAPATILITIES, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, PICTURES_FOLDER_PATH);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		// ・最後のテスト結果の画像ファイルがコピーされる。
		assertPictureResults(expectedMap, results, 2);

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		assertJsonResults(expectedMap, actualResult, RESULT_SUCCESS, RESULT_SUCCESS);
	}

	/**
	 * resultsフォルダにテスト結果が存在する。 ・resultsフォルダに1クラスの複数テスト結果が存在する。
	 */
	@Test
	public void testGetPictures_getSearchDirectory_multipleTests() throws Exception {
		String resultPicsAddr = "src/test/resources/captured/multiple";
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData, resultPicsAddr,
				DEFAULT_CAPATILITIES, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertPictureResults(expectedMap, results, 3);

		// result.jsが作成される
		Map<String, Map<String, Map<String, Map<String, String>>>> actualResult = readResult();
		assertJsonResults(expectedMap, actualResult, RESULT_SUCCESS, RESULT_SUCCESS);
	}

	/**
	 * resultsフォルダに画像が存在しない。 ・caseResultに失敗テストケースが存在する。
	 */
	@Test
	public void testGetPictures_SearchPicturesWithPruning_noImage() throws Exception {
		String resultPicsAddr = "src/test/resources/captured/emptyTree";
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = prepareTestData(testData, resultPicsAddr,
				DEFAULT_CAPATILITIES, true, false);

		// Execute
		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, resultPicsAddr);
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();

		// Assert
		// picturesMapが返却される。
		assertEquals(0, results.get(packageName1).get(className1).get(caseName1 + DEFAULT_CAPATILITIES).size());

		// result.jsが作成される
		assertJsonResults(expectedMap, readResult(), RESULT_SUCCESS, RESULT_SUCCESS);

		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertNoPicture();
	}

	/**
	 * 画像ファイルのコピーでIOExceptionのエラーが発生する。
	 */
	@Test
	public void testGetPictures_SearchPicturesWithPruning_IOException() throws Exception {
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		prepareTestData(testData, PICTURES_FOLDER_PATH, DEFAULT_CAPATILITIES, true, false);
		final FilePath jsonResultPath = workspace
				.child(PICTURES_FOLDER_PATH)
				.child("2018_01_11_18_47_11/CompareScrollBorderElementTest/result.json")
				.absolutize();
		final FilePath classPath = jsonResultPath.getParent();

		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, PICTURES_FOLDER_PATH);

		// Mock InterruptedException
		FilePath file = mock(FilePath.class, new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				if (invocation.getMethod().getName().equals("child")) {
					return jsonResultPath;
				}
				if (invocation.getMethod().getName().equals("getParent")) {
					return classPath;
				}
				if (invocation.getMethod().getName().equals("getName")) {
					return "2018_01_11_18_47_11";
				}
				try {
					return invocation.callRealMethod();
				} catch (NullPointerException e) {
					return null;
				}
			}
		});
		whenNew(FilePath.class).withAnyArguments().thenReturn(file);
		doThrow(new IOException()).when(file).copyTo(Matchers.any(FilePath.class));

		// Assert
		// picturesMapが返却される。
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();
		assertEquals(0, results.get(packageName1).get(className1).get(caseName1 + DEFAULT_CAPATILITIES).size());

		// result.jsファイルが存在しない。
		assertTrue(!resultPath.exists());

		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertNoPicture();
	}

	/**
	 * 画像ファイルのコピーでInterruptedExceptionのエラーが発生する。
	 */
	@Test
	public void testGetPictures_SearchPicturesWithPruning_InterruptedException() throws Exception {
		Map<String, Map<String, List<String>>> testData = prepareSampleTestMap();
		prepareTestData(testData, PICTURES_FOLDER_PATH, DEFAULT_CAPATILITIES, true, false);
		final FilePath jsonResultPath = workspace
				.child(PICTURES_FOLDER_PATH)
				.child("2018_01_11_18_47_11/CompareScrollBorderElementTest/result.json")
				.absolutize();
		final FilePath classPath = jsonResultPath.getParent();

		GetResultOutput obj = new GetResultOutput(build, workspace, null, null, testResult, PICTURES_FOLDER_PATH);

		// Mock InterruptedException
		FilePath file = mock(FilePath.class, new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				if (invocation.getMethod().getName().equals("child")) {
					return jsonResultPath;
				}
				if (invocation.getMethod().getName().equals("getParent")) {
					return classPath;
				}
				if (invocation.getMethod().getName().equals("getName")) {
					return "2018_01_11_18_47_11";
				}
				try {
					return invocation.callRealMethod();
				} catch (NullPointerException e) {
					return null;
				}
			}
		});
		whenNew(FilePath.class).withAnyArguments().thenReturn(file);
		doThrow(new InterruptedException()).when(file).copyTo(Matchers.any(FilePath.class));

		// Assert
		// picturesMapが返却される。
		Map<String, Map<String, Map<String, List<String>>>> results = obj.getPictures();
		assertEquals(0, results.get(packageName1).get(className1).get(caseName1 + DEFAULT_CAPATILITIES).size());

		// result.jsファイルが存在しない。
		assertTrue(!resultPath.exists());

		// picturesフォルダに画像ファイルが1つもコピーされない。
		assertNoPicture();
	}

	/**
	 * Read the result.js
	 * 
	 * @return JSON result in Map data
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Map<String, Map<String, Map<String, String>>>> readResult() throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(resultPath.getRemote()));
		StringBuilder sb = new StringBuilder();
		try {
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				line = br.readLine();
			}
		} finally {
			br.close();
		}

		Gson gson = new Gson();
		return gson.fromJson(sb.toString().substring("var resultdata=".length()), Map.class);
	}

	/**
	 * Mock test data and prepare expected data
	 * 
	 * @param testData
	 * @param resultPicsAddr
	 * @param capabilities
	 * @param isPassed
	 * @param isSkipped
	 * @return
	 */
	private Map<String, Map<String, Map<String, List<String>>>> prepareTestData(
			Map<String, Map<String, List<String>>> testData, String resultPicsAddr, String capabilities,
			boolean isPassed, boolean isSkipped) throws Exception {
		Map<String, Map<String, Map<String, List<String>>>> expectedMap = new HashMap<String, Map<String, Map<String, List<String>>>>();
		List<SuiteResult> lstSuiteResult = new ArrayList<SuiteResult>();
		for (Map.Entry<String, Map<String, List<String>>> suiteResult : testData.entrySet()) {
			String packageName = suiteResult.getKey();
			Map<String, Map<String, List<String>>> caseMap = new HashMap<String, Map<String, List<String>>>();
			for (Map.Entry<String, List<String>> classResult : suiteResult.getValue().entrySet()) {
				String className = classResult.getKey();
				String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
				List<CaseResult> lstCaseResult = new ArrayList<CaseResult>();
				Map<String, List<String>> pictureMap = new HashMap<String, List<String>>();
				for (String caseName : classResult.getValue()) {
					List<String> lstPictures = new ArrayList<String>();

					// Mock
					CaseResult caseResult = mock(CaseResult.class);
					when(caseResult.getPackageName()).thenReturn(packageName);
					when(caseResult.getClassName()).thenReturn(className);
					when(caseResult.getName()).thenReturn(caseName + capabilities);
					if (isPassed) {
						when(caseResult.isPassed()).thenReturn(true);
						lstCaseResult.add(caseResult);
					} else if (isSkipped) {
						when(caseResult.isSkipped()).thenReturn(true);

						lstCaseResult.add(caseResult);
						pictureMap.put(caseName + capabilities, lstPictures);
						continue;
					} else {
						PowerMockito
								.when(caseResult.getErrorStackTrace())
								.thenReturn(
										"junit.framework.AssertionFailedError\r\n"
												+ "	at com.htmlhifive.pitalium.core.rules.AssertionView.assertView(AssertionView.java:428)\r\n"
												+ "	at com.htmlhifive.pitalium.core.rules.AssertionView.assertView(AssertionView.java:248)\r\n"
												+ "	at com.htmlhifive.pitalium.core.rules.AssertionView.assertView(AssertionView.java:236)\r\n"
												+ "	at com.htmlhifive.pitalium.it.assertion.fullPage.CompareEntirePageTest.compareBodyWithMargin"
												+ "(CompareEntirePageTest.java:114)\r\n"
												+ "	at com.htmlhifive.pitalium.junit.ParameterizedTestWatcher$1.evaluate(ParameterizedTestWatcher.java:43)\r\n"
												+ "	at com.htmlhifive.pitalium.junit.RunParameterizedRules.evaluate(RunParameterizedRules.java:36)\r\n"
												+ "	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)\r\n"
												+ "	at java.util.concurrent.FutureTask.run(FutureTask.java:266)\r\n"
												+ "	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)\r\n"
												+ "	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)\r\n"
												+ "	at java.lang.Thread.run(Thread.java:748)");

						lstCaseResult.add(caseResult);
						pictureMap.put(caseName + capabilities, lstPictures);
						continue;
					}

					// Find images
					FilePath results = new FilePath(workspace, resultPicsAddr);
					if (!results.exists()) {
						continue;
					}
					DirectoryScanner ds = new DirectoryScanner();
					ds.setIncludes(new String[] { "*/" + simpleClassName + "/result.json" });
					ds.setBasedir(results.getRemote());
					ds.scan();
					String folders[] = ds.getIncludedFiles();
					DateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
					for (int i = folders.length - 1; i >= 0; i--) {
						FilePath folder = results.child(folders[i]).getParent();

						// Check name of parent folder by date
						try {
							format.parse(folder.getParent().getName());
							ds = new DirectoryScanner();
							ds.setIncludes(new String[] { caseName + "*.png" });
							ds.setBasedir(folder.getRemote());
							ds.scan();
							String pics[] = ds.getIncludedFiles();
							for (String imageFileName : pics) {
								imageFileName = new File(imageFileName).getName();
								lstPictures.add(imageFileName);
							}

							break;
						} catch (ParseException e) {
							// Invalid folder name
						}
					}
					pictureMap.put(caseName + capabilities, lstPictures);
				}
				when(mapSuiteResult.get(className).getCases()).thenReturn(lstCaseResult);
				lstSuiteResult.add(mapSuiteResult.get(className));

				caseMap.put(className, pictureMap);
			}

			expectedMap.put(packageName, caseMap);
		}
		when(testResult.getSuites()).thenReturn(lstSuiteResult);

		return expectedMap;
	}

	/**
	 * Assert result.js data
	 * 
	 * @param expectedMap
	 * @param actualResult
	 * @param errName
	 * @param errLocation
	 */
	private void assertJsonResults(Map<String, Map<String, Map<String, List<String>>>> expectedMap,
			Map<String, Map<String, Map<String, Map<String, String>>>> actualResult, String errName, String errLocation) {
		assertEquals(expectedMap.size(), actualResult.size());
		for (String packageName : expectedMap.keySet()) {
			actualResult.containsKey(packageName);
			for (String className : expectedMap.get(packageName).keySet()) {
				actualResult.get(packageName).containsKey(className);
				for (String caseName : expectedMap.get(packageName).get(className).keySet()) {
					actualResult.get(packageName).get(className).containsKey(caseName);
					Map<String, String> properties = actualResult.get(packageName).get(className).get(caseName);
					assertTrue(properties.containsKey(ERR_NAME));
					assertTrue(properties.containsKey(ERR_LOCATION));
					assertEquals(errName, properties.get(ERR_NAME));
					assertEquals(errLocation, properties.get(ERR_LOCATION));
				}
			}
		}
	}

	/**
	 * Compare Map data + check if file(s) is copied
	 * 
	 * @param expected
	 * @param actual
	 * @param expectedImagesCount
	 */
	private void assertPictureResults(Map<String, Map<String, Map<String, List<String>>>> expected,
			Map<String, Map<String, Map<String, List<String>>>> actual, int expectedImagesCount) {
		int count = 0;
		assertEquals(expected.size(), actual.size());
		for (String expectedPackgage : expected.keySet()) {
			assertTrue(actual.containsKey(expectedPackgage));
			assertEquals(expected.get(expectedPackgage).size(), actual.get(expectedPackgage).size());
			for (String expectedClass : expected.get(expectedPackgage).keySet()) {
				assertTrue(actual.get(expectedPackgage).containsKey(expectedClass));
				assertEquals(expected.get(expectedPackgage).get(expectedClass).size(), actual.get(expectedPackgage)
						.get(expectedClass).size());
				for (String expectedCase : expected.get(expectedPackgage).get(expectedClass).keySet()) {
					List<String> expectedImages = expected.get(expectedPackgage).get(expectedClass).get(expectedCase);
					List<String> actualImages = actual.get(expectedPackgage).get(expectedClass).get(expectedCase);
					assertTrue(actualImages.equals(expectedImages));

					// Assert files are copied
					FilePath expectedPath = PtlPublisher.getAttachmentPath(picturesPath, expectedPackgage);
					expectedPath = PtlPublisher.getAttachmentPath(expectedPath, expectedClass);
					expectedPath = PtlPublisher.getAttachmentPath(expectedPath, expectedCase);
					for (String image : expectedImages) {
						try {
							assertTrue(expectedPath.child(image).exists());
						} catch (IOException | InterruptedException e) {
							fail(e.getMessage());
						}
					}

					count += actualImages.size();
				}
			}
		}

		// resultsフォルダの画像ファイルがpicturesフォルダにコピーされる。
		assertEquals(expectedImagesCount, count);
	}

}
