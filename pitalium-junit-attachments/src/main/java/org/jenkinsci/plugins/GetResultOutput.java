package org.jenkinsci.plugins;

import com.google.gson.Gson;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import org.apache.tools.ant.DirectoryScanner;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.*;

public class GetResultOutput {

    private final Run<?, ?> build;
    private final TestResult testResult;
    private final FilePath attachmentsStorage;
    private final TaskListener listener;
    private final FilePath workspace;
    private final String resultPicsAddr;

    public GetResultOutput(Run<?, ?> build, @Nonnull FilePath workspace,
                                   @SuppressWarnings("unused") Launcher launcher,
                                   TaskListener listener, TestResult testResult,String resultPicsAddr) {
        this.build = build;
        this.testResult = testResult;
        this.listener = listener;
        this.attachmentsStorage = PitaPublisher.getAttachmentPath(build);
        this.workspace = workspace;
        this.resultPicsAddr= resultPicsAddr;
    }

    /**画像の検索と結果用jsonファイルの作成*/
    public HashMap<String,HashMap<String,HashMap<String,List<String>>>> getPictures(){
        /**@return jsfile <パッケージ名,<クラス名,<メソッド名,<key,val>>>>*/
        //注意：重複メソッド名はない仮定（上書きされる）
        Gson gson = new Gson();
        HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> json_package=new HashMap<>();
        HashMap<String,HashMap<String,HashMap<String,List<String>>>> pictures_map=new HashMap<>();

        try {
            for (SuiteResult suiteResult : testResult.getSuites()) {
                for (CaseResult caseResult : suiteResult.getCases()) {
                    String pkgName=caseResult.getPackageName();
                    String clsName=caseResult.getClassName();
                    String caseName=caseResult.getName();
                    if(!json_package.containsKey(pkgName)){
                        json_package.put(pkgName,new HashMap<String,HashMap<String,HashMap<String,String>>>());
                        pictures_map.put(pkgName,new HashMap<String,HashMap<String,List<String>>>());
                    }
                    if(!json_package.get(pkgName).containsKey(clsName)){
                        json_package.get(pkgName).put(clsName,new HashMap<String,HashMap<String,String>>());
                        pictures_map.get(pkgName).put(clsName,new HashMap<String,List<String>>());
                    }
                    HashMap<String,String> json_capabilities=new HashMap<>();
                    json_capabilities.putAll(getCapbilities(caseName));
                    json_capabilities.putAll(getErrorInfo(caseResult));
                    json_package.get(pkgName).get(clsName).put(caseName,json_capabilities);

                    //TODO ↓仕事的にはSearchPicturesが適切
                    FilePath target = PitaPublisher.getAttachmentPath(attachmentsStorage, pkgName);
                    target = PitaPublisher.getAttachmentPath(target,clsName);
                    target = PitaPublisher.getAttachmentPath(target,caseName);
                    target.mkdirs();

                    pictures_map.get(pkgName).get(clsName).put(caseName,SearchPictures(suiteResult,caseName,target));
                }
            }
            FilePath resultjspath = PitaPublisher.getAttachmentPath(attachmentsStorage, "result.js");
            resultjspath.write("var resultdata="+gson.toJson(json_package),null);
            return  pictures_map;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;//最後にpwとtryを最後に移して，削除
    }

    /**探索ディレクトリから画像ファイルを検索して，コピーし，ファイル名の一覧を作成*/
    //TODO ファイル操作
    private List<String> SearchPictures(SuiteResult suiteResult,String caseName,FilePath target){
        FilePath resultDirectory=new FilePath(workspace,resultPicsAddr);

        String keyword=getTestName(caseName,getCapbilities(caseName));
        String directory=getSearchDirectory(suiteResult.getStdout());
        FilePath dir = new FilePath(resultDirectory,directory);
        try {
            if(!dir.exists()){
                System.err.println(dir+" does not exist.");
                return Collections.emptyList();
            }
            final DirectoryScanner ds = new DirectoryScanner();
            ds.setIncludes(new String[]{keyword});
            ds.setBasedir(dir.getRemote());
            ds.scan();
            String pics[]=ds.getIncludedFiles();
            for(String var:pics){
                FilePath src=new FilePath(dir,var);
                FilePath dst=new FilePath(target,var);
                src.copyTo(dst);
            }

            return Arrays.asList(pics);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
    /**標準出力から探索ディレクトリを切り出し*/
    //TODO パス操作
    // FILEPATH型じゃないString型で\つきパスを返すので，リモートでも動くか要確認．
    private String getSearchDirectory(String stdout){
        String str=stdout.substring(stdout.lastIndexOf("[Save TestResult]"));
        Pattern p =Pattern.compile("\\\\((\\d|_){19}\\\\.*)\\\\result.json");
        Matcher m=p.matcher(str);
        if(m.find()){
            return m.group(1);
        }else{
            System.err.println("[Exception]Fail to tokenize "+str+". Following step in this case was skipped.");
            //TODO エラー処理
            return null;
        }
    }
    /**テストケース名から，探索すべき画像名を作成*/
    private String getTestName(String testName,HashMap<String,String> capbilities){
        Pattern p = Pattern.compile("^(.*)\\s?\\[Capabilities.*\\]$");
        Matcher m = p.matcher(testName);
        if (m.find()) {
            StringBuilder buff = new StringBuilder();
            buff.append("**/"+m.group(1)+"_*_");
            if (capbilities.get("platform")!=null){
                buff.append(capbilities.get("platform"));
                //TODO ヴァージョン？
            }
            buff.append('_');
            if (capbilities.get("browserName")!=null){
                buff.append(capbilities.get("browserName"));
                if(capbilities.get("version")!=null){
                    buff.append("_"+capbilities.get("version"));
                }
            }
            buff.append("*png");
            return buff.toString();
        }else{
            System.err.println("[Exception]Fail to tokenize "+testName+". Following step in this case was skipped.");
            //TODO エラー処理
            return null;
        }
    }

    /**テストケース名から，端末情報のマップを作成*/
    private HashMap<String,String> getCapbilities(String testName){
        HashMap<String, String> capbility_map = new HashMap<String, String>();
        Pattern p = Pattern.compile("^(.*)\\s?\\[Capabilities\\s?\\[\\{(.*)\\}\\]\\]$");
        Matcher m = p.matcher(testName);
        if (m.find()) {
            String[] token = m.group(2).split(", ");
            for (int i = 0; i < token.length; i++) {
                String[] caps = token[i].split("=");
                capbility_map.put(caps[0], caps[1]);//TODO ぬるぽの恐れ，try catch?
            }
        }
        return capbility_map;
    }

    private HashMap<String,String> getErrorInfo(CaseResult caseResult){
        HashMap<String, String> error_map = new HashMap<String, String>();
        if(caseResult.isSkipped()){
            error_map.put("errName","SKIPPED");
            error_map.put("errLocation","SKIPPED");
        }else if(caseResult.isPassed()){
            error_map.put("errName","SUCCESS");
            error_map.put("errLocation","SUCCESS");
        }else{//ERROR
            String trace=caseResult.getErrorStackTrace();
            String[] traceLine=trace.split("(\r)?\n");
            error_map.put("errName",traceLine[0].split(":")[0]);
            Pattern p = Pattern.compile("\\A\\t?at\\s?(.+)");
            for(String line:traceLine){
                Matcher m = p.matcher(line);
                if(m.find()){
                    error_map.put("errLocation",m.group(1));
                    break;
                }
            }
        }
        return error_map;
    }
}
