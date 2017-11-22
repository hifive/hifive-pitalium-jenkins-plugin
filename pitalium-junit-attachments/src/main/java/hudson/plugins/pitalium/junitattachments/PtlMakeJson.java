package hudson.plugins.pitalium.junitattachments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import hudson.FilePath;

public class PtlMakeJson {
	public ArrayList<PtlCaseResult> results=new ArrayList<PtlCaseResult>();


	public void putList(PtlCaseResult result){
		results.add(result);
	}

	public void makeJson(FilePath build){
		File file = new File(build+"/perse_e_result.js");
		PrintWriter pw;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			pw.print("var data = [");
			boolean frag=false;
			for(PtlCaseResult result:results){
				if(result.isFailed()){
					if(frag)pw.printf(",");
					frag=true;
					pw.printf("{\"testname\":\"%s\",\"os\":\"%s\",\"browser\":\"%s\",\"version\":\"%s\",\"errName\":\"%s\",\"errLocation\":\"%s\"}",
							result.getTestName(),result.getOs(),result.getBrowser(),result.getBrowserVersion(),result.getErrName(),result.getErrLocation());
				}
			}
			pw.print("];");
			pw.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
}
