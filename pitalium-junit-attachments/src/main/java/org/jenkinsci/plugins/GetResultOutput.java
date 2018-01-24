package org.jenkinsci.plugins;

import java.io.File;
import java.io.IOException;
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
							searchPicturesWithPruning(suiteResult, caseName, target));
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
	private List<String> searchPicturesWithPruning(SuiteResult suiteResult, String caseName, FilePath target) {
		FilePath resultDirectory = new FilePath(workspace, resultPicsAddr);
		// 日付/クラス/ワイルドカード.png
		String keyword = getTestName(caseName, getCapbilities(caseName), suiteResult);
		if (keyword == null) {
			return Collections.emptyList();
		}
		try {
			List<FilePath> dirs = resultDirectory.listDirectories();
			ArrayList<String> dstpics = new ArrayList<>();
			FilePath resultsDir = null;
			for (FilePath dir : dirs) {
				resultsDir = new FilePath(dir, "pitalium/target/work/test-cobertura/test-result/results/");
				if (!resultsDir.exists()) {
					System.err.println(resultsDir + " does not exist. continue next loop.");
					continue;
				}
				final DirectoryScanner ds = new DirectoryScanner();
				ds.setIncludes(new String[] { keyword });
				ds.setBasedir(resultsDir.getRemote());
				ds.scan();
				String pics[] = ds.getIncludedFiles();
				String fileName = null;
				for (String var : pics) {
					fileName = new File(var).getName();
					FilePath dst = new FilePath(target, fileName);
					FilePath src = new FilePath(resultsDir, var);
					src.copyTo(dst);

					dstpics.add(fileName);
				}
			}
			return dstpics;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	/**
	 * 標準出力から探索ディレクトリを切り出し
	 *
	 * @return String 探索する相対パス(パスの区切りは/に置換)
	 */
	//TODO チェック：パス操作
	// FILEPATH型じゃないString型で\つきパスを返すので，リモートでも動くか要確認．
	private String getSearchDirectory(SuiteResult suiteResult) {
		String stdout = suiteResult.getStdout();
		int loc = stdout.lastIndexOf("[Save TestResult]");
		if (loc == -1) {
			System.err.println("Fail to find out [Save TestResult] in " + suiteResult.getName() + ".");
			return null;
		}
		String str = stdout.substring(loc);
		Pattern p = Pattern.compile("\\\\((\\d|_){19}\\\\.*)\\\\result.json");
		Matcher m = p.matcher(str);
		if (m.find()) {
			String res = m.group(1);
			res = res.replace("\\", "/");
			return res;
		} else {
			System.err.println("[Exception]Fail to tokenize " + str + ". Following step in this case was skipped.");
			return null;
		}
	}

	/**
	 * テストケース名から，探索すべき画像名（ワイルドカード付）を作成
	 *
	 * @return String 検索するファイル名
	 */
	private String getTestName(String testName, Map<String, String> capbilities, SuiteResult suiteResult) {
		String directory = getSearchDirectory(suiteResult);
		if (directory == null) {
			directory = "";
		}
		Pattern p = Pattern.compile("^(.*)\\s?\\[Capabilities.*\\]$");
		Matcher m = p.matcher(testName);
		if (m.find()) {
			StringBuilder buff = new StringBuilder();
			buff.append("**/");
			buff.append(directory);
			buff.append("/" + m.group(1) + "_*_");
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
		Pattern p = Pattern.compile("^(.*)\\s?\\[Capabilities\\s?\\[\\{(.*)\\}\\]\\]$");
		Matcher m = p.matcher(testName);
		if (m.find()) {
			String[] token = m.group(2).split(", ");
			for (int i = 0; i < token.length; i++) {
				String[] caps = token[i].split("=");
				capabilityMap.put(caps[0], caps[1]);
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
