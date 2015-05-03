package com.kalessil.phpStorm.phpInspectionsEA.csFixer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.jetbrains.php.lang.PhpFileType;
import org.jetbrains.annotations.NotNull;

public class CsFixerFileTypeRegister implements ApplicationComponent {

    @Override
    public void initComponent() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                FileTypeManager.getInstance().associateExtension(PhpFileType.INSTANCE, "php_cs");
            }
        });
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "CsFixerFileTypeRegister";
    }
}
