import com.intellij.compiler.impl.FileSetCompileScope;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.util.lang.UrlClassLoader;

import javax.script.ScriptException;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE;

public class ActionToJs extends AnAction {

    @Override
    public void actionPerformed( AnActionEvent e) {
        compile(e);
    }

    private void compile(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
        PsiClass psiClass = psiJavaFile.getClasses()[0];


        VirtualFile virtualFile = e.getData(VIRTUAL_FILE);
        Project project = e.getProject();
        Module module = ModuleUtil.findModuleForFile(virtualFile, e.getProject());

        CompilerManager.getInstance(project).make(new FileSetCompileScope(Collections.singletonList(virtualFile), new Module[]{module}), new CompileStatusNotification() {
            @Override
            public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                if (aborted || errors > 0) return;
                try {
                    generate(module, psiClass);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void generate(Module module, PsiClass psiClass) throws ClassNotFoundException, MalformedURLException, ScriptException, NoSuchMethodException {
        List<URL> urls = new ArrayList<>();
        List<String> list = OrderEnumerator.orderEntries(module).recursively().runtimeOnly().getPathsList().getPathList();
        for (String path : list) {
                urls.add(new File(FileUtil.toSystemIndependentName(path)).toURI().toURL());
        }
        UrlClassLoader loader = UrlClassLoader.build().urls(urls).get();
        Class<?> targetClass =  loader.loadClass(psiClass.getQualifiedName());

        String js = Generator.generate(targetClass);
        copyToClipboard(new JavascriptBeautifierForJava().beautify(js));
    }

    private void copyToClipboard(String content) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable tText = new StringSelection(content);
        clip.setContents(tText, null);
    }


}