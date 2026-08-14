package top.apolleonce;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class HtmlWebPaneLineMarkerProvider implements LineMarkerProvider {

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        try {
            Icon icon = IconLoader.getIcon("/META-INF/webModuleGroup.svg", getClass());

            if (element instanceof XmlTag
                    && element.getParent() instanceof XmlDocument
                    && "html".equals(((XmlTag) element).getName())) {

                if (element.getContainingFile() instanceof XmlFile
                        && element.getContainingFile().getName().endsWith(".html")) {

                    VirtualFile vFile = element.getContainingFile().getVirtualFile();
                    if (vFile != null) {
                        return new LineMarkerInfo<>(
                                (XmlTag) element,
                                element.getTextRange(),
                                icon,
                                (Function<XmlTag, String>) e -> "Open in WebPane",
                                new WebPaneNavigationHandler(vFile),
                                GutterIconRenderer.Alignment.CENTER,
                                () -> "Open in WebPane"
                        );
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    static class WebPaneNavigationHandler implements GutterIconNavigationHandler<XmlTag> {
        private final VirtualFile file;

        WebPaneNavigationHandler(VirtualFile file) {
            this.file = file;
        }

        @Override
        public void navigate(MouseEvent e, XmlTag xmlTag) {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            if (projects.length == 0) return;

            Project project = projects[0];

            if (!JBCefApp.isSupported()) {
                Messages.showErrorDialog(project, "JCEF is not supported in this environment.", "Error");
                return;
            }

            String filePath = file.getPath();
            WebPaneToolWindow.loadUrlFromExternal(project, "file:///" + filePath.replace('\\', '/'));
        }
    }
}
