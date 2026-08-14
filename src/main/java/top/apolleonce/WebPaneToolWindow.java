package top.apolleonce;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class WebPaneToolWindow implements ToolWindowFactory {

    private static final String URL_MSG = "Please enter the correct URL and press Enter";
    private static final double ZOOM_STEP = 0.1;
    private static final double MEMORY_WARNING_THRESHOLD = 0.9;

    private JBCefBrowser browser;
    private JTextField urlField;
    private ContentFactory contentFactory;
    private double zoomFactor = 1.0;
    private JButton backButton;
    private JButton forwardButton;
    private JMenuItem devToolsButton;

    private static WebPaneToolWindow instance;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ApplicationManager.getApplication().invokeLater(() -> initContent(project, toolWindow));
    }

    private void initContent(Project project, ToolWindow toolWindow) {
        instance = this;

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel toolbar = createTopToolbar();

        browser = new JBCefBrowser();
        doNoOpenNewWindow();
        doMonitorUpdateURL();

        mainPanel.add(toolbar, BorderLayout.NORTH);
        mainPanel.add(browser.getComponent(), BorderLayout.CENTER);

        loadWelcomeMessage();

        if (backButton != null) backButton.setEnabled(false);
        if (forwardButton != null) forwardButton.setEnabled(false);

        com.intellij.ui.content.ContentManager contentManager = toolWindow.getContentManager();
        if (contentFactory == null) {
            contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        }
        Content content = contentFactory.createContent(mainPanel, "", false);
        contentManager.addContent(content);
    }

    public static WebPaneToolWindow getInstance() {
        return instance;
    }

    // ==================== Toolbar ====================

    private JPanel createTopToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());

        backButton = createBackButton();
        forwardButton = createForwardButton();
        JButton homeButton = createHomeButton();

        urlField = new JTextField(URL_MSG);
        urlField.setForeground(Color.GRAY);
        addPlaceholder();

        JButton moreButton = createMoreDropdownButton();

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftPanel.add(homeButton);
        leftPanel.add(backButton);
        leftPanel.add(forwardButton);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPanel.add(moreButton);

        toolbar.add(leftPanel, BorderLayout.WEST);
        toolbar.add(urlField, BorderLayout.CENTER);
        toolbar.add(rightPanel, BorderLayout.EAST);

        return toolbar;
    }

    private JButton createHomeButton() {
        JButton button = new JButton(AllIcons.Nodes.HomeFolder);
        button.setToolTipText("Home");
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(24, 24));
        button.setMaximumSize(new Dimension(24, 24));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener(e -> {
            Project project = getFirstProject();
            if (project != null && checkMemory(project)) {
                loadWelcomeMessage();
            }
        });
        return button;
    }

    private JButton createMoreDropdownButton() {
        JButton button = new JButton("Tools", AllIcons.General.ExternalTools);
        button.setHorizontalTextPosition(SwingConstants.LEFT);
        button.setIconTextGap(8);

        JPopupMenu popup = new JPopupMenu();
        applyThemeToPopupMenu(popup);

        popup.add(createDevToolMenuItem());
        popup.addSeparator();
        popup.add(createZoomInMenuItem());
        popup.add(createZoomOutMenuItem());
        popup.add(createResetZoomMenuItem());

        button.addActionListener(e -> popup.show(button, 0, button.getHeight()));
        return button;
    }

    // ==================== Theme ====================

    private void applyThemeToPopupMenu(JPopupMenu popup) {
        Color bgColor = UIUtil.getPanelBackground();
        popup.setBackground(bgColor);
        Border border = JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 1);
        popup.setBorder(border);

        for (Component comp : popup.getComponents()) {
            if (comp instanceof JMenuItem) {
                applyThemeToMenuItem((JMenuItem) comp);
            } else if (comp instanceof JSeparator) {
                comp.setBackground(bgColor);
            }
        }

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    }

    private void applyThemeToMenuItem(JMenuItem item) {
        item.setBackground(UIUtil.getPanelBackground());
        item.setForeground(UIUtil.getLabelForeground());
        item.setContentAreaFilled(true);
    }

    // ==================== Menu Items ====================

    private JButton createBackButton() {
        JButton button = new JButton(AllIcons.Actions.Back);
        button.setToolTipText("Back");
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(24, 24));
        button.setMaximumSize(new Dimension(24, 24));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener(e -> {
            if (browser == null) return;
            Project project = getFirstProject();
            if (project != null && checkMemory(project)) {
                browser.getCefBrowser().goBack();
            }
        });
        return button;
    }

    private JButton createForwardButton() {
        JButton button = new JButton(AllIcons.Actions.Forward);
        button.setToolTipText("Forward");
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(24, 24));
        button.setMaximumSize(new Dimension(24, 24));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.addActionListener(e -> {
            if (browser == null) return;
            Project project = getFirstProject();
            if (project != null && checkMemory(project)) {
                browser.getCefBrowser().goForward();
            }
        });
        return button;
    }

    private JMenuItem createDevToolMenuItem() {
        devToolsButton = new JMenuItem("F12");
        devToolsButton.setIcon(AllIcons.Toolwindows.ToolWindowDebugger);
        devToolsButton.addActionListener(e -> {
            Project project = getFirstProject();
            if (project != null && checkMemory(project)) {
                toggleDevTools();
            }
        });
        return devToolsButton;
    }

    private JMenuItem createZoomInMenuItem() {
        JMenuItem item = new JMenuItem("Zoom In");
        item.setIcon(AllIcons.General.ZoomIn);
        item.addActionListener(e -> {
            if (browser != null) {
                zoomFactor += ZOOM_STEP;
                browser.setZoomLevel(zoomFactor);
            }
        });
        return item;
    }

    private JMenuItem createZoomOutMenuItem() {
        JMenuItem item = new JMenuItem("Zoom Out");
        item.setIcon(AllIcons.General.ZoomOut);
        item.addActionListener(e -> {
            if (browser != null && zoomFactor > ZOOM_STEP) {
                zoomFactor -= ZOOM_STEP;
                browser.setZoomLevel(zoomFactor);
            }
        });
        return item;
    }

    private JMenuItem createResetZoomMenuItem() {
        JMenuItem item = new JMenuItem("Zoom Reset");
        item.setIcon(AllIcons.General.Reset);
        item.addActionListener(e -> {
            if (browser != null) {
                zoomFactor = 1.0;
                browser.setZoomLevel(zoomFactor);
            }
        });
        return item;
    }

    // ==================== Placeholder ====================

    private void addPlaceholder() {
        if (urlField == null) return;

        urlField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (URL_MSG.equals(urlField.getText())) {
                    urlField.setText("");
                    urlField.setForeground(UIUtil.getLabelForeground());
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (urlField.getText().trim().isEmpty()) {
                    urlField.setText(URL_MSG);
                    urlField.setForeground(Color.GRAY);
                }
            }
        });

        urlField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigateToUrl();
                }
            }
        });
    }

    // ==================== Navigation ====================

    private void navigateToUrl() {
        ApplicationManager.getApplication().invokeLater(() -> {
            String url = urlField.getText().trim();
            if (url.isEmpty() || browser == null) return;

            Project project = getFirstProject();
            if (project != null && !checkMemory(project)) return;

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            browser.loadURL(url);
            onUrlLoaded();
        });
    }

    private void onUrlLoaded() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (urlField != null) {
                urlField.setForeground(UIUtil.getLabelForeground());
            }
            updateNavigationButtons();
        });
    }

    private void updateNavigationButtons() {
        if (browser != null) {
            if (backButton != null) backButton.setEnabled(true);
            if (forwardButton != null) forwardButton.setEnabled(true);
        }
    }

    // ==================== Handlers ====================

    private void doMonitorUpdateURL() {
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser cefBrowser, boolean isLoading,
                                             boolean canGoBack, boolean canGoForward) {
                if (!isLoading) {
                    String url = cefBrowser.getURL();
                    if (url != null && !url.startsWith("file:///jbcefbrowser/")) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (urlField != null) {
                                urlField.setText(url);
                                urlField.setForeground(UIUtil.getLabelForeground());
                            }
                        });
                    }
                    onUrlLoaded();
                }
            }
        }, browser.getCefBrowser());
    }

    private void doNoOpenNewWindow() {
        browser.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser browser, CefFrame frame,
                                         String targetUrl, String targetFrameName) {
                if (targetUrl != null && !targetUrl.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (WebPaneToolWindow.this.browser != null) {
                            WebPaneToolWindow.this.browser.loadURL(targetUrl);
                            if (urlField != null) {
                                urlField.setText(targetUrl);
                            }
                        }
                    });
                }
                return true;
            }
        }, browser.getCefBrowser());
    }

    // ==================== DevTools ====================

    private void toggleDevTools() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!JBCefApp.isSupported()) {
                Messages.showErrorDialog("JCEF is disabled in IDE settings", "Error");
                return;
            }
            if (browser == null || browser.getCefBrowser() == null) {
                Messages.showErrorDialog("Browser is not properly initialized", "Error");
                return;
            }

            String url = browser.getCefBrowser().getURL();
            if (url == null || url.isEmpty() || "about:blank".equals(url)
                    || url.startsWith("file:///jbcefbrowser/")) {
                Messages.showInfoMessage("Please load a web page first before opening F12", "Info");
                return;
            }

            try {
                Project project = getFirstProject();
                if (project == null) return;

                if (DevToolsToolWindowFactory.isDevToolsOpen(project)) {
                    DevToolsToolWindowFactory.closeDevTools(project);
                } else {
                    DevToolsToolWindowFactory.openDevTools(project, browser.getCefBrowser());
                }
            } catch (Exception ex) {
                Messages.showErrorDialog("Failed to toggle DevTools: " + ex.getMessage(), "Error");
            }
        });
    }

    // ==================== Welcome & Resources ====================

    private void loadWelcomeMessage() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (browser == null) return;
            String html = loadHtmlFromResource("welcome.html");
            if (html != null) {
                browser.loadHTML(html);
            } else {
                System.err.println("Failed to load welcome.html");
            }
        });
    }

    private String loadHtmlFromResource(String resourceName) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    // ==================== Memory Check ====================

    private boolean checkMemory(Project project) {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        double ratio = (double) used / (double) max;

        if (ratio >= MEMORY_WARNING_THRESHOLD) {
            long usedMB = used / 1048576;
            long maxMB = max / 1048576;
            int percent = (int) (ratio * 100);

            String message = String.format(
                    "High Memory Usage Alert\n\nCurrent Usage: %d MB / %d MB (%d%%)\n\n" +
                    "Continuing may cause IDE lag or crash.\n\n" +
                    "Suggestion: Increase IDE memory configuration.\n\n" +
                    "How to fix:\nHelp \u2192 Edit Custom VM Options \u2192 Add: -Xmx\nThen restart IDE.",
                    usedMB, maxMB, percent
            );

            int result = Messages.showYesNoDialog(
                    project, message, "Memory Warning",
                    "Continue", "Cancel",
                    Messages.getWarningIcon()
            );
            return result == Messages.YES;
        }
        return true;
    }

    // ==================== Static Methods ====================

    public static void loadUrlFromExternal(Project project, String url) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (instance != null && instance.browser != null) {
                if (instance.checkMemory(project)) {
                    instance.browser.loadURL(url);
                    if (instance.urlField != null) {
                        instance.urlField.setText(url);
                        instance.urlField.setForeground(Color.LIGHT_GRAY);
                    }
                }
            }
        });
    }

    // ==================== Helpers ====================

    private Project getFirstProject() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        return projects.length > 0 ? projects[0] : null;
    }
}
