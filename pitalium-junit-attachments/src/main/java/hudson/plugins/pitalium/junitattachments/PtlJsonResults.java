package hudson.plugins.pitalium.junitattachments;
import java.util.ArrayList;
import java.util.List;

//めいめい規則
//メソド_id_Plat_borw_version
public class PtlJsonResults {
    private String resultId="";
    private List<PtlJsonScreenshotResult> screenshotResults;

    @Override
    public String toString(){
    	return resultId+"_"+screenshotResults;
    }

    public List<String[]> getFileNames(){
    	ArrayList<String[]> list=new ArrayList<String[]>();
    	for(PtlJsonScreenshotResult i: screenshotResults){
    		//日付フォルダ以降のパス,ファイル名,　　　飛ばす先の（親フォルダ）,子フォルダ
    		String addr[]=new String[4];
    		addr[0]=resultId+"/"+i.getFileName()[0];
    		addr[1]=i.getFileName()[1];

    		addr[2]=i.getTestClass();
    		addr[3]=i.getFolderName();

    		list.add(addr);
    	}
    	return list;
    }

}