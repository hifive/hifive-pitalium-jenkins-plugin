package org.jenkinsci.plugins;

import hudson.FilePath;
import hudson.model.DirectoryBrowserSupport;
import hudson.tasks.junit.*;
import hudson.tasks.test.TestObject;
import jenkins.model.Jenkins;

import java.util.List;

/**テスト結果ページの表示で利用*/
public class PitaTestAction extends TestAction {

    private final FilePath storage;
    private final List<String> attachments;
    private final TestObject testObject;
    private final String condition_showtable;
    private final String condition_pkg;
    private final String condition_cls;

    public PitaTestAction(TestObject testObject, FilePath storage, List<String> attachments) {
        this.storage = storage;
        this.testObject = testObject;
        for(int i=0;i<attachments.size();i++){//uriencorder
            attachments.set(i,attachments.get(i).replace("#","%23"));
        }
        this.attachments = attachments;
        if (testObject instanceof CaseResult) {
            //ケースレベルではテーブルを表示しない
            this.condition_showtable="false";
            this.condition_pkg=null;
            this.condition_cls=null;
        }else if (testObject instanceof ClassResult) {
            String packageName = testObject.getParent().getName();
            String className = packageName+"."+testObject.getName();
            this.condition_showtable="true";
            this.condition_pkg=packageName;
            this.condition_cls=className;
        }else if (testObject instanceof PackageResult) {
            this.condition_showtable="true";
            this.condition_pkg=testObject.getName();
            this.condition_cls=null;
        }else if (testObject instanceof TestResult) {
            this.condition_showtable="true";
            this.condition_pkg=null;
            this.condition_cls=null;
        }else{
            //例外，テーブルは表示しない
            this.condition_showtable="false";
            this.condition_pkg=null;
            this.condition_cls=null;
        }
    }

    public String getDisplayName() {
        return "Attachments";
    }

    public String getIconFileName() {
        return "package.gif";
    }

    public String getUrlName() {
        return "attachments";
    }

    public DirectoryBrowserSupport doDynamic() {
        return new DirectoryBrowserSupport(this, storage, "Attachments", "package.gif", true);
    }

    @Override
    public String annotate(String text) {
        String url = Jenkins.getActiveInstance().getRootUrl()
                + testObject.getRun().getUrl() + "testReport"
                + testObject.getUrl() + "/attachments/";
        for (String attachment : attachments) {
            text = text.replace(attachment, "<a href=\"" + url + attachment
                    + "\">" + attachment + "</a>");
        }
        return text;
    }
    public String getCondition_showtable() {
        return condition_showtable;
    }

    public String getCondition_pkg() {
        return condition_pkg;
    }

    public String getCondition_cls() {
        return condition_cls;
    }
    public List<String> getAttachments() {
        return attachments;
    }

    public TestObject getTestObject() {
        return testObject;
    }

    public static boolean isImageFile(String filename) {
        return filename.matches("(?i).+\\.(gif|jpe?g|png)$");
    }

}
