package org.jenkinsci.plugins;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.tools.ant.DirectoryScanner;

import com.google.gson.Gson;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;

/**
 * テスト結果から画像を抽出する．画像コピーと一覧作成を実行 結果テーブルで利用するjsonファイルも併せて作成
 */
public class GetResultOutput {

	private final TestResult testResult;
	private final FilePath attachmentsStorage;
	private final FilePath workspace;
	private final String resultPicsAddr;

	/**
	 * コンストラクタ
	 *
	 * @param build Run contributing test data
	 * @param workspace Run workspace
	 * @param launcher Launcher
	 * @param listener Listener
	 * @param testResult Test result
	 * @param resultPicsAddr 結果画像フォルダの探索開始パス
	 */
	public GetResultOutput(Run<?, ?> build, @Nonnull FilePath workspace, Launcher launcher, TaskListener listener,
			TestResult testResult, String resultPicsAddr) {
		this.testResult = testResult;
		this.attachmentsStorage = PtlPublisher.getAttachmentPath(build);
		this.workspace = workspace;
		this.resultPicsAddr = resultPicsAddr;
	}

	/**
	 * 画像の検索と画像コピー，画像リスト作成および，結果用jsonファイルの作成
	 *
	 * @return pictures Map
	 */
	public Map<String, Map<String, Map<String, List<String>>>> getPictures() {
		//注意：重複メソッド名はない仮定（上書きされる）
		Gson gson = new Gson();
		Map<String, Map<String, Map<String, Map<String, String>>>> jsonPackage = new HashMap<>();
		Map<String, Map<String, Map<String, List<String>>>> picturesMap = new HashMap<>();

		try {
			for (SuiteResult suiteResult : testResult.getSuites()) {
				for (CaseResult caseResult : suiteResult.getCases()) {
					String pkgName = caseResult.getPackageName();
					String clsName = caseResult.getClassName();
					String caseName = caseResult.getName();
					if (!jsonPackage.containsKey(pkgName)) {
						jsonPackage.put(pkgName, new HashMap<String, Map<String, Map<String, String>>>());
						picturesMap.put(pkgName, new HashMap<String, Map<String, List<String>>>());
					}
					if (!jsonPackage.get(pkgName).containsKey(clsName)) {
						jsonPackage.get(pkgName).put(clsName, new HashMap<String, Map<String, String>>());
						picturesMap.get(pkgName).put(clsName, new HashMap<String, List<String>>());
					}
					Map<String, String> jsonCapabilities = new HashMap<>();
					jsonCapabilities.putAll(getCapbilities(caseName));
					jsonCapabilities.putAll(getErrorInfo(caseResult));
					jsonPackage.get(pkgName).get(clsName).put(caseName, jsonCapabilities);

					//結果格納用フォルダの作成
					FilePath target = PtlPublisher.getAttachmentPath(attachmentsStorage, pkgName);
					target = PtlPublisher.getAttachmentPath(target, clsName);
					target = PtlPublisher.getAttachmentPath(target, caseName);
					target.mkdirs();

					//pictures_map.get(pkgName).get(clsName).put(caseName,SearchPictures(suiteResult,caseName,target));
					picturesMap.get(pkgName).get(clsName).put(caseName,
							searchPicturesWithPruning(clsName, caseName, target));
				}
			}
			FilePath resultjspath = PtlPublisher.getAttachmentPath(attachmentsStorage, "result.js");
			resultjspath.write("var resultdata=" + gson.toJson(jsonPackage), null);
			return picturesMap;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 探索ディレクトリから画像ファイルを検索して，コピーし，
	 * ファイル名の一覧を作成 探索に当たって，探索ディレクトリの枝切りを行う
	 * Assump：basedir/PJ名（任意）/pitalium/target/work/test-cobertura/test-result/results/日付以下 のファイル構成
	 */
	private List<String> searchPicturesWithPruning(String className, String caseName, FilePath target) {
		// 日付/クラス/ワイルドカード.png
		FilePath resultDirectory = getSearchDirectory(className);
		String keyword = getTestName(caseName, getCapbilities(caseName));
		if (resultDirectory == null || keyword == null) {
			return Collections.emptyList();
		}
		try {
			ArrayList<String> dstpics = new ArrayList<>();
			final DirectoryScanner ds = new DirectoryScanner();
			ds.setIncludes(new String[] { keyword });
			ds.setBasedir(resultDirectory.getRemote());
			ds.scan();
			String pics[] = ds.getIncludedFiles();
			String fileName = null;
			for (String var : pics) {
				fileName = new File(var).getName();
				FilePath dst = new FilePath(target, fileName);
				FilePath src = resultDirectory.child(var);
				src.copyTo(dst);

				dstpics.add(fileName);
			}
			return dstpics;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	/**
	 * 結果画像フォルダの探索開始パスから、探索ディレクトリを検索して返す。 
	 * 1. 結果画像フォルダの探索開始パス以下で、 テストクラス名に一致するディレクトリを探す。 
	 * 2-1. 1つしか見つからなければ、その親ディレクトリ名を返す。
	 * 2-2. 複数見つかった場合は、更新日時が最新のものの親ディレクトリ名を返す。
	 * 
	 * @return FilePath 探索する相対パス
	 */
	//TODO チェック：パス操作
	private FilePath getSearchDirectory(String className) {
		// Remove package name from class name
		String clsName = className.substring(className.lastIndexOf(".") + 1);
		FilePath resultDirectory = new FilePath(workspace, resultPicsAddr);

		try {
			// Check exist
			if (!resultDirectory.exists()) {
				return null;
			}

			final DirectoryScanner ds = new DirectoryScanner();
			ds.setIncludes(new String[] { "**/" + clsName + "/result.json" });
			ds.setBasedir(resultDirectory.getRemote());
			ds.scan();
			String pics[] = ds.getIncludedFiles();

			FilePath folder = null;
			DateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
			for(int i = pics.length - 1; i >= 0; i--) {
				folder = resultDirectory.child(pics[i]).getParent();

				// Check name of parent folder by date
				try {
					format.parse(folder.getParent().getName());
					return folder;
				} catch (ParseException e) {
					// Invalid folder name
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * テストケース名から，探索すべき画像名（ワイルドカード付）を作成
	 *
	 * @return String 検索するファイル名
	 */
	private String getTestName(String testName, Map<String, String> capbilities) {
		Pattern p = Pattern.compile("^(.*)\\s?\\[Capabilities.*\\]$");
		Matcher m = p.matcher(testName);
		if (m.find()) {
			StringBuilder buff = new StringBuilder();
			buff.append(m.group(1) + "_*_");
			if (capbilities.get("platform") != null) {
				buff.append(capbilities.get("platform"));
			}
			buff.append('_');
			if (capbilities.get("browserName") != null) {
				buff.append(capbilities.get("browserName"));
				if (capbilities.get("version") != null) {
					buff.append("_" + capbilities.get("version"));
				}
			}
			buff.append("*png");
			return buff.toString();
		} else {
			System.err
					.println("[Exception]Fail to tokenize " + testName + ". Following step in this case was skipped.");
			return null;
		}
	}

	/**
	 * テストケース名から，端末情報のマップを作成 Capbilitiesの要素が，
	 * os=WINDOWSのように=がついた構造になってない場合，例外発生．
	 */
	private Map<String, String> getCapbilities(String testName) {
		Map<String, String> capabilityMap = new HashMap<String, String>();
		Pattern p = Pattern.compile("^(.*)\\s?\\[Capabilities\\s?\\[?\\{(.*)\\}\\]?\\]$");
		Matcher m = p.matcher(testName);
		if (m.find()) {
			String[] token = m.group(2).split(", ");
			for (int i = 0; i < token.length; i++) {
				String[] caps = null;
				if (token[i].contains("=")) {
					caps = token[i].split("=");
				} else if (token[i].contains(":")) {
					caps = token[i].split(":");
				}

				if(caps.length == 2) {
					capabilityMap.put(caps[0].trim(), caps[1].trim());
				}
			}
		}
		return capabilityMap;
	}

	/**
	 * エラー情報を返す 成功やスキップ時には，エラー名にSUCCESS，SKIPPEDが格納される．
	 * エラー名はスタックトレースの1行目，エラー箇所はat～～の最初の行を抽出
	 */
	private Map<String, String> getErrorInfo(CaseResult caseResult) {
		Map<String, String> errorMap = new HashMap<String, String>();
		if (caseResult.isSkipped()) {
			errorMap.put("errName", "SKIPPED");
			errorMap.put("errLocation", "SKIPPED");
		} else if (caseResult.isPassed()) {
			errorMap.put("errName", "SUCCESS");
			errorMap.put("errLocation", "SUCCESS");
		} else {
			//ERROR
			String trace = caseResult.getErrorStackTrace();
			String[] traceLine = trace.split("(\r)?\n");
			errorMap.put("errName", traceLine[0].split(":")[0]);
			Pattern p = Pattern.compile("\\A\\t?at\\s?(.+)");
			for (String line : traceLine) {
				Matcher m = p.matcher(line);
				if (m.find()) {
					errorMap.put("errLocation", m.group(1));
					break;
				}
			}
		}
		return errorMap;
	}
}
