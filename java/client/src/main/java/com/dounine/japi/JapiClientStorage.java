package com.dounine.japi;

import com.alibaba.fastjson.JSON;
import com.dounine.japi.core.IAction;
import com.dounine.japi.core.IActionMethod;
import com.dounine.japi.core.IPackage;
import com.dounine.japi.core.IProject;
import com.dounine.japi.core.impl.response.ActionInfo;
import com.dounine.japi.exception.JapiException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.datetime.joda.LocalDateParser;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by lake on 17-2-24.
 */
public class JapiClientStorage {
    private String japiPath = null;
    private IProject project;
    private static final String[] TIPS = new String[]{" name not empty.", " name forbid '/' symbol."};
    private static final Logger LOGGER = LoggerFactory.getLogger(JapiClientStorage.class);
    private static final JapiClientStorage JAPI_CLIENT_STORAGE = new JapiClientStorage();

    static {
        JAPI_CLIENT_STORAGE.japiPath = FileUtils.getUserDirectoryPath() + "/.japi-client/";
        File japiClientDir = new File(JAPI_CLIENT_STORAGE.japiPath);
        if (!japiClientDir.exists()) {
            japiClientDir.mkdir();
        }
    }

    public void createProjectDir(String projectName) {
        if (StringUtils.isBlank(projectName)) {
            throw new JapiException("project" + TIPS[0]);
        }
        if (projectName.indexOf("/") != -1) {
            throw new JapiException("project" + TIPS[1]);
        }
        File file = new File(JAPI_CLIENT_STORAGE.japiPath + projectName);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public void createPackageDir(String projectName, String packageName) {
        createProjectDir(projectName);
        if (StringUtils.isBlank(packageName)) {
            throw new JapiException("package" + TIPS[0]);
        }
        if (packageName.indexOf("/") != -1) {
            throw new JapiException("package" + TIPS[1]);
        }
        File file = new File(JAPI_CLIENT_STORAGE.japiPath + projectName + "/" + packageName);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public void createFunDir(String projectName, String packageName, String funName) {
        createPackageDir(projectName, packageName);
        if (StringUtils.isBlank(funName)) {
            throw new JapiException("fun" + TIPS[0]);
        }
        if (funName.indexOf("/") != -1) {
            throw new JapiException("fun" + TIPS[1]);
        }
        File file = new File(JAPI_CLIENT_STORAGE.japiPath + projectName + "/" + packageName + "/" + funName);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public void createActionDir(String projectName, String packageName, String funName, String actionName) {
        createFunDir(projectName, packageName, funName);
        if (StringUtils.isBlank(actionName)) {
            throw new JapiException("action" + TIPS[0]);
        }
        if (actionName.indexOf("/") != -1) {
            throw new JapiException("action" + TIPS[1]);
        }
        File file = new File(JAPI_CLIENT_STORAGE.japiPath + projectName + "/" + packageName + "/" + funName + "/" + actionName);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public void createVersionDir(String projectName, String packageName, String funName, String actionName, String version) {
        createActionDir(projectName, packageName, funName, actionName);
        if (StringUtils.isBlank(actionName)) {
            throw new JapiException("version" + TIPS[0]);
        }
        if (actionName.indexOf("/") != -1) {
            throw new JapiException("version" + TIPS[1]);
        }
        File file = new File(JAPI_CLIENT_STORAGE.japiPath + projectName + "/" + packageName + "/" + funName + "/" + actionName + "/" + version);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    private void saveProjectInfo() {
        String pa = JAPI_CLIENT_STORAGE.japiPath + project.getProperties().get("japi.name");
        File projectInfoFile = new File(pa + "/project-info.txt");
        File projectInfoMd5File = new File(pa + "/project-md5.txt");
        StringBuffer stringBuffer = new StringBuffer();
        for (String key : project.getProperties().keySet()) {
            stringBuffer.append(key + "=" + project.getProperties().get(key) + "\n");
        }
        if (!projectInfoFile.exists()) {
            try {
                projectInfoFile.createNewFile();
                projectInfoMd5File.createNewFile();
                FileUtils.writeStringToFile(projectInfoFile, stringBuffer.toString(), Charset.forName("utf-8"), true);
                FileUtils.writeStringToFile(projectInfoMd5File, DigestUtils.md5Hex(stringBuffer.toString()), Charset.forName("utf-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                if(!DigestUtils.md5Hex(stringBuffer.toString()).equals(FileUtils.readFileToString(projectInfoMd5File,Charset.forName("utf-8")))){
                    projectInfoFile.delete();
                    projectInfoMd5File.delete();
                    projectInfoFile.createNewFile();
                    projectInfoMd5File.createNewFile();
                    FileUtils.writeStringToFile(projectInfoFile, stringBuffer.toString(), Charset.forName("utf-8"), true);
                    FileUtils.writeStringToFile(projectInfoMd5File, DigestUtils.md5Hex(stringBuffer.toString()), Charset.forName("utf-8"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void autoSaveToDisk() {
        String projectName = project.getProperties().get("japi.name");
        createProjectDir(projectName);
        saveProjectInfo();
        for (IPackage iPackage : project.getPackages()) {
            List<IAction> actions = iPackage.getActions();
            String packageName = iPackage.getName();
            createPackageDir(projectName, packageName);
            for (IAction action : actions) {
                String funName = action.getName();
                createFunDir(projectName, packageName, funName);
                List<IActionMethod> actionMethods = action.getMethods();
                List<ActionInfo> actionInfos = action.getActionInfos(actionMethods);
                Map<String, List<ActionInfo>> actionInfoMap = getActionVersions(actionInfos);
                for (String actionName : actionInfoMap.keySet()) {
                    createActionDir(projectName, packageName, funName, actionName);
                    for (ActionInfo versionInfo : actionInfoMap.get(actionName)) {
                        saveByTime(projectName, packageName, funName, versionInfo);
                    }
                }
            }
        }
    }

    public void saveByTime(String projectName, String packageName, String funName, ActionInfo actionInfo) {
        createVersionDir(projectName, packageName, funName, actionInfo.getActionName(), actionInfo.getVersion());
        File dateFold = new File(JAPI_CLIENT_STORAGE.japiPath + projectName + "/" + packageName + "/" + funName + "/" + actionInfo.getActionName() + "/" + actionInfo.getVersion() + "/date");
        File newDateFold = null;
        if (!dateFold.exists() || (null != dateFold && dateFold.list().length == 0)) {
            dateFold.mkdir();
            newDateFold = new File(dateFold.getAbsolutePath() + "/" + System.currentTimeMillis());
            newDateFold.mkdir();
            File infoFile = new File(newDateFold.getAbsolutePath() + "/info.txt");
            File md5File = new File(newDateFold.getAbsolutePath() + "/md5.txt");
            try {
                infoFile.createNewFile();
                md5File.createNewFile();
                FileUtils.writeStringToFile(infoFile, JSON.toJSONString(actionInfo), Charset.forName("utf-8"));
                FileUtils.writeStringToFile(md5File, DigestUtils.md5Hex(JSON.toJSONString(actionInfo)), Charset.forName("utf-8"));
                LOGGER.info(packageName + "/" + funName + "/" + actionInfo.getActionName() + " first created.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            final IOFileFilter javaFileFilter = FileFilterUtils.asFileFilter(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() && pathname.getName().matches("\\d{13}") && !pathname.getName().equals("date");
                }
            });

            List<File> dateFolds = new ArrayList<>();

            for (File file : dateFold.listFiles()) {
                dateFolds.add(file);
            }
            dateFolds.sort((b, a) -> a.getName().compareTo(b.getName()));
            File millFold = dateFolds.get(0);
            if (millFold.getName().matches("\\d{13}")) {
                try {
                    String oldMd5 = FileUtils.readFileToString(new File(millFold.getAbsolutePath() + "/md5.txt"), Charset.forName("utf-8"));
                    String newMd5 = DigestUtils.md5Hex(JSON.toJSONString(actionInfo));
                    if (!newMd5.equals(oldMd5)) {
                        LOGGER.info(packageName + "/" + funName + "/" + actionInfo.getActionName() + " has modified.");
                        newDateFold = new File(dateFold.getAbsolutePath() + "/" + System.currentTimeMillis());
                        newDateFold.mkdir();
                        File infoFile = new File(newDateFold.getAbsolutePath() + "/info.txt");
                        File md5File = new File(newDateFold.getAbsolutePath() + "/md5.txt");
                        try {
                            infoFile.createNewFile();
                            md5File.createNewFile();
                            FileUtils.writeStringToFile(infoFile, JSON.toJSONString(actionInfo), Charset.forName("utf-8"));
                            FileUtils.writeStringToFile(md5File, DigestUtils.md5Hex(JSON.toJSONString(actionInfo)), Charset.forName("utf-8"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Map<String, List<ActionInfo>> getActionVersions(List<ActionInfo> actionInfos) {
        Map<String, List<ActionInfo>> actionInfoMap = new HashMap<>();
        for (ActionInfo actionInfo : actionInfos) {
            if (actionInfoMap.get(actionInfo.getActionName()) == null) {
                actionInfoMap.put(actionInfo.getActionName(), new ArrayList<>());
                actionInfoMap.get(actionInfo.getActionName()).add(actionInfo);
            } else {
                actionInfoMap.get(actionInfo.getActionName()).add(actionInfo);
            }
        }
        return actionInfoMap;
    }

    public String getJapiPath() {
        return japiPath;
    }

    public void setJapiPath(String japiPath) {
        this.japiPath = japiPath;
    }

    public IProject getProject() {
        return project;
    }

    public void setProject(IProject project) {
        this.project = project;
    }
}