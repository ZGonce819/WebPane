package top.apolleonce;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;

public class DevToolsToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory {

    private static JBCefBrowser devToolsBrowser;
    private static Content devToolsContent;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
    }

    public static void openDevTools(Project project, CefBrowser cefBrowser) {
        if (cefBrowser == null) return;

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebPane DevTools");
        if (toolWindow == null) return;

        closeDevTools(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            CefBrowser devToolsCef = null;
            try {
                java.lang.reflect.Method method = CefBrowser.class.getMethod("getDevTools", new Class[0]);
                devToolsCef = (CefBrowser) method.invoke(cefBrowser, new Object[0]);
            } catch (NoSuchMethodException ex) {
                Messages.showInfoMessage("DevTools is not supported in this IDE version", "Info");
                return;
            } catch (Exception ex) {
                Messages.showInfoMessage("Failed to initialize DevTools: " + ex.getMessage(), "Info");
                return;
            }

            if (devToolsCef == null) {
                Messages.showInfoMessage("Developer tools are not available", "Info");
                return;
            }

            try {
                Component devToolsComponent = devToolsCef.getUIComponent();
                if (devToolsComponent != null) {
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(devToolsComponent, BorderLayout.CENTER);

                    ContentFactory factory = ApplicationManager.getApplication().getService(ContentFactory.class);
                    devToolsContent = factory.createContent(panel, "DevTools", false);

                    com.intellij.ui.content.ContentManager contentManager = toolWindow.getContentManager();
                    contentManager.addContent(devToolsContent);
                    toolWindow.show(() -> contentManager.setSelectedContent(devToolsContent));
                } else {
                    Messages.showInfoMessage("Failed to get DevTools UI component", "Info");
                }
            } catch (Exception ex) {
                Messages.showInfoMessage("Failed to open DevTools: " + ex.getMessage(), "Info");
            }
        });
    }

    public static void closeDevTools(Project project) {
        if (devToolsContent != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebPane DevTools");
                    if (toolWindow != null) {
                        com.intellij.ui.content.ContentManager contentManager = toolWindow.getContentManager();
                        contentManager.removeContent(devToolsContent, true);
                    }
                } catch (Exception ex) {
                    // Ignore errors during cleanup
                } finally {
                    if (devToolsBrowser != null) {
                        try {
                            devToolsBrowser.dispose();
                        } catch (Exception ex) {
                            // Ignore disposal errors
                        }
                        devToolsBrowser = null;
                    }
                    devToolsContent = null;
                }
            });
        }
    }

    public static boolean isDevToolsOpen(Project project) {
        if (devToolsContent == null) return false;
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebPane DevTools");
        if (toolWindow == null) return false;
        return toolWindow.getContentManager().getContents().length > 0;
    }
}
