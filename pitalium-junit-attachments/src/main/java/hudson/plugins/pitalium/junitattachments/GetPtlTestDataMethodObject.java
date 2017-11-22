/**
 * Copyright 2010-2011 Mirko Friedenhagen, Kohsuke Kawaguchi
 */

package hudson.plugins.pitalium.junitattachments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.tools.ant.DirectoryScanner;

import com.google.gson.Gson;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.junitattachments.GetTestDataMethodObject;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;

/**
 * This class is a helper for {@code hudson.tasks.junit.TestDataPublisher.getTestData(AbstractBuild<?, ?>, Launcher,
 * BuildListener, TestResult)}.
 *
 * @author mfriedenhagen
 * @author Kohsuke Kawaguchi
 */
public class GetPtlTestDataMethodObject extends GetTestDataMethodObject {


    private static final Logger LOG = Logger.getLogger(GetPtlTestDataMethodObject.class.getName());

    private final Run<?, ?> build;

    private final TestResult testResult;

    private final Map<String, Map<String, List<String>>> attachments = new HashMap<String, Map<String, List<String>>>();
    private final FilePath attachmentsStorage;
    private final TaskListener listener;

    /**
     * The workspace to check in for attachments.
     */
    private final FilePath workspace;

    /**
     * @param build
     *            see {@link GetPtlTestDataMethodObject#build}
     * @param testResult
     *            see {@link GetPtlTestDataMethodObject#testResult}
     */
    @Deprecated
    public GetPtlTestDataMethodObject(AbstractBuild<?, ?> build, @SuppressWarnings("unused") Launcher launcher,
            TaskListener listener, TestResult testResult) {
    	super(build, launcher, listener, testResult);
        this.build = build;
        this.testResult = testResult;
        this.listener = listener;
        attachmentsStorage = PtlAttachmentPublisher.getAttachmentPath(build);
        workspace = build.getWorkspace();
    }

    /**
     * @param build
     *            see {@link GetPtlTestDataMethodObject#build}
     * @param testResult
     *            see {@link GetPtlTestDataMethodObject#testResult}
     */
    public GetPtlTestDataMethodObject(Run<?, ?> build, @Nonnull FilePath workspace,
                                   @SuppressWarnings("unused") Launcher launcher,
                                   TaskListener listener, TestResult testResult) {
    	super(build, workspace, launcher, listener, testResult);
        this.build = build;
        this.testResult = testResult;
        this.listener = listener;
        attachmentsStorage = PtlAttachmentPublisher.getAttachmentPath(build);
        this.workspace = workspace;
    }

    /**
     * Returns a Map of classname vs. the stored attachments in a directory named as the test class.
     *
     * @return the map
     * @throws InterruptedException
     * @throws IOException
     * @throws IllegalStateException
     * @throws InterruptedException
     *
     */
    @Override
    public Map<String, Map<String, List<String>>> getAttachments() throws IllegalStateException, IOException, InterruptedException {
    	//テストクラス/ケース/のディレクトリ作成
    	for (SuiteResult suiteResult : testResult.getSuites()) {;
        	for (CaseResult caseResult : suiteResult.getCases()) {
        		FilePath target_tmp = PtlAttachmentPublisher.getAttachmentPath(attachmentsStorage, suiteResult.getName());
        		FilePath target = PtlAttachmentPublisher.getAttachmentPath(target_tmp, caseResult.getName());
        		target.mkdirs();
        	}
    	}

    	//テスト結果画像ファイルのコピー
    	//TODO ハードコーディングの除去
    	File dir=new File("C:/Users/user/Desktop/reports/results/");
    	readFolder(dir);
    	//ここまで

        //テストケースの走査
        for (SuiteResult suiteResult : testResult.getSuites()) {
        	for (CaseResult caseResult : suiteResult.getCases()) {
        		FilePath target = PtlAttachmentPublisher.getAttachmentPath(attachmentsStorage, suiteResult.getName());
        		target=PtlAttachmentPublisher.getAttachmentPath(target, caseResult.getName());
        		attachFilesForReportCase(suiteResult.getName(), caseResult.getName(),caseResult.getFullName(), target);
        	}
        }

        return attachments;
    }

    //以下，テスト結果画像のコピー　いつかクラス分ける
	public void readFolder( File dir ) {
		    File[] files = dir.listFiles();
		    if( files == null )
		      return;
		    for( File file : files ) {
		      if( !file.exists() )
		        continue;
		      else if( file.isDirectory() )
		        readFolder( file );
		      else if( file.isFile() )
		        execute( file );
		    }
	}

	public void execute( File file ) {
		//result.jsonならファイル内を走査
		//TODO ハードコーディング
		if(file.getName().equals("result.json"))parsejson(file.getAbsolutePath());
	}

	  public void parsejson(String path){
		  Gson gson = new Gson();
		  FileReader reader;
		try {
			reader = new FileReader(path);
			PtlJsonResults personFromFile = gson.fromJson(reader, PtlJsonResults.class);
		    reader.close();
		    //System.out.println(personFromFile.getFileNames());
		    for(String[] str:personFromFile.getFileNames()){
		    	final DirectoryScanner ds = new DirectoryScanner();
			    ds.setIncludes(new String[]{"**/"+str[1]+"*"+".png"});
			    File baseAddr=new File("C:/Users/user/Desktop/reports/results/"+str[0]+"/");//TODO ハードコーディング
			    ds.setBasedir(baseAddr);
			    ds.scan();
				 // 条件に適合したファイル・パスの一覧を配列として取得
				 final String[] files = ds.getIncludedFiles();
				 File dirName=new File(PtlAttachmentPublisher.getAttachmentPath(build).getRemote());
				 File[] files1 = dirName.listFiles();
				 Pattern p = Pattern.compile("\\Q"+str[2]+"\\E");
				 for (File file:files1) {
					 if(file.isDirectory()){
						 Matcher m = p.matcher(file.toString());
						 if(m.find()){
							 File[] files2 = file.listFiles();
							 p = Pattern.compile("\\Q"+str[3]+"\\E");
							 for (File file2:files2) {
								 if(file2.isDirectory()){
									 m = p.matcher(file2.toString());
									 if(m.find()){
										 for (int i = 0; i < files.length; i++) {
												FilePath src=new FilePath(new File(baseAddr.toString(),files[i]));
												//コピー先
												FilePath t1= PtlAttachmentPublisher.getAttachmentPath(build);
												t1= PtlAttachmentPublisher.getAttachmentPath(t1, file.getName().toString());
												t1= PtlAttachmentPublisher.getAttachmentPath(t1, file2.getName().toString());
												FilePath dst=new FilePath(t1,files[i]);
												src.copyTo(dst);
											}

									 }
								 }
							 }
						 }
					 }
				 }
		    }
		} catch (FileNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

	  }
	  //以上，結果画像のコピー

    private void attachFilesForReportCase(final String className, final String caseName, final String fullName,final FilePath target)
            throws IOException, InterruptedException {
        final FilePath testDir = PtlAttachmentPublisher.getAttachmentPath(build).child(className).child(caseName);
        if (testDir.exists()) {
	        DirectoryScanner d = new DirectoryScanner();
	        d.setBasedir(target.getRemote());
	        d.scan();
	        Map<String, List<String>> tests = new HashMap<String, List<String>>();
	        if(attachments.get(className)!=null)tests=attachments.get(className);//今の結果を取得

	        ArrayList<String> files=new ArrayList<String>(Arrays.asList(d.getIncludedFiles()));
	        for(int i = 0; i < files.size(); ++i){
	        files.set(i,caseName+"/"+files.get(i));
	        }
	        tests.put(caseName, files);
	        //tests.put(caseName, new ArrayList<String>(Arrays.asList(d.getIncludedFiles())));
	        attachments.put(className, tests);
        }
    }

}
