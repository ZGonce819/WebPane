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
    private static boolean nativeDevToolsOpen = false;
    private static CefBrowser nativeDevToolsBrowser;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
    }

    public static void openDevTools(Project project, CefBrowser cefBrowser) {
        if (cefBrowser == null) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            // Approach 1: openDevTools() - opens DevTools in a native CEF window (newer JCEF versions)
            try {
                java.lang.reflect.Method method = CefBrowser.class.getMethod("openDevTools");
                method.invoke(cefBrowser);
                nativeDevToolsOpen = true;
                nativeDevToolsBrowser = cefBrowser;
                return;
            } catch (NoSuchMethodException ignored) {
                // Method doesn't exist in this JCEF version, try fallback
            } catch (Exception ex) {
                Messages.showInfoMessage("Failed to open DevTools: " + ex.getMessage(), "Error");
                return;
            }

            // Approach 2: getDevTools() - embeds DevTools in a panel (older JCEF versions)
            try {
                java.lang.reflect.Method method = CefBrowser.class.getMethod("getDevTools");
                CefBrowser devToolsCef = (CefBrowser) method.invoke(cefBrowser);
                if (devToolsCef != null) {
                    Component devToolsComponent = devToolsCef.getUIComponent();
                    if (devToolsComponent != null) {
                        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebPane DevTools");
                        if (toolWindow != null) {
                            JPanel panel = new JPanel(new BorderLayout());
                            panel.add(devToolsComponent, BorderLayout.CENTER);

                            ContentFactory factory = ApplicationManager.getApplication().getService(ContentFactory.class);
                            devToolsContent = factory.createContent(panel, "DevTools", false);

                            com.intellij.ui.content.ContentManager contentManager = toolWindow.getContentManager();
                            contentManager.addContent(devToolsContent);
                            toolWindow.show(() -> contentManager.setSelectedContent(devToolsContent));
                        }
                    }
                }
            } catch (Exception ex) {
                Messages.showInfoMessage("DevTools is not supported in this IDE version", "Info");
            }
        });
    }

    public static void closeDevTools(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // Close native DevTools if open
            if (nativeDevToolsOpen && nativeDevToolsBrowser != null) {
                try {
                    java.lang.reflect.Method method = CefBrowser.class.getMethod("closeDevTools");
                    method.invoke(nativeDevToolsBrowser);
                } catch (Exception ignored) {
                    // Method might not exist in older JCEF versions
                }
                nativeDevToolsOpen = false;
                nativeDevToolsBrowser = null;
            }

            // Close embedded DevTools if open
            if (devToolsContent != null) {
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
            }
        });
    }

    public static boolean isDevToolsOpen(Project project) {
        // Check native DevTools first
        if (nativeDevToolsOpen) return true;

        // Check embedded DevTools
        if (devToolsContent == null) return false;
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("WebPane DevTools");
        if (toolWindow == null) return false;
        return toolWindow.getContentManager().getContents().length > 0;
    }
}
