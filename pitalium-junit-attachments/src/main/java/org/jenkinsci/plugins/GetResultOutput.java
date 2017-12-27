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
import java.util.*;
import java.util.regex.*;

/**テスト結果から画像を抽出する．画像コピーと一覧作成を実行
 * 結果テーブルで利用するjsonファイルも併せて作成*/
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
        this.attachmentsStorage = PtlPublisher.getAttachmentPath(build);
        this.workspace = workspace;
        this.resultPicsAddr= resultPicsAddr;
    }

    /**画像の検索と画像コピー，画像リスト作成および，結果用jsonファイルの作成*/
    public HashMap<String,HashMap<String,HashMap<String,List<String>>>> getPictures(){
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

                    //結果格納用フォルダの作成
                    FilePath target = PtlPublisher.getAttachmentPath(attachmentsStorage, pkgName);
                    target = PtlPublisher.getAttachmentPath(target,clsName);
                    target = PtlPublisher.getAttachmentPath(target,caseName);
                    target.mkdirs();

                    //pictures_map.get(pkgName).get(clsName).put(caseName,SearchPictures(suiteResult,caseName,target));
                    pictures_map.get(pkgName).get(clsName).put(caseName,SearchPicturesWithPruning(suiteResult,caseName,target));
                }
            }
            FilePath resultjspath = PtlPublisher.getAttachmentPath(attachmentsStorage, "result.js");
            resultjspath.write("var resultdata="+gson.toJson(json_package),null);
            return  pictures_map;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**探索ディレクトリから画像ファイルを検索して，コピーし，ファイル名の一覧を作成*/
    //TODO チェック：ファイル操作
    private List<String> SearchPictures(SuiteResult suiteResult,String caseName,FilePath target){
        FilePath resultDirectory=new FilePath(workspace,resultPicsAddr);
        String keyword=getTestName(caseName,getCapbilities(caseName),suiteResult);
        if(keyword==null)return Collections.emptyList();
        FilePath dir = resultDirectory;
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
            ArrayList<String> dstpics=new ArrayList<>();
            int filecounter=0;//ファイル名長すぎると500エラーになる対策
            for(String var:pics){
                FilePath src=new FilePath(dir,var);
                FilePath dst=new FilePath(target,filecounter+".png");
                dstpics.add(filecounter+".png");
                src.copyTo(dst);
                filecounter++;
            }
            return dstpics;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
    /**探索ディレクトリから画像ファイルを検索して，コピーし，ファイル名の一覧を作成
     * 探索に当たって，探索ディレクトリの枝切りを行う
     * Assump：basedir/PJ名（任意）/pitalium/target/work/test-cobertura/test-result/results/日付以下　のファイル構成*/
    private List<String> SearchPicturesWithPruning(SuiteResult suiteResult,String caseName,FilePath target){
        FilePath resultDirectory=new FilePath(workspace,resultPicsAddr);
        String keyword=getTestName(caseName,getCapbilities(caseName),suiteResult);//日付/クラス/ワイルドカード.png
        if(keyword==null)return Collections.emptyList();
        try {
            List<FilePath> dirs= resultDirectory.listDirectories();
            ArrayList<String> dstpics=new ArrayList<>();
           for(FilePath dir:dirs){
               dir=new FilePath(dir,"pitalium/target/work/test-cobertura/test-result/results/");
               if(!dir.exists()){
                   System.err.println(dir+" does not exist. continue next loop.");
                   continue;
               }
               final DirectoryScanner ds = new DirectoryScanner();
               ds.setIncludes(new String[]{keyword});
               ds.setBasedir(dir.getRemote());
               ds.scan();
               String pics[]=ds.getIncludedFiles();
               for(String var:pics){
                   FilePath src=new FilePath(dir,var);
                   var= new File(var).getName();
                   dstpics.add(var);
                   FilePath dst=new FilePath(target,var);
                   src.copyTo(dst);
               }
           }
            return dstpics;
        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**標準出力から探索ディレクトリを切り出し
     * @return String 探索する相対パス(パスの区切りは/に置換)*/
    //TODO チェック：パス操作
    // FILEPATH型じゃないString型で\つきパスを返すので，リモートでも動くか要確認．
    private String getSearchDirectory(SuiteResult suiteResult){
        String stdout=suiteResult.getStdout();
        int loc=stdout.lastIndexOf("[Save TestResult]");
        if (loc==-1){
            System.err.println("Fail to find out [Save TestResult] in "+suiteResult.getName()+".");
            return null;
        }
        String str=stdout.substring(loc);
        Pattern p =Pattern.compile("\\\\((\\d|_){19}\\\\.*)\\\\result.json");
        Matcher m=p.matcher(str);
        if(m.find()){
            String res=m.group(1);
            res=res.replace("\\","/");
            return res;
        }else{
            System.err.println("[Exception]Fail to tokenize "+str+". Following step in this case was skipped.");
            return null;
        }
    }
    /**テストケース名から，探索すべき画像名（ワイルドカード付）を作成
     * @return String 検索するファイル名*/
    private String getTestName(String testName,HashMap<String,String> capbilities,SuiteResult suiteResult){
        String directory=getSearchDirectory(suiteResult);
        if(directory==null){
            directory="";
        }
        Pattern p = Pattern.compile("^(.*)\\s?\\[Capabilities.*\\]$");
        Matcher m = p.matcher(testName);
        if (m.find()) {
            StringBuilder buff = new StringBuilder();
            buff.append("**/");
            buff.append(directory);
            buff.append("/"+m.group(1)+"_*_");
            if (capbilities.get("platform")!=null){
                buff.append(capbilities.get("platform"));
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
            return null;
        }
    }

    /**テストケース名から，端末情報のマップを作成
     * Capbilitiesの要素が，os=WINDOWSのように=がついた構造になってない場合，例外発生．*/
    private HashMap<String,String> getCapbilities(String testName){
        HashMap<String, String> capbility_map = new HashMap<String, String>();
        Pattern p = Pattern.compile("^(.*)\\s?\\[Capabilities\\s?\\[\\{(.*)\\}\\]\\]$");
        Matcher m = p.matcher(testName);
        if (m.find()) {
            String[] token = m.group(2).split(", ");
            for (int i = 0; i < token.length; i++) {
                String[] caps = token[i].split("=");
                capbility_map.put(caps[0], caps[1]);
            }
        }
        return capbility_map;
    }

    /**エラー情報を返す
     * 成功やスキップ時には，エラー名にSUCCESS，SKIPPEDが格納される．
     * エラー名はスタックトレースの1行目，エラー箇所はat～～の最初の行を抽出*/
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
