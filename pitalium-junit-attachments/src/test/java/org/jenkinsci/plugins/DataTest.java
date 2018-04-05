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
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import hudson.model.Run;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.test.SimpleCaseResult;
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.MetaTabulatedResult;
import hudson.tasks.test.TestObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.PtlPublisher.Data;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test Data
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TestObject.class, CaseResult.class, ClassResult.class, PackageResult.class, TestResult.class,
		Jenkins.class, Run.class })
@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
public class DataTest {
	private static final String SHOW_TABLE = "true";
	private static final String NOT_SHOW_TABLE = "false";
	private static final String DOT = ".";
	private static final String UNDERSCORE = "_";

	private static final String PICTURE1 = "#Test1";
	private static final String PICTURE2 = "Test2#";
	private static final String PICTURE3 = "Test#3";

	private static final String package1_ = "com.htmlhifive.pitalium.it.assertion.scroll";
	private static final String package2_ = "com.htmlhifive.pitalium.it.assertion.fullPage";
	private static final String package3_ = "com.htmlhifive.pitalium.it.assertion.exclude";

	private static final String className1_ = "CompareScrollBorderElementTest";
	private static final String className2_ = "CompareScrollMarginElementTest";
	private static final String className3_ = "CompareSingleScrollElementTest";
	private static final String className4_ = "CompareEntirePageTest";

	private static final String methodName1_ = "compareDifferentBorderColor";
	private static final String methodName2_ = "compareDifferentBorderWidth";
	private static final String methodName3_ = "compareDifferentMargin";
	private static final String methodName4_ = "compareScrollableDivElement";

	/**
	 * Execute Data.getTestAction()
	 * 
	 * @param instance
	 * @param inputData
	 * @return
	 */
	private List<TestAction> executeGetTestAction(Class instance, Object inputData) {
		// Mock
		TestObject testObject = (TestObject) mock(instance);
		if (instance.equals(CaseResult.class)) {
			TestObject parent = mock(ClassResult.class);
			TestObject grandParent = mock(PackageResult.class);
			when(testObject.getParent()).thenReturn(parent);
			when(parent.getParent()).thenReturn(grandParent);
			when(grandParent.getName()).thenReturn(package1_);
			when(parent.getName()).thenReturn(className1_);
			when(testObject.getName()).thenReturn(methodName1_);
		} else if (instance.equals(ClassResult.class)) {
			TestObject parent = mock(PackageResult.class);
			when(testObject.getParent()).thenReturn(parent);
			when(parent.getName()).thenReturn(package1_);
			when(testObject.getName()).thenReturn(className1_);
		} else if (instance.equals(PackageResult.class)) {
			when(testObject.getName()).thenReturn(package1_);
		}
		Run owner = mock(Run.class);
		File file = new File("C:\\samples\\");
		when(testObject.getRun()).thenReturn(owner);
		when(owner.getRootDir()).thenReturn(file);

		// Prepare pictures
		Map<String, Map<String, Map<String, List<String>>>> pictures = new HashMap<String, Map<String, Map<String, List<String>>>>();
		if (instance.equals(CaseResult.class)) {
			Map<String, Map<String, List<String>>> mapClasses = new HashMap<String, Map<String, List<String>>>();
			Map<String, List<String>> mapMethods = new HashMap<String, List<String>>();
			mapMethods.put(methodName1_, (List<String>) inputData);
			mapClasses.put(package1_ + DOT + className1_, mapMethods);
			pictures.put(package1_, mapClasses);
		} else if (instance.equals(ClassResult.class)) {
			Map<String, Map<String, List<String>>> mapClasses = new HashMap<String, Map<String, List<String>>>();
			mapClasses.put(package1_ + DOT + className1_, (Map<String, List<String>>) inputData);
			pictures.put(package1_, mapClasses);
		} else if (instance.equals(PackageResult.class)) {
			pictures.put(package1_, (Map<String, Map<String, List<String>>>) inputData);
		} else if (instance.equals(TestResult.class)) {
			pictures = (Map<String, Map<String, Map<String, List<String>>>>) inputData;
		}

		// Execute
		Data data = new Data(pictures);
		return data.getTestAction(testObject);
	}

	// Assert TestAction
	private void assertTestAction(List<TestAction> results, Class instance, int size) {
		// ・結果は1項目が存在するリストである。
		assertEquals(1, results.size());
		PtlTestAction action = (PtlTestAction) results.get(0);

		// testObjectがinstanceのインスタンスである。
		if (instance.equals(CaseResult.class)) {
			assertTrue(action.getTestObject() instanceof CaseResult);

			assertEquals(NOT_SHOW_TABLE, action.getShowTable());
			assertNull(action.getPackageName());
			assertNull(action.getClassName());
		} else if (instance.equals(ClassResult.class)) {
			assertTrue(action.getTestObject() instanceof ClassResult);

			assertEquals(SHOW_TABLE, action.getShowTable());
			assertEquals(package1_, action.getPackageName());
			assertEquals(package1_ + DOT + className1_, action.getClassName());
		} else if (instance.equals(PackageResult.class)) {
			assertTrue(action.getTestObject() instanceof PackageResult);

			assertEquals(SHOW_TABLE, action.getShowTable());
			assertEquals(package1_, action.getPackageName());
			assertNull(action.getClassName());
		} else if (instance.equals(TestResult.class)) {
			assertTrue(action.getTestObject() instanceof TestResult);

			assertEquals(SHOW_TABLE, action.getShowTable());
			assertNull(action.getPackageName());
			assertNull(action.getClassName());
		} else {
			assertEquals(NOT_SHOW_TABLE, action.getShowTable());
			assertNull(action.getPackageName());
			assertNull(action.getClassName());
		}

		// attachmentsが空のリストである。
		int count = 0;
		Map<String, Map<String, List<String>>> actualAttachments = action.getAttachments();
		for (Map<String, List<String>> actualCaseResults : actualAttachments.values()) {
			for (List<String> actualPictures : actualCaseResults.values()) {
				count += actualPictures.size();
			}
		}
		assertEquals(size, count);
	}

	/**
	 * Dataコンストラクタのテスト
	 */
	@Test
	public void testData() {
		List<String> lstPictures = new ArrayList<String>();
		lstPictures.add(PICTURE1);
		Map<String, List<String>> mapMethods = new HashMap<String, List<String>>();
		mapMethods.put(methodName1_, lstPictures);
		Map<String, Map<String, List<String>>> mapClasses = new HashMap<String, Map<String, List<String>>>();
		mapClasses.put(package1_ + DOT + className1_, mapMethods);
		Map<String, Map<String, Map<String, List<String>>>> pictures = new HashMap<String, Map<String, Map<String, List<String>>>>();
		pictures.put(package1_, mapClasses);

		try {
			new Data(pictures);
		} catch (Throwable ex) {
			fail(ex.getMessage());
		}
	}

	/**
	 * ケースレベルでのテスト結果を確認する。
	 *  ・testObjectがCaseResultのインスタンスである。
	 *  ・picturesが空のリストである。
	 */
	@Test
	public void testGetTestAction_CaseResult_empty() {
		List<String> lstPictures = new ArrayList<String>();

		// Execute
		List<TestAction> results = executeGetTestAction(CaseResult.class, lstPictures);

		// Assert
		assertTestAction(results, CaseResult.class, 0);
	}

	/**
	 * ケースレベルでのテスト結果を確認する。
	 *  ・testObjectがCaseResultのインスタンスである。
	 *  ・picturesに1項目が存在する。
	 */
	@Test
	public void testGetTestAction_CaseResult_singlePicture() {
		List<String> lstPictures = new ArrayList<String>();
		lstPictures.add(PICTURE1);

		// Execute
		List<TestAction> results = executeGetTestAction(CaseResult.class, lstPictures);

		// Assert
		assertTestAction(results, CaseResult.class, 1);
	}

	/**
	 * ケースレベルでのテスト結果を確認する。
	 *  ・testObjectがCaseResultのインスタンスである。 
	 *  ・picturesに複数の項目が存在する。
	 */
	@Test
	public void testGetTestAction_CaseResult_multiplePicture() {
		List<String> lstPictures = new ArrayList<String>();
		lstPictures.add(PICTURE1);
		lstPictures.add(PICTURE2);
		lstPictures.add(PICTURE3);

		// Execute
		List<TestAction> results = executeGetTestAction(CaseResult.class, lstPictures);

		// Assert
		assertTestAction(results, CaseResult.class, lstPictures.size());
	}

	/**
	 * ケースレベルでのテスト結果を確認する。 
	 *  ・testObjectがClassResultのインスタンスである。
	 *  ・picturesが空のリストである。
	 */
	@Test
	public void testGetTestAction_ClassResult_empty() {
		// No test methods
		Map<String, List<String>> mapMethods = new HashMap<String, List<String>>();
		List<TestAction> results = executeGetTestAction(ClassResult.class, mapMethods);
		assertTestAction(results, ClassResult.class, 0);

		// 1 method - 0 pictures
		mapMethods.put(methodName1_, new ArrayList<String>());
		results = executeGetTestAction(ClassResult.class, mapMethods);
		assertTestAction(results, ClassResult.class, 0);
	}

	/**
	 * クラスレベルでのテスト結果を確認する。
	 *  ・testObjectがClassResultのインスタンスである。 
	 *  ・picturesに1項目が存在する。
	 */
	@Test
	public void testGetTestAction_ClassResult_singlePicture() {
		// 1 method - many pictures
		List<String> lstPictures = new ArrayList<String>();
		lstPictures.add(PICTURE1);
		Map<String, List<String>> mapMethods = new HashMap<String, List<String>>();
		mapMethods.put(methodName1_, lstPictures);

		// Execute
		List<TestAction> results = executeGetTestAction(ClassResult.class, mapMethods);

		// Assert
		assertTestAction(results, ClassResult.class, 1);
	}

	/**
	 * クラスレベルでのテスト結果を確認する。
	 *  ・testObjectがClassResultのインスタンスである。 
	 *  ・picturesに複数の項目が存在する。
	 */
	@Test
	public void testGetTestAction_ClassResult_multiplePicture() {
		// single method, multiple pictures
		List<String> lstPictures1 = new ArrayList<String>();
		lstPictures1.add(methodName1_ + UNDERSCORE + PICTURE1);
		lstPictures1.add(methodName1_ + UNDERSCORE + PICTURE2);
		lstPictures1.add(methodName1_ + UNDERSCORE + PICTURE3);
		Map<String, List<String>> mapMethods = new HashMap<String, List<String>>();
		mapMethods.put(methodName1_, lstPictures1);
		// Execute
		List<TestAction> results = executeGetTestAction(ClassResult.class, mapMethods);
		// Assert
		assertTestAction(results, ClassResult.class, lstPictures1.size());

		// multiple methods - multiple pictures
		List<String> lstPictures2 = new ArrayList<String>();
		List<String> lstPictures3 = new ArrayList<String>();
		lstPictures2.add(methodName2_ + UNDERSCORE + PICTURE3);
		lstPictures3.add(methodName3_ + UNDERSCORE + PICTURE1);
		lstPictures3.add(methodName3_ + UNDERSCORE + PICTURE2);
		mapMethods.put(methodName2_, lstPictures2);
		mapMethods.put(methodName3_, lstPictures3);
		// Execute
		results = executeGetTestAction(ClassResult.class, mapMethods);
		// Assert
		assertTestAction(results, ClassResult.class, lstPictures1.size() + lstPictures2.size() + lstPictures3.size());
	}

	/**
	 * パッケージレベルでのテスト結果を確認する。 
	 *  ・testObjectがPackageResultのインスタンスである。 
	 *  ・picturesが空のリストである。
	 */
	@Test
	public void testGetTestAction_PackageResult_empty() {
		// No test class
		Map<String, Map<String, List<String>>> mapClasses = new HashMap<String, Map<String, List<String>>>();
		// Execute
		List<TestAction> results = executeGetTestAction(PackageResult.class, mapClasses);
		// Assert
		assertTestAction(results, PackageResult.class, 0);

		// 1 class, no method
		Map<String, List<String>> mapMethods1 = new HashMap<String, List<String>>();
		mapClasses.put(className1_, mapMethods1);
		// Execute
		results = executeGetTestAction(PackageResult.class, mapClasses);
		// Assert
		assertTestAction(results, PackageResult.class, 0);

		// many class, no method
		Map<String, List<String>> mapMethods2 = new HashMap<String, List<String>>();
		Map<String, List<String>> mapMethods3 = new HashMap<String, List<String>>();
		mapClasses.put(className2_, mapMethods2);
		mapClasses.put(className3_, mapMethods3);
		// Execute
		results = executeGetTestAction(PackageResult.class, mapClasses);
		// Assert
		assertTestAction(results, PackageResult.class, 0);

		// 1 method but no pictures
		mapMethods1.put(methodName1_, new ArrayList<String>());
		// Execute
		results = executeGetTestAction(PackageResult.class, mapClasses);
		// Assert
		assertTestAction(results, PackageResult.class, 0);

		// Multiple methods and no picture
		mapMethods2.put(methodName2_, new ArrayList<String>());
		mapMethods2.put(methodName3_, new ArrayList<String>());
		// Execute
		results = executeGetTestAction(PackageResult.class, mapClasses);
		// Assert
		assertTestAction(results, PackageResult.class, 0);
	}

	/**
	 * パッケージレベルでのテスト結果を確認する。
	 *  ・testObjectがPackageResultのインスタンスである。 
	 *  ・picturesに1項目が存在する。
	 */
	@Test
	public void testGetTestAction_PackageResult_singlePicture() {
		List<String> lstPictures = new ArrayList<String>();
		lstPictures.add(PICTURE1);
		Map<String, List<String>> mapMethods = new HashMap<String, List<String>>();
		mapMethods.put(methodName1_, lstPictures);
		Map<String, Map<String, List<String>>> mapClasses = new HashMap<String, Map<String, List<String>>>();
		mapClasses.put(className1_, mapMethods);

		// Execute
		List<TestAction> results = executeGetTestAction(PackageResult.class, mapClasses);

		// Assert
		assertTestAction(results, PackageResult.class, 1);
	}

	/**
	 * パッケージレベルでのテスト結果を確認する。
	 *  ・testObjectがPackageResultのインスタンスである。 
	 *  ・picturesに複数の項目が存在する。
	 */
	@Test
	public void testGetTestAction_PackageResult_multiplePicture() {
		// Multiple pictures in 1 method
		List<String> lstPictures1 = new ArrayList<String>();
		lstPictures1.add(methodName1_ + UNDERSCORE + PICTURE1);
		lstPictures1.add(methodName1_ + UNDERSCORE + PICTURE2);
		lstPictures1.add(methodName1_ + UNDERSCORE + PICTURE3);
		Map<String, List<String>> mapMethods1 = new HashMap<String, List<String>>();
		mapMethods1.put(methodName1_, lstPictures1);
		Map<String, Map<String, List<String>>> mapClasses = new HashMap<String, Map<String, List<String>>>();
		mapClasses.put(className1_, mapMethods1);
		// Execute
		List<TestAction> results = executeGetTestAction(PackageResult.class, mapClasses);
		// Assert
		assertTestAction(results, PackageResult.class, lstPictures1.size());

		// Multiple pictures in 1 class
		List<String> lstPictures2 = new ArrayList<String>();
		lstPictures2.add(methodName2_ + UNDERSCORE + PICTURE1);
		lstPictures2.add(methodName2_ + UNDERSCORE + PICTURE3);
		mapMethods1.put(methodName2_, lstPictures2);
		// Execute
		results = executeGetTestAction(PackageResult.class, mapClasses);
		// Assert
		assertTestAction(results, PackageResult.class, lstPictures1.size() + lstPictures2.size());

		// Multiple pictures in 1 package
		Map<String, List<String>> mapMethods2 = new HashMap<String, List<String>>();
		List<String> lstPictures3 = new ArrayList<String>();
		lstPictures3.add(methodName3_ + UNDERSCORE + PICTURE1);
		lstPictures3.add(methodName3_ + UNDERSCORE + PICTURE2);
		mapMethods2.put(methodName3_, lstPictures3);
		mapClasses.put(className2_, mapMethods2);
		// Execute
		results = executeGetTestAction(PackageResult.class, mapClasses);
		// Assert
		assertTestAction(results, PackageResult.class, lstPictures1.size() + lstPictures2.size() + lstPictures3.size());

		// Multiple pictures in many package
		Map<String, List<String>> mapMethods3 = new HashMap<String, List<String>>();
		List<String> lstPictures4 = new ArrayList<String>();
		lstPictures4.add(methodName4_ + UNDERSCORE + PICTURE2);
		lstPictures4.add(methodName4_ + UNDERSCORE + PICTURE3);
		mapMethods3.put(methodName4_, lstPictures4);
		mapClasses.put(className3_, mapMethods3);
		// Execute
		results = executeGetTestAction(PackageResult.class, mapClasses);
		// Assert
		assertTestAction(results, PackageResult.class, lstPictures1.size() + lstPictures2.size() + lstPictures3.size()
				+ lstPictures4.size());
	}

	/**
	 * パッケージレベルでのテスト結果を確認する。
	 *  ・testObjectがTestResultのインスタンスである。
	 *  ・picturesが空のリストである。
	 */
	@Test
	public void testGetTestAction_TestResult_empty() {
		// No package
		Map<String, Map<String, Map<String, List<String>>>> pictures = new HashMap<String, Map<String, Map<String, List<String>>>>();
		// Execute
		List<TestAction> results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, 0);

		// 1 Package, no class
		Map<String, Map<String, List<String>>> mapClasses = new HashMap<String, Map<String, List<String>>>();
		pictures.put(package1_, mapClasses);
		// Execute
		results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, 0);

		// multiple Package, no class
		Map<String, Map<String, List<String>>> mapClasses2 = new HashMap<String, Map<String, List<String>>>();
		Map<String, Map<String, List<String>>> mapClasses3 = new HashMap<String, Map<String, List<String>>>();
		pictures.put(package2_, mapClasses2);
		pictures.put(package3_, mapClasses3);
		// Execute
		results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, 0);

		// 1 class, no method
		Map<String, List<String>> mapMethods1 = new HashMap<String, List<String>>();
		mapClasses.put(className1_, mapMethods1);
		// Execute
		results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, 0);

		// many class, no method
		Map<String, List<String>> mapMethods2 = new HashMap<String, List<String>>();
		Map<String, List<String>> mapMethods3 = new HashMap<String, List<String>>();
		mapClasses.put(className2_, mapMethods2);
		mapClasses.put(className3_, mapMethods3);
		// Execute
		results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, 0);

		// 1 method but no pictures
		mapMethods1.put(methodName1_, new ArrayList<String>());
		// Execute
		results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, 0);

		// Multiple methods and no picture
		mapMethods2.put(methodName2_, new ArrayList<String>());
		mapMethods2.put(methodName3_, new ArrayList<String>());
		// Execute
		results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, 0);
	}

	/**
	 * パッケージレベルでのテスト結果を確認する。
	 *  ・testObjectがTestResultのインスタンスである。
	 *  ・picturesに1項目が存在する。
	 */
	@Test
	public void testGetTestAction_TestResult_singlePicture() {
		List<String> lstPictures = new ArrayList<String>();
		lstPictures.add(PICTURE1);
		Map<String, List<String>> mapMethods = new HashMap<String, List<String>>();
		mapMethods.put(methodName1_, lstPictures);
		Map<String, Map<String, List<String>>> mapClasses = new HashMap<String, Map<String, List<String>>>();
		mapClasses.put(className1_, mapMethods);
		Map<String, Map<String, Map<String, List<String>>>> pictures = new HashMap<String, Map<String, Map<String, List<String>>>>();
		pictures.put(package1_, mapClasses);

		// Execute
		List<TestAction> results = executeGetTestAction(TestResult.class, pictures);

		// Assert
		assertTestAction(results, TestResult.class, 1);
	}

	/**
	 * パッケージレベルでのテスト結果を確認する。
	 *  ・testObjectがTestResultのインスタンスである。
	 *  ・picturesに複数の項目が存在する。
	 */
	@Test
	public void testGetTestAction_TestResult_multiplePicture() {
		// Multiple pictures in 1 method
		List<String> lstPictures1 = new ArrayList<String>();
		lstPictures1.add(methodName1_ + UNDERSCORE + PICTURE1);
		lstPictures1.add(methodName1_ + UNDERSCORE + PICTURE2);
		lstPictures1.add(methodName1_ + UNDERSCORE + PICTURE3);
		Map<String, List<String>> mapMethods1 = new HashMap<String, List<String>>();
		mapMethods1.put(methodName1_, lstPictures1);
		Map<String, Map<String, List<String>>> mapClasses = new HashMap<String, Map<String, List<String>>>();
		mapClasses.put(className1_, mapMethods1);
		Map<String, Map<String, Map<String, List<String>>>> pictures = new HashMap<String, Map<String, Map<String, List<String>>>>();
		pictures.put(package1_, mapClasses);
		// Execute
		List<TestAction> results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, lstPictures1.size());

		// Multiple pictures in 1 class
		List<String> lstPictures2 = new ArrayList<String>();
		lstPictures2.add(methodName2_ + UNDERSCORE + PICTURE1);
		lstPictures2.add(methodName2_ + UNDERSCORE + PICTURE2);
		mapMethods1.put(methodName2_, lstPictures2);
		// Execute
		results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, lstPictures1.size() + lstPictures2.size());

		// Multiple pictures in 1 package
		Map<String, List<String>> mapMethods2 = new HashMap<String, List<String>>();
		List<String> lstPictures3 = new ArrayList<String>();
		lstPictures3.add(methodName3_ + UNDERSCORE + PICTURE3);
		mapMethods2.put(methodName3_, lstPictures3);
		mapClasses.put(className2_, mapMethods2);
		// Execute
		results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, lstPictures1.size() + lstPictures2.size() + lstPictures3.size());

		// Multiple pictures in many package
		List<String> lstPicturesB = new ArrayList<String>();
		lstPicturesB.add(methodName4_ + UNDERSCORE + PICTURE1);
		lstPicturesB.add(methodName4_ + UNDERSCORE + PICTURE3);
		Map<String, List<String>> mapMethodsB = new HashMap<String, List<String>>();
		mapMethodsB.put(methodName4_, lstPicturesB);
		Map<String, Map<String, List<String>>> mapClassesB = new HashMap<String, Map<String, List<String>>>();
		mapClassesB.put(className4_, mapMethodsB);
		pictures.put(package2_, mapClassesB);
		// Execute
		results = executeGetTestAction(TestResult.class, pictures);
		// Assert
		assertTestAction(results, TestResult.class, lstPictures1.size() + lstPictures2.size() + lstPictures3.size()
				+ lstPicturesB.size());
	}

	/**
	 * testObjectが上記の条件を満たさない。
	 */
	@Test
	public void testGetTestAction_other() {
		Class[] classes = new Class[] { SimpleCaseResult.class, MetaTabulatedResult.class, TabulatedResult.class };
		List<TestAction> results = null;
		for (Class testInstance : classes) {
			// Execute
			results = executeGetTestAction(testInstance, null);

			// 結果は空のリストである。
			assertEquals(0, results.size());
		}
	}

}
