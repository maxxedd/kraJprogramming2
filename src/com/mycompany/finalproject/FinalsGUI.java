package com.mycompany.finalproject;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import static java.lang.ProcessHandle.current;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;



 /*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author Maxxedd
 * 
 * 
 */

//import java.aboutFrame;
//import java.aboutFrame;
        
public class FinalsGUI extends javax.swing.JFrame {
    
    private java.util.Map<javax.swing.JTextArea, java.util.Deque<String>> undoStacks = new java.util.HashMap<>();
    private java.util.Map<javax.swing.JTextArea, java.util.Deque<String>> redoStacks = new java.util.HashMap<>();
    private java.util.Map<javax.swing.JTextArea, Boolean> isUndoRedoInProgress = new java.util.HashMap<>();
    private java.util.Map<javax.swing.JTextArea, javax.swing.JTextArea> lineNumberMap = new java.util.HashMap<>();
    private boolean isDarkMode = false;
    
    private java.util.List<Project> projects = new java.util.ArrayList<>();
    private Project currentProject = null;
    private javax.swing.JPanel projectListPanel;

    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(FinalsGUI.class.getName());
    
   /* private void cleanupTextAreaResources(javax.swing.JTextArea textArea) {
        // Remove from undo/redo maps
        undoStacks.remove(textArea);
        redoStacks.remove(textArea);
        isUndoRedoInProgress.remove(textArea);
        lineNumberMap.remove(textArea);
    }*/
    private class Project {
        String name;
        java.util.List<TabData> tabs;
        java.util.Map<String, String> fileContents;
        java.io.File savePath; // Add this field to store the save location

        Project(String name) {
            this.name = name;
            this.tabs = new java.util.ArrayList<>();
            this.fileContents = new java.util.HashMap<>();
            this.savePath = null; // Initially no save location
        }

        Project(String name, java.io.File savePath) { // Optional constructor with save path
            this.name = name;
            this.tabs = new java.util.ArrayList<>();
            this.fileContents = new java.util.HashMap<>();
            this.savePath = savePath;
        }

        void addTab(String title, String content) {
            tabs.add(new TabData(title, content));
            fileContents.put(title, content);
        }

        void saveTabContent(String title, String content) {
            fileContents.put(title, content);
        }

        String getTabContent(String title) {
            return fileContents.get(title);
        }

        void removeTab(String title) {
            // Remove from tabs list
            tabs.removeIf(tab -> tab.title.equals(title));
            // Remove from file contents map
            fileContents.remove(title);
        }

        void setSavePath(java.io.File path) {
            this.savePath = path;
        }

        java.io.File getSavePath() {
            return savePath;
        }

    }
    
    private class TabData {
        String title;
        String content;
        
        TabData(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    
    /**
      * Creates new form FinalsGUI
        */
    public FinalsGUI() {
        initComponents();
        initializeProjectSystem();

        // Initialize undo/redo for the default text area
        initializeUndoRedoForTextArea(jTextArea1);

        addLineNumbersToTextArea(jTextArea1, jScrollPane1);
        
        setupKeyboardShortcuts();
        applySyntaxHighlighting();
        jButton1.setToolTipText("Run KRAJ Code (F5) - Executes the current file");
        
                jTextArea2.setEditable(false);


    }
    
    private void closeCurrentFile() {
        int selectedIndex = jTabbedPane1.getSelectedIndex();
        
        if (selectedIndex == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "No file is currently open.");
            return;
        }

        // Get tab title
        String tabTitle = jTabbedPane1.getTitleAt(selectedIndex);
        
        // Check if there are unsaved changes
        javax.swing.JTextArea currentArea = getCurrentTextArea();
        if (currentArea != null && currentProject != null) {
            String savedContent = currentProject.getTabContent(tabTitle);
            String currentContent = currentArea.getText();
            
            if (savedContent != null && !savedContent.equals(currentContent)) {
                int choice = javax.swing.JOptionPane.showConfirmDialog(
                    this,
                    "File '" + tabTitle + "' has unsaved changes. Save before closing?",
                    "Unsaved Changes",
                    javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
                
                if (choice == javax.swing.JOptionPane.YES_OPTION) {
                    saveFile();
                } else if (choice == javax.swing.JOptionPane.CANCEL_OPTION) {
                    return; // Don't close
                }
            }
        }

        // Remove the tab from tabbed pane
        jTabbedPane1.remove(selectedIndex);

        // If no tabs left, create a new one
        if (jTabbedPane1.getTabCount() == 0 && currentProject != null) {
            createNewFileInProject();
        }

        // Update UI
        jTabbedPane1.revalidate();
        jTabbedPane1.repaint();

        javax.swing.JOptionPane.showMessageDialog(this, "File closed: " + tabTitle);
    }

    private void setupKeyboardShortcuts() {
    // Setup Run shortcut (F5)
        KeyStroke runKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
        jTextArea1.getInputMap(JComponent.WHEN_FOCUSED).put(runKeyStroke, "runCode");
        jTextArea1.getActionMap().put("runCode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jButton1ActionPerformed(null);
            }
        });

        // Also setup for all text areas in tabs
        for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
            java.awt.Component component = jTabbedPane1.getComponentAt(i);
            javax.swing.JTextArea textArea = getTextAreaFromComponent(component);
            if (textArea != null) {
                textArea.getInputMap(JComponent.WHEN_FOCUSED).put(runKeyStroke, "runCode");
                textArea.getActionMap().put("runCode", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        jButton1ActionPerformed(null);
                    }
                });
            }
        }
    }

    private void quickSave() {
    if (currentProject == null) {
        javax.swing.JOptionPane.showMessageDialog(
            this, 
            "No project selected. Please create or open a project first.",
            "Quick Save Error",
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        return;
    }

    int currentTabIndex = jTabbedPane1.getSelectedIndex();
    if (currentTabIndex < 0) {
        javax.swing.JOptionPane.showMessageDialog(
            this, 
            "No file is currently open.",
            "Quick Save Error",
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        return;
    }

    // Check if the project itself is saved (has a save path)
    if (currentProject.savePath == null || !currentProject.savePath.exists()) {
        // Project is not saved, prompt user to save project first
        int choice = javax.swing.JOptionPane.showConfirmDialog(
            this,
            "The project '" + currentProject.name + "' is not saved.\n" +
            "You must save the project first before using Quick Save.\n\n" +
            "Do you want to save the project now?",
            "Project Not Saved",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );
        
        if (choice == javax.swing.JOptionPane.YES_OPTION) {
            // Save the project first
            saveProject();
            
            // Check if project was successfully saved
            if (currentProject.savePath == null || !currentProject.savePath.exists()) {
                // User cancelled or save failed
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Quick Save cancelled. Project was not saved.",
                    "Quick Save Cancelled",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            
            // Now proceed with quick save since project is saved
            performQuickSave(currentTabIndex);
        } else {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Quick Save cancelled. Save the project first to use Quick Save.",
                "Quick Save Cancelled",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        }
        } else {
            // Project is already saved, proceed with quick save
            performQuickSave(currentTabIndex);
        }
    }
    

    private void applySyntaxHighlighting() {
        javax.swing.JTextArea currentArea = getCurrentTextArea();
        if (currentArea != null) {
            // Create syntax highlighter
            krajHighlight highlighter = new krajHighlight(currentArea);
            highlighter.highlight();
        }
    }

    private void runKRAJCode(String code) {
        try {
            // Clear the output area
            SwingUtilities.invokeLater(() -> {
                jTextArea2.setText("");
                jTextArea2.append("🔄 Running KRAJ code...\n");
                jTextArea2.append("=".repeat(50) + "\n\n");
            });

            // Create an instance of krajInterpret
            krajInterpret interpreter = new krajInterpret();

            // Set up output listener to capture output
            interpreter.setOutputListener(message -> {
                SwingUtilities.invokeLater(() -> {
                    // Format error messages differently
                    if (message.contains("❌") || message.contains("Error") || 
                        message.contains("ERROR") || message.contains("Exception")) {
                        jTextArea2.setForeground(java.awt.Color.RED);
                    } else {
                        jTextArea2.setForeground(isDarkMode ? java.awt.Color.WHITE : java.awt.Color.BLACK);
                    }

                    jTextArea2.append(message + "\n");

                    // Auto-scroll to bottom
                    jTextArea2.setCaretPosition(jTextArea2.getDocument().getLength());
                });
            });

            // Set up input listener to show input dialogs
            interpreter.setInputListener(prompt -> {
                final String[] result = new String[1];
                try {
                    // Use invokeAndWait to show modal dialog
                    SwingUtilities.invokeAndWait(() -> {
                        result[0] = JOptionPane.showInputDialog(
                            FinalsGUI.this,
                            prompt,
                            "KRAJ Input Required",
                            JOptionPane.QUESTION_MESSAGE
                        );
                        // If user cancels, return empty string
                        if (result[0] == null) {
                            result[0] = "";
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    result[0] = "";
                }
                return result[0];
            });

            // Run the interpreter
            interpreter.run(code);

            // Show success message
            SwingUtilities.invokeLater(() -> {
                jTextArea2.append("\n" + "=".repeat(50) + "\n");
                jTextArea2.append("✅ Code execution completed successfully!\n");
            });

        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                jTextArea2.setForeground(java.awt.Color.RED);
                jTextArea2.append("\n❌ Unexpected error: " + e.getClass().getSimpleName() + "\n");
                jTextArea2.append("Message: " + e.getMessage() + "\n");

                // Print stack trace to output
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                jTextArea2.append(sw.toString());
            });
        }
    }



    private void openSampleProject() {
        // Get the current working directory (where the application is running from)
        String currentDir = System.getProperty("user.dir");

        // Construct the path to the samples folder
        java.io.File samplesFolder = new java.io.File(currentDir, "samples");

        // Alternative: If samples folder is in a subdirectory of the project
        // java.io.File samplesFolder = new java.io.File(currentDir, "FinalProject/samples");

        // Check if the samples folder exists
        if (!samplesFolder.exists() || !samplesFolder.isDirectory()) {
            javax.swing.JOptionPane.showMessageDialog(
                this, 
                "Samples folder not found at: " + samplesFolder.getAbsolutePath() + 
                "\n\nPlease make sure the 'samples' folder exists in the application directory.",
                "Samples Not Found",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Check if a project named "Samples" already exists
        Project existingSampleProject = null;
        for (Project p : projects) {
            if (p.name.equals("Samples")) {
                existingSampleProject = p;
                break;
            }
        }

        if (existingSampleProject != null) {
            // Ask if user wants to reload or switch to existing
            int choice = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "A 'Samples' project already exists. Switch to existing project?",
                "Samples Project Exists",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE
            );

            if (choice == javax.swing.JOptionPane.YES_OPTION) {
                switchToProject(existingSampleProject);
                return;
            }
        }

        // Create new project with the samples folder as save path
        Project sampleProject = new Project("Samples", samplesFolder);

        // List all text files in the samples directory
        java.io.File[] files = samplesFolder.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".txt") && 
            !name.equals("project.info") // exclude project info file
        );

        if (files == null || files.length == 0) {
            javax.swing.JOptionPane.showMessageDialog(
                this, 
                "No sample files found in the samples folder.\n" +
                "Please add some .txt files to the samples directory.",
                "No Sample Files",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Load all sample files into the project
        for (java.io.File file : files) {
            if (file.isFile()) {
                try {
                    String content = java.nio.file.Files.readString(file.toPath());
                    sampleProject.addTab(file.getName(), content);
                } catch (Exception e) {
                    javax.swing.JOptionPane.showMessageDialog(
                        this, 
                        "Error reading sample file: " + file.getName() + " - " + e.getMessage(),
                        "Read Error",
                        javax.swing.JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        }

        // Add project to the list
        projects.add(sampleProject);

        // Create project button
        String buttonText = sampleProject.name;
        if (sampleProject.savePath != null) {
            buttonText = sampleProject.name + " ✓"; // Add saved indicator
        }

        javax.swing.JButton projectButton = new javax.swing.JButton(buttonText);
        projectButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        java.awt.Dimension buttonSize = new java.awt.Dimension(270, 35);
        projectButton.setPreferredSize(buttonSize);
        projectButton.setMaximumSize(buttonSize);
        projectButton.setMinimumSize(buttonSize);

        // Tooltip with full path
        String tooltip = sampleProject.name;
        if (sampleProject.savePath != null) {
            tooltip = sampleProject.name + " - " + sampleProject.savePath.getAbsolutePath();
        }
        projectButton.setToolTipText(tooltip);

        // Apply theme
        if (isDarkMode) {
            projectButton.setBackground(new java.awt.Color(60, 60, 60));
            projectButton.setForeground(java.awt.Color.WHITE);
            projectButton.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(100, 100, 100)));
        } else {
            projectButton.setBackground(java.awt.Color.WHITE);
            projectButton.setForeground(java.awt.Color.BLACK);
            projectButton.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
        }

        projectButton.setMargin(new java.awt.Insets(5, 10, 5, 10));
        projectButton.addActionListener(e -> switchToProject(sampleProject));

        // Add button to project list panel
        projectListPanel.add(projectButton);
        projectListPanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 5)));
        projectListPanel.revalidate();
        projectListPanel.repaint();

        // Switch to the sample project
        switchToProject(sampleProject);

        // Show success message
        javax.swing.JOptionPane.showMessageDialog(
            this, 
            "Sample project loaded successfully!\n" +
            "Project: Samples\n" +
            "Location: " + samplesFolder.getAbsolutePath() + "\n" +
            "Files loaded: " + sampleProject.tabs.size(),
            "Samples Loaded",
            javax.swing.JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void performQuickSave(int tabIndex) {
        String tabTitle = jTabbedPane1.getTitleAt(tabIndex);
        javax.swing.JTextArea currentArea = getCurrentTextArea();

        if (currentArea == null) {
            javax.swing.JOptionPane.showMessageDialog(
                this, 
                "No text area found for the current tab.",
                "Quick Save Error",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Check if file is "Untitled" (new file that hasn't been saved before)
        if (tabTitle.equals("Untitled") || tabTitle.startsWith("Untitled ")) {
            // For untitled files in a saved project, we need to save them with a proper name
            saveFileAs();  // Use saveFileAs to get a proper filename
            return;
        }

        // File already has a name, save it to the project folder
        try {
            // Create safe filename
            String safeFileName = sanitizeFileName(tabTitle);
            if (!safeFileName.toLowerCase().endsWith(".txt")) {
                safeFileName += ".txt";
            }

            java.io.File file = new java.io.File(currentProject.savePath, safeFileName);

            // Save the file
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.write(currentArea.getText());

                // Update project content
                currentProject.saveTabContent(tabTitle, currentArea.getText());

                // Update tab title if filename was sanitized
                if (!safeFileName.equals(tabTitle + ".txt")) {
                    String displayName = getFileNameWithoutExtension(safeFileName);
                    updateTabTitle(tabIndex, displayName);
                }

                javax.swing.JOptionPane.showMessageDialog(
                    this, 
                    "File quickly saved: " + tabTitle + "\n" +
                    "Location: " + currentProject.savePath.getAbsolutePath(),
                    "Quick Save Successful",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (java.io.IOException e) {
            javax.swing.JOptionPane.showMessageDialog(
                this, 
                "Error during quick save: " + e.getMessage(),
                "Quick Save Error",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private void highlightCurrentProjectButton(Project projectToHighlight) {
        // Reset all buttons to normal style
        for (java.awt.Component comp : projectListPanel.getComponents()) {
            if (comp instanceof javax.swing.JButton) {
                javax.swing.JButton button = (javax.swing.JButton) comp;

                // Get clean button text (without indicators)
                String buttonText = button.getText();
                String cleanButtonText = buttonText
                    .replace(" *", "")
                    .replace(" ✓", "");

                // Apply normal style
                if (isDarkMode) {
                    button.setBackground(new java.awt.Color(60, 60, 60));
                    button.setForeground(java.awt.Color.WHITE);
                } else {
                    button.setBackground(java.awt.Color.WHITE);
                    button.setForeground(java.awt.Color.BLACK);
                }

                // If this is the current project button, highlight it
                if (cleanButtonText.equals(projectToHighlight.name)) {
                    if (isDarkMode) {
                        button.setBackground(new java.awt.Color(100, 100, 150));
                        button.setForeground(java.awt.Color.WHITE);
                    } else {
                        button.setBackground(new java.awt.Color(200, 220, 255));
                        button.setForeground(java.awt.Color.BLACK);
                    }
                }
            }
        }
    }


    private void closeCurrentProject() {
        if (currentProject == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No project is currently open.");
            return;
        }

        // Check for unsaved changes
        boolean hasUnsavedChanges = false;
        for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
            String tabTitle = jTabbedPane1.getTitleAt(i);
            java.awt.Component component = jTabbedPane1.getComponentAt(i);
            javax.swing.JTextArea textArea = getTextAreaFromComponent(component);

            if (textArea != null) {
                String savedContent = currentProject.getTabContent(tabTitle);
                String currentContent = textArea.getText();

                if (savedContent == null || !savedContent.equals(currentContent)) {
                    hasUnsavedChanges = true;
                    break;
                }
            }
        }

        // If there are unsaved changes, prompt the user
        if (hasUnsavedChanges) {
            int choice = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "Project '" + currentProject.name + "' has unsaved changes. Save before closing?",
                "Unsaved Changes",
                javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE
            );

            if (choice == javax.swing.JOptionPane.YES_OPTION) {
                saveProject();
            } else if (choice == javax.swing.JOptionPane.CANCEL_OPTION) {
                return; // Don't close
            }
            // If NO_OPTION, continue without saving
        }

        // Remove project button from projectListPanel
        removeProjectButton(currentProject);

        // Remove project from projects list
        projects.remove(currentProject);

        // Clear tabs
        jTabbedPane1.removeAll();

        // Reset current project
        Project closedProject = currentProject;
        currentProject = null;

        // Update project label
        updateProjectLabel("No Project Selected");

        // If there are other projects, switch to the first one
        if (!projects.isEmpty()) {
            switchToProject(projects.get(0));
        } else {
            // Create a default project if no projects left
            createNewProject("Default Project");
        }

        javax.swing.JOptionPane.showMessageDialog(
            this, 
            "Project closed: " + closedProject.name,
            "Project Closed",
            javax.swing.JOptionPane.INFORMATION_MESSAGE
        );
    }

    // Helper method to remove project button from panel
    private void removeProjectButton(Project project) {
        if (projectListPanel == null) return;

        // Find and remove the button for this project
        for (java.awt.Component comp : projectListPanel.getComponents()) {
            if (comp instanceof javax.swing.JButton) {
                javax.swing.JButton button = (javax.swing.JButton) comp;
                // Check if this button matches our project
                String buttonText = button.getText();

                // Remove any save indicators (* or ✓) for comparison
                String cleanButtonText = buttonText
                    .replace(" *", "")
                    .replace(" ✓", "");

                if (cleanButtonText.equals(project.name)) {
                    projectListPanel.remove(comp);
                    break;
                }
            }
        }

        // Remove any rigid area after the button
        for (java.awt.Component comp : projectListPanel.getComponents()) {
            if (comp instanceof javax.swing.Box.Filler) {
                projectListPanel.remove(comp);
                break;
            }
        }

        // Update the panel
        projectListPanel.revalidate();
        projectListPanel.repaint();
    }

    private void updateTabTitle(int tabIndex, String newTitle) {
        String oldTitle = jTabbedPane1.getTitleAt(tabIndex);
        jTabbedPane1.setTitleAt(tabIndex, newTitle);

        // Update in current project
        if (currentProject != null) {
            java.awt.Component component = jTabbedPane1.getComponentAt(tabIndex);
            javax.swing.JTextArea textArea = getTextAreaFromComponent(component);

            if (textArea != null) {
                String content = textArea.getText();

                // Update project data structure
                currentProject.fileContents.remove(oldTitle);
                currentProject.fileContents.put(newTitle, content);

                // Update TabData object
                for (TabData tab : currentProject.tabs) {
                    if (tab.title.equals(oldTitle)) {
                        tab.title = newTitle;
                        tab.content = content;
                        break;
                    }
                }
            }
        }
    }

    private void debugSaveProject() {
        try {
            saveProject();
        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(
                this, 
                "Save failed with error: " + e.toString() + 
                "\n\nStack trace printed to console.",
                "Save Error",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }


    private String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }
    
    private void saveFile() {
        if (currentProject == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No project selected.");
            return;
        }

        int currentTabIndex = jTabbedPane1.getSelectedIndex();
        if (currentTabIndex < 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "No file to save.");
            return;
        }

        String tabTitle = jTabbedPane1.getTitleAt(currentTabIndex);
        javax.swing.JTextArea currentArea = getCurrentTextArea();

        if (currentArea != null) {
            // Check if file is already saved (not Untitled)
            if (tabTitle.equals("Untitled") || tabTitle.startsWith("Untitled ")) {
                // Use Save As instead
                saveFileAs();
            } else {
                // Just update the content in project
                currentProject.saveTabContent(tabTitle, currentArea.getText());
                javax.swing.JOptionPane.showMessageDialog(
                    this, 
                    "File saved: " + tabTitle,
                    "Save Successful",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
            }
        }
    }


    private void openFileIntoProject() {
        if (currentProject == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No project selected. Please create or select a project first.");
            return;
        }

        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Open File into Project");

        int result = chooser.showOpenDialog(this);

        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();

            try {
                String content = java.nio.file.Files.readString(file.toPath());
                String fileName = file.getName();

                // Check if file already exists in current tab
                boolean fileExists = false;
                for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
                    if (jTabbedPane1.getTitleAt(i).equals(fileName)) {
                        fileExists = true;
                        break;
                    }
                }

                if (fileExists) {
                    int response = javax.swing.JOptionPane.showConfirmDialog(
                        this,
                        "A file with name '" + fileName + "' already exists. Open as duplicate?",
                        "File Already Exists",
                        javax.swing.JOptionPane.YES_NO_OPTION
                    );

                    if (response != javax.swing.JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                // Create new tab with the file content
                javax.swing.JPanel newPanel = createTextAreaWithLineNumbers();
                javax.swing.JTextArea openedArea = getTextAreaFromPanel(newPanel);
                openedArea.setText(content);

                jTabbedPane1.addTab(fileName, newPanel);
                jTabbedPane1.setSelectedComponent(newPanel);

                // Initialize undo/redo for the opened text area
                initializeUndoRedoForTextArea(openedArea);

                // Add to current project
                currentProject.addTab(fileName, content);

                javax.swing.JOptionPane.showMessageDialog(
                    this, 
                    "File opened successfully: " + fileName,
                    "File Opened",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );

            } catch (Exception e) {
                javax.swing.JOptionPane.showMessageDialog(this, "Failed to open file: " + e.getMessage());
            }
        }
    }
    
    private void openProjectFromFolder() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Open Project Folder");
        chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);

        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File projectFolder = chooser.getSelectedFile();

            if (!projectFolder.exists() || !projectFolder.isDirectory()) {
                javax.swing.JOptionPane.showMessageDialog(
                    this, 
                    "Selected path is not a valid directory.",
                    "Invalid Directory",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Get project name from folder
            String projectName = projectFolder.getName();

            // Create new project with save path
            Project newProject = new Project(projectName, projectFolder);

            // List all files in directory
            java.io.File[] files = projectFolder.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".txt") || 
                !name.equals("project.info") // exclude project info file from tab list
            );

            if (files != null && files.length > 0) {
                // Add files to project
                for (java.io.File file : files) {
                    if (file.isFile() && !file.getName().equals("project.info")) {
                        try {
                            String content = java.nio.file.Files.readString(file.toPath());
                            newProject.addTab(file.getName(), content);
                        } catch (Exception e) {
                            javax.swing.JOptionPane.showMessageDialog(
                                this, 
                                "Error reading file: " + file.getName() + " - " + e.getMessage(),
                                "Read Error",
                                javax.swing.JOptionPane.WARNING_MESSAGE
                            );
                        }
                    }
                }

                // Add project to list and switch to it
                projects.add(newProject);

                // Create project button with save path indicator
                String buttonText = newProject.name;
                if (newProject.savePath != null) {
                    buttonText = newProject.name + " *"; // Add indicator
                }

                javax.swing.JButton projectButton = new javax.swing.JButton(buttonText);
                projectButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

                java.awt.Dimension buttonSize = new java.awt.Dimension(270, 35);
                projectButton.setPreferredSize(buttonSize);
                projectButton.setMaximumSize(buttonSize);
                projectButton.setMinimumSize(buttonSize);

                // Tooltip with full path
                String tooltip = newProject.name;
                if (newProject.savePath != null) {
                    tooltip = newProject.name + " - " + newProject.savePath.getAbsolutePath();
                }
                projectButton.setToolTipText(tooltip);

                if (isDarkMode) {
                    projectButton.setBackground(new java.awt.Color(60, 60, 60));
                    projectButton.setForeground(java.awt.Color.WHITE);
                    projectButton.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(100, 100, 100)));
                } else {
                    projectButton.setBackground(java.awt.Color.WHITE);
                    projectButton.setForeground(java.awt.Color.BLACK);
                    projectButton.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
                }

                projectButton.setMargin(new java.awt.Insets(5, 10, 5, 10));
                projectButton.addActionListener(e -> switchToProject(newProject));

                projectListPanel.add(projectButton);
                projectListPanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 5)));
                projectListPanel.revalidate();
                projectListPanel.repaint();

                // Switch to the new project
                switchToProject(newProject);

                javax.swing.JOptionPane.showMessageDialog(
                    this, 
                    "Project loaded successfully!\n" +
                    "Project: " + projectName + "\n" +
                    "Location: " + projectFolder.getAbsolutePath() + "\n" +
                    "Files loaded: " + newProject.tabs.size(),
                    "Project Loaded",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                    this, 
                    "No files found in the selected folder.",
                    "Empty Project",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
            }
        }

    }
    
    private boolean isValidDirectory(java.io.File directory) {
        if (directory == null) return false;

        // Check if it exists
        if (!directory.exists()) {
            // Try to create it
            return directory.mkdirs();
        }

        // Check if it's a directory
        if (!directory.isDirectory()) {
            return false;
        }

        // Check if we can write to it
        return directory.canWrite();
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "untitled";
        }

        // Remove invalid characters for filenames
        String sanitized = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // Remove leading/trailing spaces and dots
        sanitized = sanitized.trim();
        sanitized = sanitized.replaceAll("^\\.+", "").replaceAll("\\.+$", "");

        // Ensure not empty
        if (sanitized.isEmpty()) {
            sanitized = "untitled";
        }

        // Limit length (Windows has 260 char limit for path)
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }

    
    private void saveProject() {
        if (currentProject == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No project selected.");
            return;
        }

        java.io.File projectFolder;

        // Check if project already has a save path
        if (currentProject.savePath != null && currentProject.savePath.exists()) {
            // Ask if user wants to save to existing location or choose new one
            int choice = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "Project already has a save location:\n" +
                currentProject.savePath.getAbsolutePath() + "\n\n" +
                "Save to this location? (No = Choose new location)",
                "Save Project",
                javax.swing.JOptionPane.YES_NO_OPTION
            );

            if (choice == javax.swing.JOptionPane.YES_OPTION) {
                // Save to existing location
                projectFolder = currentProject.savePath;
            } else {
                // Let user choose new location
                projectFolder = chooseSaveLocation();
                if (projectFolder == null) {
                    return; // User cancelled
                }
                currentProject.setSavePath(projectFolder);
            }
        } else {
            // No existing save location, ask user to choose one
            projectFolder = chooseSaveLocation();
            if (projectFolder == null) {
                return; // User cancelled
            }
            currentProject.setSavePath(projectFolder);
        }

        // Update project button text to show it's saved
        updateProjectButton(currentProject);

        // Save the project to the folder
        boolean success = saveProjectToFolder(projectFolder);

        if (success) {
            javax.swing.JOptionPane.showMessageDialog(
                this, 
                "Project saved successfully!\n" +
                "Location: " + projectFolder.getAbsolutePath() + "\n" +
                "Project Name: " + currentProject.name,
                "Project Saved",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private java.io.File chooseSaveLocation() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Save Project As");
        chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        // Set default directory
        if (currentProject.savePath != null) {
            chooser.setCurrentDirectory(currentProject.savePath.getParentFile());
        } else {
            chooser.setCurrentDirectory(new java.io.File(System.getProperty("user.home")));
        }

        // Suggest project folder name
        chooser.setSelectedFile(new java.io.File(currentProject.name));

        if (chooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File selectedDir = chooser.getSelectedFile();
            java.io.File projectFolder = new java.io.File(selectedDir, currentProject.name);

            // Check if folder already exists
            if (projectFolder.exists()) {
                int overwrite = javax.swing.JOptionPane.showConfirmDialog(
                    this,
                    "A folder named '" + currentProject.name + "' already exists.\n" +
                    "Do you want to overwrite it?",
                    "Folder Exists",
                    javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );

                if (overwrite != javax.swing.JOptionPane.YES_OPTION) {
                    return null;
                }
            }

            return projectFolder;
        }

        return null; // User cancelled
    }

    private void updateProjectButton(Project project) {
        // Update the project button in the list to show saved status
        for (java.awt.Component comp : projectListPanel.getComponents()) {
            if (comp instanceof javax.swing.JButton) {
                javax.swing.JButton button = (javax.swing.JButton) comp;
                if (button.getText().startsWith(project.name)) {
                    // Update button text and tooltip
                    String buttonText = project.name;
                    if (project.savePath != null) {
                        buttonText = project.name + " ✓";
                    }
                    button.setText(buttonText);

                    String tooltip = project.name;
                    if (project.savePath != null) {
                        tooltip = project.name + " - " + project.savePath.getAbsolutePath();
                    }
                    button.setToolTipText(tooltip);
                    break;
                }
            }
        }
    }

    private boolean saveProjectToFolder(java.io.File projectFolder) {
        // Create folder if it doesn't exist
        if (!projectFolder.exists()) {
            if (!projectFolder.mkdirs()) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Failed to create project folder: " + projectFolder.getAbsolutePath(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                );
                return false;
            }
        }

        boolean success = true;
        int filesSaved = 0;

        try {
            // Save all tabs as files
            for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
                String tabTitle = jTabbedPane1.getTitleAt(i);
                java.awt.Component component = jTabbedPane1.getComponentAt(i);
                javax.swing.JTextArea textArea = getTextAreaFromComponent(component);

                if (textArea != null) {
                    // Create safe filename
                    String safeFileName = sanitizeFileName(tabTitle);
                    if (!safeFileName.toLowerCase().endsWith(".txt")) {
                        safeFileName += ".txt";
                    }

                    java.io.File file = new java.io.File(projectFolder, safeFileName);
                    try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                        writer.write(textArea.getText());
                        filesSaved++;

                        // Update project content
                        currentProject.saveTabContent(tabTitle, textArea.getText());

                        // Update tab title if filename was sanitized
                        if (!safeFileName.equals(tabTitle + ".txt")) {
                            String displayName = getFileNameWithoutExtension(safeFileName);
                            updateTabTitle(i, displayName);
                        }
                    } catch (java.io.IOException e) {
                        success = false;
                        javax.swing.JOptionPane.showMessageDialog(
                            this, 
                            "Error saving file: " + safeFileName + " - " + e.getMessage(),
                            "Save Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }

            // Save project info
            java.io.File projectInfo = new java.io.File(projectFolder, "project.info");
            try (java.io.FileWriter writer = new java.io.FileWriter(projectInfo)) {
                writer.write("Project Name: " + currentProject.name + "\n");
                writer.write("Files Count: " + filesSaved + "\n");
                writer.write("Created: " + new java.util.Date() + "\n");
                writer.write("Last Modified: " + new java.util.Date() + "\n");
                writer.write("IDE: Simple IDE Project\n");
            } catch (java.io.IOException e) {
                javax.swing.JOptionPane.showMessageDialog(
                    this, 
                    "Error saving project info: " + e.getMessage(),
                    "Save Error",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
            }

            return success;

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(
                this, 
                "Error saving project: " + e.getMessage(),
                "Save Error",
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

   
    private void saveFileAs() {
        if (currentProject == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No project selected.");
            return;
        }

        int currentTabIndex = jTabbedPane1.getSelectedIndex();
        if (currentTabIndex < 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "No file to save.");
            return;
        }

        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Save File As");

        String currentTabTitle = jTabbedPane1.getTitleAt(currentTabIndex);
        if (!currentTabTitle.equals("Untitled") && !currentTabTitle.startsWith("Untitled ")) {
            chooser.setSelectedFile(new java.io.File(currentTabTitle));
        }

        int result = chooser.showSaveDialog(this);

        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();

            // Ensure .txt extension
            String fileName = file.getName();
            if (!fileName.toLowerCase().endsWith(".txt")) {
                file = new java.io.File(file.getAbsolutePath() + ".txt");
                fileName = file.getName();
            }

            try {
                // Ensure parent directory exists
                java.io.File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        javax.swing.JOptionPane.showMessageDialog(
                            this, 
                            "Cannot create directory: " + parentDir.getAbsolutePath(),
                            "Save Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }
                }

                try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                    javax.swing.JTextArea currentArea = getCurrentTextArea();
                    if (currentArea != null) {
                        writer.write(currentArea.getText());

                        // Update tab title with saved filename
                        updateTabTitle(currentTabIndex, fileName);

                        javax.swing.JOptionPane.showMessageDialog(
                            this, 
                            "File saved as: " + fileName,
                            "Save Successful",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                }
            } catch (java.io.IOException e) {
                javax.swing.JOptionPane.showMessageDialog(
                    this, 
                    "Error saving file: " + e.getMessage() + "\nPath: " + file.getAbsolutePath(),
                    "Save Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }


    private void initializeProjectSystem() {
        // Create project list panel
        projectListPanel = new javax.swing.JPanel();
        projectListPanel.setLayout(new javax.swing.BoxLayout(projectListPanel, javax.swing.BoxLayout.Y_AXIS));

        // Add scroll pane for projects
        javax.swing.JScrollPane projectScrollPane = new javax.swing.JScrollPane(projectListPanel);
        projectScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Projects"));
        projectScrollPane.setPreferredSize(new java.awt.Dimension(280, 350));
        projectScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        projectScrollPane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Clear and setup jPanel2 properly
        jPanel2.removeAll();

        // Set fixed size for jPanel2 to match the original layout
        jPanel2.setPreferredSize(new java.awt.Dimension(300, 440));
        jPanel2.setMinimumSize(new java.awt.Dimension(300, 440));
        jPanel2.setMaximumSize(new java.awt.Dimension(300, 440));

        jPanel2.setLayout(new java.awt.BorderLayout(0, 10));

        // Create a panel for the fileNameLabel
        javax.swing.JPanel labelPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        labelPanel.setPreferredSize(new java.awt.Dimension(280, 30));
        labelPanel.setMaximumSize(new java.awt.Dimension(280, 30));
        labelPanel.add(fileNameLabel);

        // Create panel for the "Create New Project" button
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        buttonPanel.setPreferredSize(new java.awt.Dimension(280, 40));

        // Style the "Create New Project" button
        jButton2.setPreferredSize(new java.awt.Dimension(200, 30));
        jButton2.setMaximumSize(new java.awt.Dimension(200, 30));

        // Apply theme to the button
        if (isDarkMode) {
            jButton2.setBackground(new java.awt.Color(80, 80, 80));
            jButton2.setForeground(java.awt.Color.WHITE);
            jButton2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(100, 100, 100)));
        } else {
            jButton2.setBackground(new java.awt.Color(240, 240, 240));
            jButton2.setForeground(java.awt.Color.BLACK);
            jButton2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
        }

        buttonPanel.add(jButton2);

        // Add components to jPanel2
        jPanel2.add(labelPanel, java.awt.BorderLayout.NORTH);
        jPanel2.add(projectScrollPane, java.awt.BorderLayout.CENTER);
        jPanel2.add(buttonPanel, java.awt.BorderLayout.SOUTH);

        // Refresh the panel
        jPanel2.revalidate();
        jPanel2.repaint();

        // Create default project
        createNewProject("Default Project");
    }

    
    // Create a new project
    private void createNewProject(String projectName) {
        Project newProject = new Project(projectName);
        projects.add(newProject);

        // Create project button with fixed size
        javax.swing.JButton projectButton = new javax.swing.JButton(projectName);
        projectButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        // Fixed size for all project buttons
        java.awt.Dimension buttonSize = new java.awt.Dimension(270, 35);
        projectButton.setPreferredSize(buttonSize);
        projectButton.setMaximumSize(buttonSize);
        projectButton.setMinimumSize(buttonSize);

        // Handle long project names with ellipsis
        projectButton.setToolTipText(projectName); // Show full name on hover

        // Style the button based on current theme
        if (isDarkMode) {
            projectButton.setBackground(new java.awt.Color(60, 60, 60));
            projectButton.setForeground(java.awt.Color.WHITE);
            projectButton.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(100, 100, 100)));
        } else {
            projectButton.setBackground(java.awt.Color.WHITE);
            projectButton.setForeground(java.awt.Color.BLACK);
            projectButton.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
        }

        // Add margin between buttons
        projectButton.setMargin(new java.awt.Insets(5, 10, 5, 10));

        projectButton.addActionListener(e -> switchToProject(newProject));

        projectListPanel.add(projectButton);

        // Add some spacing between buttons (optional)
        projectListPanel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 5)));

        projectListPanel.revalidate();
        projectListPanel.repaint();

        // Switch to the new project
        switchToProject(newProject);
    }

    
    // Switch to a specific project
    private void switchToProject(Project project) {
        // Save current project state if exists
        if (currentProject != null) {
            saveCurrentProjectState();
        }
        
        // Set new current project
        currentProject = project;
        
        // Clear all tabs
        jTabbedPane1.removeAll();
        
        // Restore project tabs
        for (TabData tabData : project.tabs) {
            javax.swing.JPanel newPanel = createTextAreaWithLineNumbers();
            javax.swing.JTextArea newArea = getTextAreaFromPanel(newPanel);
            
            // Restore content
            String content = project.getTabContent(tabData.title);
            if (content != null) {
                newArea.setText(content);
            }
            
            // Add tab
            jTabbedPane1.addTab(tabData.title, newPanel);
            
            // Initialize undo/redo for the new text area
            initializeUndoRedoForTextArea(newArea);
        }
        
        // If no tabs, create a default one
        if (project.tabs.isEmpty()) {
            createNewFileInProject();
        }
        
        // Update UI
        jTabbedPane1.revalidate();
        jTabbedPane1.repaint();
        
        // Update project label
        updateProjectLabel(project.name);
        //highlightCurrentProjectButton(newProject);

    }
    
    // Save current project state
    private void saveCurrentProjectState() {
        if (currentProject == null) return;
        
        // Clear existing tabs in project
        currentProject.tabs.clear();
        
        // Save all current tabs
        for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
            String title = jTabbedPane1.getTitleAt(i);
            java.awt.Component component = jTabbedPane1.getComponentAt(i);
            javax.swing.JTextArea textArea = getTextAreaFromComponent(component);
            
            if (textArea != null) {
                String content = textArea.getText();
                currentProject.addTab(title, content);
            }
        }
    }
    
    // Create new file in current project
    private void createNewFileInProject() {
        if (currentProject == null) return;
        
        javax.swing.JPanel newPanel = createTextAreaWithLineNumbers();
        javax.swing.JTextArea newArea = getTextAreaFromPanel(newPanel);

        // Unique tab title
        String base = "Untitled";
        int count = 0;
        for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
            String t = jTabbedPane1.getTitleAt(i);
            if (t.equals(base) || t.startsWith(base + " ")) count++;
        }
        String title = (count == 0) ? base : base + " " + (count + 1);

        jTabbedPane1.addTab(title, newPanel);
        jTabbedPane1.setSelectedComponent(newPanel);

        // Initialize undo/redo for the new text area
        initializeUndoRedoForTextArea(newArea);
        
        // Add to current project
        currentProject.addTab(title, "");
    }
    
    // Update project label
    private void updateProjectLabel(String projectName) {
        if (fileNameLabel != null) {
            fileNameLabel.setText("Project: " + projectName);
            fileNameLabel.repaint(); // Force repaint
        }
    }

        private void applyThemeToTextArea(javax.swing.JTextArea textArea, javax.swing.JScrollPane scrollPane) {
        if (isDarkMode) {
            // DARK THEME for text area
            textArea.setBackground(new java.awt.Color(60, 60, 60));
            textArea.setForeground(java.awt.Color.WHITE);
            textArea.setCaretColor(java.awt.Color.WHITE); // Make cursor visible in dark mode
            
            // DARK THEME for scroll pane
            if (scrollPane != null) {
                scrollPane.getViewport().setBackground(new java.awt.Color(60, 60, 60));
            }
            
            // DARK THEME for line numbers
            javax.swing.JTextArea lineNumbers = lineNumberMap.get(textArea);
            if (lineNumbers != null) {
                lineNumbers.setBackground(new java.awt.Color(40, 40, 40));
                lineNumbers.setForeground(java.awt.Color.GRAY);
            }
        } else {
            // LIGHT THEME for text area
            textArea.setBackground(java.awt.Color.WHITE);
            textArea.setForeground(java.awt.Color.BLACK);
            textArea.setCaretColor(java.awt.Color.BLACK); // Reset cursor color
            
            // LIGHT THEME for scroll pane
            if (scrollPane != null) {
                scrollPane.getViewport().setBackground(java.awt.Color.WHITE);
            }
            
            // LIGHT THEME for line numbers
            javax.swing.JTextArea lineNumbers = lineNumberMap.get(textArea);
            if (lineNumbers != null) {
                lineNumbers.setBackground(new java.awt.Color(240, 240, 240));
                lineNumbers.setForeground(java.awt.Color.GRAY);
            }
        }
    }
    
    private void applyThemeToAllTextAreas() {
        // Apply to main text area
        applyThemeToTextArea(jTextArea1, jScrollPane1);

        // Apply to output area
        jTextArea2.setBackground(isDarkMode ? new java.awt.Color(60, 60, 60) : java.awt.Color.WHITE);
        jTextArea2.setForeground(isDarkMode ? java.awt.Color.WHITE : java.awt.Color.BLACK);
        jScrollPane2.getViewport().setBackground(isDarkMode ? new java.awt.Color(60, 60, 60) : java.awt.Color.WHITE);

        // Apply to all text areas in tabs
        for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
            java.awt.Component component = jTabbedPane1.getComponentAt(i);
            javax.swing.JTextArea textArea = getTextAreaFromComponent(component);
            if (textArea != null) {
                javax.swing.JScrollPane scrollPane = getScrollPaneFromComponent(component);
                applyThemeToTextArea(textArea, scrollPane);
            }
        }

        // Apply theme to project buttons and Create New Project button
        if (projectListPanel != null) {
            for (java.awt.Component comp : projectListPanel.getComponents()) {
                if (comp instanceof javax.swing.JButton) {
                    javax.swing.JButton button = (javax.swing.JButton) comp;
                    if (isDarkMode) {
                        button.setBackground(new java.awt.Color(60, 60, 60));
                        button.setForeground(java.awt.Color.WHITE);
                        button.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(100, 100, 100)));
                    } else {
                        button.setBackground(java.awt.Color.WHITE);
                        button.setForeground(java.awt.Color.BLACK);
                        button.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
                    }
                }
            }
        }

        // Apply theme to Create New Project button
        if (jButton2 != null) {
            if (isDarkMode) {
                jButton2.setBackground(new java.awt.Color(80, 80, 80));
                jButton2.setForeground(java.awt.Color.WHITE);
                jButton2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(100, 100, 100)));
            } else {
                jButton2.setBackground(new java.awt.Color(240, 240, 240));
                jButton2.setForeground(java.awt.Color.BLACK);
                jButton2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200)));
            }
        }
    }

    private javax.swing.JTextArea getTextAreaFromComponent(java.awt.Component component) {
        if (component instanceof javax.swing.JScrollPane) {
            javax.swing.JScrollPane sp = (javax.swing.JScrollPane) component;
            java.awt.Component view = sp.getViewport().getView();
            if (view instanceof javax.swing.JTextArea) {
                return (javax.swing.JTextArea) view;
            }
        } else if (component instanceof javax.swing.JPanel) {
            javax.swing.JPanel panel = (javax.swing.JPanel) component;
            for (java.awt.Component comp : panel.getComponents()) {
                if (comp instanceof javax.swing.JScrollPane) {
                    javax.swing.JScrollPane sp = (javax.swing.JScrollPane) comp;
                    java.awt.Component view = sp.getViewport().getView();
                    if (view instanceof javax.swing.JTextArea) {
                        return (javax.swing.JTextArea) view;
                    }
                }
            }
        }
        return null;
    }

    
    private javax.swing.JScrollPane getScrollPaneFromComponent(java.awt.Component component) {
        if (component instanceof javax.swing.JScrollPane) {
            return (javax.swing.JScrollPane) component;
        } else if (component instanceof javax.swing.JPanel) {
            javax.swing.JPanel panel = (javax.swing.JPanel) component;
            for (java.awt.Component comp : panel.getComponents()) {
                if (comp instanceof javax.swing.JScrollPane) {
                    return (javax.swing.JScrollPane) comp;
                }
            }
        }
        return null;
    }



    private void addLineNumbersToTextArea(javax.swing.JTextArea textArea, javax.swing.JScrollPane scrollPane) {
        // Create line number area
        javax.swing.JTextArea lineNumbers = new javax.swing.JTextArea();
        lineNumbers.setEditable(false);
        lineNumbers.setBackground(isDarkMode ? new java.awt.Color(40, 40, 40) : new java.awt.Color(240, 240, 240));
        lineNumbers.setForeground(java.awt.Color.GRAY);
        lineNumbers.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, textArea.getFont().getSize()));
        lineNumbers.setFocusable(false);
        
        // Set fixed width for line numbers
        lineNumbers.setColumns(4);
        lineNumbers.setMargin(new java.awt.Insets(0, 5, 0, 5));
        
        // Store the mapping
        lineNumberMap.put(textArea, lineNumbers);
        
        // Set line numbers as row header
        scrollPane.setRowHeaderView(lineNumbers);
        
        // Add document listener to update line numbers
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers(textArea);
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers(textArea);
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateLineNumbers(textArea);
            }
        });
        
        // Add caret listener to update line numbers on cursor movement
        textArea.addCaretListener(e -> updateLineNumbers(textArea));
        
        // Initial line number update
        updateLineNumbers(textArea);
    }


    // Create a new text area with line numbers
    public javax.swing.JPanel createTextAreaWithLineNumbers() {
        javax.swing.JTextArea textArea = new javax.swing.JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setColumns(20);
        textArea.setRows(5);
        
        // Apply current theme to the new text area
        if (isDarkMode) {
            textArea.setBackground(new java.awt.Color(60, 60, 60));
            textArea.setForeground(java.awt.Color.WHITE);
            textArea.setCaretColor(java.awt.Color.WHITE);
        }
        
        // Create scroll pane for text area
        javax.swing.JScrollPane textScrollPane = new javax.swing.JScrollPane(textArea);
        
        // Apply theme to scroll pane
        if (isDarkMode) {
            textScrollPane.getViewport().setBackground(new java.awt.Color(60, 60, 60));
        }
        
        // Add line numbers
        addLineNumbersToTextArea(textArea, textScrollPane);
        
        // Create panel to hold everything
        javax.swing.JPanel panel = new javax.swing.JPanel();
        panel.setLayout(new java.awt.BorderLayout());
        panel.add(textScrollPane, java.awt.BorderLayout.CENTER);
        
        return panel;
    }


    
    // Update line numbers based on text area content
    private void updateLineNumbers(javax.swing.JTextArea textArea) {
        javax.swing.JTextArea lineNumbers = lineNumberMap.get(textArea);
        if (lineNumbers == null) return;
        
        try {
            // Calculate number of lines
            int totalLines = textArea.getLineCount();
            if (totalLines == 0) totalLines = 1;
            
            // Build line numbers string
            StringBuilder numbers = new StringBuilder();
            for (int i = 1; i <= totalLines; i++) {
                numbers.append(i).append("\n");
            }
            
            // Set the line numbers text
            lineNumbers.setText(numbers.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Helper method to extract text area from panel
    public javax.swing.JTextArea getTextAreaFromPanel(javax.swing.JPanel panel) {
        for (java.awt.Component comp : panel.getComponents()) {
            if (comp instanceof javax.swing.JScrollPane) {
                javax.swing.JScrollPane sp = (javax.swing.JScrollPane) comp;
                java.awt.Component view = sp.getViewport().getView();
                if (view instanceof javax.swing.JTextArea) {
                    return (javax.swing.JTextArea) view;
                }
            }
        }
        return null;
    }



    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        fileNameLabel = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jToggleButton1 = new javax.swing.JToggleButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem16 = new javax.swing.JMenuItem();
        jMenuItem15 = new javax.swing.JMenuItem();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenuItem14 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem13 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(102, 102, 102));
        setMinimumSize(new java.awt.Dimension(1280, 720));
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(204, 204, 204));
        jPanel1.setMaximumSize(new java.awt.Dimension(1280, 720));
        jPanel1.setMinimumSize(new java.awt.Dimension(1280, 720));
        jPanel1.setPreferredSize(new java.awt.Dimension(1280, 720));

        fileNameLabel.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        fileNameLabel.setText("Projects");

        jButton2.setText("Create New Project");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(fileNameLabel))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(82, 82, 82)
                        .addComponent(jButton2)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileNameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 390, Short.MAX_VALUE)
                .addComponent(jButton2)
                .addContainerGap())
        );

        jButton1.setBackground(new java.awt.Color(102, 255, 204));
        jButton1.setFont(new java.awt.Font("Broadway", 1, 14)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/settings-gears.png"))); // NOI18N
        jButton1.setText("RUN");
        jButton1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jButton3.setBackground(new java.awt.Color(255, 51, 0));
        jButton3.setFont(new java.awt.Font("Broadway", 1, 14)); // NOI18N
        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/delete-file.png"))); // NOI18N
        jButton3.setText("CLOSE FILE");
        jButton3.setMaximumSize(new java.awt.Dimension(73, 36));
        jButton3.setMinimumSize(new java.awt.Dimension(73, 36));
        jButton3.setPreferredSize(new java.awt.Dimension(73, 36));
        jButton3.addActionListener(this::jButton3ActionPerformed);

        jButton4.setBackground(new java.awt.Color(255, 0, 0));
        jButton4.setFont(new java.awt.Font("Broadway", 1, 14)); // NOI18N
        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/delete-project.png"))); // NOI18N
        jButton4.setText("CLOSE PROJECT");
        jButton4.addActionListener(this::jButton4ActionPerformed);

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane2.setViewportView(jTextArea2);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel1.setText("OUTPUT:");

        jToggleButton1.setText("Dark Mode");
        jToggleButton1.addActionListener(this::jToggleButton1ActionPerformed);

        jPanel3.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N

        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 946, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("jajja", jPanel3);

        jPanel4.setMaximumSize(new java.awt.Dimension(304, 227));
        jPanel4.setMinimumSize(new java.awt.Dimension(304, 227));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/KRAJ1.png"))); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 292, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 277, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jButton5.setFont(new java.awt.Font("Broadway", 0, 12)); // NOI18N
        jButton5.setText("CLEAR");
        jButton5.addActionListener(this::jButton5ActionPerformed);

        jButton6.setFont(new java.awt.Font("Broadway", 0, 12)); // NOI18N
        jButton6.setText("COPY");
        jButton6.addActionListener(this::jButton6ActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(jTabbedPane1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jToggleButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jToggleButton1))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jButton1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );

        jMenuBar1.setBackground(new java.awt.Color(102, 102, 102));

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/new-project.png"))); // NOI18N
        jMenuItem1.setText("New Project");
        jMenuItem1.addActionListener(this::jMenuItem1ActionPerformed);
        jMenu1.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/new file.png"))); // NOI18N
        jMenuItem2.setText("New File");
        jMenuItem2.addActionListener(this::jMenuItem2ActionPerformed);
        jMenu1.add(jMenuItem2);

        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/database.png"))); // NOI18N
        jMenuItem3.setText("Open Project");
        jMenuItem3.addActionListener(this::jMenuItem3ActionPerformed);
        jMenu1.add(jMenuItem3);

        jMenuItem16.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem16.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/open_file.png"))); // NOI18N
        jMenuItem16.setText("Open File");
        jMenuItem16.addActionListener(this::jMenuItem16ActionPerformed);
        jMenu1.add(jMenuItem16);

        jMenuItem15.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/save.png"))); // NOI18N
        jMenuItem15.setText("Save Project");
        jMenuItem15.addActionListener(this::jMenuItem15ActionPerformed);
        jMenu1.add(jMenuItem15);

        jMenuItem9.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/save_file.png"))); // NOI18N
        jMenuItem9.setText("Save File");
        jMenuItem9.addActionListener(this::jMenuItem9ActionPerformed);
        jMenu1.add(jMenuItem9);

        jMenuItem10.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/exit.png"))); // NOI18N
        jMenuItem10.setText("Exit");
        jMenuItem10.addActionListener(this::jMenuItem10ActionPerformed);
        jMenu1.add(jMenuItem10);

        jMenuItem14.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_DOWN_MASK));
        jMenuItem14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/save2.png"))); // NOI18N
        jMenuItem14.setText("Quick Save");
        jMenuItem14.addActionListener(this::jMenuItem14ActionPerformed);
        jMenu1.add(jMenuItem14);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/undo-circular-arrow.png"))); // NOI18N
        jMenuItem4.setText("Undo");
        jMenuItem4.addActionListener(this::jMenuItem4ActionPerformed);
        jMenu2.add(jMenuItem4);

        jMenuItem5.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/redo-arrow-symbol.png"))); // NOI18N
        jMenuItem5.setText("Redo");
        jMenuItem5.addActionListener(this::jMenuItem5ActionPerformed);
        jMenu2.add(jMenuItem5);

        jMenuItem6.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/scissors.png"))); // NOI18N
        jMenuItem6.setText("Cut");
        jMenuItem6.addActionListener(this::jMenuItem6ActionPerformed);
        jMenu2.add(jMenuItem6);

        jMenuItem7.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/copy.png"))); // NOI18N
        jMenuItem7.setText("Copy");
        jMenuItem7.addActionListener(this::jMenuItem7ActionPerformed);
        jMenu2.add(jMenuItem7);

        jMenuItem8.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/paste.png"))); // NOI18N
        jMenuItem8.setText("Paste");
        jMenuItem8.addActionListener(this::jMenuItem8ActionPerformed);
        jMenu2.add(jMenuItem8);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Help");

        jMenuItem11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/instructions.png"))); // NOI18N
        jMenuItem11.setText("Instructions");
        jMenuItem11.addActionListener(this::jMenuItem11ActionPerformed);
        jMenu3.add(jMenuItem11);

        jMenuItem12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/sample.png"))); // NOI18N
        jMenuItem12.setText("Sample Codes");
        jMenuItem12.addActionListener(this::jMenuItem12ActionPerformed);
        jMenu3.add(jMenuItem12);

        jMenuBar1.add(jMenu3);

        jMenu4.setText("About");
        jMenu4.addActionListener(this::jMenu4ActionPerformed);

        jMenuItem13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/question.png"))); // NOI18N
        jMenuItem13.setText("About IDE");
        jMenuItem13.addActionListener(this::jMenuItem13ActionPerformed);
        jMenu4.add(jMenuItem13);

        jMenuBar1.add(jMenu4);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents
    
   
    
  /* private void saveToFile() {
        if (currentProject == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No project selected.");
            return;
        }

        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Save File");

        // Get current tab index and title
        int currentTabIndex = jTabbedPane1.getSelectedIndex();
        String currentTabTitle = (currentTabIndex >= 0) ? 
            jTabbedPane1.getTitleAt(currentTabIndex) : "Untitled";

        // Set default filename based on current tab title
        if (!currentTabTitle.equals("Untitled") && 
            !currentTabTitle.startsWith("Untitled ")) {
            chooser.setSelectedFile(new java.io.File(currentTabTitle));
        }

        int result = chooser.showSaveDialog(this);

        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();

            // Ensure .txt extension
            String fileName = file.getName();
            if (!fileName.toLowerCase().endsWith(".txt")) {
                file = new java.io.File(file.getAbsolutePath() + ".txt");
                fileName = file.getName();
            }

            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                javax.swing.JTextArea currentArea = getCurrentTextArea();
                if (currentArea != null) {
                    writer.write(currentArea.getText());

                    // Update tab title to saved file name (without path)
                    if (currentTabIndex >= 0) {
                        // Extract just the filename without path
                        String savedFileName = file.getName();
                        jTabbedPane1.setTitleAt(currentTabIndex, savedFileName);

                        // Update project data with new title
                        String oldTitle = currentTabTitle;
                        String newTitle = savedFileName;

                        // If the title changed, update project references
                        if (!oldTitle.equals(newTitle)) {
                            // Get the content
                            String content = currentArea.getText();

                            // Remove old entry and add new one
                            currentProject.fileContents.remove(oldTitle);
                            currentProject.fileContents.put(newTitle, content);

                            // Update tab title in project's tabs list
                            for (TabData tab : currentProject.tabs) {
                                if (tab.title.equals(oldTitle)) {
                                    tab.title = newTitle;
                                    break;
                                }
                            }
                        } else {
                            // Just update content if title didn't change
                            currentProject.saveTabContent(newTitle, currentArea.getText());
                        }
                    }

                    javax.swing.JOptionPane.showMessageDialog(
                        this, 
                        "File saved as: " + fileName,
                        "Save Successful",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                    );
                }
            } catch (java.io.IOException e) {
                javax.swing.JOptionPane.showMessageDialog(
                    this, 
                    "Error saving file: " + e.getMessage(),
                    "Save Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }*/


    
    private javax.swing.JTextArea getCurrentTextArea() {
        java.awt.Component selectedComponent = jTabbedPane1.getSelectedComponent();

        if (selectedComponent == null) {
            return null;
        }

        // If the selected component is a JScrollPane, get the text area directly
        if (selectedComponent instanceof javax.swing.JScrollPane) {
            javax.swing.JScrollPane sp = (javax.swing.JScrollPane) selectedComponent;
            return (javax.swing.JTextArea) sp.getViewport().getView();
        }
        // If the selected component is a JPanel, look for JScrollPane inside it
        else if (selectedComponent instanceof javax.swing.JPanel) {
            javax.swing.JPanel panel = (javax.swing.JPanel) selectedComponent;
            for (java.awt.Component comp : panel.getComponents()) {
                if (comp instanceof javax.swing.JScrollPane) {
                    javax.swing.JScrollPane sp = (javax.swing.JScrollPane) comp;
                    java.awt.Component view = sp.getViewport().getView();
                    if (view instanceof javax.swing.JTextArea) {
                        return (javax.swing.JTextArea) view;
                    }
                }
            }
        }

        return null;
    }

    private void highlightErrorLine(int lineNumber) {
        javax.swing.JTextArea currentArea = getCurrentTextArea();
        if (currentArea != null) {
            try {
                int start = currentArea.getLineStartOffset(lineNumber - 1);
                int end = currentArea.getLineEndOffset(lineNumber - 1);

                // Highlight the error line
                currentArea.setCaretPosition(start);
                currentArea.moveCaretPosition(end);
                currentArea.grabFocus();

                // Scroll to the error line
                currentArea.setCaretPosition(start);

            } catch (Exception e) {
                // Line number might be out of range
                System.err.println("Could not highlight line " + lineNumber + ": " + e.getMessage());
            }
        }
    }

    public void initializeUndoRedoForTextArea(javax.swing.JTextArea textArea) {
        undoStacks.put(textArea, new java.util.ArrayDeque<>());
        redoStacks.put(textArea, new java.util.ArrayDeque<>());
        isUndoRedoInProgress.put(textArea, false);

        // Push initial state
        undoStacks.get(textArea).push(textArea.getText());

        // Add document listener for undo/redo AND syntax highlighting
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (!isUndoRedoInProgress.get(textArea)) {
                    recordState(textArea);
                }
                // Apply syntax highlighting
                SwingUtilities.invokeLater(() -> {
                    applySyntaxHighlighting();
                });
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (!isUndoRedoInProgress.get(textArea)) {
                    recordState(textArea);
                }
                // Apply syntax highlighting
                SwingUtilities.invokeLater(() -> {
                    applySyntaxHighlighting();
                });
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                // Style change, apply syntax highlighting
                SwingUtilities.invokeLater(() -> {
                    applySyntaxHighlighting();
                });
            }
        });
    }


    
    // Record current state for undo
    private void recordState(javax.swing.JTextArea textArea) {
        String currentText = textArea.getText();
        java.util.Deque<String> undoStack = undoStacks.get(textArea);

        if (undoStack.isEmpty() || !currentText.equals(undoStack.peek())) {
            undoStack.push(currentText);
            // Limit stack size to prevent memory issues
            if (undoStack.size() > 100) {
                // Remove oldest entry
                java.util.ArrayDeque<String> temp = new java.util.ArrayDeque<>();
                while (undoStack.size() > 99) { // Keep 99 + current = 100
                    temp.addLast(undoStack.removeLast());
                }
            }
            // Clear redo stack when new changes are made
            redoStacks.get(textArea).clear();
        }
    }


    
   private void undoAction() {
        javax.swing.JTextArea area = getCurrentTextArea();
        if (area == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No text area selected.");
            return;
        }

        java.util.Deque<String> undoStack = undoStacks.get(area);
        java.util.Deque<String> redoStack = redoStacks.get(area);

        if (undoStack != null && undoStack.size() > 1) {
            isUndoRedoInProgress.put(area, true);
            
            // Push current state to redo stack
            redoStack.push(area.getText());
            
            // Pop previous state from undo stack and set it
            undoStack.pop(); // Remove current state
            String previousState = undoStack.peek(); // Get previous state without removing it
            area.setText(previousState != null ? previousState : "");
            
            isUndoRedoInProgress.put(area, false);
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Nothing to undo.");
        }
    }

    private void redoAction() {
        javax.swing.JTextArea area = getCurrentTextArea();
        if (area == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No text area selected.");
            return;
        }

        java.util.Deque<String> undoStack = undoStacks.get(area);
        java.util.Deque<String> redoStack = redoStacks.get(area);

        if (redoStack != null && !redoStack.isEmpty()) {
            isUndoRedoInProgress.put(area, true);
            
            // Push current state to undo stack
            undoStack.push(area.getText());
            
            // Pop next state from redo stack and set it
            String nextState = redoStack.pop();
            area.setText(nextState);
            
            isUndoRedoInProgress.put(area, false);
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Nothing to redo.");
        }
    }

    public FinalsGUI(java.io.File file) {
        /*initComponents();
        // Initialize undo/redo for the default text area
        initializeUndoRedoForTextArea(jTextArea1);

        fileNameLabel = new javax.swing.JLabel("No file opened");
        fileNameLabel.setForeground(java.awt.Color.BLACK);
        fileNameLabel.setFont(new java.awt.Font("Tahoma", 1, 16));

        jPanel2.add(fileNameLabel);

        // Add line numbers to the default text area
        addLineNumbersToTextArea(jTextArea1, jScrollPane1);

        // Open the file
        openFile(file);*/
        
          // public FinalsGUI() {
       initComponents();
       initializeProjectSystem();

       // Initialize undo/redo for the default text area
       initializeUndoRedoForTextArea(jTextArea1);

       addLineNumbersToTextArea(jTextArea1, jScrollPane1);
       openFile(file);
      }
    //}

// Method to open a file (you can move your existing open logic here)
    private void openFile(java.io.File file) {
        try {
            String content = java.nio.file.Files.readString(file.toPath());

            // Use the existing text area for the first file
            jTextArea1.setText(content);
            jTabbedPane1.setTitleAt(0, file.getName());

            // Update fileNameLabel if you have one
            if (fileNameLabel != null) {
                fileNameLabel.setText(file.getName());
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Failed to open file: " + e.getMessage());
        }
    }


   /* private void attachUndoListener(javax.swing.JTextArea area) {
        final java.util.Deque<String> localUndoStack = new java.util.ArrayDeque<>();
            final java.util.Deque<String> localRedoStack = new java.util.ArrayDeque<>();

            area.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private boolean isUndoOrRedo = false; // prevent recursion

                private void record() {
                    if (!isUndoOrRedo) {
                        localUndoStack.push(area.getText());
                        localRedoStack.clear();
                    }
                }

                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) { record(); }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) { record(); }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) { record(); }

                public void undo() {
                    if (!localUndoStack.isEmpty()) {
                        isUndoOrRedo = true;
                        localRedoStack.push(area.getText());
                        String prev = localUndoStack.pop();
                        area.setText(prev);
                        isUndoOrRedo = false;
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(null, "Nothing to undo.");
                    }
                }

                public void redo() {
                    if (!localRedoStack.isEmpty()) {
                        isUndoOrRedo = true;
                        localUndoStack.push(area.getText());
                        String next = localRedoStack.pop();
                        area.setText(next);
                        isUndoOrRedo = false;
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(null, "Nothing to redo.");
                    }
                }
            });

    }*/



    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        String projectName = javax.swing.JOptionPane.showInputDialog(this, "Enter project name:", "New Project", javax.swing.JOptionPane.QUESTION_MESSAGE);
        if (projectName != null && !projectName.trim().isEmpty()) {
            createNewProject(projectName);
            javax.swing.JOptionPane.showMessageDialog(this, "New project created: " + projectName);
        }
// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        if (currentProject != null) {
                    createNewFileInProject();
                    javax.swing.JOptionPane.showMessageDialog(this, "New file created in current project.");
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, "No project selected. Please create a project first.");
                }
// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        saveFileAs(); 
 // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
            openProjectFromFolder();
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
            System.exit(0);// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        javax.swing.JTextArea current = getCurrentTextArea();
            if (current != null) {
                undoAction();
            }// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        javax.swing.JTextArea current = getCurrentTextArea();
             if (current != null) {
                  redoAction();
             }// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        javax.swing.JTextArea current = getCurrentTextArea();
            if (current != null) {
                current.cut();
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "No text area selected.");
            }// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        javax.swing.JTextArea current = getCurrentTextArea();
            if (current != null) {
                current.copy();
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "No text area selected.");
            }// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        javax.swing.JTextArea current = getCurrentTextArea();
            if (current != null) {
                current.paste();
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "No text area selected.");
            }// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
        isDarkMode = jToggleButton1.isSelected();
        
        if (isDarkMode) {
            // DARK THEME
            jToggleButton1.setText("Light Mode");

            // Panels
            jPanel1.setBackground(new java.awt.Color(40, 40, 40));
            jPanel2.setBackground(new java.awt.Color(30, 30, 30));

            // Button
            jButton1.setBackground(new java.awt.Color(102, 255, 204));
            jButton1.setForeground(java.awt.Color.BLACK);

            // Label
            jLabel1.setForeground(java.awt.Color.WHITE);
            if (fileNameLabel != null) {
                fileNameLabel.setForeground(java.awt.Color.BLACK);
            }

            // Apply theme to all text areas and project buttons
            applyThemeToAllTextAreas();

        } else {
            // LIGHT THEME
            jToggleButton1.setText("Dark Mode");

            jPanel1.setBackground(new java.awt.Color(204, 204, 204));
            jPanel2.setBackground(new java.awt.Color(240, 240, 240));

            jButton1.setBackground(new java.awt.Color(102, 255, 204));
            jButton1.setForeground(java.awt.Color.BLACK);

            jLabel1.setForeground(java.awt.Color.BLACK);
            if (fileNameLabel != null) {
                fileNameLabel.setForeground(java.awt.Color.BLACK);
            }

            // Apply theme to all text areas and project buttons
            applyThemeToAllTextAreas();
        }// TODO add your handling code here:
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        String projectName = javax.swing.JOptionPane.showInputDialog(this, "Enter project name:", "New Project", javax.swing.JOptionPane.QUESTION_MESSAGE);
            if (projectName != null && !projectName.trim().isEmpty()) {
                createNewProject(projectName);
                javax.swing.JOptionPane.showMessageDialog(this, "New project created: " + projectName);
        }// TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
        instructionsFrame about = new instructionsFrame();
        about.setVisible(true);        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem11ActionPerformed

    private void jMenu4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu4ActionPerformed

    }//GEN-LAST:event_jMenu4ActionPerformed

    private void jMenuItem13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem13ActionPerformed
        AboutFrame about = new AboutFrame();
        about.setVisible(true);// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem13ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        int selectedIndex = jTabbedPane1.getSelectedIndex();
    
        if (selectedIndex == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "No file is currently open.");
            return;
        }

        String tabTitle = jTabbedPane1.getTitleAt(selectedIndex);

        int confirm = javax.swing.JOptionPane.showConfirmDialog(
            this, 
            "Are you sure you want to close the file: '" + tabTitle + "'?",
            "Close File",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );

        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            // Remove from current project if exists
            if (currentProject != null) {
                currentProject.removeTab(tabTitle);
            }

            // Remove the tab from tabbed pane
            jTabbedPane1.remove(selectedIndex);

            // If no tabs left, create a new one
            if (jTabbedPane1.getTabCount() == 0 && currentProject != null) {
                createNewFileInProject();
            }

            // Update UI
            jTabbedPane1.revalidate();
            jTabbedPane1.repaint();
        }// TODO add your handling code here:
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
           javax.swing.JTextArea currentArea = getCurrentTextArea();
           if (currentArea == null) {
                javax.swing.JOptionPane.showMessageDialog(this, "No file is currently open.");
                return;
            }

            String code = currentArea.getText();

            if (code == null || code.trim().isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this, "The file is empty. Please write some KRAJ code first.");
                return;
            }

            // Clear output area
            jTextArea2.setText("");
            jTextArea2.append("Running KRAJ code...\n");
            jTextArea2.append("=".repeat(50) + "\n");

            // Run the code in a separate thread to avoid freezing the GUI
            new Thread(() -> {
                runKRAJCode(code);
            }).start();
 // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        if (currentProject == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No project is currently open.");
            return;
        }

        // Check for unsaved changes
        boolean hasUnsavedChanges = false;
        for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
            String tabTitle = jTabbedPane1.getTitleAt(i);
            java.awt.Component component = jTabbedPane1.getComponentAt(i);
            javax.swing.JTextArea textArea = getTextAreaFromComponent(component);

            if (textArea != null) {
                String savedContent = currentProject.getTabContent(tabTitle);
                String currentContent = textArea.getText();

                if (savedContent == null || !savedContent.equals(currentContent)) {
                    hasUnsavedChanges = true;
                    break;
                }
            }
        }

        // If there are unsaved changes, prompt the user
        if (hasUnsavedChanges) {
            int choice = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "Project '" + currentProject.name + "' has unsaved changes. Save before deleting?",
                "Unsaved Changes",
                javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE
            );

            if (choice == javax.swing.JOptionPane.YES_OPTION) {
                saveProject();
            } else if (choice == javax.swing.JOptionPane.CANCEL_OPTION) {
                return; // Don't delete
            }
            // If NO_OPTION, continue without saving
        }

        // Confirm deletion
        int confirm = javax.swing.JOptionPane.showConfirmDialog(
            this, 
            "Are you sure you want to DELETE the project: '" + currentProject.name + "'?\nThis action cannot be undone!",
            "Delete Project",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE
        );

        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            // Remove project button from panel
            removeProjectButton(currentProject);

            // Remove project from list
            projects.remove(currentProject);

            // Clear tabs
            jTabbedPane1.removeAll();

            // Reset current project
            Project deletedProject = currentProject;
            currentProject = null;

            // If there are other projects, switch to the first one
            if (!projects.isEmpty()) {
                switchToProject(projects.get(0));
            } else {
                // Create a default project if no projects left
                createNewProject("Default Project");
            }

            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Project deleted: " + deletedProject.name,
                "Project Deleted",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        }// TODO add your handling code here:
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jMenuItem16ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem16ActionPerformed
        openFileIntoProject();// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem16ActionPerformed

    private void jMenuItem15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem15ActionPerformed
        //debugSaveProject();
        saveProject();// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem15ActionPerformed

    private void jMenuItem14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem14ActionPerformed
        quickSave();// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem14ActionPerformed

    private void jMenuItem12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem12ActionPerformed
        openSampleProject();// TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem12ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        jTextArea2.setText("");
        jTextArea2.append("Output cleared. Ready to run code.\n");
        if (isDarkMode) {
            jTextArea2.setForeground(java.awt.Color.WHITE);
        } else {
            jTextArea2.setForeground(java.awt.Color.BLACK);
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        String output = jTextArea2.getText();
        if (output != null && !output.trim().isEmpty()) {
            java.awt.datatransfer.StringSelection selection = 
                new java.awt.datatransfer.StringSelection(output);
            java.awt.datatransfer.Clipboard clipboard = 
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            javax.swing.JOptionPane.showMessageDialog(this, "Output copied to clipboard!");
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "No output to copy.");
        }// TODO add your handling code here:
    }//GEN-LAST:event_jButton6ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new FinalsGUI().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel fileNameLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem12;
    private javax.swing.JMenuItem jMenuItem13;
    private javax.swing.JMenuItem jMenuItem14;
    private javax.swing.JMenuItem jMenuItem15;
    private javax.swing.JMenuItem jMenuItem16;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JToggleButton jToggleButton1;
    // End of variables declaration//GEN-END:variables
}

