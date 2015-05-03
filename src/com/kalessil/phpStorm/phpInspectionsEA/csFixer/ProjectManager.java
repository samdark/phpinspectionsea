package com.kalessil.phpStorm.phpInspectionsEA.csFixer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class ProjectManager {
    private static ProjectManager instance = null;

    /* singleton, just one repository needed */
    public static ProjectManager getInstance() {
        if (null == instance) {
            instance = new ProjectManager();
        }

        return instance;
    }

    private ProjectManager() {
        loadedConfigurations = new HashMap<Project, Long>();
    }

    private HashMap<Project, Long> loadedConfigurations = null;

    @NotNull
    public HashMap<String, Boolean> getProjectConfiguration(@NotNull Project project) {
        /* find .php_cs in projects root */
        VirtualFile csFixerVirtualFile = project.getBaseDir().findChild(".php_cs");
        final boolean configurationExists = null != csFixerVirtualFile;
        if (configurationExists) {
            final boolean configurationLoaded = loadedConfigurations.containsKey(project);
            if (!configurationLoaded || csFixerVirtualFile.getModificationStamp() != loadedConfigurations.get(project)) {
                /* load or re-load */
                ConfigurationRepository.getInstance().store(project, readConfiguration(csFixerVirtualFile, project));
                loadedConfigurations.put(project, csFixerVirtualFile.getModificationStamp());
            }
        } else {
            /* put empty configuration associated with a project */
            ConfigurationRepository.getInstance().store(project, new HashMap<String, Boolean>());
            loadedConfigurations.put(project, (long) -1);
        }

        return ConfigurationRepository.getInstance().read(project);
    }

    @Nullable
    private HashMap<String, Boolean> readConfiguration(@NotNull VirtualFile configuration, @NotNull Project project) {
        HashMap<String, Boolean> settings = new HashMap<String, Boolean>();

        /* Get PSI file from Virtual one */
        PsiFile csFixerFile = PsiManager.getInstance(project).findFile(configuration);
        if (null != csFixerFile) {
            PsiTreeUtil.findChildrenOfType(csFixerFile, MethodReference.class);
        }

        return settings;
    }
}
