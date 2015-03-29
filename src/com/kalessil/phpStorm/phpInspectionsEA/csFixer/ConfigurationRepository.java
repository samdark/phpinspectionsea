package com.kalessil.phpStorm.phpInspectionsEA.csFixer;

import com.intellij.openapi.project.Project;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps fixer-inspection mapping and individual projects fixers status
 */
public class ConfigurationRepository {
    private static ConcurrentHashMap<Project, HashMap<String, Boolean>> projectsSettings = null;
    private static ConfigurationRepository instance = null;

    /* singleton, just one repository needed */
    public static ConfigurationRepository getInstance() {
        if (null == instance) {
            instance = new ConfigurationRepository();
        }

        return instance;
    }
    private ConfigurationRepository() {
        projectsSettings = new ConcurrentHashMap<Project, HashMap<String, Boolean>>();
    }

    /**
     * can be called both after actualization and configurations change,
     * so thread safe operations
     */
    public void store(Project project, HashMap<String, Boolean> fixersActivationSettings) {
        /* it's atomic operations, but main safety is due to re-creating objects in actualize */
        if (null == projectsSettings.replace(project, fixersActivationSettings)) {
            projectsSettings.putIfAbsent(project, fixersActivationSettings);
        }
    }

    /**
     * When no changes, false returned back. True means cleanup done
     * and keys allocated with empty settings paired.
     *
     * @param openProjects collection of actual projects
     * @return boolean
     */
    public boolean actualize(Project[] openProjects) {
        /* first ensure amount of open projects matched */
        boolean isActual = projectsSettings.size() == openProjects.length;
        if (isActual) {
            /* ensure projects are the same */
            for (Project project : openProjects) {
                if (!projectsSettings.containsKey(project)) {
                    isActual = false;
                    break;
                }
            }
            if (isActual) {
                return false;
            }
        }

        /* allocate new storage */
        ConcurrentHashMap<Project, HashMap<String, Boolean>> newProjectsSettings = new ConcurrentHashMap<Project, HashMap<String, Boolean>>();
        /* copy actual projects, no thread issues, as this is just local variable for a while */
        for (Project project : openProjects) {
            if (projectsSettings.containsKey(project)) {
                newProjectsSettings.put(project, projectsSettings.get(project));
            }
        }
        /* replace storage */
        projectsSettings = newProjectsSettings;

        /* flag cleanup performed, new projects shall be registered with store */
        return true;
    }
}
